using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class TupleExpr<TExpr>
    {
        public readonly List<TExpr> Fields = new List<TExpr>();

        public TupleExpr(IEnumerable<TExpr> fields)
        {
            Fields.AddRange(fields);
        }

        public override string ToString()
        {
            return "(" + Fields.JoinAll(", ") + ")";
        }
    }

    public class TupleExpr : TupleExpr<IUnboundExpr>, IUnboundExpr
    {
        public Position Position { get { return Fields[0].Position; } }

        public TupleExpr(IEnumerable<IUnboundExpr> fields)
            : base(fields)
        {
        }

        public TupleExpr(params IUnboundExpr[] fields)
            : this((IEnumerable<IUnboundExpr>)fields)
        {
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public IUnboundExpr AcceptTransformer(IUnboundExprTransformer transformer)
        {
            for (int i = 0; i < Fields.Count; i++)
            {
                Fields[i] = Fields[i].AcceptTransformer(transformer);
            }

            return transformer.Transform(this);
        }
    }

    //### bob: this class should be renamed StructureExpr since it's used for more than
    // just tuples
    public class BoundTupleExpr : TupleExpr<IBoundExpr>, IBoundExpr
    {
        public BoundTupleExpr(IEnumerable<IBoundExpr> fields) : base(fields) { }

        public BoundTupleExpr(IEnumerable<IBoundExpr> fields, IBoundDecl type) : base(fields)
        {
            mType = type;
        }

        public IBoundDecl Type
        {
            get
            {
                // allow overriding the type. this lets us use bound tuples for other distinct
                // types
                //### bob: this is a work-in-progress. eventually, this should become a 
                // StructureExpr that's used for constructing all structure objects: structs,
                // arrays, tuples, unions. the idea is to reduce the number of bound
                // expression classes.
                if (mType != null) return mType;

                return new BoundTupleType(Fields.ConvertAll(field => field.Type));
            }
        }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        private IBoundDecl mType;
    }
}
