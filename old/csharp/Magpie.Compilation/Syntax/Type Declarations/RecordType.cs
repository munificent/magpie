using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class RecordType<TDecl>
    {
        public IDictionary<string, TDecl> Fields { get; private set; }

        public RecordType(IDictionary<string, TDecl> fields)
        {
            Fields = new SortedDictionary<string, TDecl>(fields);
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

        private readonly List<TDecl> mFields = new List<TDecl>();
    }

    /// <summary>
    /// Defines an unbound record type declaration, including the names and types of the fields.
    /// </summary>
    public class RecordType : RecordType<IUnboundDecl>, IUnboundDecl
    {
        public Position Position { get { return Fields.First().Value.Position; } }

        public RecordType(IDictionary<string, IUnboundDecl> fields)
            : base(fields)
        {
        }

        #region IUnboundDecl Members

        TReturn IUnboundDecl.Accept<TReturn>(IUnboundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }

    public class BoundRecordType : RecordType<IBoundDecl>, IBoundDecl
    {
        public BoundRecordType(IDictionary<string, IBoundDecl> fields)
            : base(fields)
        {
        }

        #region IBoundDecl Members

        TReturn IBoundDecl.Accept<TReturn>(IBoundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
