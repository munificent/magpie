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
        IBoundDecl ParameterType { get; }

        bool HasInferrableTypeArguments { get; }
        IBoundDecl[] TypeArguments { get; }
    }

    public static class Callable
    {
        public static string UniqueName(string name,
            IEnumerable<IBoundDecl> typeArgs,
            IBoundDecl paramType)
        {
            string typeArgString = ((typeArgs != null) && typeArgs.Any()) ? "'(" + typeArgs.JoinAll(", ") + ")" : "";
            return name + " " + typeArgString + paramType.ToString();
        }

        /// <summary>
        /// Creates a unique name for the ICallable, taking into account parameter types and type arguments
        /// so that overloads and instantiated functions can be distinguished from each other.
        /// </summary>
        public static string UniqueName(this ICallable callable)
        {
            return UniqueName(callable.Name, callable.TypeArguments, callable.ParameterType);
        }

        /// <summary>
        /// Creates a unique name for the ICallable, ignoring its type parameters.
        /// </summary>
        public static string UniqueInferredName(this ICallable callable)
        {
            return UniqueName(callable.Name, null, callable.ParameterType);
        }
    }
}
