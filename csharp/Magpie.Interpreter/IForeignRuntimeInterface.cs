using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Interpreter
{
    public interface IForeignRuntimeInterface
    {
        Value ForeignCall(int id, Value[] args);
    }
}
