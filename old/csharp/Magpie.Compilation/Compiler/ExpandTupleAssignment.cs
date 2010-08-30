using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Compilation pass to take assignment to a tuple and split it out into separate
    /// assignments.
    /// </summary>
    public class ExpandTupleAssignment : UnboundExprTransformer
    {
        public ExpandTupleAssignment(NameGenerator generator)
        {
            mNameGenerator = generator;
        }

        public override IUnboundExpr Transform(AssignExpr expr)
        {
            // replaces:
            // 
            //     a, b <- Foo
            // 
            // with:
            // 
            //     def __temp1 <- Foo
            //     a <- _temp1.0
            //     b <- _temp1.1

            TupleExpr tuple = expr.Target as TupleExpr;

            // ignore other assignments
            if (tuple == null) return expr;

            var exprs = new List<IUnboundExpr>();

            var temp = mNameGenerator.Generate();

            // evaluate the right-hand side once
            exprs.Add(new DefineExpr(expr.Target.Position, temp, expr.Value, false));

            // split out the fields
            int index = 0;
            foreach (var field in tuple.Fields)
            {
                exprs.Add(new AssignExpr(expr.Position, field, new CallExpr(new IntExpr(index), new NameExpr(expr.Position, temp))));
                index++;
            }

            return new BlockExpr(false, exprs);
        }

        private NameGenerator mNameGenerator;
    }
}
