using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.Serialization;
using System.Runtime.Serialization.Formatters.Binary;
using System.Text;

namespace Magpie.Compilation
{
    public class BytecodeFile
    {
        public static BytecodeFile Load(Stream stream)
        {
            var file = new BytecodeFile();

            var reader = new BinaryReader(stream, Encoding.UTF8);

            // strings
            int numStrings = reader.ReadInt32();
            for (int i = 0; i < numStrings; i++)
            {
                file.Strings.Add(reader.ReadString());
            }

            // functions
            int numFunctions = reader.ReadInt32();
            int indexOfMain = reader.ReadInt32();
            for (int i = 0; i < numFunctions; i++)
            {
                int numParams = reader.ReadInt32();
                int numLocals = reader.ReadInt32();
                int codeSize = reader.ReadInt32();

                byte[] code = reader.ReadBytes((int)codeSize);

                var function = new FunctionBlock(i == indexOfMain, numParams, numLocals, code);
                file.Functions.Add(function);
            }

            return file;
        }

        public IList<FunctionBlock> Functions { get { return mFunctions; } }
        public IList<String> Strings { get { return mStrings; } }

        public void Save(Stream stream)
        {
            // save the file
            var writer = new BinaryWriter(stream, Encoding.UTF8);

            // strings
            writer.Write(Strings.Count);
            foreach (var s in Strings)
            {
                writer.Write(s);
            }

            // functions
            writer.Write(Functions.Count);
            writer.Write(Functions.IndexOf(Functions.First(fn => fn.Name.Contains("Main__"))));
            foreach (var function in Functions)
            {
                writer.Write(function.NumParameters);
                writer.Write(function.NumLocals);
                writer.Write(function.Code.Length);

                writer.Write(function.Code);
            }
        }

        public void SetStrings(IEnumerable<string> strings)
        {
            mStrings.AddRange(strings);
        }

        public FunctionBlock AddFunction(string name, int numParameters, int numLocals)
        {
            FunctionBlock function = new FunctionBlock(name, numParameters, numLocals);
            mFunctions.Add(function);

            return function;
        }

        private readonly List<string> mStrings = new List<string>();
        private readonly List<FunctionBlock> mFunctions = new List<FunctionBlock>();
    }
}
