using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Defines a tuple type declaration, including the types of the fields.
    /// </summary>
    public class TupleType : Decl
    {
        public IList<Decl> Fields { get { return mFields; } }

        public override Decl[] Expanded { get { return mFields.ToArray(); } }

        public TupleType(IEnumerable<Decl> fields)
        {
            mFields.AddRange(fields);
        }

        public override string ToString()
        {
            return "(" + mFields.JoinAll(", ") + ")";
        }

        public override TReturn Accept<TReturn>(IDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        private readonly List<Decl> mFields = new List<Decl>();
    }
}
