using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Contains an ordered set of namespaces that can be searched through to fully qualify
    /// an identifier.
    /// </summary>
    public class NameSearchSpace
    {
        public static string Qualify(string namespaceName, string name)
        {
            if (String.IsNullOrEmpty(namespaceName)) return name;
            return namespaceName + ":" + name;
        }

        /// <summary>
        /// The current namespace. This will be searched first.
        /// </summary>
        public string Namespace { get; private set; }

        /// <summary>
        /// The namespaces that have been opened using "using". These will be searched in
        /// reverse order.
        /// </summary>
        public IList<string> UsingNamespaces { get; private set; }

        /// <summary>
        /// Creates a new search space.
        /// </summary>
        /// <param name="name">The name of the current namespace.</param>
        /// <param name="usings">Opened namespaces.</param>
        public NameSearchSpace(string name, IEnumerable<string> usings)
        {
            Namespace = name;
            UsingNamespaces = new List<string>(usings);
        }

        /// <summary>
        /// Yields every possible fully-qualified name given a base name, in appropriate
        /// search order.
        /// </summary>
        /// <param name="name">The short or partial name being searched for.</param>
        /// <returns>Each of the potential fully-qualified names.</returns>
        public IEnumerable<string> SearchFor(string name)
        {
            // try the name as-is
            yield return name;

            // try the current namespace
            yield return Qualify(Namespace, name);

            // try each of the open namespaces
            foreach (var usingNamespace in UsingNamespaces.Reverse())
            {
                yield return Qualify(usingNamespace, name);
            }
        }
    }
}
