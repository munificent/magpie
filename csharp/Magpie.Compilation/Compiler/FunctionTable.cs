using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Maintains the symbol table of instantiated functions used by a program.
    /// </summary>
    public class FunctionTable
    {
        /// <summary>
        /// Gets all of the compiled user-defined functions in the function table, ignoring
        /// intrinsics and other non-function "functions".
        /// </summary>
        public IEnumerable<Function> Functions
        {
            get { return mCallables.Values.OfType<Function>(); }
        }

        /// <summary>
        /// Adds the given fully-unbound (i.e. just parsed) user-defined function to the symbol
        /// table.
        /// </summary>
        public void AddUnbound(Function function)
        {
            mUnbound.Add(function);
        }

        /// <summary>
        /// Adds the given fully-unbound (i.e. just parsed) user-defined function to the symbol
        /// table and immediately binds it. Should only be called for functions created during
        /// the binding process (i.e. local functions).
        /// </summary>
        public void AddAndBind(Compiler compiler, Function function)
        {
            AddUnbound(function);
            Bind(compiler, function);
            mToBind.Enqueue(function);
        }

        /// <summary>
        /// Adds the given function to the symbol table.
        /// </summary>
        public void Add(ICallable callable)
        {
            // match against the unique name
            var uniqueName = callable.UniqueName();

            //### bob: if we want users to be able to override intrinsics, we may need to handle this differently
            if (mCallables.ContainsKey(uniqueName)) throw new CompileException("A function named " + uniqueName + " has already been declared.");

            mCallables.Add(uniqueName, callable);

            // if there is an inferrable name, also include the name without the type arguments
            if (callable.HasInferrableTypeArguments)
            {
                mCallables.Add(callable.UniqueInferredName(), callable);
            }
        }

        /// <summary>
        /// Adds the given generic function to the symbol table.
        /// </summary>
        public void Add(IGenericCallable generic)
        {
            mGenerics.Add(generic);
        }

        public void AddRange(IEnumerable<ICallable> callables)
        {
            foreach (var callable in callables)
            {
                Add(callable);
            }
        }

        public void AddRange(IEnumerable<IGenericCallable> generics)
        {
            foreach (var generic in generics)
            {
                Add(generic);
            }
        }

        /// <summary>
        /// Looks for a function with the given name in all of the currently used namespaces.
        /// </summary>
        public ICallable Find(BindingContext context,
            string name, IList<IUnboundDecl> typeArgs, IBoundDecl argType)
        {
            foreach (var potentialName in context.SearchSpace.SearchFor(name))
            {
                var bound = LookUpFunction(context, potentialName, typeArgs, argType);
                if (bound != null) return bound;
            }

            // not found
            return null;
        }

        private ICallable LookUpFunction(BindingContext context, string fullName,
            IList<IUnboundDecl> typeArgs, IBoundDecl argType)
        {
            // with this line uncommented, the "InferredGenericUsesExplicitType" test
            // passes, but the self-hosting compiler doesn't compile. the line after
            // the next will get the compiler to break at a different point (earlier?
            // later?), but breaks the test
            // the first line is newer and (i think) correct. the old line is what it was
            // doing before.
            var boundTypeArgs = TypeBinder.Bind(context, typeArgs);
            //var boundTypeArgs = TypeBinder.Bind(new BindingContext(context.Compiler, context.SearchSpace), typeArgs);

            string uniqueName = Callable.UniqueName(fullName, boundTypeArgs, argType);

            // try the already bound functions
            ICallable callable;
            if (TryFind(fullName, boundTypeArgs, argType, out callable)) return callable;

            // try to instantiate a generic
            foreach (var generic in mGenerics)
            {
                // names must match
                if (generic.Name != fullName) continue;

                ICallable instance = generic.Instantiate(context, boundTypeArgs, argType);

                //### bob: there's a bug here. it doesn't check that the *unique* names of the two functions
                // match, just the base names. i think this means it could incorrectly collide:
                // List'Int ()
                // List'Bool ()
                // but i'm not positive

                if (instance != null) return instance;
            }

            // couldn't find it
            return null;
        }

        private bool TryFind(string name, IEnumerable<IBoundDecl> typeArguments, IBoundDecl paramType, out ICallable bound)
        {
            string uniqueName = Callable.UniqueName(name, typeArguments, paramType);

            // look up by unique name
            if (mCallables.TryGetValue(uniqueName, out bound)) return true;

            // wasn't found
            return false;
        }

        public void BindAll(Compiler compiler)
        {
            // bind the types of the user functions and add them to the main table
            foreach (var unbound in mUnbound)
            {
                Bind(compiler, unbound);
            }

            // copy the functions to a queue because binding a function may cause generics
            // to be instantiated, adding to the collection.
            // (we don't need to worry about binding the newly added generics, because they
            // are bound as part of the instantiation process. binding is how the compiler
            // determines if a generic's type arguments are valid.)
            mToBind = new Queue<Function>(Functions);

            // bind the bodies of all of the functions
            while (mToBind.Count > 0)
            {
                var function = mToBind.Dequeue();
                var context = new BindingContext(compiler, function.SearchSpace);
                FunctionBinder.Bind(context, function);
            }
        }

        private void Bind(Compiler compiler, Function unbound)
        {
            var context = new BindingContext(compiler, unbound.SearchSpace);
            TypeBinder.Bind(context, unbound.Type);
            Add(unbound);
        }

        private readonly List<Function>                mUnbound   = new List<Function>();
        private readonly Dictionary<string, ICallable> mCallables = new Dictionary<string, ICallable>();
        private readonly List<IGenericCallable>        mGenerics = new List<IGenericCallable>();

        private Queue<Function> mToBind;
    }
}
