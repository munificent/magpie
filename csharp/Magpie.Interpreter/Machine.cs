using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

using Magpie.Compilation;

namespace Magpie.Interpreter
{
    public class Machine
    {
        public event EventHandler<PrintEventArgs> Printed;

        public Machine(IForeignRuntimeInterface foreignInterface)
        {
            mForeignInterface = foreignInterface;
        }

        public void Interpret(Stream stream)
        {
            Interpret(BytecodeFile.Load(stream), String.Empty);
        }

        public void Interpret(BytecodeFile file, string argument)
        {
            mFile = file;

            // find "main"
            FunctionBlock main = mFile.Functions.First(func => func.Name == "Main__()");
            int index = mFile.Functions.IndexOf(main);

            mCurrentFrame = MakeCallFrame(main.NumLocals, null, -1, -1);

            mCurrentFunc = index;
            mCurrentOp = 0;

            mCode = main.Code;

            // if main takes an argument, pass it
            if (main.NumParameters == 1)
            {
                mCurrentFrame[0] = new Value(argument);
            }

            Interpret();
        }

        public void Interpret(BytecodeFile file)
        {
            Interpret(file, String.Empty);
        }

        public void Interpret()
        {
            bool running = true;

            while (running)
            {
                OpCode theOp = ReadOpCode();
                //Console.WriteLine(theOp.ToString());

                switch (theOp)
                {
                    case OpCode.PushNull:       Push((Structure)null); break;
                    case OpCode.PushBool:       Push(ReadByte() != 0); break;
                    case OpCode.PushInt:        Push(ReadInt()); break;
                    case OpCode.PushString:     Push(mFile.Strings[ReadInt()]); break;

                    case OpCode.PushLocals:     Push(mCurrentFrame); break;

                    case OpCode.Alloc:
                        {
                            int slots = ReadInt();
                            Structure structure = new Structure(slots);

                            // initialize it
                            // note: slots are on stack in reverse order
                            // because they have been pushed in forward order.
                            // this ensures that arguments are evaluated left to right.
                            // for example: calling Foo (1, 2, 3) will create the arg
                            // tuple by evaluating 1, 2, 3 in order. this leaves the
                            // stack (from top down) looking like 3 2 1.
                            for (int i = slots - 1; i >= 0; i--)
                            {
                                structure[i] = Pop();
                            }

                            Push(structure);
                        }
                        break;

                    case OpCode.Load:
                        {
                            byte index = ReadByte();
                            Structure struc = PopStructure();
                            Value op = struc[index];

                            Push(op);
                        }
                        break;

                    case OpCode.Store:
                        {
                            byte index = ReadByte();
                            Structure struc = PopStructure();
                            struc[index] = Pop();
                        }
                        break;

                    case OpCode.Call:
                        {
                            // jump to the function
                            int index = PopInt();
                            FunctionBlock function = mFile.Functions[index];

                            Structure callFrame = MakeCallFrame(function.NumLocals, mCurrentFrame, mCurrentOp, mCurrentFunc);

                            mCurrentFrame = callFrame;
                            mCurrentFunc = index;
                            mCurrentOp = 0;

                            mCode = function.Code;

                            // pop and store the arguments into locals
                            switch (function.NumParameters)
                            {
                                case 0: break; // do nothing
                                case 1: mCurrentFrame[0] = Pop(); break; // single value on stack
                                default:
                                    // multiple values, so pop and unwrap tuple
                                    Structure tuple = PopStructure();
                                    for (int i = 0; i < function.NumParameters; i++)
                                    {
                                        mCurrentFrame[i] = tuple[i];
                                    }
                                    break;
                            }
                        }
                        break;

                    case OpCode.ForeignCall0:
                        {
                            int id = ReadInt();

                            ForeignCall(id, new Value[0]);
                        }
                        break;

                    case OpCode.ForeignCall1:
                        {
                            int id = ReadInt();
                            Value arg = Pop();

                            ForeignCall(id, new Value[] { arg });
                        }
                        break;

                    case OpCode.ForeignCallTuple:
                        {
                            int id = ReadInt();
                            Structure args = PopStructure();

                            ForeignCall(id, args.Fields.ToArray());
                        }
                        break;

                    case OpCode.Return:
                        {
                            mCurrentOp = mCurrentFrame[mCurrentFrame.Count - 2].Int;
                            mCurrentFunc = mCurrentFrame[mCurrentFrame.Count - 1].Int;

                            mCurrentFrame = mCurrentFrame[mCurrentFrame.Count - 3].Struct;

                            // stop completely if we've returned from main
                            if (mCurrentFrame == null)
                            {
                                running = false;
                            }
                            else
                            {
                                mCode = mFile.Functions[mCurrentFunc].Code;
                            }
                        }
                        break;
                        
                    case OpCode.Jump:               mCurrentOp = ReadInt(); break;
                    case OpCode.JumpIfFalse:        int offset = ReadInt();
                                                    if (!PopBool()) mCurrentOp = offset;
                                                    break;

                    case OpCode.BoolToString:       Push(PopBool() ? "true" : "false"); break;
                    case OpCode.IntToString:        Push(PopInt().ToString()); break;

                    case OpCode.EqualBool:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].Bool == tuple[1].Bool);
                        }
                        break;

