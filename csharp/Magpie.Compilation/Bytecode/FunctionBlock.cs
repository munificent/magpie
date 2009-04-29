using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class FunctionBlock
    {
        public string Name { get { return mName; } }

        public int NumParameters { get { return mNumParameters; } }
        public int NumLocals { get { return mNumLocals; } }

        public byte[] Code
        {
            get
            {
                // lazy initialize
                if (mCodeArray == null) mCodeArray = mCode.ToArray();

                return mCodeArray;
            }
        }

        public FunctionBlock(string name, int numParameters, int numLocals)
        {
            mName = name;
            mNumParameters = numParameters;
            mNumLocals = numLocals;
        }

        public FunctionBlock(bool isMain, int numParameters, int numLocals, byte[] code)
        {
            mName = isMain ? "Main__()" : "__deserialized"; // function names are not serialized
            mNumParameters = numParameters;
            mNumLocals = numLocals;
            mCodeArray = code;
        }

        public void PushBool(bool value)    { Add(OpCode.PushBool, value ? (byte)1 : (byte)0); }
        public void PushInt(int value)      { Add(OpCode.PushInt, value); }
        public void PushString(int index)   { Add(OpCode.PushString, index); }
        public void Alloc(int slots)        { Add(OpCode.Alloc, slots); }
        public void Load(byte index)        { Add(OpCode.Load, index); }
        public void Store(byte index)       { Add(OpCode.Store, index); }

        public void Jump(string jumpLabel)
        {
            AddJump(OpCode.Jump, jumpLabel);
        }

        public void JumpIfFalse(string jumpLabel)
        {
            AddJump(OpCode.JumpIfFalse, jumpLabel);
        }

        public void JumpBack(string jumpLabel)
        {
            AddJumpBack(OpCode.Jump, jumpLabel);
        }

        public void JumpIfFalseBack(string jumpLabel)
        {
            AddJumpBack(OpCode.JumpIfFalse, jumpLabel);
        }

        public void PatchJump(string jumpLabel)
        {
            // pop the most recently-added jump with the label
            JumpPatch jump = mJumpPatches.Last(j => j.Label == jumpLabel);
            mJumpPatches.Remove(jump);

            int position = jump.Location;
            int destination = mCode.Count;

            mCode[position++] = ((byte)((destination & 0x000000ff) >> 0));
            mCode[position++] = ((byte)((destination & 0x0000ff00) >> 8));
            mCode[position++] = ((byte)((destination & 0x00ff0000) >> 16));
            mCode[position++] = ((byte)((destination & 0xff000000) >> 32));
        }

        public void PatchJumpBack(string jumpLabel)
        {
            mJumpBackPatches.Add(new JumpPatch(jumpLabel, mCode.Count));
        }
        
        public void Byte(byte value)
        {
            mCode.Add(value);
        }

        public void Add(OpCode op)
        {
            Byte((byte)op);
        }

        public void AddJump(OpCode jumpOp, string jumpLabel)
        {
            // add the jump and a blank space for the destination
            Add(jumpOp, 0);

            // patch the destination later
            mJumpPatches.Add(new JumpPatch(jumpLabel, mCode.Count - 4));
        }

        public void AddJumpBack(OpCode jumpOp, string jumpLabel)
        {
            // pop the most recently-added jump with the label
            JumpPatch jump = mJumpBackPatches.Last(j => j.Label == jumpLabel);
            mJumpBackPatches.Remove(jump);

            Add(jumpOp, jump.Location);
        }

        public void Add(OpCode op, byte operand)
        {
            Byte((byte)op);
            Byte(operand);
        }

        public void Add(OpCode op, char c)
        {
            Byte((byte)op);

            int operand = c;

            Byte((byte)((operand & 0x000000ff) >> 0));
            Byte((byte)((operand & 0x0000ff00) >> 8));
        }

        public void Add(OpCode op, int operand)
        {
            Byte((byte)op);

            Byte((byte)((operand & 0x000000ff) >> 0));
            Byte((byte)((operand & 0x0000ff00) >> 8));
            Byte((byte)((operand & 0x00ff0000) >> 16));
            Byte((byte)((operand & 0xff000000) >> 32));
        }

        private string mName;
        private int mNumParameters;
        private int mNumLocals;

        private class JumpPatch
        {
            public string Label;
            public int Location;

            public JumpPatch(string label, int location)
            {
                Label = label;
                Location = location;
            }
        }

        private readonly List<byte> mCode = new List<byte>();

        private readonly List<JumpPatch> mJumpPatches = new List<JumpPatch>();
        private readonly List<JumpPatch> mJumpBackPatches = new List<JumpPatch>();

        private byte[] mCodeArray;
    }
}
