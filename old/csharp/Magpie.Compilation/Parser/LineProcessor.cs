using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Handles stripping out unneeded newlines after scanning but before parsing.
    /// </summary>
    public class LineProcessor : IEnumerable<Token>
    {
        public LineProcessor(IEnumerable<Token> tokens)
        {
            mTokens = tokens;
        }

        #region IEnumerable<Token> Members

        public IEnumerator<Token> GetEnumerator()
        {
            foreach (Token token in mTokens)
            {
                if (token.Type == TokenType.Line)
                {
                    // eat the line
                    if (mEatLines) continue;

                    // collapse duplicate lines
                    mEatLines = true;
                }
                else if ((token.Type == TokenType.Comma) ||
                         (token.Type == TokenType.Dot) ||
                         (token.Type == TokenType.Operator))
                {
                    // ignore lines after a token that implies a line continuation
                    mEatLines = true;
                }
                else
                {
                    // any other token resets
                    mEatLines = false;
                }

                yield return token;
            }
        }

        #endregion

        #region IEnumerable Members

        System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator()
        {
            return GetEnumerator();
        }

        #endregion

        private bool mEatLines = true;
        private IEnumerable<Token> mTokens;
    }
}
