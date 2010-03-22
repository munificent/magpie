using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Keeps track of which values are matched by a series of pattern cases. Used to
    /// determine if a case will never match because previous cases will cover all
    /// possible matched values, or if there are potential values that will not be
    /// matched by any case.
    /// </summary>
    public class Coverage : IPatternVisitor<bool>
    {
        public static void Validate(MatchExpr expr, IBoundDecl valueType)
        {
            var coverage = new Coverage(valueType);

            foreach (var matchCase in expr.Cases)
            {
                if (coverage.Cover(matchCase.Pattern)) throw new CompileException(matchCase.Position,
                    "This pattern will never be matched because previous patterns cover it.");
            }

            // make sure the cases are exhaustive
            if (!coverage.FullyCovered)
            {
                throw new CompileException(expr.Position, "Not all possible values will be matched.");
            }
        }

        private Coverage(IBoundDecl matchType)
        {
            mMatchType = matchType;

            // since unit has exactly one value, if we've got a coverage for it
            // at all, it's fully covered. this handles union cases where the
            // value type is unit. the presence of the union case at all means
            // it's fully-covered.
            if (mMatchType == Decl.Unit) FullyCovered = true;
        }

        private bool FullyCovered { get; set; }

        /// <summary>
        /// Gets whether or not the given pattern is covered and will not
        /// be matched. If it can be, adds the pattern to the set of covered values.
        /// </summary>
        /// <param name="pattern">The pattern being matched.</param>
        /// <returns><c>true</c> if the pattern could be matched, <c>false</c>
        /// if a previous patterns cover it.</returns>
        private bool Cover(IPattern pattern)
        {
            if (FullyCovered) return true;
            return pattern.Accept(this);
        }

        private bool CoverName(string name)
        {
            if (mCoveredValues.ContainsKey(name)) return true;

            mCoveredValues[name] = null;
            return false;
        }

        #region IPatternVisitor<bool> Members

        bool IPatternVisitor<bool>.Visit(AnyPattern expr)
        {
            FullyCovered = true;
            return false;
        }

        bool IPatternVisitor<bool>.Visit(BoolPattern expr)
        {
            var result = CoverName(expr.Value.ToString());

            // if we got true and false, we're fully covered
            if (mCoveredValues.Count == 2) FullyCovered = true;

            return result;
        }

        bool IPatternVisitor<bool>.Visit(IntPattern expr)
        {
            return CoverName(expr.Value.ToString());
        }

        bool IPatternVisitor<bool>.Visit(StringPattern expr)
        {
            return CoverName(expr.Value.ToString());
        }

        bool IPatternVisitor<bool>.Visit(UnionPattern expr)
        {
            // if this union case has never been matched, add it
            bool covered = false;
            if (!mCoveredValues.ContainsKey(expr.Name))
            {
                var matchedCase = ((Union)mMatchType).Cases.First(unionCase => unionCase.Name == expr.Name);
                mCoveredValues[expr.Name] = new Coverage(matchedCase.ValueType.Bound);
            }
            else
            {
                // union case already present, so we may be covered
                covered = true;
            }

            // if we have a value for the case, recurse in to cover it
            if (expr.Value != null)
            {
                covered = mCoveredValues[expr.Name].Cover(expr.Value);
            }

            // if we have all cases, and all of their values are covered, we're covered
            if ((mCoveredValues.Count == ((Union)mMatchType).Cases.Count) &&
                mCoveredValues.Values.All(cover => cover.FullyCovered))
            {
                FullyCovered = true;
            }

            return covered;
        }

        bool IPatternVisitor<bool>.Visit(TuplePattern expr)
        {
            throw new NotImplementedException();
        }

        #endregion

        private IBoundDecl mMatchType;
        private Dictionary<string, Coverage> mCoveredValues = new Dictionary<string, Coverage>();
    }
}
