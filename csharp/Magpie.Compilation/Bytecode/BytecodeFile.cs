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
        public BytecodeFile(CompileUnit unit)
        {
            mUnit = unit;
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
            writer.Write(1); // number of exported functions ### bob: temp
            strings.InsertOffset("Main__()");
            exportTable.InsertOffset("main");

            // code section
            foreach (BoundFunction function in mUnit.Bound)
            {
                if (function.Name == "Main__()") exportTable.DefineOffset("main");

                BytecodeGenerator.Generate(mUnit, writer, funcPatcher, strings, function);
            }

            // now wire up all of the function offsets to each other
            funcPatcher.PatchOffsets();
            exportTable.PatchOffsets();

            // strings
            strings.WriteStrings();
        }

        private CompileUnit mUnit;
    }
}
