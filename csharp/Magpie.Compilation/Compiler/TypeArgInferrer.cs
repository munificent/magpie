using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class TypeArgInferrer : IDeclVisitor<bool>
    {
        /// <summary>
        /// Infers the types of the given collection of named type arguments from the given collection
        /// of parameters.
        /// </summary>
        /// <param name="typeArgNames">The names of the type parameters.</param>
        /// <param name="parameters"></param>
        /// <returns></returns>
        public static IList<Decl> Infer(Function generic, IEnumerable<Decl> argTypes)
        {
            var inferrer = new TypeArgInferrer(generic);

            // go through the args, inferring the type arguments
            Decl[] argTypeArray = argTypes.ToArray();

            if (argTypeArray.Length != generic.FuncType.Parameters.Count) return null;

            for (int i = 0; i < argTypeArray.Length; i++)
            {
                inferrer.mParamTypes.Push(generic.FuncType.Parameters[i].Type);
                bool dummy = argTypeArray[i].Accept(inferrer);
                inferrer.mParamTypes.Pop();
            }

            // Foo[A, B, C] (arg1 A, arg2 (int, B), arg3 (bool, (C, int)) ->)
            // Foo          (   "a",      (432, 2),      (true, (4, 123)) ->)

            // if the inference failed (like from a type collision) then fail
            if (inferrer.mFailed) return null;

            // if any type argument is left unfilled then fail
            if (inferrer.mTypeArguments.Contains(null)) return null;

            return inferrer.mTypeArguments;
        }

        #region IDeclVisitor<bool> Members

        public bool Visit(AnyType decl)
        {
            throw new NotImplementedException();
        }

        public bool Visit(ArrayType decl)
        {
            throw new NotImplementedException();
        }

        public bool Visit(AtomicDecl decl)
        {
            TryInferParam(decl);
            return false;
        }

        public bool Visit(FuncType decl)
        {
            if (!TryInferParam(decl))
            {
                FuncType paramFunc = mParamTypes.Peek() as FuncType;
                if (paramFunc == null)
                {
                    mFailed = true;
                    return false;
                }

                // .ToArray() is to make sure the result is fully enumerated
                if (decl.Parameters.Count != paramFunc.Parameters.Count)
                {
                    mFailed = true;
                    return false;
                }

                for (int i = 0; i < decl.Parameters.Count; i++)
                {
                    mParamTypes.Push(paramFunc.Parameters[i].Type);
                    decl.Parameters[i].Type.Accept(this);
                    mParamTypes.Pop();
                }

                mParamTypes.Push(paramFunc.Return);
                decl.Return.Accept(this);
                mParamTypes.Pop();
            }

            return false;
        }

        public bool Visit(NamedType decl)
        {
            if (!TryInferParam(decl))
            {
                NamedType paramDecl = mParamTypes.Peek() as NamedType;
                if (paramDecl == null)
                {
                    mFailed = true;
                    return false;
                }

                int paramTypeArgs = (paramDecl.TypeArgs == null) ? 0 : paramDecl.TypeArgs.Length;
                int argTypeArgs = (decl.TypeArgs == null) ? 0 : decl.TypeArgs.Length;

                if (paramTypeArgs != argTypeArgs)
                {
                    mFailed = true;
                    return false;
                }

                for (int i = 0; i < paramTypeArgs; i++)
                {
                    mParamTypes.Push(paramDecl.TypeArgs[i]);
                    decl.TypeArgs[i].Accept(this);
                    mParamTypes.Pop();
                }
            }

            return false;
        }

        public bool Visit(TupleType decl)
        {
            if (!TryInferParam(decl))
            {
                TupleType paramDecl = mParamTypes.Peek() as TupleType;
                if (paramDecl == null)
                {
                    mFailed = true;
                    return false;
                }

                if (paramDecl.Fields.Count != decl.Fields.Count)
                {
                    mFailed = true;
                    return false;
                }

                for (int i = 0; i < paramDecl.Fields.Count; i++)
                {
                    mParamTypes.Push(paramDecl.Fields[i]);
                    decl.Fields[i].Accept(this);
                    mParamTypes.Pop();
                }
            }

            return false;
        }

        #endregion

        private TypeArgInferrer(Function generic)
        {
            mGeneric = generic;

            // make an empty list with slots for each filled argument
            mTypeArguments = new List<Decl>(mGeneric.TypeParameters.Count);
            for (int i = 0; i < mGeneric.TypeParameters.Count; i++)
            {
                mTypeArguments.Add(null);
            }

            mTypeParamNames = new List<string>();
            foreach (NamedType decl in mGeneric.TypeParameters.Cast<NamedType>())
            {
                mTypeParamNames.Add(decl.Name);
            }
        }

        private bool TryInferParam(Decl argType)
        {
            // see if the parameter type on top of the stack is a generic type
            NamedType named = mParamTypes.Peek() as NamedType;
            if (named == null) return false;

            // see if the named type is a generic type (instead of an actual concrete named type)
            //### bob: a separate naming convention and Decl type for type parameters would make this
            //         a little simpler.
            int typeParamIndex = mTypeParamNames.IndexOf(named.Name);
            if (typeParamIndex == -1) return false;

            // it is, so infer it from the arg
            if (mTypeArguments[typeParamIndex] == null)
            {
                // first time inferring the arg
                mTypeArguments[typeParamIndex] = argType;
            }
            else
            {
                // already inferred, make sure it matches
                if (!DeclComparer.TypesMatch(mTypeArguments[typeParamIndex], argType))
                {
                    // can't infer the same type parameter to multiple different types
                    // example: Foo[A] (a A, b A)
                    //          Foo    (123, "string")
                    mFailed = true;
                }
            }

            return true;
        }

        private readonly Function mGeneric;

        private readonly List<string> mTypeParamNames;
        private readonly List<Decl> mTypeArguments;

        private bool mFailed;

        /// <summary>
        /// The current location while walking the parameter type tree. Used to keep the arg
        /// walk in sync with the corresponding param walk.
        /// </summary>
        private readonly Stack<Decl> mParamTypes = new Stack<Decl>();
    }
}
