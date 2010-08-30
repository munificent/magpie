using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Simple class for auto-generating unique identifier names. Used to create variable
    /// names for desugared auto-generated code.
    /// </summary>
    public class NameGenerator
    {
        //### bob: should refactor code to use this for all temps
        public string Generate()
        {
            mTempIndex++;

            // using space in identifiers ensures it can't collide with a user-defined name
            return "__temp " + mTempIndex;
        }

        private int mTempIndex;
    }
}
