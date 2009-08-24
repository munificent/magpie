﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Given a case, generates an expression that will evaluate to true if the case matches
    /// a given value.
    /// </summary>
    public class CaseDeclMatcher : ICaseExprVisitor<IUnboundExpr>
    {
        public static IUnboundExpr Match(BindingContext context, IBoundDecl decl,
            ICaseExpr caseExpr,
            IUnboundExpr value)
        {
            var matcher = new CaseDeclMatcher(context, decl, value);
            return caseExpr.Accept(matcher);
        }

        private CaseDeclMatcher(BindingContext context, IBoundDecl decl, IUnboundExpr value)
        {
            mContext = context;
            mDecl = decl;
            mValue = value;
        }

        #region ICaseExprVisitor<bool> Members

        IUnboundExpr ICaseExprVisitor<IUnboundExpr>.Visit(AnyCase expr)
        {
            // always succeed
            return new BoolExpr(expr.Position, true);
        }

        IUnboundExpr ICaseExprVisitor<IUnboundExpr>.Visit(LiteralCase expr)
        {
            if (expr.Type != mDecl) throw new CompileException(expr.Position,
                String.Format("Cannot match a value of type {0} against a literal case of type {1}.",
                    mDecl, expr.Type));

            var equals = new NameExpr(Position.None, "=");

            return new CallExpr(equals, new TupleExpr(mValue, expr.Value));
        }

        IUnboundExpr ICaseExprVisitor<IUnboundExpr>.Visit(UnionCaseCase expr)
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
                var matchValue = CaseDeclMatcher.Match(mContext, unionCase.ValueType.Bound, expr.Value, unionValue);

                if (matchValue != null)
                {
                    // combine with the previous check
                    matchExpr = new CallExpr(new NameExpr(Position.None, "&"),
                        new TupleExpr(matchExpr, matchValue));
                }
            }

            return matchExpr;
        }

        IUnboundExpr ICaseExprVisitor<IUnboundExpr>.Visit(TupleCase expr)
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
                var matchField = CaseDeclMatcher.Match(mContext, tupleDecl.Fields[i], expr.Fields[i], fieldValue);

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
        private IBoundDecl mDecl;
        private IUnboundExpr mValue;
    }
}