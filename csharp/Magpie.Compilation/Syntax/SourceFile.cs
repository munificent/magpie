using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class SourceFile : Namespace
    {
        public IList<string> UsingNamespaces { get { return mUsing; } }

        public SourceFile(IEnumerable<string> usingDeclarations, IEnumerable<object> contents) : base("", contents)
        {
            mUsing.AddRange(usingDeclarations);
        }

        public override string ToString()
        {
            var builder = new StringBuilder();

            foreach (var usingName in mUsing)
            {
                builder.AppendLine("using " + usingName);
            }

            builder.AppendLine();
            builder.Append(ContentsToString());

            return builder.ToString();
        }

        private readonly List<string> mUsing = new List<string>();
    }
}
