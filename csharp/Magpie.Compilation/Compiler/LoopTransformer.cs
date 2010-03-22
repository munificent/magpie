using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class LoopTransformer : UnboundExprTransformer
    {
        public LoopTransformer(NameGenerator generator)
        {
            mNameGenerator = generator;
        }

        public override IUnboundExpr Transform(LoopExpr expr)
        {
            // a for expression is basically syntactic sugar for a while expression
            // and one or more iterators. for example, the following:
            //
            // for foo <- bar do
            //     Print foo
            // end
            //
            // is equivalent to:
            //
            // def _fooIter <- Iterate bar
            // while MoveNext _fooIter do
            //     def foo <- Current _fooIter
            //     Print foo
            // end
            //
            // so, to bind a for expression, we just desugar it, then bind that.

            var c = new CodeBuilder(mNameGenerator, expr.Position);

            var topExprs = new List<IUnboundExpr>();
            var conditionExpr = (IUnboundExpr)null;
            var whileExprs = new List<IUnboundExpr>();

            // instantiate each clause
            foreach (var clause in expr.Clauses)
            {
                c.SetPosition(clause.Position);

                var condition = (IUnboundExpr)null;

                if (clause.IsWhile)
                {
                    condition = clause.Expression;
                }
                else
                {
                    var iterName = mNameGenerator.Generate();
                    topExprs.Add(c.Def(iterName, c.Call("Iterate", clause.Expression)));
                    whileExprs.Add(c.Def(clause.Name, c.Call("Current", iterName)));

                    condition = c.Call("MoveNext", iterName);
                }

                if (conditionExpr == null)
                {
                    conditionExpr = condition;
                }
                else
                {
                    // combine with previous condition(s)
                    conditionExpr = c.Op(conditionExpr, "&", condition);
                }
            }

            // create the while loop
            c.SetPosition(expr.Position);
            whileExprs.Add(expr.Body);
            var whileExpr = c.While(
                conditionExpr,
                c.Block(whileExprs));
            topExprs.Add(whileExpr);

            // build the whole block
            return c.Block(topExprs);
        }

        private NameGenerator mNameGenerator;
    }
}
