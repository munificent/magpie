using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class NameContext
    {
        public string Namespace { get; private set; }
        public IList<string> UsingNamespaces { get; private set; }

        public NameContext(string name, IEnumerable<string> usings)
        {
            Namespace = name;
            UsingNamespaces = new List<string>(usings);
        }
    }
}
