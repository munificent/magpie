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

            // flag for whether or not main takes a string argument
            int mainArgOffset = (int)writer.BaseStream.Position;
            writer.Write((byte)0);

            // export table
            writer.Write(1); // number of exported functions ### bob: temp
            strings.InsertOffset("Main ()");
            exportTable.InsertOffset("main");

            // code section
            bool mainTakesArg = false;
            foreach (Function function in mCompiler.Functions.Functions)
            {
                if (function.Name == "Main")
                {
                    string uniqueName = function.UniqueName();

                    // allow either a main with a string arg, or without
                    if (function.UniqueName() == "Main ()") exportTable.DefineOffset("main");

                    if (function.UniqueName() == "Main String")
                    {
                        exportTable.DefineOffset("main");
                        mainTakesArg = true;
                    }
                }

                BytecodeGenerator.Generate(mCompiler, writer, funcPatcher, strings, function);
            }

            if (mainTakesArg)
            {
                writer.Seek(mainArgOffset, SeekOrigin.Begin);
                writer.Write((byte)1);
                writer.Seek(0, SeekOrigin.End);
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
