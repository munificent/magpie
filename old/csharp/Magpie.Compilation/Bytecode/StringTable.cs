using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class StringTable
    {
        public StringTable(BinaryWriter writer)
        {
            mWriter = writer;
            mPatcher = new OffsetTable(mWriter);
        }

        public void InsertOffset(string name)
        {
            mPatcher.InsertOffset(name);
        }

        public void WriteStrings()
        {
            // build the list of unique strings
            var strings = new SortedList<string, bool>();
            foreach (var offset in mPatcher.Offsets)
            {
                if (!strings.ContainsKey(offset.Key)) strings.Add(offset.Key, false);
            }

            // write the string table
            OffsetTable patcher = new OffsetTable(mWriter);
            foreach (string s in strings.Keys)
            {
                mPatcher.DefineOffset(s);
                mWriter.Write(Encoding.UTF8.GetBytes(s));
                mWriter.Write(0);
            }

            mPatcher.PatchOffsets();
        }

        private readonly BinaryWriter mWriter;
        private readonly OffsetTable mPatcher;
    }
}
