using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class Generic<TType> where TType : Definition
    {
        public string Name { get { return BaseType.Name; } }

        public IList<string> TypeParameters { get; private set; }
        public TType BaseType { get; private set; }

        public Generic(TType baseType, IEnumerable<string> typeParameters)
        {
            BaseType = baseType;
            TypeParameters = new List<string>(typeParameters);
        }

        public BindingContext BuildContext(BindingContext callingContext,
            IUnboundDecl parameterType, IBoundDecl argType,
            ref IEnumerable<IBoundDecl> typeArgs, out bool canInferArgs)
        {
            // try to infer the args if not passed in
            IList<IBoundDecl> inferredTypeArgs = TypeArgInferrer.Infer(TypeParameters,
                parameterType, argType);

            canInferArgs = inferredTypeArgs != null;

            if (canInferArgs)
            {
                typeArgs = inferredTypeArgs;
            }

            if (typeArgs.IsEmpty())
            {
                typeArgs = null;
            }

            // include the open namespaces of the calling context. this was the instantiated
            // generic has access to everything that the instantiation call site has access
            // to
            var searchSpace = new NameSearchSpace(BaseType.SearchSpace, callingContext.SearchSpace);

            return new BindingContext(callingContext.Compiler, searchSpace, TypeParameters, typeArgs);
        }
    }
}
