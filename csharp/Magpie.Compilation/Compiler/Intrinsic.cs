using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class Intrinsic : ICallable
    {
        public static Intrinsic Find(string name, IBoundExpr arg)
        {
            Intrinsic intrinsic = null;

            //### bob: hackish
            string argString = arg.Type.ToString();
            switch (name + " " + argString)
            {
                case "Not Bool": intrinsic = new Intrinsic(OpCode.NegateBool, FuncType.Create(Decl.Bool, Decl.Bool)); break;
                case "Neg Int": intrinsic = new Intrinsic(OpCode.NegateInt, FuncType.Create(Decl.Int, Decl.Int)); break;

                case "= (Bool, Bool)": intrinsic = new Intrinsic(OpCode.EqualBool, FuncType.Create(Decl.Bool, Decl.Bool, Decl.Bool)); break;
                case "= (Int, Int)": intrinsic = new Intrinsic(OpCode.EqualInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool)); break;
                case "= (String, String)": intrinsic = new Intrinsic(OpCode.EqualString, FuncType.Create(Decl.String, Decl.String, Decl.Bool)); break;

                case "!= (Bool, Bool)": intrinsic = new Intrinsic(OpCode.EqualBool, OpCode.NegateBool, FuncType.Create(Decl.Bool, Decl.Bool, Decl.Bool)); break;
                case "!= (Int, Int)": intrinsic = new Intrinsic(OpCode.EqualInt, OpCode.NegateBool, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool)); break;
                case "!= (String, String)": intrinsic = new Intrinsic(OpCode.EqualString, OpCode.NegateBool, FuncType.Create(Decl.String, Decl.String, Decl.Bool)); break;

                //### bob: should short-circuit!
                case "| (Bool, Bool)": intrinsic = new Intrinsic(OpCode.OrBool, FuncType.Create(Decl.Bool, Decl.Bool, Decl.Bool)); break;
                case "& (Bool, Bool)": intrinsic = new Intrinsic(OpCode.AndBool, FuncType.Create(Decl.Bool, Decl.Bool, Decl.Bool)); break;

                case "< (Int, Int)": intrinsic = new Intrinsic(OpCode.LessInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool)); break;
                case "> (Int, Int)": intrinsic = new Intrinsic(OpCode.GreaterInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool)); break;
                case "<= (Int, Int)": intrinsic = new Intrinsic(OpCode.GreaterInt, OpCode.NegateBool, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool)); break;
                case ">= (Int, Int)": intrinsic = new Intrinsic(OpCode.LessInt, OpCode.NegateBool, FuncType.Create(Decl.Int, Decl.Int, Decl.Bool)); break;

                case "+ (Int, Int)": intrinsic = new Intrinsic(OpCode.AddInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Int)); break;
                case "- (Int, Int)": intrinsic = new Intrinsic(OpCode.SubInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Int)); break;
                case "* (Int, Int)": intrinsic = new Intrinsic(OpCode.MultInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Int)); break;
                case "/ (Int, Int)": intrinsic = new Intrinsic(OpCode.DivInt, FuncType.Create(Decl.Int, Decl.Int, Decl.Int)); break;

                case "String Bool": intrinsic = new Intrinsic(OpCode.BoolToString, FuncType.Create(Decl.Bool, Decl.String)); break;
                case "String Int": intrinsic = new Intrinsic(OpCode.IntToString, FuncType.Create(Decl.Int, Decl.String)); break;
                case "String String": intrinsic = new Intrinsic(FuncType.Create(Decl.String, Decl.String)); break;

                case "+ (String, String)": intrinsic = new Intrinsic(OpCode.AddString, FuncType.Create(Decl.String, Decl.String, Decl.String)); break;

                case "Print String": intrinsic = new Intrinsic(OpCode.Print, FuncType.Create(Decl.String, Decl.Unit)); break;
                case "Size String": intrinsic = new Intrinsic(OpCode.StringSize, FuncType.Create(Decl.String, Decl.Int)); break;
                case "Substring (String, Int, Int)": intrinsic = new Intrinsic(OpCode.Substring, FuncType.Create(Decl.String, Decl.Int, Decl.Int, Decl.String)); break;
            }

            // attach the argument if we succeeded
            if (intrinsic != null)
            {
                intrinsic.Name = name;

                return intrinsic;
            }

            return null;
        }

        public string Name { get; set; }
        public Decl Type { get { return mType.Return; } }
        public List<OpCode> OpCodes = new List<OpCode>();

        public Intrinsic(OpCode opCode1, OpCode opCode2, FuncType type)
            : this(type)
        {
            OpCodes.Add(opCode1);
            OpCodes.Add(opCode2);
        }

        public Intrinsic(OpCode opCode, FuncType type)
            : this(type)
        {
            OpCodes.Add(opCode);
        }

        public Intrinsic(FuncType type)
        {
            mType = type;
        }

        #region ICallable Members

        public IBoundExpr CreateCall(IBoundExpr arg)
        {
            return new IntrinsicExpr(this, arg);
            throw new NotImplementedException();
        }

        #endregion

        private FuncType mType;
    }
}
