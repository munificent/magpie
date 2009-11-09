using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Interpreter
{
    /// <summary>
    /// Provides human-readable debug information for a compiled
    /// bytecode file.
    /// </summary>
    public class DebugInfo
    {
        public string GetFunctionName(long offset)
        {
            var found = mFunctions.LastOrDefault(region => region.Start <= offset);

            if (found != null) return found.Name;

            return String.Empty;
        }

        public void StartFunction(string name, long offset)
        {
            mFunctions.Add(new NamedRegion(name, offset));
        }

        private class NamedRegion
        {
            public string Name;
            public long Start;

            public NamedRegion(string name, long start)
            {
                Name = name;
                Start = start;
            }
        }

        private List<NamedRegion> mFunctions = new List<NamedRegion>();
    }
}
