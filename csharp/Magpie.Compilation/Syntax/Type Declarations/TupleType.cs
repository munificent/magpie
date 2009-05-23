using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class TupleType<TDecl>
    {
        public IList<TDecl> Fields { get { return mFields; } }

        public TupleType(IEnumerable<TDecl> fields)
        {
            mFields.AddRange(fields);
        }

        public override string ToString()
        {
            return "(" + mFields.JoinAll(", ") + ")";
        }

        private readonly List<TDecl> mFields = new List<TDecl>();
    }

    /// <summary>
    /// Defines a tuple type declaration, including the types of the fields.
    /// </summary>
    public class TupleType : TupleType<IUnboundDecl>, IUnboundDecl
    {
        public TokenPosition Position { get; private set; }

        public TupleType(Tuple<IEnumerable<IUnboundDecl>, TokenPosition> args)
            : base(args.Item1)
        {
            Position = args.Item2;
        }

        #region IUnboundDecl Members

        TReturn IUnboundDecl.Accept<TReturn>(IUnboundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }

    public class BoundTupleType : TupleType<IBoundDecl>, IBoundDecl
    {
        public BoundTupleType(IEnumerable<IBoundDecl> fields)
            : base(fields)
        {
        }

        #region IBoundDecl Members

        TReturn IBoundDecl.Accept<TReturn>(IBoundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
