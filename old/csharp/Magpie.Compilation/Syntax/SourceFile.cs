using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A parsed .mag source file.
    /// </summary>
    public class SourceFile : Namespace
    {
        /// <summary>
        /// The collection of namespaces this source file is using.
        /// </summary>
        public IList<string> UsingNamespaces { get { return mUsing; } }

        /// <summary>
        /// Initializes a new instance of SourceFile.
        /// </summary>
        /// <param name="usingDeclarations">The namespaces this file is using.</param>
        public SourceFile(IEnumerable<string> usingDeclarations) : base("")
        {
            mUsing.AddRange(usingDeclarations);
        }

        private readonly List<string> mUsing = new List<string>();
    }
}
