using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class DeclComparer : IBoundDeclVisitor<bool>
    {
        public static bool TypesMatch(IBoundDecl[] parameters, IBoundDecl[] arguments)
        {
            if (parameters.Length != arguments.Length) return false;

            for (int i = 0; i < parameters.Length; i++)
            {
                if (!TypesMatch(parameters[i], arguments[i])) return false;
            }

            return true;
        }

        public static bool TypesMatch(IBoundDecl parameter, IBoundDecl argument)
        {
            if (parameter is AnyType) return true;

            return argument.Accept(new DeclComparer(parameter));
        }

        #region IBoundDeclVisitor<bool> Members

        bool IBoundDeclVisitor<bool>.Visit(AnyType decl)
        {
            return true;
        }

        bool IBoundDeclVisitor<bool>.Visit(AtomicDecl decl)
        {
            // there is a single instance of each atomic type, so they must match exactly
            return ReferenceEquals(mParam, decl);
        }

        bool IBoundDeclVisitor<bool>.Visit(BoundArrayType decl)
        {
            var array = mParam as BoundArrayType;

            if (array == null) return false;

            // mutability must match
            //### bob: are there cases where we want to let this slide?
            //         can you pass a mutable array everywhere an immutable one is allowed?
            if (decl.IsMutable != array.IsMutable) return false;

            // element type must match
            return TypesMatch(decl.ElementType, array.ElementType);
        }

        bool IBoundDeclVisitor<bool>.Visit(FuncType decl)
        {
            var paramFunc = mParam as FuncType;

            if (paramFunc == null) return false;

            // args must match
            if (paramFunc.Parameters.Count != decl.Parameters.Count) return false;

            for (int i = 0; i < paramFunc.Parameters.Count; i++)
            {
                if (!TypesMatch(paramFunc.Parameters[i].Type.Bound, decl.Parameters[i].Type.Bound))
                {
                    return false;
                }
            }

            // return type must match
            return TypesMatch(paramFunc.Return.Bound, decl.Return.Bound);
        }

        bool IBoundDeclVisitor<bool>.Visit(BoundTupleType decl)
        {
            var paramTuple = mParam as BoundTupleType;
            if (paramTuple == null) return false;

            if (paramTuple.Fields.Count != decl.Fields.Count) return false;

            // fields must match
            for (int i = 0; i < paramTuple.Fields.Count; i++)
            {
                if (!TypesMatch(paramTuple.Fields[i], decl.Fields[i]))
                {
                    return false;
                }
            }

            return true;
        }

        bool IBoundDeclVisitor<bool>.Visit(Struct decl)
        {
            // should be same structure
            return ReferenceEquals(mParam, decl);
        }

        bool IBoundDeclVisitor<bool>.Visit(Union decl)
        {
            // should be same structure
            return ReferenceEquals(mParam, decl);
        }

        #endregion

        private DeclComparer(IBoundDecl parameter)
        {
            mParam = parameter;
        }

        private IBoundDecl mParam;
    }
}
