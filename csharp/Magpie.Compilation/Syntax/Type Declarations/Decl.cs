using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A type declaration in both its unbound and bound form.
    /// </summary>
    public class Decl
    {
        public static readonly AtomicDecl Unit = new AtomicDecl("Unit");
        public static readonly AtomicDecl Bool = new AtomicDecl("Bool");
        public static readonly AtomicDecl Int = new AtomicDecl("Int");
        public static readonly AtomicDecl String = new AtomicDecl("String");

        /// <summary>
        /// This special type is the return type of an early return expression.
        /// It lets us ensure that return expressions are only valid in certain
        /// places.
        /// </summary>
        public static readonly AtomicDecl EarlyReturn = new AtomicDecl("return");

        public IUnboundDecl Unbound { get; private set; }
        public IBoundDecl Bound { get; private set; }

        public bool IsBound { get { return Bound != null; } }

        public Decl(IUnboundDecl unbound)
        {
            if (unbound == null) throw new ArgumentNullException("unbound");

            Unbound = unbound;
        }

        public Decl(IBoundDecl bound)
        {
            if (bound == null) throw new ArgumentNullException("bound");

            Bound = bound;
        }

        public void Bind(IBoundDecl bound)
        {
            if (bound == null) throw new ArgumentNullException("bound");

            Bound = bound;

            // discard the unbound one now. makes sure we're clear on what state we expect
            // the declaration to be in.
            Unbound = null;
        }
    }
}
