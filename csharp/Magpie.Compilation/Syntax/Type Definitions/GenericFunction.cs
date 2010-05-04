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

        public ICallable Instantiate(BindingContext context, IEnumerable<IBoundDecl> typeArgs,
            IBoundDecl argType)
        {
            bool canInfer;
            var genericContext = BuildContext(context,
                BaseType.Type.Parameter.Unbound,
                argType, ref typeArgs, out canInfer);

            // bail if we couldn't get the right number of type arguments
            if ((typeArgs == null) || (TypeParameters.Count != typeArgs.Count())) return null;

            // create a new bound function type with the type arguments applied
            FuncType funcType = BaseType.Type.CloneFunc();
            TypeBinder.Bind(genericContext, funcType);

            // make sure the concrete argument types of this instance match what we're actually given
            if (!DeclComparer.TypesMatch(funcType.Parameter.Bound, argType)) return null;

            // create a new unbound function with the proper type
            Function instance = new Function(BaseType.Position, BaseType.BaseName,
                funcType, BaseType.ParamNames, BaseType.Body.Unbound, typeArgs, canInfer);

            instance.BindSearchSpace(BaseType.SearchSpace);

            // don't instantiate it multiple times
            // note that this must happen *before* the function is bound, in case the
            // newly instantiated generic function is recursive.
            context.Compiler.Functions.Add(instance);

            // bind it with the type arguments in context
            FunctionBinder.Bind(genericContext, instance);

            return instance;
        }
    }
}
