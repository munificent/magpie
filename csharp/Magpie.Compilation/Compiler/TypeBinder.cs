using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class TypeBinder : IUnboundDeclVisitor<IBoundDecl>
    {
        public static IBoundDecl Bind(BindingContext context, IUnboundDecl unbound)
        {
            return unbound.Accept(new TypeBinder(context));
        }

        public static IBoundDecl[] Bind(BindingContext context, IEnumerable<IUnboundDecl> unbounds)
        {
            var bound = new List<IBoundDecl>();

            var binder = new TypeBinder(context);
            foreach (var unbound in unbounds)
            {
                bound.Add(unbound.Accept(binder));
            }

            return bound.ToArray();
        }

        public static void Bind(BindingContext context, FuncType func)
        {
            var binder = new TypeBinder(context);

            binder.Bind(func);
        }

        public static void Bind(BindingContext context, Struct structure)
        {
            var binder = new TypeBinder(context);

            // bind the fields
            foreach (var field in structure.Fields)
            {
                field.Type.Bind(binder);
            }
        }

        public static void Bind(Compiler compiler, Struct structure)
        {
            Bind(new BindingContext(compiler, structure.SearchSpace), structure);
        }

        public static void Bind(BindingContext context, Union union)
        {
            var binder = new TypeBinder(context);

            // bind the cases
            foreach (var unionCase in union.Cases)
            {
                unionCase.SetUnion(union);
                unionCase.ValueType.Bind(binder);
            }
        }

        public static void Bind(Compiler compiler, Union union)
        {
            Bind(new BindingContext(compiler, union.SearchSpace), union);
        }

        private TypeBinder(BindingContext context)
        {
            mContext = context;
        }

        private void Bind(FuncType func)
        {
            // do nothing if already bound. some functions such as intrinsics or
            // auto-functions are created in bound form.
            if (!func.Return.IsBound)
            {
                func.Return.Bind(this);

                foreach (var parameter in func.Parameters)
                {
                    parameter.Type.Bind(this);
                }
            }
        }

        #region IUnboundDeclVisitor<IBoundDecl> Members

        IBoundDecl IUnboundDeclVisitor<IBoundDecl>.Visit(ArrayType decl)
        {
            return new BoundArrayType(decl.ElementType.Accept(this), decl.IsMutable);
        }

        IBoundDecl IUnboundDeclVisitor<IBoundDecl>.Visit(AtomicDecl decl)
        {
            // atomics are already bound
            return decl;
        }

        IBoundDecl IUnboundDeclVisitor<IBoundDecl>.Visit(FuncType decl)
        {
            Bind(decl);
            return decl;
        }

        IBoundDecl IUnboundDeclVisitor<IBoundDecl>.Visit(TupleType decl)
        {
            return new BoundTupleType(decl.Fields.Select(field => field.Accept(this)));
        }

        IBoundDecl IUnboundDeclVisitor<IBoundDecl>.Visit(NamedType decl)
        {
            // see if it's a type parameter
            if (mContext.TypeArguments.ContainsKey(decl.Name))
            {
                //### bob: need a test for this
                // this basically means
                //    Foo'T (->) T'Int
                // is bad. this *could* be made to work and might be useful in some cases
                if (decl.IsGeneric) throw new CompileException("Cannot apply type arguments to a type parameter.");

                return mContext.TypeArguments[decl.Name];
            }

            // bind the type arguments
            var typeArgs = decl.TypeArgs.Accept(this);

            // look up the named type
            return mContext.Compiler.Types.Find(mContext.SearchSpace, decl.Position, decl.Name, typeArgs);
        }

        #endregion

        private BindingContext mContext;
    }
}
