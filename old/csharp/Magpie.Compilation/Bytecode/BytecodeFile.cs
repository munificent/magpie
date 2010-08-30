using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class BytecodeFile
    {
        public BytecodeFile(Compiler compiler)
        {
            mCompiler = compiler;
        }

        public void Save(Stream outputStream)
        {
            // save the file
            var writer = new BinaryWriter(outputStream, Encoding.UTF8);
            var funcPatcher = new OffsetTable(writer);
            var strings = new StringTable(writer);
            var exportTable = new OffsetTable(writer);

            // magic number
            writer.Write(Encoding.ASCII.GetBytes(new char[] { 'p', 'i', 'e', '!' }));

            // version
            writer.Write(new byte[] { 0, 0, 0, 0 });

            // export table
            // number of exported functions
            //### bob: hack temp. exports all functions
            int numFunctions = mCompiler.Functions.Functions.Count();
            writer.Write(numFunctions);
            foreach (Function function in mCompiler.Functions.Functions)
            {
                string uniqueName = function.UniqueName();

                strings.InsertOffset(uniqueName);
                exportTable.InsertOffset(uniqueName);
            }

            // code section
            foreach (Function function in mCompiler.Functions.Functions)
            {
                string uniqueName = function.UniqueName();

                exportTable.DefineOffset(uniqueName);
                BytecodeGenerator.Generate(mCompiler, writer, funcPatcher, strings, function);
            }

            // now wire up all of the function offsets to each other
            funcPatcher.PatchOffsets();
            exportTable.PatchOffsets();

            // strings
            strings.WriteStrings();
        }

        private Compiler mCompiler;
    }
}
