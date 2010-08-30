using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.IO;
using System.Linq;
using System.Text;

using Magpie.Compilation;
using Magpie.Foreign;
using Magpie.Interpreter;

namespace Magpie.App
{
    public class Script
    {
        public event EventHandler<PrintEventArgs> Printed;

        public int MaxStackDepth { get; set; }

        public ReadOnlyCollection<CompileError> Errors { get { return new ReadOnlyCollection<CompileError>(mErrors); } }

        public StreamForeign Foreign { get { return mForeign; } }

        public Script(string path)
        {
            mPath = path;
        }

        public Script(byte[] bytecode)
        {
            mBytecode = new Magpie.Interpreter.BytecodeFile(bytecode);
            mErrors = new List<CompileError>();
        }

        public void Compile()
        {
            if (mBytecode != null) throw new InvalidOperationException("Can only compile a script once.");

            var compiler = new Compiler(mForeign);

            compiler.FunctionStarted = mDebug.StartFunction;

            // add the source files
            foreach (var sourceFile in GetSourceFiles(mPath))
            {
                compiler.AddSourceFile(sourceFile);
            }

            using (var stream = new MemoryStream())
            {
                // compile the code
                mErrors = compiler.Compile(stream);

                // bail if there were compile errors
                if (mErrors.Count > 0) return;

                mBytecode = new Magpie.Interpreter.BytecodeFile(stream.ToArray());
            }
        }

        public bool HasFunction(string function)
        {
            if (!EnsureCompiled()) return false;

            return mBytecode.FindFunction(function) != -1;
        }

        public void Run(string argument)
        {
            if (!EnsureCompiled()) return;

            // interpret the resulting bytecode
            var machine = new Machine(mForeign);
            machine.Printed += Machine_Printed;
            machine.MaxStackDepth = MaxStackDepth;

            try
            {
                machine.Interpret(mBytecode, mDebug, argument);
            }
            catch (InterpreterException ex)
            {
                // do nothing
                //### bob: should report runtime errors
                Console.WriteLine(ex.ToString());
            }
            finally
            {
                machine.Printed -= Machine_Printed;
            }
        }

        public Value Run(string functionName, Value argument)
        {
            if (!EnsureCompiled()) return null;

            // interpret the resulting bytecode
            var machine = new Machine(mForeign);
            machine.Printed += Machine_Printed;
            machine.MaxStackDepth = MaxStackDepth;

            try
            {
                return machine.Interpret(mBytecode, mDebug, functionName, argument);
            }
            catch (InterpreterException ex)
            {
                // do nothing
                //### bob: should report runtime errors
                Console.WriteLine(ex.ToString());

                return null;
            }
            finally
            {
                machine.Printed -= Machine_Printed;
            }
        }

        private bool EnsureCompiled()
        {
            // compile it first, if not already compiled
            if (mBytecode == null) Compile();

            // bail if we didn't compile successfully
            return (mBytecode != null);
        }

        private IEnumerable<string> GetSourceFiles(string path)
        {
            // add the base sources
            //### bob: hack. assumes location of base relative to working directory. :(
            string baseDir = Path.Combine(Environment.CurrentDirectory, @"..\..\..\base\runtime");

            foreach (var baseFile in Directory.GetFiles(baseDir, "*.mag"))
            {
                yield return baseFile;
            }

            // add the main file(s) to compile
            if (Directory.Exists(path))
            {
                // path is a directory, so include all files in it
                foreach (var file in Directory.GetFiles(path, "*.mag", SearchOption.AllDirectories))
                {
                    yield return file;
                }
            }
            else
            {
                // path is a single file
                yield return path;
            }
        }

        private void Machine_Printed(object sender, PrintEventArgs e)
        {
            if (Printed == null)
            {
                // if there are no event handlers, default to printing to the console
                Console.WriteLine(e.Text);
            }
            else
            {
                Printed(this, e);
            }
        }

        private string mPath;
        private IList<CompileError> mErrors;
        private Magpie.Interpreter.BytecodeFile mBytecode;
        private StreamForeign mForeign = new StreamForeign();

        //### bob: temp. work-in-progress debug stuff
        private DebugInfo mDebug = new DebugInfo();
    }
}
