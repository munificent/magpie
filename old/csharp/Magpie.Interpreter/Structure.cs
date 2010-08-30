using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Interpreter
{
    /// <summary>
    /// A heap-allocated data structure. Used for closures, tuples, structs, etc.
    /// </summary>
    public class Structure
    {
        public IEnumerable<Value> Fields { get { return mFields; } }

        public Structure(int numFields)
        {
            mFields = new Value[numFields];
        }

        public int Count { get { return mFields.Length; } }

        public Value this[int index]
        {
            get { return mFields[index]; }
            set { mFields[index] = value; }
        }

        public override string ToString()
        {
            StringBuilder builder = new StringBuilder();

            builder.Append("structure [ ");

            foreach (Value field in mFields)
            {
                builder.Append(field.ToString());
                builder.Append(" ");
            }

            builder.Append("]");

            return builder.ToString();
        }

        private readonly Value[] mFields;
    }
}
