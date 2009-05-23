using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Walks a case tree to make sure its shape can be matched against a value of the
    /// given type.
    /// </summary>
    public class CaseDeclMatcher : ICaseExprVisitor<bool>
    {
        public static bool Matches(Compiler compiler, IBoundDecl decl, ICaseExpr caseExpr)
        {
            var matcher = new CaseDeclMatcher(compiler, decl);
            return caseExpr.Accept(matcher);
        }

        private CaseDeclMatcher(Compiler compiler, IBoundDecl decl)
        {
            mCompiler = compiler;
            mDecl = decl;
        }

        #region ICaseExprVisitor<bool> Members

        bool ICaseExprVisitor<bool>.Visit(AnyCase expr)
        {
            throw new NotImplementedException();
        }

        bool ICaseExprVisitor<bool>.Visit(BoolCase expr)
        {
            return mDecl == Decl.Bool;
        }

        bool ICaseExprVisitor<bool>.Visit(IntCase expr)
        {
            return mDecl == Decl.Int;
        }

        bool ICaseExprVisitor<bool>.Visit(StringCase expr)
        {
            return mDecl == Decl.String;
        }

        bool ICaseExprVisitor<bool>.Visit(NameCase expr)
        {
            throw new NotImplementedException();
            /*
            // the decl must be a named type
            NamedType named = mDecl as NamedType;

            if (named == null) return false;

            //### bob: if we bind decls so that union is a real decl, we won't have
            //         to look it up from the compiler any more
            
            // find the union
            Union union = mCompiler.FindUnion(named);
            if (union == null) return false;

            // the name must be a case in the union
            foreach (var unionCase in union.Cases)
            {
                //### bob: what about the value?
                if (unionCase.Name == expr.Name) return true;
            }

            return false;
             */
        }

        bool ICaseExprVisitor<bool>.Visit(CallCase expr)
        {
            throw new NotImplementedException();
        }

        bool ICaseExprVisitor<bool>.Visit(TupleCase expr)
        {
            var tuple = mDecl as BoundTupleType;

            if (tuple == null) return false;
            if (tuple.Fields.Count != expr.Fields.Count) return false;

            // fields must match
            for (int i = 0; i < tuple.Fields.Count; i++)
            {
                if (!CaseDeclMatcher.Matches(mCompiler, tuple.Fields[i], expr.Fields[i])) return false;
            }

            return true;
        }

        #endregion

        private Compiler mCompiler;
        private IBoundDecl mDecl;
    }
}
