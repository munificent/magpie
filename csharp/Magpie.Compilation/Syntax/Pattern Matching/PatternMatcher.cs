using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    //### bob: the overall process is:
    /// <summary>
    /// Translates a pattern matching expression into simpler primitive conditinal form.
    /// Validates that the pattern cases are non-overlapping and exhaustive. The basic
    /// process is:
    /// 1. Make sure the cases are the right shape (i.e. type) to match the value
    /// 2. Make sure the cases are non-overlapping and exhaustive
    /// 3. Desugar each case to a conditional and variable binding
    /// 4. Combine the cases into a single series of if/then expressions.
    /// </summary>
    public class PatternMatcher : IPatternVisitor<IUnboundExpr>
    {
        public static IUnboundExpr Match(BindingContext context, MatchExpr expr, IBoundExpr boundValue)
        {
            // validate the patterns
            ShapeChecker.Validate(expr, boundValue.Type);
            Coverage.Validate(expr, boundValue.Type);

            // convert the cases to vanilla if/then blocks
            var valueName = context.NameGenerator.Generate();
            var defineValueExpr = new DefineExpr(expr.Value.Position, valueName, expr.Value, false);
            var valueExpr = new NameExpr(expr.Value.Position, valueName);

            var cases = new Stack<Tuple<IUnboundExpr, IUnboundExpr>>();

            foreach (var matchCase in expr.Cases)
            {
                var compare = PatternMatcher.Match(context,
                    boundValue.Type, matchCase.Pattern, valueExpr);

                cases.Push(Tuple.Create(compare, matchCase.Body));
            }

            // build a single compound "if" expression
            var ifExpr = (IUnboundExpr)null;

            if (cases.Count == 0)
            {
                throw new CompileException(expr.Position, "Match expression has no cases.");
            }
            else if (cases.Count == 1)
            {
                // if there is only a since case, and we've ensured exhaustivity already, 
                // then it must match all values, so just use the body directly
                var ifThen = cases.Pop();
                ifExpr = ifThen.Item2;
            }
            else
            {
                foreach (var ifThen in cases)
                {
                    if (ifExpr == null)
                    {
                        // for the first popped (i.e. last appearing) case, we know it must
                        // match because the match expression is exhaustive, so just use its
                        // body directly
                        ifExpr = ifThen.Item2;
                    }
                    else
                    {
                        ifExpr = new IfExpr(ifThen.Item1.Position, ifThen.Item1, ifThen.Item2, ifExpr);
                    }
                }
            }

            return new BlockExpr(new IUnboundExpr[] { defineValueExpr, ifExpr });
        }

        private static IUnboundExpr Match(BindingContext context, IBoundDecl decl,
            IPattern caseExpr, IUnboundExpr value)
        {
            var matcher = new PatternMatcher(context, decl, value);
            return caseExpr.Accept(matcher);
        }

        private PatternMatcher(BindingContext context, IBoundDecl decl, IUnboundExpr value)
        {
            mContext = context;
            mDecl = decl;
            mValue = value;
        }

        private IUnboundExpr VisitLiteral(LiteralPattern expr)
        {
            return new CallExpr(new NameExpr(expr.Position, "="), new TupleExpr(mValue, expr.ValueExpr));
        }

        #region ICaseExprVisitor<bool> Members

        IUnboundExpr IPatternVisitor<IUnboundExpr>.Visit(AnyPattern expr)
        {
            // always succeed
            return new BoolExpr(expr.Position, true);
        }

        IUnboundExpr IPatternVisitor<IUnboundExpr>.Visit(BoolPattern expr)
        {
            return VisitLiteral(expr);
        }

        IUnboundExpr IPatternVisitor<IUnboundExpr>.Visit(IntPattern expr)
        {
            return VisitLiteral(expr);
        }

        IUnboundExpr IPatternVisitor<IUnboundExpr>.Visit(StringPattern expr)
        {
            return VisitLiteral(expr);
        }

        IUnboundExpr IPatternVisitor<IUnboundExpr>.Visit(UnionPattern expr)
        {
            var unionDecl = (Union)mDecl;
            var unionCase = unionDecl.Cases.First(thisCase => thisCase.Name == expr.Name);

            // create an expression to match the union case
            var caseCheck = new NameExpr(Position.None, unionCase.Name + "?");
            var matchExpr = new CallExpr(caseCheck, mValue);

            // match the value
            if (expr.Value != null)
            {
                // create an expression to pull out the union value
                var unionValue = new CallExpr(new NameExpr(expr.Position, unionCase.Name + "Value"), mValue);

                // match it
                var matchValue = PatternMatcher.Match(mContext, unionCase.ValueType.Bound, expr.Value,
                    unionValue);

                if (matchValue != null)
                {
                    // combine with the previous check
                    matchExpr = new CallExpr(new NameExpr(expr.Position, "&"),
                        new TupleExpr(matchExpr, matchValue));
                }
            }

            return matchExpr;
        }

        IUnboundExpr IPatternVisitor<IUnboundExpr>.Visit(TuplePattern expr)
        {
            var tupleDecl = (BoundTupleType)mDecl;

            // match each field
            var match = (IUnboundExpr)null;
            for (int i = 0; i < tupleDecl.Fields.Count; i++)
            {
                var position = expr.Fields[i].Position;

                // create an expression to pull out the tuple field
                var fieldValue = new CallExpr(new IntExpr(position, i), mValue);

                // match it
                var matchField = PatternMatcher.Match(mContext, tupleDecl.Fields[i], expr.Fields[i],
                    fieldValue);

                if (match == null)
                {
                    match = matchField;
                }
                else
                {
                    // combine with the previous matches
                    match = new CallExpr(new NameExpr(position, "&"), 
                        new TupleExpr(match, matchField));
                }
            }

            return match;
        }

        #endregion

        private BindingContext mContext;
        private IBoundDecl mDecl;
        private IUnboundExpr mValue;
    }
}
