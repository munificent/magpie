using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Traverses the tree of declarations in a type declaration.
    /// </summary>
    public class DeclPredicate : IBoundDeclVisitor<bool>
    {
        /// <summary>
        /// Returns true if any of the declarations in the tree pass the predicate.
        /// </summary>
        public static bool Any(IBoundDecl decl, Func<IBoundDecl, bool> predicate)
        {
            return decl.Accept(new DeclPredicate(predicate, (a, b) => a || b));
        }

        /// <summary>
        /// Returns true if any of the declarations in the tree pass the predicate.
        /// </summary>
        public static bool Any(IEnumerable<IBoundDecl> decls, Func<IBoundDecl, bool> predicate)
        {
            foreach (var decl in decls)
            {
                if (decl.Accept(new DeclPredicate(predicate, (a, b) => a || b)))
                {
                    return true;
                }
            }

            return false;
        }

        #region IBoundDeclVisitor Members

        bool IBoundDeclVisitor<bool>.Visit(AnyType decl)
        {
            return mPredicate(decl);
        }

        bool IBoundDeclVisitor<bool>.Visit(AtomicDecl decl)
        {
            return mPredicate(decl);
        }

        bool IBoundDeclVisitor<bool>.Visit(BoundArrayType decl)
        {
            return mCombine(mPredicate(decl), decl.ElementType.Accept(this));
        }

        bool IBoundDeclVisitor<bool>.Visit(FuncType decl)
        {
            var result = mPredicate(decl);

            foreach (var paramType in decl.ParameterTypes)
            {
                result = mCombine(result, paramType.Accept(this));
            }

            return mCombine(result, decl.Return.Bound.Accept(this));
        }

        bool IBoundDeclVisitor<bool>.Visit(Struct decl)
        {
            return mPredicate(decl);
        }

        bool IBoundDeclVisitor<bool>.Visit(BoundTupleType decl)
        {
            var result = mPredicate(decl);

            foreach (var field in decl.Fields)
            {
                result = mCombine(result, field.Accept(this));
            }

            return result;
        }

        bool IBoundDeclVisitor<bool>.Visit(Union decl)
        {
            return mPredicate(decl);
        }

        #endregion

        private DeclPredicate(Func<IBoundDecl, bool> predicate, Func<bool, bool, bool> combine)
        {
            mPredicate = predicate;
            mCombine = combine;
        }

        Func<IBoundDecl, bool> mPredicate;
        Func<bool, bool, bool> mCombine;
    }
}
