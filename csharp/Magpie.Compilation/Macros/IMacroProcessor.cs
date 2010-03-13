using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IMacroProcessor
    {
        IUnboundExpr Process(string name, IUnboundExpr arg);
    }

    //### bob: for testing. short-circuits & and |
    public class HackTempMacro : IMacroProcessor
    {
        #region IMacroProcessor Members

        public IUnboundExpr Process(string name, IUnboundExpr arg)
        {
            var tuple = arg as TupleExpr;

            if (tuple == null) return null;
            if (tuple.Fields.Count != 2) return null;

            if (name == "&")
            {
                // a & b -> if a then b else false
                return new IfExpr(arg.Position, tuple.Fields[0], tuple.Fields[1], new BoolExpr(Position.None, false));
            }
            else if (name == "|")
            {
                // a | b -> if a then true else b
                return new IfExpr(arg.Position, tuple.Fields[0], new BoolExpr(Position.None, true), tuple.Fields[1]);
            }

            return null;
        }

        #endregion
    }
}
