using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Interpreter
{
    public class Machine
    {
        public event EventHandler<PrintEventArgs> Printed;

        /// <summary>
        /// Gets and sets the maximum stack depth. If set to a non-zero value, the interpreter
        /// will track it during calls and immediately stop if the max is exceeded.
        /// </summary>
        public int MaxStackDepth { get; set; }

        public Machine(IForeignRuntimeInterface foreignInterface)
        {
            mForeignInterface = foreignInterface;
        }

        public Value Interpret(BytecodeFile file, DebugInfo debug, string argument)
        {
            if (file.FindFunction("Main String") != -1)
            {
                return Interpret(file, debug, "Main String", new Value(argument));
            }
            else
            {
                // no main that takes a string, so look for one with no string
                return Interpret(file, debug, "Main ()", null);
            }
        }

        public Value Interpret(BytecodeFile file, DebugInfo debug, string function, Value argument)
        {
            mFile = file;
            mDebug = debug;

            int functionOffset = file.FindFunction(function);

            if (argument != null)
            {
                // push the argument
                Push(argument);

                Push(functionOffset);
                Call(1, false);
            }
            else
            {
                Push(functionOffset);
                Call(0, false);
            }

            return Interpret();
        }

        public Value Interpret(BytecodeFile file)
        {
            return Interpret(file, null, String.Empty);
        }

        public Value Interpret()
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
                    case OpCode.PushString:     Push(mFile.ReadString(ReadInt())); break;

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

                    case OpCode.LoadArray:
                        {
                            Structure struc = PopStructure();

                            int index = struc[0].Int;
                            Structure array = struc[1].Struct;

                            //### bob: should bounds-check

                            // add one to skip the first slot which holds the array size
                            Push(array[index + 1]);
                        }
                        break;

                    case OpCode.StoreArray:
                        {
                            Structure struc = PopStructure();
                            int index = struc[0].Int;
                            Structure array = struc[1].Struct;
                            Value value = struc[2];

                            //### bob: should bounds-check

                            // add one to skip the first slot which holds the array size
                            array[index + 1] = value;
                        }
                        break;

                    case OpCode.SizeArray:
                        {
                            Structure struc = PopStructure();

                            // array size is the first element
                            Push(struc[0]);
                        }
                        break;

                    case OpCode.Call0: Call(0, false); break;
                    case OpCode.Call1: Call(1, false); break;
                    case OpCode.CallN: Call(2, false); break;

                    case OpCode.TailCall0: Call(0, true); break;
                    case OpCode.TailCall1: Call(1, true); break;
                    case OpCode.TailCallN: Call(2, true); break;

                    case OpCode.ForeignCall0: ForeignCall(0); break;
                    case OpCode.ForeignCall1: ForeignCall(1); break;
                    case OpCode.ForeignCallN: ForeignCall(2); break;

                    case OpCode.Return:
                        {
                            mInstruction = mCurrentFrame[mCurrentFrame.Count - 1].Int;
                            mCurrentFrame = mCurrentFrame[mCurrentFrame.Count - 2].Struct;

                            // stop completely if we've returned from main
                            if (mCurrentFrame == null) running = false;
                        }
                        break;
                        
                    case OpCode.Jump:               mInstruction = ReadInt(); break;
                    case OpCode.JumpIfFalse:        int offset = ReadInt();
                                                    if (!PopBool()) mInstruction = offset;
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

                    case OpCode.Random:
                        {
                            // only create if needed
                            if (mRandom == null)
                            {
                                mRandom = new Random();
                            }

                            Push(mRandom.Next(PopInt()));
                        }
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

                    case OpCode.StringCount:         Push(PopString().Length); break;

                    case OpCode.Substring:
                        {
                            Structure tuple = PopStructure();
                            Push(tuple[0].String.Substring(tuple[1].Int, tuple[2].Int));
                        }
                        break;

                    default: throw new Exception("Unknown opcode.");
                }
            }

            // if there is anything left on the stack, return it
            if (mOperands.Count > 0)
            {
                return mOperands.Peek();
            }

            return null;
        }

        private OpCode ReadOpCode()
        {
            return (OpCode)mFile.Bytes[mInstruction++];
        }

        private byte ReadByte()
        {
            return mFile.Bytes[mInstruction++];
        }

        private char ReadChar()
        {
            int c = ((int)mFile.Bytes[mInstruction++]) |
                    ((int)mFile.Bytes[mInstruction++] << 8);

            return (char)c;
        }

        private int ReadInt()
        {
            return ((int)mFile.Bytes[mInstruction++]) |
                   ((int)mFile.Bytes[mInstruction++] << 8) |
                   ((int)mFile.Bytes[mInstruction++] << 16) |
                   ((int)mFile.Bytes[mInstruction++] << 24);
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

        private Structure MakeCallFrame(int numLocals, Structure parentFrame, int instruction)
        {
            var frame = new Structure(numLocals + 2);
            frame[numLocals] = new Value(parentFrame);
            frame[numLocals + 1] = new Value(instruction);

            return frame;
        }

        private void Call(int paramType, bool isTailCall)
        {
            // jump to the function
            int previousInstruction = mInstruction;

            mInstruction = PopInt();
            int numLocals = ReadInt();

            //### bob: work in progress stuff.
            if (mDebug != null)
            {
                string function = mDebug.GetFunctionName(mInstruction);
                //Console.WriteLine(">> calling " + function);
                //System.Diagnostics.Debug.WriteLine("calling " + function);
            }

            // if it's a tail call, discard the parent frame
            var parent = mCurrentFrame;
            if (isTailCall)
            {
                parent = mCurrentFrame[mCurrentFrame.Count - 2].Struct;
                previousInstruction = mCurrentFrame[mCurrentFrame.Count - 1].Int;
            }

            mCurrentFrame = MakeCallFrame(numLocals, parent, previousInstruction);

            // pop and store the argument
            if (paramType != 0) mCurrentFrame[0] = Pop();

            // track stack depth
            if (MaxStackDepth != 0)
            {
                int stackDepth = GetStackDepth(mCurrentFrame);

                if (stackDepth > MaxStackDepth) throw new MaxStackDepthExceededException(MaxStackDepth);
            }
        }

        private void ForeignCall(int paramType) // (int id, Value[] args)
        {
            int id = ReadInt();

            Value[] args;
            switch (paramType)
            {
                case 0: args = new Value[0]; break;
                case 1: args = new Value[] { Pop() }; break;
                case 2: args = PopStructure().Fields.ToArray(); break;
                default: throw new ArgumentException("Unknown parameter type.");
            }

            Value result = mForeignInterface.ForeignCall(id, args);
            if (result != null) Push(result);
        }

        private int GetStackDepth(Structure callFrame)
        {
            if (callFrame == null) return 0;
            return 1 + GetStackDepth(callFrame[callFrame.Count - 2].Struct);
        }

        private BytecodeFile mFile;
        private DebugInfo mDebug;

        //### bob: could also just store this in the current frame
        private int mInstruction; // position in bytecode file

        private readonly Stack<Value> mOperands = new Stack<Value>();

        // call frame for the current function
        // contains:
        // 0 ... n: local variables
        // n + 1  : reference to parent call frame
        // n + 2  : instruction pointer for parent frame
        private Structure mCurrentFrame;

        private Random mRandom;

        private readonly IForeignRuntimeInterface mForeignInterface;
    }
}
