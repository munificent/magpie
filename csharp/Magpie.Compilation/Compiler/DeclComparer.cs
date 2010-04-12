using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class DeclComparer : IBoundDeclVisitor<bool>
    {
        public static bool TypesMatch(IBoundDecl parameter, IBoundDecl argument)
        {
            return argument.Accept(new DeclComparer(parameter));
        }

        #region IBoundDeclVisitor<bool> Members

        bool IBoundDeclVisitor<bool>.Visit(AtomicDecl decl)
        {
            // there is a single instance of each atomic type, so they must match exactly
            return ReferenceEquals(mParam, decl);
        }

        bool IBoundDeclVisitor<bool>.Visit(BoundArrayType decl)
        {
            var array = mParam as BoundArrayType;

            if (array == null) return false;

            // element type must match
            return TypesMatch(decl.ElementType, array.ElementType);
        }

        bool IBoundDeclVisitor<bool>.Visit(FuncType decl)
        {
            var paramFunc = mParam as FuncType;

            if (paramFunc == null) return false;

            // arg must match
            if (!TypesMatch(paramFunc.Parameter.Bound, decl.Parameter.Bound)) return false;

            // return type must match
            return TypesMatch(paramFunc.Return.Bound, decl.Return.Bound);
        }

        bool IBoundDeclVisitor<bool>.Visit(BoundRecordType decl)
        {
            var paramRecord = mParam as BoundRecordType;
            if (paramRecord == null) return false;

            if (paramRecord.Fields.Count != decl.Fields.Count) return false;

            // fields must match
            foreach (var pair in paramRecord.Fields.Zip(decl.Fields))
            {
                if (pair.Item1.Key != pair.Item2.Key) return false;
                if (!TypesMatch(pair.Item1.Value, pair.Item2.Value)) return false;
            }

            return true;
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

        bool IBoundDeclVisitor<bool>.Visit(ForeignType decl)
        {
            // should be a foreign type with the same name
            var foreignParam = mParam as ForeignType;
            if (foreignParam == null) return false;

            return foreignParam.Name == decl.Name;
        }

        #endregion

        private DeclComparer(IBoundDecl parameter)
        {
            mParam = parameter;
        }

        private IBoundDecl mParam;
    }
}
