using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Transforms "let" expressions into their desugared form using just
    /// "def" and "if".
    /// </summary>
    public class LetTransformer : UnboundExprTransformer
    {
        public LetTransformer(NameGenerator generator)
        {
            mNameGenerator = generator;
        }

        public override IUnboundExpr Transform(LetExpr expr)
        {
            // a let expression desugars like this:
            //
            //      let a b <- Foo then Bar else Bang
            // 
            // becomes...
            //
            //      def let__ <- Foo
            //      if Some? let__ then
            //          def a b <- 0 SomeValue let__
            //          Bar
            //      else Bang

            // def a__ <- Foo
            var optionName = mNameGenerator.Generate();
            var defineOption = new DefineExpr(expr.Position, optionName, expr.Condition, false);

            // Some? a__
            var condition = new CallExpr(new NameExpr(expr.Position, "Some?"),
                                         new NameExpr(expr.Position, optionName));

            // def a <- SomeValue a__
            var getValue = new CallExpr(new NameExpr(expr.Position, "SomeValue"),
                                        new NameExpr(expr.Position, optionName));
            var defineValue = new DefineExpr(expr.Position, expr.Names, getValue, false);

            var thenBody = new BlockExpr(new IUnboundExpr[] { defineValue, expr.ThenBody });
            var ifThen = new IfExpr(expr.Position, condition, thenBody, expr.ElseBody);

            return new BlockExpr(new IUnboundExpr[] { defineOption, ifThen });
        }

        private NameGenerator mNameGenerator;
    }
}
