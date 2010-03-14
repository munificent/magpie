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

            var topExprs = new List<IUnboundExpr>();
            var conditionExpr = (IUnboundExpr)null;
            var whileExprs = new List<IUnboundExpr>();

            // instantiate each clause
            foreach (var clause in expr.Clauses)
            {
                if (clause.IsWhile)
                {
                    if (conditionExpr == null)
                    {
                        conditionExpr = clause.Expression;
                    }
                    else
                    {
                        // combine with previous condition(s)
                        conditionExpr = new CallExpr(new NameExpr(clause.Position, "&"), new TupleExpr(conditionExpr, clause.Expression));
                    }
                }
                else
                {
                    var iterName = mNameGenerator.Generate();
                    var createIterator = new CallExpr(new NameExpr(clause.Position, "Iterate"), clause.Expression);
                    topExprs.Add(new DefineExpr(clause.Position, iterName, createIterator, false));

                    var condition = new CallExpr(new NameExpr(clause.Position, "MoveNext"), new NameExpr(clause.Position, iterName));
                    if (conditionExpr == null)
                    {
                        conditionExpr = condition;
                    }
                    else
                    {
                        // combine with previous condition(s)
                        conditionExpr = new CallExpr(new NameExpr(clause.Position, "&"), new TupleExpr(conditionExpr, condition));
                    }

                    var currentValue = new CallExpr(new NameExpr(clause.Position, "Current"), new NameExpr(clause.Position, iterName));
                    whileExprs.Add(new DefineExpr(clause.Position, clause.Name, currentValue, false));
                }
            }

            // create the while loop
            whileExprs.Add(expr.Body);
            var whileExpr = new WhileExpr(expr.Position,
                conditionExpr,
                new BlockExpr(whileExprs));
            topExprs.Add(whileExpr);

            // build the whole block
            return new BlockExpr(topExprs);
        }

        private NameGenerator mNameGenerator;
    }
}