                    case OpCode.EqualInt:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].Int == tuple[1].Int);
                        }
                        break;

                    case OpCode.EqualString:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].String == tuple[1].String);
                        }
                        break;

                    case OpCode.LessInt:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].Int < tuple[1].Int);
                        }
                        break;

                    case OpCode.GreaterInt:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].Int > tuple[1].Int);
                        }
                        break;

                    case OpCode.NegateBool:         Push(!PopBool()); break;
                    case OpCode.NegateInt:          Push(-PopInt()); break;

                    case OpCode.AndBool:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].Bool && tuple[1].Bool);
                        }
                        break;

                    case OpCode.OrBool:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].Bool || tuple[1].Bool);
                        }
                        break;

                    case OpCode.AddInt:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].Int + tuple[1].Int);
                        }
                        break;

                    case OpCode.SubInt:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].Int - tuple[1].Int);
                        }
                        break;

                    case OpCode.MultInt:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].Int * tuple[1].Int);
                        }
                        break;

                    case OpCode.DivInt:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].Int / tuple[1].Int);
                        }
                        break;

                    case OpCode.HasValue:
                        Push(PopStructure() != null);
                        break;

                    case OpCode.BoxValue:
                        {
                            Structure structure = new Structure(1);
                            structure[0] = Pop();
                            Push(structure);
                        }
                        break;

                    case OpCode.UnboxValue:
                        Push(PopStructure()[0]);
                        break;

                    case OpCode.AddString:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].String + tuple[1].String);
                        }
                        break;

                    case OpCode.Print:
                        string text = PopString();
                        if (Printed != null) Printed(this, new PrintEventArgs(text));
                        break;

                    case OpCode.StringSize:         Push(PopString().Length); break;

                    case OpCode.Substring:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].String.Substring(tuple[1].Int, tuple[2].Int));
                        }
                        break;

                    default: throw new Exception("Unknown opcode.");
                }
            }
        }

        private OpCode ReadOpCode()
        {
            return (OpCode)mCode[mCurrentOp++];
        }

        private byte ReadByte()
        {
            return mCode[mCurrentOp++];
        }

        private char ReadChar()
        {
            int c = ((int)mCode[mCurrentOp++]) |
                    ((int)mCode[mCurrentOp++] << 8);

            return (char)c;
        }

        private int ReadInt()
        {
            return ((int)mCode[mCurrentOp++]) |
                   ((int)mCode[mCurrentOp++] << 8) |
                   ((int)mCode[mCurrentOp++] << 16) |
                   ((int)mCode[mCurrentOp++] << 24);
        }

        private void Push(Value value) { mOperands.Push(value); }
        private void Push(bool value) { Push(new Value(value)); }
        private void Push(char value) { Push(new Value(value)); }
        private void Push(int value) { Push(new Value(value)); }
        private void Push(string value) { Push(new Value(value)); }
        private void Push(Structure value) { Push(new Value(value)); }

        private Value Pop() { return mOperands.Pop(); }
        private bool PopBool() { return mOperands.Pop().Bool; }
        private int PopInt() { return mOperands.Pop().Int; }
        private string PopString() { return mOperands.Pop().String; }
        private Structure PopStructure() { return mOperands.Pop().Struct; }

        private Structure MakeCallFrame(int numLocals, Structure parentFrame, int opCode, int parentFunc)
        {
            var frame = new Structure(numLocals + 3);
            frame[numLocals] = new Value(parentFrame);
            frame[numLocals + 1] = new Value(opCode);
            frame[numLocals + 2] = new Value(parentFunc);

            return frame;
        }

        private void ForeignCall(int id, Value[] args)
        {
            Value result = mForeignInterface.ForeignCall(id, args);

            if (result != null) Push(result);
        }

        private BytecodeFile mFile;

        private byte[] mCode;
        private int mCurrentFunc;
        private int mCurrentOp;

        private readonly Stack<Value> mOperands = new Stack<Value>();

        // call frame for the current function
        // contains:
        // 0 ... n: local variables
        // n + 1  : reference to parent call frame
        // n + 2  : instruction pointer for parent frame
        // n + 3  : function index for parent frame
        private Structure mCurrentFrame;

        private readonly IForeignRuntimeInterface mForeignInterface;
    }
}
