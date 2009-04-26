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
    public class Union : TypeDefinition
    {
        public ReadOnlyCollection<UnionCase> Cases { get { return mCases; } }

        public Union(string name, IEnumerable<Decl> typeParams, IEnumerable<UnionCase> cases)
            : base(name, typeParams)
        {
            mCases = new ReadOnlyCollection<UnionCase>(new List<UnionCase>(cases));

            for (int i = 0; i < mCases.Count; i++)
            {
                mCases[i].SetUnion(this);
            }
        }

        private readonly ReadOnlyCollection<UnionCase> mCases;
    }

    public class UnionCase
    {
        public string Name { get { return mName; } }
        public Decl ValueType { get { return mValueType; } }

        public Union Union { get { return mUnion; } }
        public int Index
        {
            get
            {
                if (mUnion != null) return mUnion.Cases.IndexOf(this);

                return mIndex;
            }
        }

        public UnionCase(string name, Decl valueType)
        {
            mName = name;
            mValueType = valueType;
        }

        public void SetUnion(Union union)
        {
            // only set once
            if (mUnion != null) throw new InvalidOperationException();

            mUnion = union;
        }

        public void SetIndex(int index)
        {
            mIndex = index;
        }

        public override string ToString()
        {
            return mUnion.Name + "/" + mName;
        }

        private int mIndex = -1; // used for instanced generic cases that are not bound to a union
        private string mName;
        private Union mUnion;
        private Decl mValueType;
    }
}
