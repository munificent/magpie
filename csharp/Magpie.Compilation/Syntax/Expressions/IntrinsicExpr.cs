using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// An intrinsic function is an operation in Magpie that has function syntax,
    /// but compiles down to native opcodes in the interpreter. In other words, an
    /// intrinsic could not otherwise be implemented in Magpie.
    /// </summary>
    public class IntrinsicExpr : IBoundExpr
    {
        public Intrinsic Intrinsic { get; private set; }
        public IBoundExpr Arg { get; private set; }

        public IntrinsicExpr(Intrinsic intrinsic, IBoundExpr arg)
        {
            Intrinsic = intrinsic;
            Arg = arg;
        }

        public override string ToString()
        {
            return String.Format("{0} {1}", Intrinsic.Name, Arg);
        }

        public IBoundDecl Type { get { return Intrinsic.Type; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
