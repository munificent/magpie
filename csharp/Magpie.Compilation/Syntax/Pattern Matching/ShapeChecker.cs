using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Compares patterns to the type of the value being matched to ensure
    /// that the patterns are valid. For example, prevents matching a bool
    /// against an int literal.
    /// </summary>
    public class ShapeChecker : IPatternVisitor<bool>
    {
        public static void Validate(MatchExpr expr, IBoundDecl valueType)
        {
            foreach (var matchCase in expr.Cases)
            {
                var usedVariables = new Dictionary<string, bool>();
                matchCase.Pattern.Accept(new ShapeChecker(valueType, usedVariables));
            }
        }

        private ShapeChecker(IBoundDecl type, IDictionary<string, bool> usedVariables)
        {
            mType = type;
            mUsedVariables = usedVariables;
        }

        #region IPatternVisitor<bool> Members

        bool IPatternVisitor<bool>.Visit(AnyPattern expr)
        {
            // an any pattern matches any type
            return true;
        }

        bool IPatternVisitor<bool>.Visit(BoolPattern expr)
        {
            if (mType != Decl.Bool) throw new CompileException(expr.Position,
                String.Format("A bool pattern cannot be used to match against a value of type {0}.", mType));
            return true;
        }

        bool IPatternVisitor<bool>.Visit(IntPattern expr)
        {
            if (mType != Decl.Int) throw new CompileException(expr.Position,
                String.Format("An int pattern cannot be used to match against a value of type {0}.", mType));
            return true;
        }

        bool IPatternVisitor<bool>.Visit(StringPattern expr)
        {
            if (mType != Decl.String) throw new CompileException(expr.Position,
                String.Format("A string pattern cannot be used to match against a value of type {0}.", mType));
            return true;
        }

        bool IPatternVisitor<bool>.Visit(UnionPattern expr)
        {
            var union = mType as Union;

            // it must be a union
            if (union == null) throw new CompileException(expr.Position,
                String.Format("A union case pattern cannot be used to match against a value of type {0}.", mType));

            // and it must be a valid case
            var unionCase = union.Cases.FirstOrDefault(it => it.Name == expr.Name);
            if (unionCase == null) throw new CompileException(expr.Position,
                String.Format("Union type {0} does not have a case named {1}.", union.Name, expr.Name));

            // and the value must match
            if (unionCase.ValueType.Bound == Decl.Unit)
            {
                if (expr.Value != null) throw new CompileException(expr.Position,
                    String.Format("Union case {0} does not expect a value.", unionCase.Name));
            }
            else
            {
                if (expr.Value == null) throw new CompileException(expr.Position,
                    String.Format("Union case {0} expects a value.", unionCase.Name));

                expr.Value.Accept(new ShapeChecker(unionCase.ValueType.Bound, mUsedVariables));
            }

            return true;
        }

        bool IPatternVisitor<bool>.Visit(TuplePattern expr)
        {
            var tuple = mType as BoundTupleType;

            // it must be a tuple
            if (tuple == null) throw new CompileException(expr.Position,
                String.Format("A tuple pattern cannot be used to match against a value of type {0}.", mType));

            // if must be the right length
            if (tuple.Fields.Count != expr.Fields.Count) throw new CompileException(expr.Position,
                String.Format("The tuple pattern has {0} fields when {1} are expected.", expr.Fields.Count, tuple.Fields.Count));

            // the fields must match for
            for (int i = 0; i < expr.Fields.Count; i++)
            {
                expr.Fields[i].Accept(new ShapeChecker(tuple.Fields[i], mUsedVariables));
            }

            return true;
        }

        bool IPatternVisitor<bool>.Visit(VariablePattern expr)
        {
            // make sure the pattern is linear
            if (mUsedVariables.ContainsKey(expr.Name)) throw new CompileException(
                expr.Position, "The variable \"" + expr.Name + "\" cannot appear more than once in a single pattern.");

            mUsedVariables[expr.Name] = true;

            // a variable pattern matches any type
            return true;
        }

        #endregion

        private readonly IBoundDecl mType;
        private readonly IDictionary<string, bool> mUsedVariables;
    }
}
