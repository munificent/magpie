using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    //### bob: move to separate file
    public class Coverage
    {
        /// <summary>
        /// Adds the given pattern's values to the set of covered values. Returns true if
        /// the pattern's values are already covered.
        /// </summary>
        /// <param name="pattern"></param>
        /// <returns></returns>
        public bool Cover(IPattern pattern)
        {
            // bail if already covered
            if (pattern.Accept(new CheckCoverage(this))) return true;

            pattern.Accept(new AddCoverage(this));
            return false;
        }

        private class CheckCoverage : IPatternVisitor<bool>
        {
            public CheckCoverage(Coverage coverage)
            {
                mCoverage = coverage;
            }

            #region IPatternVisitor<bool> Members

            bool IPatternVisitor<bool>.Visit(AnyPattern expr)
            {
                return mCoverage.mFullyCovered;
            }

            bool IPatternVisitor<bool>.Visit(BoolPattern expr)
            {
                if (mCoverage.mFullyCovered) return true;
                if (mCoverage.mCoveredBools.ContainsKey(expr.Value)) return true;

                return false;
            }

            bool IPatternVisitor<bool>.Visit(IntPattern expr)
            {
                if (mCoverage.mFullyCovered) return true;
                if (mCoverage.mCoveredInts.ContainsKey(expr.Value)) return true;

                return false;
            }

            bool IPatternVisitor<bool>.Visit(StringPattern expr)
            {
                if (mCoverage.mFullyCovered) return true;
                if (mCoverage.mCoveredStrings.ContainsKey(expr.Value)) return true;

                return mCoverage.mFullyCovered;
            }

            bool IPatternVisitor<bool>.Visit(UnionPattern expr)
            {
                //### bob: not implemented
                return mCoverage.mFullyCovered;
            }

            bool IPatternVisitor<bool>.Visit(TuplePattern expr)
            {
                //### bob: not implemented
                return mCoverage.mFullyCovered;
            }

            #endregion

            private Coverage mCoverage;
        }

        private class AddCoverage : IPatternVisitor<bool>
        {
            public AddCoverage(Coverage coverage)
            {
                mCoverage = coverage;
            }

            #region IPatternVisitor<bool> Members

            bool IPatternVisitor<bool>.Visit(AnyPattern expr)
            {
                mCoverage.mFullyCovered = true;
                return false;
            }

            bool IPatternVisitor<bool>.Visit(BoolPattern expr)
            {
                mCoverage.mCoveredBools[expr.Value] = true;

                // note if we've covered all bools
                if (mCoverage.mCoveredBools.Count == 2) mCoverage.mFullyCovered = true;
                return false;
            }

            bool IPatternVisitor<bool>.Visit(IntPattern expr)
            {
                mCoverage.mCoveredInts[expr.Value] = true;
                return false;
            }

            bool IPatternVisitor<bool>.Visit(StringPattern expr)
            {
                mCoverage.mCoveredStrings[expr.Value] = true;
                return false;
            }

            bool IPatternVisitor<bool>.Visit(UnionPattern expr)
            {
                //### bob: not implemented
                return false;
            }

            bool IPatternVisitor<bool>.Visit(TuplePattern expr)
            {
                //### bob: not implemented
                return false;
            }

            #endregion

            private Coverage mCoverage;
        }

        private bool mFullyCovered;
        //### bob: hack. only one of these will be needed for any given coverage
        private readonly Dictionary<bool, bool> mCoveredBools = new Dictionary<bool, bool>();
        private readonly Dictionary<int, bool> mCoveredInts = new Dictionary<int, bool>();
        private readonly Dictionary<string, bool> mCoveredStrings = new Dictionary<string, bool>();
    }

    /// <summary>
    /// Given a pattern, generates a conditional expression that will evaluate to true if the
    /// pattern matches a given value.
    /// </summary>
    public class PatternMatcher : IPatternVisitor<IUnboundExpr>
    {
        public static IUnboundExpr Match(BindingContext context, MatchExpr expr, IBoundExpr boundValue)
        {
            // convert the cases to vanilla if/then blocks
            //### bob: name is a hack here. need to make sure it won't collide on nested
            // matches
            var defineValueExpr = new DefineExpr(expr.Value.Position, " m", expr.Value, false);
            var valueExpr = new NameExpr(expr.Value.Position, " m");

            var ifThens = new Stack<Tuple<IUnboundExpr, IUnboundExpr>>();
            var coverage = new Coverage();

            foreach (var matchCase in expr.Cases)
            {
                var compare = PatternMatcher.Match(context, coverage,
                    boundValue.Type, matchCase.Pattern, valueExpr);

                ifThens.Push(Tuple.Create(compare, matchCase.Body));
            }

            //### bob: check to see if coverage is exhaustive here by doing something like
            // coverage.IsExhaustive...

            // build a single compound if expression
            var ifExpr = (IUnboundExpr)null;

            if (ifThens.Count == 0)
            {
                throw new CompileException(expr.Position, "Match expression has no cases.");
            }
            else if (ifThens.Count == 1)
            {
                var ifThen = ifThens.Pop();
                ifExpr = new IfThenExpr(ifThen.Item1.Position, ifThen.Item1, ifThen.Item2);
                //### bob: using IfThen here (and below) means that match expressions can only
                // return Unit. if we can get the compiler to ensure exhaustive cases, we can
                // have matches that return other values.
            }
            else
            {
                foreach (var ifThen in ifThens)
                {
                    if (ifExpr == null)
                    {
                        ifExpr = new IfThenExpr(ifThen.Item1.Position, ifThen.Item1, ifThen.Item2);
                    }
                    else
                    {
                        ifExpr = new IfThenElseExpr(ifThen.Item1.Position, ifThen.Item1, ifThen.Item2, ifExpr);
                    }
                }
            }

            return new BlockExpr(new IUnboundExpr[] { defineValueExpr, ifExpr });
        }

        private static IUnboundExpr Match(BindingContext context, Coverage coverage, IBoundDecl decl,
            IPattern caseExpr, IUnboundExpr value)
        {
            bool covered = coverage.Cover(caseExpr);

            if (covered) throw new CompileException(caseExpr.Position,
                "This pattern will never be matched because previous patterns cover it.");

            var matcher = new PatternMatcher(context, coverage, decl, value);
            return caseExpr.Accept(matcher);
        }

        private PatternMatcher(BindingContext context, Coverage coverage, IBoundDecl decl, IUnboundExpr value)
        {
            mContext = context;
            mCoverage = coverage;
            mDecl = decl;
            mValue = value;
        }

        private IUnboundExpr VisitLiteral(LiteralPattern expr)
        {
            if (expr.Type != mDecl) throw new CompileException(expr.Position,
                String.Format("Cannot match a value of type {0} against a literal case of type {1}.",
                    mDecl, expr.Type));

            var equals = new NameExpr(Position.None, "=");

            return new CallExpr(equals, new TupleExpr(mValue, expr.ValueExpr));
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
            var unionDecl = mDecl as Union;
            if (unionDecl == null) throw new CompileException(expr.Position,
                String.Format("Could not match union case {0} because type {1} is not a union type.", expr.Name, mDecl));

            var unionCase = unionDecl.Cases.FirstOrDefault(thisCase => thisCase.Name == expr.Name);
            if (unionCase == null) throw new CompileException(expr.Position,
                String.Format("Could not find a case named {0} in union type {1}.", expr.Name, unionDecl.Name));

            if ((unionCase.ValueType.Bound == Decl.Unit) && (expr.Value != null)) throw new CompileException(expr.Position,
                String.Format("Union case {0} does not have a value, so should not be matched against a case using one.", unionCase));

            if ((unionCase.ValueType.Bound != Decl.Unit) && (expr.Value == null)) throw new CompileException(expr.Position,
                String.Format("Union case {0} has value of type {1}, so cannot be matched against a case with no value.", unionCase, unionCase.ValueType));
 
            // create an expression to match the union case
            var caseCheck = new NameExpr(Position.None, unionCase.Name + "?");
            var matchExpr = new CallExpr(caseCheck, mValue);

            // match the value
            if (expr.Value != null)
            {
                // create an expression to pull out the tuple field
                var unionValue = new CallExpr(new NameExpr(expr.Position, unionCase.Name + "Value"), mValue);

                // match it
                var coverage = new Coverage(); //### bob: not implemented yet
                var matchValue = PatternMatcher.Match(mContext, coverage, unionCase.ValueType.Bound, expr.Value,
                    unionValue);

                if (matchValue != null)
                {
                    // combine with the previous check
                    matchExpr = new CallExpr(new NameExpr(Position.None, "&"),
                        new TupleExpr(matchExpr, matchValue));
                }
            }

            return matchExpr;
        }

        IUnboundExpr IPatternVisitor<IUnboundExpr>.Visit(TuplePattern expr)
        {
            var tupleDecl = mDecl as BoundTupleType;

            if (tupleDecl == null) throw new CompileException(expr.Position,
                String.Format("Can not match a tuple case against a non-tuple value of type {0}.", mDecl));

            if (tupleDecl.Fields.Count != expr.Fields.Count) throw new CompileException(expr.Position,
                String.Format("Can not match a tuple with {0} fields against a value with {1} fields.",
                tupleDecl.Fields.Count, expr.Fields.Count));

            // match each field
            var match = (IUnboundExpr)null;
            for (int i = 0; i < tupleDecl.Fields.Count; i++)
            {
                // create an expression to pull out the tuple field
                var fieldValue = new TupleFieldExpr(mValue, i);

                // match it
                var coverage = new Coverage(); //### bob: not implemented yet
                var matchField = PatternMatcher.Match(mContext, coverage, tupleDecl.Fields[i], expr.Fields[i],
                    fieldValue);

                if (match == null)
                {
                    match = matchField;
                }
                else
                {
                    // combine with the previous matches
                    match = new CallExpr(new NameExpr(Position.None, "&"), 
                        new TupleExpr(match, matchField));
                }
            }

            return match;
        }

        #endregion

        private BindingContext mContext;
        private Coverage mCoverage;
        private IBoundDecl mDecl;
        private IUnboundExpr mValue;
    }
}
