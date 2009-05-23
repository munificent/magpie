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
                yield return new Intrinsic("Neg", OpCode.NegateInt, FuncType.Create(Decl.Int, Decl.Int));

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
                yield return new Intrinsic("Size", OpCode.StringSize, FuncType.Create(Decl.String, Decl.Int));
                yield return new Intrinsic("Substring", OpCode.Substring, FuncType.Create(Decl.String, Decl.Int, Decl.Int, Decl.String));

                yield return new Intrinsic("Size", OpCode.SizeArray, FuncType.Create(new BoundArrayType(new AnyType(), false), Decl.Int));
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

        public IBoundDecl[] ParameterTypes { get { return FuncType.ParameterTypes; } }

        //### bob: no generic intrinsics
        public IBoundDecl[] TypeArguments { get { return new IBoundDecl[0]; } }

        public bool HasInferrableTypeArguments { get { return false; } }

        #endregion
    }
}
