using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class TypeArgInferrer : IBoundDeclVisitor<bool>
    {
        /// <summary>
        /// Infers the types of the given collection of named type arguments from the given collection
        /// of parameters.
        /// </summary>
        /// <param name="typeArgNames">The names of the type parameters.</param>
        /// <param name="parameters"></param>
        /// <returns></returns>
        public static IList<IBoundDecl> Infer(IEnumerable<string> typeParameters,
            IUnboundDecl parameterType,
            IBoundDecl argType)
        {
            var inferrer = new TypeArgInferrer(typeParameters);

            inferrer.mParamTypes.Push(parameterType);
            bool dummy = argType.Accept(inferrer);
            inferrer.mParamTypes.Pop();

            // if the inference failed (like from a type collision) then fail
            if (inferrer.mFailed) return null;

            // if any type argument is left unfilled then fail
            if (inferrer.mTypeArguments.Contains(null)) return null;

            return inferrer.mTypeArguments;
        }

        #region IBoundDeclVisitor<bool> Members

        bool IBoundDeclVisitor<bool>.Visit(BoundArrayType decl)
        {
            if (!TryInferParam(decl))
            {
                var paramDecl = ParamType as ArrayType;
                if (paramDecl == null)
                {
                    mFailed = true;
                    return false;
                }

                mParamTypes.Push(paramDecl.ElementType);
                decl.ElementType.Accept(this);
                mParamTypes.Pop();
            }

            return false;
        }

        bool IBoundDeclVisitor<bool>.Visit(AtomicDecl decl)
        {
            TryInferParam(decl);
            return false;
        }

        bool IBoundDeclVisitor<bool>.Visit(FuncType decl)
        {
            if (!TryInferParam(decl))
            {
                FuncType paramFunc = ParamType as FuncType;
                if (paramFunc == null)
                {
                    mFailed = true;
                    return false;
                }

                mParamTypes.Push(paramFunc.Parameter.Unbound);
                decl.Parameter.Bound.Accept(this);
                mParamTypes.Pop();

                mParamTypes.Push(paramFunc.Return.Unbound);
                decl.Return.Bound.Accept(this);
                mParamTypes.Pop();
            }

            return false;
        }

        bool IBoundDeclVisitor<bool>.Visit(BoundTupleType decl)
        {
            if (!TryInferParam(decl))
            {
                TupleType paramDecl = ParamType as TupleType;
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

        bool IBoundDeclVisitor<bool>.Visit(Struct decl)
        {
            return InferNamedType(decl);
        }

        bool IBoundDeclVisitor<bool>.Visit(Union decl)
        {
            return InferNamedType(decl);
        }

        #endregion

        private IUnboundDecl ParamType
        {
            get { return mParamTypes.Peek(); }
        }

        private TypeArgInferrer(IEnumerable<string> typeParameters)
        {
            // make an empty list with slots for each filled argument
            mTypeArguments = new List<IBoundDecl>(typeParameters.Select(p => (IBoundDecl)null));
            mTypeParamNames = new List<string>(typeParameters);
        }

        private bool InferNamedType(INamedType decl)
        {
            if (!TryInferParam(decl))
            {
                var named = ParamType as NamedType;
                if (named == null)
                {
                    mFailed = true;
                    return false;
                }

                if (named.TypeArgs.Length != decl.TypeArguments.Length)
                {
                    mFailed = true;
                    return false;
                }

                for (int i = 0; i < named.TypeArgs.Length; i++)
                {
                    mParamTypes.Push(named.TypeArgs[i]);
                    decl.TypeArguments[i].Accept(this);
                    mParamTypes.Pop();
                }
            }

            return false;
        }

        private bool TryInferParam(IBoundDecl argType)
        {
            // see if the parameter type on top of the stack is a generic type
            NamedType named = ParamType as NamedType;
            if (named == null) return false;

            // see if the named type is a generic type (instead of an actual concrete named type)
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
                    // example: Foo'A (a A, b A)
                    //          Foo    (123, "string")
                    mFailed = true;
                }
            }

            return true;
        }

        private readonly List<string> mTypeParamNames;
        private readonly List<IBoundDecl> mTypeArguments;

        private bool mFailed;

        /// <summary>
        /// The current location while walking the parameter type tree. Used to keep the arg
        /// walk in sync with the corresponding param walk.
        /// </summary>
        private readonly Stack<IUnboundDecl> mParamTypes = new Stack<IUnboundDecl>();
    }
}
