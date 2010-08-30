using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A union type definition.
    /// </summary>
    public class Union : Definition, INamedType
    {
        public ReadOnlyCollection<UnionCase> Cases { get; private set; }

        public Union(Position position, string name, IEnumerable<UnionCase> cases)
            : base(position, name)
        {
            Cases = new ReadOnlyCollection<UnionCase>(new List<UnionCase>(cases));
        }

        public IEnumerable<ICallable> BuildFunctions()
        {
            // constructors for each case
            foreach (var unionCase in Cases)
            {
                yield return new UnionConstructor(this, unionCase);
                yield return new UnionCaseChecker(this, unionCase);

                if (unionCase.ValueType.Bound != Decl.Unit)
                {
                    yield return new UnionValueGetter(this, unionCase);
                }
            }
        }

        /// <summary>
        /// Creates a new deep copy of this structure in unbound form.
        /// </summary>
        public Union Clone(IEnumerable<IBoundDecl> typeArguments)
        {
            var union = new Union(Position, BaseName,
                Cases.Select(unionCase => new UnionCase(unionCase.Name,
                    unionCase.ValueType.Unbound.Clone(), unionCase.Index)));

            union.BindSearchSpace(SearchSpace);
            union.BindTypeArguments(typeArguments);

            return union;
        }

        #region IBoundDecl Members

        TReturn IBoundDecl.Accept<TReturn>(IBoundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }

    public class UnionCase
    {
        public Union Union { get; private set; }
        public string Name { get; private set; }
        public Decl ValueType { get; private set; }

        public int Index { get; private set; }

        public bool HasInferrableTypeArguments { get; set; }

        public UnionCase(string name, IUnboundDecl valueType, int index)
        {
            if (valueType == null) throw new ArgumentNullException("valueType");

            Name = name;
            ValueType = new Decl(valueType);
            Index = index;
        }

        public void SetUnion(Union union)
        {
            if (union == null) throw new ArgumentNullException("union");
            Union = union;
        }
    }
}
