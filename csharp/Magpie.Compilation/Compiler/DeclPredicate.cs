using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Traverses the tree of declarations in a type declaration.
    /// </summary>
    public class DeclPredicate : IDeclVisitor<bool>
    {
        /// <summary>
        /// Returns true if any of the declarations in the tree pass the predicate.
        /// </summary>
        public static bool Any(Decl decl, Func<Decl, bool> predicate)
        {
            return decl.Accept(new DeclPredicate(predicate, (a, b) => a || b));
        }

        /// <summary>
        /// Returns true if any of the declarations in the tree pass the predicate.
        /// </summary>
        public static bool Any(IEnumerable<Decl> decls, Func<Decl, bool> predicate)
        {
            foreach (Decl decl in decls)
            {
                if (decl.Accept(new DeclPredicate(predicate, (a, b) => a || b)))
                {
                    return true;
                }
            }

            return false;
        }

        #region IDeclVisitor Members

        bool IDeclVisitor<bool>.Visit(AnyType decl)
        {
            return mPredicate(decl);
        }

        bool IDeclVisitor<bool>.Visit(AtomicDecl decl)
        {
            return mPredicate(decl);
        }

        bool IDeclVisitor<bool>.Visit(ArrayType decl)
        {
            return mCombine(mPredicate(decl), decl.ElementType.Accept(this));
        }

        bool IDeclVisitor<bool>.Visit(FuncType decl)
        {
            bool result = mPredicate(decl);

            foreach (Decl paramType in decl.ParameterTypes)
            {
                result = mCombine(result, paramType.Accept(this));
            }

            return mCombine(result, decl.Return.Accept(this));
        }

        bool IDeclVisitor<bool>.Visit(NamedType decl)
        {
            bool result = mPredicate(decl);

            foreach (Decl typeArg in decl.TypeArgs)
            {
                result = mCombine(result, typeArg.Accept(this));
            }

            return result;
        }

        bool IDeclVisitor<bool>.Visit(TupleType decl)
        {
            bool result = mPredicate(decl);

            foreach (Decl field in decl.Fields)
            {
                result = mCombine(result, field.Accept(this));
            }

            return result;
        }

        #endregion

        private DeclPredicate(Func<Decl, bool> predicate, Func<bool, bool, bool> combine)
        {
            mPredicate = predicate;
            mCombine = combine;
        }

        Func<Decl, bool> mPredicate;
        Func<bool, bool, bool> mCombine;
    }
}
