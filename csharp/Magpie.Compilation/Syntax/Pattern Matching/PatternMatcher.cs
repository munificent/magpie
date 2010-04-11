using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
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

            var conditions = new List<DesugaredMatchCase>();

            foreach (var matchCase in expr.Cases)
            {
                var result = new DesugaredMatchCase();

                result.Condition = Match(context, boundValue.Type,
                    matchCase.Pattern, valueExpr, result.Variables);

                conditions.Add(result);
            }

            // build a single compound "if" expression
            var ifExpr = (IUnboundExpr)null;

            if (expr.Cases.Count == 0)
            {
                throw new CompileException(expr.Position, "Match expression has no cases.");
            }
            else if (expr.Cases.Count == 1)
            {
                // if there is only a since case, and we've ensured exhaustivity already, 
                // then it must match all values, so just use the body directly
                ifExpr = CreateCaseBody(conditions[0].Variables, expr.Cases[0].Body);
            }
            else
            {
                // go in reverse order so that we start with the trailing else and build
                // forward
                for (int i = expr.Cases.Count - 1; i >= 0; i--)
                {
                    if (ifExpr == null)
                    {
                        // for the first popped (i.e. last appearing) case, we know it must
                        // match because the match expression is exhaustive, so just use its
                        // body directly
                        ifExpr = CreateCaseBody(conditions[i].Variables, expr.Cases[i].Body);
                    }
                    else
                    {
                        var body = CreateCaseBody(conditions[i].Variables, expr.Cases[i].Body);
                        ifExpr = new IfExpr(expr.Cases[i].Position, conditions[i].Condition,
                            body, ifExpr);
                    }
                }
            }

            return new BlockExpr(new IUnboundExpr[] { defineValueExpr, ifExpr });
        }

        private static IUnboundExpr CreateCaseBody(IDictionary<string, IUnboundExpr> variables, IUnboundExpr body)
        {
            // if there are no variables, just use the body directly
            if (variables.Count == 0) return body;

            // otherwise, define all of the variables and then execute the body
            var exprs = new List<IUnboundExpr>();
            foreach (var pair in variables)
            {
                exprs.Add(new DefineExpr(Position.None, pair.Key, pair.Value, false));
            }

            exprs.Add(body);

            return new BlockExpr(true, exprs);
        }

        private static IUnboundExpr Match(BindingContext context, IBoundDecl decl,
            IPattern caseExpr, IUnboundExpr value, IDictionary<string, IUnboundExpr> variables)
        {
            var matcher = new PatternMatcher(context, decl, value, caseExpr, variables);
            return caseExpr.Accept(matcher);
        }

        private PatternMatcher(BindingContext context, IBoundDecl decl, IUnboundExpr value,
            IPattern caseExpr, IDictionary<string, IUnboundExpr> variables)
        {
            mContext = context;
            mDecl = decl;
            mValue = value;
            mVariables = variables;
            mC = new CodeBuilder(context.NameGenerator, caseExpr.Position);
        }

        private IUnboundExpr VisitLiteral(LiteralPattern expr)
        {
            return mC.Op(mValue, "=", expr.ValueExpr);
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
            var matchExpr = mC.Call(unionCase.Name + "?", mValue);

            // match the value
            if (expr.Value != null)
            {
                // create an expression to pull out the union value
                var unionValue = mC.Call(unionCase.Name + "Value", mValue);

                // match it
                var matchValue = Match(mContext, unionCase.ValueType.Bound, expr.Value,
                    unionValue, mVariables);

                if (matchValue != null)
                {
                    // combine with the previous check
                    matchExpr = mC.Op(matchExpr, "&", matchValue);
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
                mC.SetPosition(expr.Fields[i].Position);

                // create an expression to pull out the tuple field
                var fieldValue = mC.Call(mC.Int(i), mValue);

                // match it
                var matchField = Match(mContext, tupleDecl.Fields[i], expr.Fields[i],
                    fieldValue, mVariables);

                if (match == null)
                {
                    match = matchField;
                }
                else
                {
                    // combine with the previous matches
                    match = mC.Op(match, "&", matchField);
                }
            }

            return match;
        }

        IUnboundExpr IPatternVisitor<IUnboundExpr>.Visit(VariablePattern expr)
        {
            // bind the variable
            mVariables[expr.Name] = mValue;

            // always succeed
            return new BoolExpr(expr.Position, true);
        }

        #endregion

        private BindingContext mContext;
        private IBoundDecl mDecl;
        private IUnboundExpr mValue;
        private IDictionary<string, IUnboundExpr> mVariables;
        private CodeBuilder mC;
    }
}
