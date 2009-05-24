using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface ICallable
    {
        string Name { get; }
        IBoundExpr CreateCall(IBoundExpr arg);
        IBoundDecl[] ParameterTypes { get; }

        bool HasInferrableTypeArguments { get; }
        IBoundDecl[] TypeArguments { get; }
    }

    public static class Callable
    {
        public static string UniqueName(string name,
            IEnumerable<IBoundDecl> typeArgs,
            IEnumerable<IBoundDecl> paramTypes)
        {
            string typeArgString = ((typeArgs != null) && typeArgs.Any()) ? "'(" + typeArgs.JoinAll(", ") + ")" : "";
            string argTypes = "(" + paramTypes.JoinAll(", ") + ")";
            return name + " " + typeArgString + argTypes;
        }

        /// <summary>
        /// Creates a unique name for the ICallable, taking into account parameter types and type arguments
        /// so that overloads and instantiated functions can be distinguished from each other.
        /// </summary>
        public static string UniqueName(this ICallable callable)
        {
            return UniqueName(callable.Name, callable.TypeArguments, callable.ParameterTypes);
        }

        /// <summary>
        /// Creates a unique name for the ICallable, ignoring its type parameters.
        /// </summary>
        public static string UniqueInferredName(this ICallable callable)
        {
            return UniqueName(callable.Name, null, callable.ParameterTypes);
        }
    }
}
