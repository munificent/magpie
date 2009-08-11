using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public abstract class CaseBase
    {
        public Position Position { get; private set; }

        protected CaseBase(Position position)
        {
            Position = position;
        }
    }
}
