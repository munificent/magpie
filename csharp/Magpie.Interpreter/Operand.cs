using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Interpreter
{
    /// <summary>
    /// A variant value.
    /// </summary>
    public class Value
    {
        public bool Bool        { get { return (bool)mValue; } }
        public int Int          { get { return (int)mValue; } }
        public string String    { get { return (string)mValue; } }
        public Structure Struct { get { return (Structure)mValue; } }

        public Value(bool value)      : this((object)value) { }
        public Value(int value)       : this((object)value) { }
        public Value(string value)    : this((object)value) { }
        public Value(Structure value) : this((object)value) { }

        private Value(object value)
        {
            mValue = value;
        }

        public override string ToString()
        {
            if (mValue != null)
            {
                return mValue.ToString();
            }
            else
            {
                return "null";
            }
        }

        //### bob: in a c implementation, this would be a union
        private object mValue = null;
    }
}
