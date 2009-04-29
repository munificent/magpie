using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IForeignStaticInterface
    {
        ForeignFunction FindFunction(string uniqueName);
    }
}
