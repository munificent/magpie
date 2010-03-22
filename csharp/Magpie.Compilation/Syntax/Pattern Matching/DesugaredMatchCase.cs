using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class DesugaredMatchCase
    {
        public IUnboundExpr Condition;
        public readonly IDictionary<string, IUnboundExpr> Variables = new Dictionary<string, IUnboundExpr>();
    }
}
