using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Maintains the collection of compiled functions used by a program.
    /// </summary>
    public class FunctionTable
    {
        public static string GetUniqueName(string name,
            IEnumerable<IBoundDecl> typeArgs,
            IEnumerable<IBoundDecl> paramTypes)
        {
            string typeArgString = ((typeArgs != null) && typeArgs.Any()) ? "'(" + typeArgs.JoinAll(", ") + ")" : "";
            string argTypes = "(" + paramTypes.JoinAll(", ") + ")";
            return name + " " + typeArgString + argTypes;
        }

        //### bob: is this the best place for this?
        public static string GetUniqueName(string name, IEnumerable<IBoundDecl> paramTypes)
        {
            return GetUniqueName(name, null, paramTypes);
        }

        public IEnumerable<Function> Functions
        {
            get { return mCallables.Values.OfType<Function>(); }
        }

        public void Add(IEnumerable<ICallable> callables)
        {
            foreach (var callable in callables)
            {
                Add(callable);
            }
        }

        public bool TryFind(string name, IEnumerable<IBoundDecl> paramTypes, out ICallable bound)
        {
            string uniqueName = GetUniqueName(name, paramTypes);

            // look up by unique name
            if (mCallables.TryGetValue(uniqueName, out bound)) return true;

            // barring that, look for a wildcard
            foreach (var pair in mWildcardCallables)
            {
                if ((pair.Key == name) && (DeclComparer.TypesMatch(pair.Value.ParameterTypes, paramTypes.ToArray())))
                {
                    bound = pair.Value;
                    return true;
                }
            }

            // wasn't found
            return false;
        }

        public void BindAll(Compiler compiler)
        {
            // copy the functions to an array because binding a function may cause generics
            // to be instantiated, adding to the collection.
            // (we don't need to worry about binding the newly added generics, because they
            // are bound as part of the instantiation process. binding is how the compiler
            // determines if a generic's type arguments are valid.)
            foreach (var function in Functions.ToArray())
            {
                var context = new BindingContext(compiler, function.NameContext);
                FunctionBinder.Bind(context, function);
            }
        }

        public void Add(ICallable callable)
        {
            if (DeclPredicate.Any(callable.ParameterTypes, decl => decl is AnyType))
            {
                // has a wildcard, so will need to match against the actual parameter types
                mWildcardCallables.Add(new KeyValuePair<string, ICallable>(callable.Name, callable));
            }
            else
            {
                // no wildcards, so can simply match against the unique name
                var uniqueName = GetUniqueName(callable.Name, callable.TypeArguments, callable.ParameterTypes);

                //### bob: if we want users to be able to override intrinsics, we may need to handle this differently
                if (mCallables.ContainsKey(uniqueName)) throw new CompileException("A function named " + uniqueName + " has already been declared.");

                mCallables.Add(uniqueName, callable);

                // if there is an inferrable name, also add it without the type arguments
                if (callable.HasInferrableTypeArguments)
                {
                    uniqueName = GetUniqueName(callable.Name, null, callable.ParameterTypes);
                    mCallables.Add(uniqueName, callable);
                }
            }
        }

        private readonly Dictionary<string, ICallable> mCallables = new Dictionary<string, ICallable>();

        //### bob: this wildcard stuff is hokey. can we get rid of this using the new IGenericCallable stuff?
        private readonly List<KeyValuePair<string, ICallable>> mWildcardCallables = new List<KeyValuePair<string, ICallable>>();
    }
}
