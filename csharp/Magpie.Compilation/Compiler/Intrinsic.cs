using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Defines an intrinsic function. An intrinsic is a "function" that is supported directly
    /// by opcodes in the virtual machine. Unlike other functions, instrinsics don't compile
    /// to a call and return. Instead, their opcodes are inserted directly inline.
    /// </summary>
    public class Intrinsic : ICallable
    {
        public static IEnumerable<ICallable> All
        {
            get
            {
                yield return new Intrinsic("Not", OpCode.NegateBool, FuncType.Create(Decl.Bool, Decl.Bool));
                yield return new Intrinsic("Negate", OpCode.NegateInt, FuncType.Create(Decl.Int, Decl.Int));

                yield return new Intrinsic("=", OpCode.EqualBool, FuncType.Create(Decl.Bool, Decl.Bool, Decl.Bool));
                yield return new Intrinsic("=", OpCode.EqualInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool));
                yield return new Intrinsic("=", OpCode.EqualString, FuncType.Create(Decl.String, Decl.String, Decl.Bool));

                yield return new Intrinsic("!=", OpCode.EqualBool, OpCode.NegateBool, FuncType.Create(Decl.Bool, Decl.Bool, Decl.Bool));
                yield return new Intrinsic("!=", OpCode.EqualInt, OpCode.NegateBool, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool));
                yield return new Intrinsic("!=", OpCode.EqualString, OpCode.NegateBool, FuncType.Create(Decl.String, Decl.String, Decl.Bool));

                //### bob: should short-circuit!
                yield return new Intrinsic("|", OpCode.OrBool, FuncType.Create(Decl.Bool, Decl.Bool, Decl.Bool));
                yield return new Intrinsic("&", OpCode.AndBool, FuncType.Create(Decl.Bool, Decl.Bool, Decl.Bool));

                yield return new Intrinsic("<", OpCode.LessInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool));
                yield return new Intrinsic(">", OpCode.GreaterInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool));
                yield return new Intrinsic("<=", OpCode.GreaterInt, OpCode.NegateBool, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool));
                yield return new Intrinsic(">=", OpCode.LessInt, OpCode.NegateBool, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool));

                yield return new Intrinsic("+", OpCode.AddInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Int));
                yield return new Intrinsic("-", OpCode.SubInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Int));
                yield return new Intrinsic("*", OpCode.MultInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Int));
                yield return new Intrinsic("/", OpCode.DivInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Int));

                yield return new Intrinsic("String", OpCode.BoolToString, FuncType.Create(Decl.Bool, Decl.String));
                yield return new Intrinsic("String", OpCode.IntToString, FuncType.Create(Decl.Int, Decl.String));
                yield return new Intrinsic("String", FuncType.Create(Decl.String, Decl.String));

                yield return new Intrinsic("+", OpCode.AddString, FuncType.Create(Decl.String, Decl.String, Decl.String));

                yield return new Intrinsic("Print", OpCode.Print, FuncType.Create(Decl.String, Decl.Unit));
                yield return new Intrinsic("Count", OpCode.StringCount, FuncType.Create(Decl.String, Decl.Int));
                yield return new Intrinsic("Substring", OpCode.Substring, FuncType.Create(Decl.String, Decl.Int, Decl.Int, Decl.String));

                yield return new Intrinsic("Math:Random", OpCode.Random, FuncType.Create(Decl.Int, Decl.Int));
            }
        }

        public static IEnumerable<IGenericCallable> AllGenerics
        {
            get
            {
                yield return new GetArrayElement();
                yield return new SetArrayElement();
                yield return new ArraySize();
            }
        }

        public static IBoundExpr EqualInt(IBoundExpr left, IBoundExpr right)
        {
            var intrinsic = new Intrinsic("=", OpCode.EqualInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool));
            return intrinsic.CreateCall(new BoundTupleExpr(new IBoundExpr[] { left, right }));
        }

        public string Name { get; private set; }
        public FuncType FuncType { get; private set; }
        public IBoundDecl Type { get { return FuncType.Return.Bound; } }
        public List<OpCode> OpCodes = new List<OpCode>();

        private Intrinsic(string name, OpCode opCode1, OpCode opCode2, FuncType type)
            : this(name, type)
        {
            OpCodes.Add(opCode1);
            OpCodes.Add(opCode2);
        }

        private Intrinsic(string name, OpCode opCode, FuncType type)
            : this(name, type)
        {
            OpCodes.Add(opCode);
        }

        private Intrinsic(string name, FuncType type)
        {
            Name = name;
            FuncType = type;
        }

        #region ICallable Members

        public IBoundExpr CreateCall(IBoundExpr arg)
        {
            //### bob: constant folding goes here...

            return new IntrinsicExpr(this, arg);
        }

        public IBoundDecl ParameterType { get { return FuncType.Parameter.Bound; } }

        public IBoundDecl[] TypeArguments { get { return new IBoundDecl[0]; } }

        public bool HasInferrableTypeArguments { get { return false; } }

        #endregion

        #region Generic intrinsics

        private class GetArrayElement : IGenericCallable
        {
            #region IGenericCallable Members

            public string Name { get { return "__Call"; } }

            public ICallable Instantiate(BindingContext context, IEnumerable<IBoundDecl> typeArgs,
                IBoundDecl argType)
            {
                // should have two args: an int and an array
                var argTuple = argType as BoundTupleType;
                if (argTuple == null) return null;

                if (argTuple.Fields[0] != Decl.Int) return null;

                var arrayType = argTuple.Fields[1] as BoundArrayType;
                if (arrayType == null) return null;

                // make the intrinsic
                return new Intrinsic("__Call", OpCode.LoadArray, FuncType.Create(argTuple, arrayType.ElementType));
            }

            #endregion
        }

        private class SetArrayElement : IGenericCallable
        {
            #region IGenericCallable Members

            public string Name { get { return "__Call<-"; } }

            public ICallable Instantiate(BindingContext context, IEnumerable<IBoundDecl> typeArgs,
                IBoundDecl argType)
            {
                // should have three args: an int, an array, and a value
                var argTuple = argType as BoundTupleType;
                if (argTuple == null) return null;

                // index
                if (argTuple.Fields[0] != Decl.Int) return null;

                // array
                var arrayType = argTuple.Fields[1] as BoundArrayType;
                if (arrayType == null) return null;

                // value
                if (!DeclComparer.TypesMatch(argTuple.Fields[2], arrayType.ElementType)) return null;

                // make the intrinsic
                return new Intrinsic("__Call<-", OpCode.StoreArray, FuncType.Create(argTuple, Decl.Unit));
            }

            #endregion
        }

        private class ArraySize : IGenericCallable
        {
            #region IGenericCallable Members

            public string Name { get { return "Size"; } }

            public ICallable Instantiate(BindingContext context, IEnumerable<IBoundDecl> typeArgs,
                IBoundDecl argType)
            {
                var arrayType = argType as BoundArrayType;
                if (arrayType == null) return null;

                // make the intrinsic
                return new Intrinsic("Size", OpCode.SizeArray, FuncType.Create(arrayType, Decl.Int));
            }

            #endregion
        }

        #endregion
    }
}
