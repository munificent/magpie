using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A type declaration.
    /// </summary>
    public abstract class Decl
    {
        public static readonly Decl Unit   = new AtomicDecl("Unit");
        public static readonly Decl Bool   = new AtomicDecl("Bool");
        public static readonly Decl Char   = new AtomicDecl("Char");
        public static readonly Decl Int    = new AtomicDecl("Int");
        public static readonly Decl String = new AtomicDecl("String");

        /// <summary>
        /// This special type is the return type of an early return expression.
        /// It lets us ensure that return expressions are only valid in certain
        /// places.
        /// </summary>
        public static readonly Decl EarlyReturn = new AtomicDecl("return");

        public static Decl FromName(string name, IEnumerable<Decl> args)
        {
            // see if it's an atomic type
            //### bob: should barf if it's an atomic type and there is an arg
            switch (name)
            {
                case "Bool": return Bool;
                case "Char": return Char;
                case "Int": return Int;
                case "String": return String;
            }

            // must be a user-defined type
            return new NamedType(name, args);
        }

        public static Decl FromName(string name)
        {
            return FromName(name, null);
        }

        /// <summary>
        /// Expands a type declaration. Most types return themselves, but
        /// a tuple will be flattened and unit will return an empty array.
        /// </summary>
        public virtual Decl[] Expanded
        {
            get
            {
                if (ReferenceEquals(this, Unit)) return new Decl[0];

                return new Decl[] { this };
            }
        }

        public abstract TReturn Accept<TReturn>(IDeclVisitor<TReturn> visitor);
    }
}
