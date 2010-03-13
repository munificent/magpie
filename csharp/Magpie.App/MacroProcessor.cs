using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

using Magpie.Compilation;
using Magpie.Interpreter;

namespace Magpie.App
{
    //### bob: this really doesn't belong in this project, but the compiler
    // doesn't have access to the interpreter.
    /// <summary>
    /// Processes macros defined using Magpie.
    /// </summary>
    public class MacroProcessor : IMacroProcessor
    {
        public MacroProcessor()
        {
            //### bob: hack. assumes location of base relative to working directory. :(
            string baseDir = Path.Combine(Environment.CurrentDirectory, @"..\..\..\base\macro");

            mScript = new Script(baseDir);
        }

        #region IMacroProcessor Members

        public IUnboundExpr Process(string name, IUnboundExpr arg)
        {
            string macro = name + " Expression";
            if (mScript.HasFunction(macro))
            {
                var argument = ToValue(arg);
                var result = mScript.Run(macro, argument);

                return ToExpr(result);
            }

            return null;
        }

        #endregion

        private Value ToValue(IUnboundExpr expr)
        {
            //### bob: implement me!
            return new Value(true);
        }

        private IUnboundExpr ToExpr(Value value)
        {
            //### bob: implement me!
            return null;
        }

        private Script mScript;
    }
}
