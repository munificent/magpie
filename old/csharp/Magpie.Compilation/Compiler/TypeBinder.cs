using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class TypeBinder
    {
        public static IBoundDecl Bind(BindingContext context, IUnboundDecl unbound)
        {
            return unbound.Match<IBoundDecl>(
                // atomics are already bound
                atomic  =>  atomic,
                func    =>  {
                                Bind(context, func);
                                return func;
                            },
                record  =>  {
                                var fields = new Dictionary<string, IBoundDecl>();

                                foreach (var field in record.Fields)
                                {
                                    fields.Add(field.Key, Bind(context, field.Value));
                                }

                                return new BoundRecordType(fields);
                            },
                tuple   =>  new BoundTupleType(tuple.Fields.Select(field => Bind(context, field))),
                named   =>  BindNamed(context, named));
        }

        public static IBoundDecl[] Bind(BindingContext context, IEnumerable<IUnboundDecl> unbounds)
        {
            return unbounds.Select(unbound => Bind(context, unbound)).ToArray();
        }

        public static void Bind(BindingContext context, FuncType func)
        {
            // do nothing if already bound. some functions such as intrinsics or
            // auto-functions are created in bound form.
            if (!func.Parameter.IsBound)
            {
                var binder = MakeBinder(context);
                func.Parameter.Bind(binder);
                func.Return.Bind(binder);
            }
        }

        public static void Bind(BindingContext context, Struct structure)
        {
            // bind the fields
            foreach (var field in structure.Fields)
            {
                field.Type.Bind(MakeBinder(context));
            }
        }

        public static void Bind(Compiler compiler, Struct structure)
        {
            Bind(new BindingContext(compiler, structure.SearchSpace), structure);
        }

        public static void Bind(BindingContext context, Union union)
        {
            // bind the cases
            foreach (var unionCase in union.Cases)
            {
                unionCase.SetUnion(union);
                unionCase.ValueType.Bind(MakeBinder(context));
            }
        }

        public static void Bind(Compiler compiler, Union union)
        {
            Bind(new BindingContext(compiler, union.SearchSpace), union);
        }

        private static Func<IUnboundDecl, IBoundDecl> MakeBinder(BindingContext context)
        {
            return new Func<IUnboundDecl, IBoundDecl>(unbound => Bind(context, unbound));
        }

        private static IBoundDecl BindNamed(BindingContext context, NamedType decl)
        {
            // see if it's a type parameter
            if (context.TypeArguments.ContainsKey(decl.Name))
            {
                //### bob: need a test for this
                // this basically means
                //    Foo'T (->) T'Int
                // is bad. this *could* be made to work and might be useful in some cases
                if (decl.IsGeneric) throw new CompileException("Cannot apply type arguments to a type parameter.");

                return context.TypeArguments[decl.Name];
            }

            // see if it's an array
            if ((decl.Name == "Array") && (decl.TypeArgs.Count() == 1))
            {
                return new BoundArrayType(Bind(context, decl.TypeArgs.First()));
            }

            // bind the type arguments
            var typeArgs = Bind(context, decl.TypeArgs);

            // look up the named type
            return context.Compiler.Types.Find(context.SearchSpace, decl.Position, decl.Name, typeArgs);
        }
    }
}
