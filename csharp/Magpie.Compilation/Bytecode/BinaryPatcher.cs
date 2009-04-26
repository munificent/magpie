using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class BinaryPatcher
    {
        public BinaryPatcher(BinaryWriter writer)
        {
            mWriter = writer;
        }

        public void InsertOffset(string name)
        {
            mPatches[name] = mWriter.BaseStream.Position;
        }

        public void PatchOffset(string name)
        {
            // get the patch
            long patchLocation = mPatches[name];
            mPatches.Remove(name);

            // jump to it
            long value = mWriter.BaseStream.Position;
            mWriter.BaseStream.Seek(patchLocation, SeekOrigin.Begin);

            // write the position
            mWriter.Write((int)value);
            mWriter.BaseStream.Seek(0, SeekOrigin.End);
        }

        private BinaryWriter mWriter;
        private readonly Dictionary<string, long> mPatches = new Dictionary<string, long>();
    }
}
