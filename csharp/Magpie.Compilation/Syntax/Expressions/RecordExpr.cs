using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class RecordExpr : IUnboundExpr
    {
        public IDictionary<string, IUnboundExpr> Fields { get { return mFields; } }

        public RecordExpr(IDictionary<string, IUnboundExpr> fields)
        {
            mFields = new SortedDictionary<string, IUnboundExpr>(fields);
        }

        public Position Position
        {
            get { return mFields.First().Value.Position; }
        }

        public override string ToString()
        {
            var builder = new StringBuilder();

            builder.Append("(");

            foreach (var field in Fields)
            {
                if (builder.Length > 1)
                {
                    builder.Append(" ");
                }

                builder.Append(field.Key)
                       .Append(": ")
                       .Append(field.Value.ToString());
            }

            builder.Append(")");

            return builder.ToString();
        }

        TReturn IUnboundExpr.Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        IUnboundExpr IUnboundExpr.AcceptTransformer(IUnboundExprTransformer transformer)
        {
            var fields = new Dictionary<string, IUnboundExpr>();
            foreach (var field in mFields)
            {
                fields[field.Key] = field.Value.AcceptTransformer(transformer);
            }

            mFields.Clear();
            foreach (var field in fields)
            {
                mFields.Add(field.Key, field.Value);
            }

            return transformer.Transform(this);
        }

        private readonly SortedDictionary<string, IUnboundExpr> mFields;
    }
}
