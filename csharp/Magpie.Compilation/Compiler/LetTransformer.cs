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
            var c = new CodeBuilder(mNameGenerator, expr.Position);
            var option = c.TempName();

            return c.Block(
                c.Def(option, expr.Condition),
                c.If(c.Call("Some?", option),
                    c.Block(
                        c.Def(expr.Names, c.Call("SomeValue", option)),
                        expr.ThenBody),
                    expr.ElseBody));
        }

        private NameGenerator mNameGenerator;
    }
}
