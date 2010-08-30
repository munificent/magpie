using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Pattern for matching a tuple value.
    /// </summary>
    public class TuplePattern : IPattern
    {
        /// <summary>
        /// The patterns for each field in the tuple.
        /// </summary>
        public IList<IPattern> Fields { get; private set; }

        public TuplePattern(IList<IPattern> fields)
        {
            Fields = fields;
        }

        public override string ToString()
        {
            return "(" + Fields.JoinAll(", ") + ")";
        }

        #region ICaseExpr Members

        public Position Position
        {
            get
            {
                return Fields[0].Position;
            }
        }

        TReturn IPattern.Accept<TReturn>(IPatternVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
