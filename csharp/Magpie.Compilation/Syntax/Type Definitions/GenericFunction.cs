using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class GenericFunction : Generic<Function>, IGenericCallable
    {
        public GenericFunction(Function function, IEnumerable<string> typeParameters)
            : base(function, typeParameters)
        {
        }

        public ICallable Instantiate(Compiler compiler, IEnumerable<IBoundDecl> typeArgs,
            IBoundDecl argType)
        {
            bool canInfer;
            var context = BuildContext(compiler,
                BaseType.Type.Parameter.Unbound,
                argType, ref typeArgs, out canInfer);

            // bail if we couldn't get the right number of type arguments
            if ((typeArgs == null) || (TypeParameters.Count != typeArgs.Count())) return null;

            // create a new bound function type with the type arguments applied
            FuncType funcType = BaseType.Type.Clone();
            TypeBinder.Bind(context, funcType);

            // create a new unbound function with the proper type
            Function instance = new Function(BaseType.Position, BaseType.Name,
                funcType, BaseType.ParamNames, BaseType.Body.Unbound, typeArgs, canInfer);

            instance.BindSearchSpace(BaseType.SearchSpace);

            // bind it with the type arguments in context
            FunctionBinder.Bind(context, instance);

            // don't instantiate it multiple times
            compiler.Functions.Add(instance);

            return instance;
        }
    }
}
