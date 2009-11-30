using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Interpreter
{
    public class BytecodeFile
    {
        public byte[] Bytes { get { return mData; } }

        public bool MainTakesArgument
        {
            get
            {
                // main can take a string or not
                int offset = FindFunction("Main String");

                return offset != -1;
            }
        }

        public int OffsetToMain
        {
            get
            {
                // main can take a string or not
                int offset = FindFunction("Main String");
                if (offset != -1) return offset;

                return FindFunction("Main ()");
            }
        }

        public BytecodeFile(byte[] data)
        {
            mData = data;

            // check the header

            // magic number
            Expect(0, Encoding.ASCII.GetBytes(new char[] { 'p', 'i', 'e', '!' }));

            // version
            Expect(4, new byte[] { 0, 0, 0, 0 });
        }

        public int FindFunction(string uniqueName)
        {
            int offset = 4 + 4; // magic num + version
            int numExports = ReadInt(offset);

            offset += 4; // num exports
            for (int i = 0; i < numExports; i++)
            {
                int stringOffset = ReadInt(offset);
                string name = ReadString(stringOffset);

                offset += 4;
                int functionOffset = ReadInt(offset);
                if (name == uniqueName) return functionOffset;

                offset += 4;
            }

            // not found
            return -1;
        }

        public int ReadInt(int offset)
        {
            return ((int)mData[offset++]) |
                   ((int)mData[offset++] << 8) |
                   ((int)mData[offset++] << 16) |
                   ((int)mData[offset++] << 24);
        }

        public string ReadString(int offset)
        {
            int start = offset;

            // find the end of the string
            while (mData[offset] != 0) offset++;

            return Encoding.UTF8.GetString(mData, start, offset - start);
        }

        private void Expect(int start, byte[] expected)
        {
            for (int i = 0; i < expected.Length; i++)
            {
                if ((byte)mData[start + i] != expected[i]) throw new Exception("Header doesn't match.");
            }
        }

        private byte[] mData;
    }
}
