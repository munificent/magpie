using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IForeignStaticInterface
    {
        /// <summary>
        /// Gets the collection of foreign functions exposed by this interface.
        /// </summary>
        IEnumerable<ForeignFunction> Functions { get; }
    }
}
