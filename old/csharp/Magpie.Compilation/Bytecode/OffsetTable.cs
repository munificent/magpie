using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Handles a collection of named offsets to locations within a binary stream. Each offset
    /// takes up 4 bytes in the stream. Multiple offsets can refer to the same named location.
    /// </summary>
    public class OffsetTable
    {
        public IEnumerable<KeyValuePair<string, int>> Offsets { get { return mOffsets; } }

        public OffsetTable(BinaryWriter writer)
        {
            mWriter = writer;
        }

        public void DefineOffset(string name)
        {
            mDefinitions[name] = (int)mWriter.BaseStream.Position;
        }

        public void InsertOffset(string name)
        {
            mOffsets.Add(new KeyValuePair<string, int>(name, (int)mWriter.BaseStream.Position));

            // insert a space for the offset
            mWriter.Write((int)0);
        }

        public void PatchOffsets()
        {
            foreach (var offset in mOffsets)
            {
                long location = mDefinitions[offset.Key];

                mWriter.Seek(offset.Value, SeekOrigin.Begin);
                mWriter.Write((int)location);
            }
            mWriter.Seek(0, SeekOrigin.End);
        }

        private BinaryWriter mWriter;
        private Dictionary<string, int> mDefinitions = new Dictionary<string, int>();
        private List<KeyValuePair<string, int>> mOffsets = new List<KeyValuePair<string, int>>();
    }
}
