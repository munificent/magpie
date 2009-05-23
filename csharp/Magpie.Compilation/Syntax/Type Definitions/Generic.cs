using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class Generic<TType> where TType : TypeDefinition
    {
        public string Name { get { return BaseType.Name; } }

        public IList<string> TypeParameters { get; private set; }
        public TType BaseType { get; private set; }

        public Generic(TType baseType, IEnumerable<string> typeParameters)
        {
            BaseType = baseType;
            TypeParameters = new List<string>(typeParameters);
        }

        public BindingContext BuildContext(Compiler compiler,
            IEnumerable<IUnboundDecl> parameterTypes, IEnumerable<IBoundDecl> argTypes,
            ref IEnumerable<IBoundDecl> typeArgs, out bool canInferArgs)
        {
            // try to infer the args if not passed in
            IList<IBoundDecl> inferredTypeArgs = TypeArgInferrer.Infer(TypeParameters,
                parameterTypes.ToArray(), argTypes);

            canInferArgs = inferredTypeArgs != null;

            if ((typeArgs == null) || typeArgs.IsEmpty())
            {
                typeArgs = inferredTypeArgs;
            }

            // build the argument dictionary
            var argDictionary = new Dictionary<string, IBoundDecl>();

            foreach (var pair in TypeParameters.Zip(typeArgs))
            {
                argDictionary[pair.Item1] = pair.Item2;
            }

            return new BindingContext(compiler, BaseType.NameContext, argDictionary);
        }
    }
}
