using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class SourceFile : Namespace
    {
        public IList<string> UsingNamespaces { get { return mUsing; } }

        public SourceFile(IEnumerable<string> usingDeclarations) : base("")
        {
            mUsing.AddRange(usingDeclarations);
        }

        private readonly List<string> mUsing = new List<string>();
    }
}
