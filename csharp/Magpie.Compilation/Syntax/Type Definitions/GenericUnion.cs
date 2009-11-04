using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class GenericUnion : Generic<Union>
    {
        public IUnboundDecl Type
        {
            get
            {
                return new NamedType(BaseType.Name,
                    TypeParameters.Select(p => (IUnboundDecl)(new NamedType(p))));
            }
        }

        public GenericUnion(Union union, IEnumerable<string> typeParameters)
            : base(union, typeParameters)
        {
        }

        //### bob: this is essentially a copy of the one in Union :(
        public IEnumerable<IGenericCallable> BuildFunctions()
        {
            // constructors for each case
            foreach (var unionCase in BaseType.Cases)
            {
                yield return new GenericUnionConstructor(this, unionCase.Index);
                yield return new GenericUnionCaseChecker(this, unionCase.Index);

                if (unionCase.ValueType.Bound != Decl.Unit)
                {
                    yield return new GenericUnionValueGetter(this, unionCase.Index);
                }
            }
        }

        //### bob: this is basically a copy from Struct :(
        public Union Instantiate(Compiler compiler, IEnumerable<IBoundDecl> typeArgs)
        {
            // look for a previously instantiated one
            var union = compiler.Types.FindUnion(BaseType.Name, typeArgs);

            if (union == null)
            {
                // instantiate the structure
                union = BaseType.Clone(typeArgs);

                // add it to the list of known types. this must happen before
                // the subsequent binding in case the type is recursive.
                compiler.Types.Add(union, typeArgs);

                // immediately bind it with the type arguments
                BindingContext context = new BindingContext(compiler, union.SearchSpace, TypeParameters, typeArgs);
                TypeBinder.Bind(context, union);

                // figure out which of the cases can infer type arguments. this
                // needs to be done now while we still know the type parameters
                // and the unbound uses of them in the cases.
                for (int i = 0; i < union.Cases.Count; i++)
                {
                    var inferredTypeArgs = TypeArgInferrer.Infer(TypeParameters,
                        BaseType.Cases[i].ValueType.Unbound,
                        union.Cases[i].ValueType.Bound);

                    union.Cases[i].HasInferrableTypeArguments = inferredTypeArgs != null;
                }
            }

            return union;
        }
    }

    //### bob: copy/paste of GenericStructFunction
    public abstract class GenericUnionFunction : IGenericCallable
    {
        public GenericUnionFunction(GenericUnion union, int caseIndex)
        {
            Union = union;
            mCaseIndex = caseIndex;
        }

        public abstract string Name { get; }

        public ICallable Instantiate(Compiler compiler, IEnumerable<IBoundDecl> typeArgs,
            IBoundDecl argType)
        {
            bool dummy;
            var context = Union.BuildContext(compiler,
                ParameterType, argType, ref typeArgs, out dummy);

            var union = Union.Instantiate(compiler, typeArgs);

            // now build the auto functions for it
            ICallable instantiated = null;
            foreach (ICallable function in union.BuildFunctions())
            {
                // add to the symbol table so they are only instantiated once
                compiler.Functions.Add(function);

                if ((function.Name == Name) && function.GetType().Equals(FunctionType)) instantiated = function;
            }

            return instantiated;
        }

        protected abstract IUnboundDecl ParameterType { get; }
        protected abstract Type FunctionType { get; }

        protected GenericUnion Union { get; private set; }
        protected UnionCase Case { get { return Union.BaseType.Cases[mCaseIndex]; } }

        private int mCaseIndex;
    }

    public class GenericUnionConstructor : GenericUnionFunction
    {
        public GenericUnionConstructor(GenericUnion union, int caseIndex)
            : base(union, caseIndex) { }

        //### bob: need to qualify
        public override string Name { get { return Case.Name; } }

        protected override IUnboundDecl ParameterType
        {
            get { return Case.ValueType.Unbound; }
        }

        protected override Type FunctionType
        {
            get { return typeof(UnionConstructor); }
        }
    }

    public class GenericUnionCaseChecker : GenericUnionFunction
    {
        public GenericUnionCaseChecker(GenericUnion union, int caseIndex)
            : base(union, caseIndex) { }

        //### bob: need to qualify
        public override string Name { get { return Case.Name + "?"; } }

        protected override IUnboundDecl ParameterType
        {
            get { return Union.Type; }
        }

        protected override Type FunctionType
        {
            get { return typeof(UnionCaseChecker); }
        }
    }

    public class GenericUnionValueGetter : GenericUnionFunction
    {
        public GenericUnionValueGetter(GenericUnion union, int caseIndex)
            : base(union, caseIndex) { }

        //### bob: need to qualify
        public override string Name { get { return Case.Name + "Value"; } }

        protected override IUnboundDecl ParameterType
        {
            get { return Union.Type; }
        }

        protected override Type FunctionType
        {
            get { return typeof(UnionValueGetter); }
        }
    }

}
