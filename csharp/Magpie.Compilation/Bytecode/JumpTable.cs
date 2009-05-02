using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class JumpTable
    {
        public JumpTable(BytecodeGenerator bytecode)
        {
            mBytecode = bytecode;
        }

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
            int destination = (int)mBytecode.Position;

            //### bob: see if we can use a regular write int here
            mBytecode.SeekTo(position);
            mBytecode.Write(destination);

            mBytecode.SeekToEnd();
        }

        public void PatchJumpBack(string jumpLabel)
        {
            mJumpBackPatches.Add(new JumpPatch(jumpLabel, mBytecode.Position));
        }

        private void AddJump(OpCode jumpOp, string jumpLabel)
        {
            // add the jump and a blank space for the destination
            mBytecode.Write(jumpOp, 0);

            // patch the destination later
            mJumpPatches.Add(new JumpPatch(jumpLabel, mBytecode.Position - 4));
        }

        private void AddJumpBack(OpCode jumpOp, string jumpLabel)
        {
            // pop the most recently-added jump with the label
            JumpPatch jump = mJumpBackPatches.Last(j => j.Label == jumpLabel);
            mJumpBackPatches.Remove(jump);

            mBytecode.Write(jumpOp, jump.Location);
        }

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

        private BytecodeGenerator mBytecode;

        private readonly List<JumpPatch> mJumpPatches = new List<JumpPatch>();
        private readonly List<JumpPatch> mJumpBackPatches = new List<JumpPatch>();
    }
}
