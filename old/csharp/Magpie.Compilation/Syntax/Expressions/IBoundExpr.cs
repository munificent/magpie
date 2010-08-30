using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Represents a bound expression. This is created by binding an unbound expression, and represents
    /// a fully compilable valid expression.
    /// </summary>
    public interface IBoundExpr
    {
        IBoundDecl Type { get; }
        TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor);
    }

    public static class IBoundExprExtensions
    {
        public static IBoundExpr AppendArg(this IBoundExpr arg, IBoundExpr value)
        {
            if (arg is UnitExpr)
            {
                // no arg, so just use the value
                return value;
            }

            var tuple = arg as BoundTupleExpr;
            if (tuple != null)
            {
                // multiple args, so just add another
                var newArg = new BoundTupleExpr(tuple.Fields);
                newArg.Fields.Add(value);
                return newArg;
            }

            // single arg, so create a tuple
            return new BoundTupleExpr(new IBoundExpr[] { arg, value });
        }

        public static IBoundExpr PrependArg(this IBoundExpr arg, IBoundExpr value)
        {
            if (arg is UnitExpr)
            {
                // no arg, so just use the value
                return value;
            }

            var tuple = arg as BoundTupleExpr;
            if (tuple != null)
            {
                // multiple args, so just add another
                var newArg = new BoundTupleExpr(tuple.Fields);
                newArg.Fields.Insert(0, value);
                return newArg;
            }

            // single arg, so create a tuple
            return new BoundTupleExpr(new IBoundExpr[] { value, arg });
        }
    }
}
