using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// An intrinsic function is an operation in Magpie that has function syntax,
    /// but compiles down to native opcodes in the interpreter. In other words, an
    /// intrinsic could not otherwise be implemented in Magpie.
    /// </summary>
    public class IntrinsicExpr : IBoundExpr
    {
        public static IntrinsicExpr EqualInt(IBoundExpr arg)
        {
            var expr = new IntrinsicExpr(OpCode.EqualInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool));
            expr.mName = "=";
            expr.Arg = arg;

            return expr;
        }

        public static IntrinsicExpr Find(string name, IBoundExpr arg)
        {
            IntrinsicExpr func = null;

            //### bob: hackish
            string argString = arg.Type.ToString();
            switch (name + " " + argString)
            {
                case "Not Bool":            func = new IntrinsicExpr(OpCode.NegateBool, FuncType.Create(Decl.Bool, Decl.Bool)); break;
                case "Neg Int":             func = new IntrinsicExpr(OpCode.NegateInt, FuncType.Create(Decl.Int, Decl.Int)); break;

                case "= (Bool, Bool)":      func = new IntrinsicExpr(OpCode.EqualBool, FuncType.Create(Decl.Bool, Decl.Bool, Decl.Bool)); break;
                case "= (Int, Int)":        func = new IntrinsicExpr(OpCode.EqualInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool)); break;
                case "= (String, String)":  func = new IntrinsicExpr(OpCode.EqualString, FuncType.Create(Decl.String, Decl.String, Decl.Bool)); break;

                case "!= (Bool, Bool)":     func = new IntrinsicExpr(OpCode.EqualBool, OpCode.NegateBool, FuncType.Create(Decl.Bool, Decl.Bool, Decl.Bool)); break;
                case "!= (Int, Int)":       func = new IntrinsicExpr(OpCode.EqualInt, OpCode.NegateBool, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool)); break;
                case "!= (String, String)": func = new IntrinsicExpr(OpCode.EqualString, OpCode.NegateBool, FuncType.Create(Decl.String, Decl.String, Decl.Bool)); break;

                    //### bob: should short-circuit!
                case "| (Bool, Bool)":      func = new IntrinsicExpr(OpCode.OrBool, FuncType.Create(Decl.Bool, Decl.Bool, Decl.Bool)); break;
                case "& (Bool, Bool)":      func = new IntrinsicExpr(OpCode.AndBool, FuncType.Create(Decl.Bool, Decl.Bool, Decl.Bool)); break;

                case "< (Int, Int)":        func = new IntrinsicExpr(OpCode.LessInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool)); break;
                case "> (Int, Int)":        func = new IntrinsicExpr(OpCode.GreaterInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool)); break;
                case "<= (Int, Int)":       func = new IntrinsicExpr(OpCode.GreaterInt, OpCode.NegateBool, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool)); break;
                case ">= (Int, Int)":       func = new IntrinsicExpr(OpCode.LessInt, OpCode.NegateBool, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool)); break;

                case "+ (Int, Int)":        func = new IntrinsicExpr(OpCode.AddInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Int)); break;
                case "- (Int, Int)":        func = new IntrinsicExpr(OpCode.SubInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Int)); break;
                case "* (Int, Int)":        func = new IntrinsicExpr(OpCode.MultInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Int)); break;
                case "/ (Int, Int)":        func = new IntrinsicExpr(OpCode.DivInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Int)); break;
                
                case "String Bool":         func = new IntrinsicExpr(OpCode.BoolToString, FuncType.Create(Decl.Bool, Decl.String)); break;
                case "String Int":          func = new IntrinsicExpr(OpCode.IntToString, FuncType.Create(Decl.Int, Decl.String)); break;
                case "String String":       func = new IntrinsicExpr(FuncType.Create(Decl.String, Decl.String)); break;

                case "+ (String, String)":  func = new IntrinsicExpr(OpCode.AddString, FuncType.Create(Decl.String, Decl.String, Decl.String)); break;

                case "Print String":                    func = new IntrinsicExpr(OpCode.Print, FuncType.Create(Decl.String, Decl.Unit)); break;
                case "Size String":                     func = new IntrinsicExpr(OpCode.StringSize, FuncType.Create(Decl.String, Decl.Int)); break;
                case "Substring (String, Int, Int)":    func = new IntrinsicExpr(OpCode.Substring, FuncType.Create(Decl.String, Decl.Int, Decl.Int, Decl.String)); break;
            }

            // attach the argument if we succeeded
            if (func != null)
            {
                func.mName = name;
                func.Arg = arg;
            }

            return func;
        }

        public Decl Type { get { return mType.Return; } }
        public List<OpCode> OpCodes = new List<OpCode>();
        public IBoundExpr Arg;

        public IntrinsicExpr(OpCode opCode1, OpCode opCode2, FuncType type)
            : this(type)
        {
            OpCodes.Add(opCode1);
            OpCodes.Add(opCode2);
        }

        public IntrinsicExpr(OpCode opCode, FuncType type)
            : this(type)
        {
            OpCodes.Add(opCode);
        }

        public IntrinsicExpr(FuncType type)
        {
            mType = type;
        }

        public IntrinsicExpr(IntrinsicExpr cloneFrom, IBoundExpr arg)
        {
            mType = cloneFrom.mType;
            OpCodes = cloneFrom.OpCodes;
            Arg = arg;
        }

        public override string ToString()
        {
            return String.Format("{0} {1}", mName, Arg);
        }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        private FuncType mType;
        private string mName;
    }
}
