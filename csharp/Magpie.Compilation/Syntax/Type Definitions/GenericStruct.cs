using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class GenericStruct : Generic<Struct>
    {
        public IUnboundDecl Type
        {
            get
            {
                return new NamedType(BaseType.Name,
                    TypeParameters.Select(p => (IUnboundDecl)(new NamedType(p))));
            }
        }

        public GenericStruct(Struct structure, IEnumerable<string> typeParameters)
            : base(structure, typeParameters)
        {
        }

        public IEnumerable<IGenericCallable> BuildFunctions()
        {
            yield return new GenericStructConstructor(this);

            foreach (var field in BaseType.Fields)
            {
                yield return new GenericFieldGetter(this, field.Index);
                yield return new GenericFieldSetter(this, field.Index);
            }
        }

        public Struct Instantiate(Compiler compiler, IEnumerable<IBoundDecl> typeArgs)
        {
            // look for a previously instantiated one
            var structure = compiler.Types.FindStruct(BaseType.Name, typeArgs);

            if (structure == null)
            {
                // instantiate the structure
                structure = BaseType.Clone(typeArgs);

                // add it to the list of known types. this must happen before
                // the subsequent binding in case the type is recursive.
                compiler.Types.Add(structure, typeArgs);

                // immediately bind it with the type arguments
                BindingContext context = new BindingContext(compiler, structure.SearchSpace, TypeParameters, typeArgs);
                TypeBinder.Bind(context, structure);
            }

            return structure;
        }
    }

    public abstract class GenericStructFunction : IGenericCallable
    {
        public GenericStructFunction(GenericStruct structure)
        {
            Struct = structure;
        }
        
        public abstract string Name { get; }

        public ICallable Instantiate(BindingContext context, IEnumerable<IBoundDecl> typeArgs,
            IBoundDecl argType)
        {
            bool dummy;
            Struct.BuildContext(context,
                ParameterType, argType, ref typeArgs, out dummy);

            // bail if we couldn't get the right number of type arguments
            if ((typeArgs == null) || (Struct.TypeParameters.Count != typeArgs.Count())) return null;

            // instantiate the structure
            var structure = Struct.Instantiate(context.Compiler, typeArgs);

            // now build the auto functions for it
            ICallable instantiated = null;
            foreach (ICallable function in structure.BuildFunctions())
            {
                // add to the symbol table so they are only instantiated once
                context.Compiler.Functions.Add(function);

                if ((Name == function.Name) && function.GetType().Equals(FunctionType)) instantiated = function;
            }

            return instantiated;
        }

        protected abstract IUnboundDecl ParameterType { get; }
        protected abstract Type FunctionType { get; }

        protected GenericStruct Struct { get; private set; }
    }

    //### bob: there's a lot of overlap between these and the nongeneric ones :(

    public class GenericStructConstructor : GenericStructFunction
    {
        public GenericStructConstructor(GenericStruct structure)
            : base(structure) { }

        public override string Name { get { return Struct.Name; } }

        protected override IUnboundDecl ParameterType
        {
            get
            {
                if (Struct.BaseType.Fields.Count == 0) return Decl.Unit;
                if (Struct.BaseType.Fields.Count == 1) return Struct.BaseType.Fields[0].Type.Unbound;

                return new TupleType(Struct.BaseType.Fields.Select(field => field.Type.Unbound));
            }
        }

        protected override Type FunctionType
        {
            get { return typeof(StructConstructor); }
        }
    }

    public class GenericFieldGetter : GenericStructFunction
    {
        public GenericFieldGetter(GenericStruct structure, int fieldIndex)
            : base(structure)
        {
            mFieldIndex = fieldIndex;
        }

        public override string Name { get { return Struct.BaseType.Fields[mFieldIndex].Name; } }

        protected override IUnboundDecl ParameterType
        {
            get { return Struct.Type; }
        }

        protected override Type FunctionType
        {
            get { return typeof(FieldGetter); }
        }

        private int mFieldIndex;
    }

    public class GenericFieldSetter : GenericStructFunction
    {
        public GenericFieldSetter(GenericStruct structure, int fieldIndex)
            : base(structure)
        {
            mFieldIndex = fieldIndex;
        }

        public override string Name { get { return Struct.BaseType.Fields[mFieldIndex].Name + "<-"; } }

        protected override IUnboundDecl ParameterType
        {
            get { return new TupleType(new IUnboundDecl[] { Struct.Type, Struct.BaseType.Fields[mFieldIndex].Type.Unbound }); }
        }

        protected override Type FunctionType
        {
            get { return typeof(FieldSetter); }
        }

        private int mFieldIndex;
    }
}
