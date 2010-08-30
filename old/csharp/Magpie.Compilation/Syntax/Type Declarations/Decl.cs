using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A type declaration in unbound or bound form.
    /// </summary>
    public class Decl
    {
        public static readonly AtomicDecl Unit = new AtomicDecl("()");
        public static readonly AtomicDecl Bool = new AtomicDecl("Bool");
        public static readonly AtomicDecl Int = new AtomicDecl("Int");
        public static readonly AtomicDecl String = new AtomicDecl("String");

        /// <summary>
        /// This special type is the return type of an early return expression.
        /// It lets us ensure that return expressions are only valid in certain
        /// places.
        /// </summary>
        public static readonly AtomicDecl EarlyReturn = new AtomicDecl("return");

        /// <summary>
        /// Gets the unbound type declaration. Will be <c>null</c> if this declaration
        /// has been bound.
        /// </summary>
        public IUnboundDecl Unbound { get; private set; }

        /// <summary>
        /// Gets the bound type declaration. Will be <c>null</c> if this declaration
        /// has not been bound.
        /// </summary>
        public IBoundDecl Bound { get; private set; }

        /// <summary>
        /// Gets whether or not this declaration has been bound.
        /// </summary>
        public bool IsBound { get { return Bound != null; } }

        /// <summary>
        /// Creates a new unbound declaration.
        /// </summary>
        public Decl(IUnboundDecl unbound)
        {
            if (unbound == null) throw new ArgumentNullException("unbound");

            Unbound = unbound;
        }

        /// <summary>
        /// Creates a new bound declaration.
        /// </summary>
        public Decl(IBoundDecl bound)
        {
            if (bound == null) throw new ArgumentNullException("bound");

            Bound = bound;
        }

        public override string ToString()
        {
            if (IsBound) return Bound.ToString();
            return Unbound.ToString();
        }

        /// <summary>
        /// Binds this declaration to the given binder.
        /// </summary>
        public void Bind(Func<IUnboundDecl, IBoundDecl> binder)
        {
            if (binder == null) throw new ArgumentNullException("binder");
            if (IsBound) throw new InvalidOperationException("Cannot bind a Decl that is already bound.");

            Bound = binder(Unbound);

            // discard the unbound one now. makes sure we're clear on what state we expect
            // the declaration to be in.
            Unbound = null;
        }
        /*
        /// <summary>
        /// Binds this declaration to the given binder.
        /// </summary>
        public void Bind(IUnboundDeclVisitor<IBoundDecl> binder)
        {
            if (binder == null) throw new ArgumentNullException("binder");
            if (IsBound) throw new InvalidOperationException("Cannot bind a Decl that is already bound.");

            Bound = Unbound.Accept(binder);

            // discard the unbound one now. makes sure we're clear on what state we expect
            // the declaration to be in.
            Unbound = null;
        }
        */
    }
}
