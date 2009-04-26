using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Interpreter
{
    public class PrintEventArgs : EventArgs
    {
        public string Text { get { return mText; } }

        public PrintEventArgs(string text)
        {
            mText = text;
        }

        private string mText;
    }
}
