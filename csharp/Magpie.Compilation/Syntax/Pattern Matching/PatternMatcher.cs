using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /* pattern matching needs to:
     * 
     * static checking, raise an error if:
     * + the shape of a pattern doesn't match the value type
     * - a pattern cannot be matched because a previous pattern covers it
     * - there are possible values that will not be matched
     *   by any pattern (i.e. the patterns are not exhaustive)
     * - a variable is used twice in a pattern (i.e. the pattern is not linear)
     * + the expressions for each pattern do not all return the same type
     * 
     * runtime behavior:
     * - generate a conditional expression that determines which pattern is matched
     *   given a value.
     * - assign values to variables defined in the pattern
     */
    //### bob: move to separate file
    //### bob: this whole class is pretty hideous, but once it's more or less working,
    //         i can refactor it into something more sensible
    public class Coverage
    {
        /// <summary>
        /// Creates an empty coverage for values of the given type.
        /// </summary>
        /// <param name="decl">The value type being matched against.</param>
        /// <returns></returns>
        public static Coverage Create(IBoundDecl decl)
        {
            return decl.Accept(new CreateCoverage());
        }

        /// <summary>
        /// Returns true if the pattern's values are already covered.
        /// </summary>
        /// <param name="pattern"></param>
        /// <returns></returns>
        public bool Covers(IPattern pattern)
        {
            // bail if already covered
            if (mFullyCovered) return true;
            if (this.Match(pattern,
                a => Covers(a),
                a => Covers(a),
                a => Covers(a),
                a => Covers(a),
                a => Covers(a),
                a => Covers(a))) return true;

            return false;
        }

        /// <summary>
        /// Adds the given pattern's values to the set of covered values.
        /// </summary>
        /// <param name="pattern"></param>
        /// <returns></returns>
        public void Cover(IPattern pattern)
        {
            this.Match(pattern,
                a => Cover(a),
                a => Cover(a),
                a => Cover(a),
                a => Cover(a),
                a => Cover(a),
                a => Cover(a));
        }

        //### bob: should these throw NotImpl by default?
        protected virtual bool Covers(AnyPattern pattern) { return false; }
        protected virtual bool Covers(BoolPattern pattern) { return false; }
        protected virtual bool Covers(IntPattern pattern) { return false; }
        protected virtual bool Covers(StringPattern pattern) { return false; }
        protected virtual bool Covers(UnionPattern pattern) { return false; } //### bob: not implemented
        protected virtual bool Covers(TuplePattern pattern) { return false; } //### bob: not implemented

        protected virtual void Cover(AnyPattern pattern)
        {
            FullyCover();
        }

        protected virtual void Cover(BoolPattern pattern) { }
        protected virtual void Cover(IntPattern pattern) { }
        protected virtual void Cover(StringPattern pattern) { }
        protected virtual void Cover(UnionPattern pattern) { } //### bob: not implemented
        protected virtual void Cover(TuplePattern pattern) { } //### bob: not implemented

        protected void FullyCover()
        {
            mFullyCovered = true;
        }

        private class CreateCoverage : IBoundDeclVisitor<Coverage>
        {
            #region IBoundDeclVisitor<Coverage> Members

            Coverage IBoundDeclVisitor<Coverage>.Visit(BoundArrayType decl)
            {
                throw new NotSupportedException();
            }

            Coverage IBoundDeclVisitor<Coverage>.Visit(AtomicDecl decl)
            {
                if (decl == Decl.Bool) return new BoolCoverage();
                if (decl == Decl.Int) return new IntCoverage();
                if (decl == Decl.String) return new StringCoverage();

                throw new ArgumentException();
            }

            Coverage IBoundDeclVisitor<Coverage>.Visit(FuncType decl)
            {
                throw new NotSupportedException();
            }

            Coverage IBoundDeclVisitor<Coverage>.Visit(Struct decl)
            {
                throw new NotSupportedException();
            }

            Coverage IBoundDeclVisitor<Coverage>.Visit(BoundTupleType decl)
            {
                return new TupleCoverage(decl);
            }

            Coverage IBoundDeclVisitor<Coverage>.Visit(Union decl)
            {
                return new Coverage(); //### bob: not implemented
            }

            #endregion
        }

        private bool mFullyCovered;
    }

    public class BoolCoverage : Coverage
    {
        protected override bool Covers(BoolPattern pattern) { return mCoveredBools.ContainsKey(pattern.Value); }

        protected override void Cover(BoolPattern pattern)
        {
            mCoveredBools[pattern.Value] = true;

            // note if we've covered all bools
            if (mCoveredBools.Count == 2) FullyCover();
        }

        private readonly Dictionary<bool, bool> mCoveredBools = new Dictionary<bool, bool>();
    }

    public class IntCoverage : Coverage
    {
        protected override bool Covers(IntPattern pattern) { return mCoveredInts.ContainsKey(pattern.Value); }

        protected override void Cover(IntPattern pattern)
        {
            mCoveredInts[pattern.Value] = true;
        }

        private readonly Dictionary<int, bool> mCoveredInts = new Dictionary<int, bool>();
    }

    public class StringCoverage : Coverage
    {
        protected override bool Covers(StringPattern pattern) { return mCoveredStrings.ContainsKey(pattern.Value); }

        protected override void Cover(StringPattern pattern)
        {
            mCoveredStrings[pattern.Value] = true;
        }

        private readonly Dictionary<string, bool> mCoveredStrings = new Dictionary<string, bool>();
    }

    //### bob: this is wrong. it needs to handle tuples combinatorially. the implementation below will
    // fail on:
    // match foo
    // case (true, true)
    // case (false, false)
    // end
    //
    // it will incorrectly think all possible cases are covered.
    public class TupleCoverage : Coverage
    {
        public TupleCoverage(BoundTupleType tupleType)
        {
            foreach (var field in tupleType.Fields)
            {
                mFields.Add(Coverage.Create(field));
            }
        }

        protected override bool Covers(TuplePattern pattern)
        {
            for (int i = 0; i < pattern.Fields.Count; i++)
            {
                if (!mFields[i].Covers(pattern.Fields[i])) return false;
            }

            // if we got here, all fields are covered
            return true;
        }

        protected override void Cover(TuplePattern pattern)
        {
            for (int i = 0; i < pattern.Fields.Count; i++)
            {
                mFields[i].Cover(pattern.Fields[i]);
            }
        }

        private readonly List<Coverage> mFields = new List<Coverage>();
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
            var coverage = Coverage.Create(boundValue.Type);

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
            bool covered = coverage.Covers(caseExpr);
            if (covered) throw new CompileException(caseExpr.Position,
                "This pattern will never be matched because previous patterns cover it.");

            // add coverage for this case
            coverage.Cover(caseExpr);

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
