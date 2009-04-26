using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class DeclComparer : IDeclVisitor<bool>
    {
        public static bool Equals(Decl[] parameters, Decl[] arguments)
        {
            if (parameters.Length != arguments.Length) return false;

            for (int i = 0; i < parameters.Length; i++)
            {
                if (!Equals(parameters[i], arguments[i])) return false;
            }

            return true;
        }

        public static bool Equals(Decl parameter, Decl argument)
        {
            return argument.Accept(new DeclComparer(parameter));
        }

        #region IDeclVisitor Members

        public bool Visit(AtomicDecl decl)
        {
            // there is a single instance of each atomic type, so they must match exactly
            return ReferenceEquals(mParam, decl);
        }

        public bool Visit(ArrayType decl)
        {
            ArrayType array = mParam as ArrayType;

            if (array == null) return false;

            // element type must match
            return Equals(decl.ElementType, array.ElementType);
        }

        public bool Visit(FuncType decl)
        {
            FuncType paramFunc = mParam as FuncType;

            if (paramFunc == null) return false;

            // args must match
            if (paramFunc.Parameters.Count != decl.Parameters.Count) return false;

            for (int i = 0; i < paramFunc.Parameters.Count; i++)
            {
                if (!Equals(paramFunc.Parameters[i].Type, decl.Parameters[i].Type))
                {
                    return false;
                }
            }

            // return type must match
            return Equals(paramFunc.Return, decl.Return);
        }

        public bool Visit(NamedType decl)
        {
            NamedType named = mParam as NamedType;
            if (named == null) return false;

            //### bob: should this be checking the qualified name?
            if (named.Name != decl.Name) return false;

            // the type args must match
            if (named.TypeArgs.Length != decl.TypeArgs.Length) return false;
            return Equals(named.TypeArgs, decl.TypeArgs);
        }

        public bool Visit(TupleType decl)
        {
            TupleType paramTuple = mParam as TupleType;
            if (paramTuple == null) return false;

            if (paramTuple.Fields.Count != decl.Fields.Count) return false;

            // fields must match
            for (int i = 0; i < paramTuple.Fields.Count; i++)
            {
                if (!Equals(paramTuple.Fields[i], decl.Fields[i]))
                {
                    return false;
                }
            }

            return true;
        }

        #endregion

        private DeclComparer(Decl parameter)
        {
            mParam = parameter;
        }

        private Decl mParam;
    }
}
