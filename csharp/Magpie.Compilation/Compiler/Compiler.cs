using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class Compiler
    {
        public Compiler(IForeignStaticInterface foreignInterface)
        {
            mOutput = new CompileUnit(foreignInterface);
        }

        public void AddSourceFile(string filePath)
        {
            try
            {
                SourceFile source = MagpieParser.ParseSourceFile(filePath);

                mOutput.Include(source);
            }
            catch (Exception ex)
            {
                throw new Exception("Exception parsing \"" + filePath + "\".", ex);
            }
        }

        public void Compile(Stream outputStream)
        {
            mOutput.Compile(outputStream);
        }

        private CompileUnit mOutput;
    }
}
