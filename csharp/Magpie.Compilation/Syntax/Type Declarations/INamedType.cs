using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface INamedType : IBoundDecl
    {
        Position Position { get; }
        string Name { get; }
    }
}
