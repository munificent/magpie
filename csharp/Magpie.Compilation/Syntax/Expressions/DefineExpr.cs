using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class DefineExpr : IUnboundExpr
    {
        public Position Position
        {
            get { return Definitions[0].Position; }
        }

        public bool IsMutable { get; private set; }

        public readonly List<Define> Definitions = new List<Define>();

        public DefineExpr(Position position, string name, IUnboundExpr value, bool isMutable)
            : this(isMutable, new Define[] { new Define(position, new string[] { name }, value) })
        {
        }

        public DefineExpr(Position position, IEnumerable<string> names, IUnboundExpr value, bool isMutable)
            : this(isMutable, new Define[] { new Define(position, names, value) })
        {
        }

        public DefineExpr(bool isMutable, IEnumerable<Define> definitions)
        {
            IsMutable = isMutable;
            Definitions.AddRange(definitions);
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString()
        {
            var keyword = IsMutable ? "var" : "def";

            if (Definitions.Count == 1)
            {
                return keyword + " " + Definitions[0].ToString();
            }
            else
            {
                var builder = new StringBuilder();
                builder.AppendLine(keyword);

                foreach (var define in Definitions)
                {
                    builder.Append("    ").AppendLine(define.ToString());
                }

                builder.AppendLine("end");

                return builder.ToString();
            }
        }
    }

    /// <summary>
    /// A single named definition in a define expression.
    /// </summary>
    public class Define
    {
        public Position Position { get; private set; }
        public IList<string> Names { get; private set; }
        public IUnboundExpr Value { get; private set; }

        public Define(Position position, IEnumerable<string> names, IUnboundExpr value)
        {
            Position = position;
            Names = new List<string>(names);
            Value = value;
        }

        public override string ToString()
        {
            return String.Format("{0} <- {1}", String.Join(" ", Names.ToArray()), Value);
        }
    }
}
