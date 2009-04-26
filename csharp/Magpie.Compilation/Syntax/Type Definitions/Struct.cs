using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A structure type definition.
    /// </summary>
    public class Struct : TypeDefinition
    {
        public readonly List<Field> Fields = new List<Field>();

        public Field this[string name] { get { return Fields.Find(field => field.Name == name); } }

        public Struct(string name, IEnumerable<Decl> typeParams, IEnumerable<Field> fields)
            : base(name, typeParams)
        {
            if (fields != null)
            {
                byte index = 0;
                foreach (Field field in fields)
                {
                    Fields.Add(field);
                    field.SetIndex(index++);
                }
            }
        }

        public bool Contains(string name)
        {
            return Fields.Any(field => field.Name == name);
        }

        public void Define(string name, Decl type, bool isMutable)
        {
            Fields.Add(new Field(name, type, isMutable, (byte)Fields.Count));
        }
    }

    public class Field
    {
        public string Name;
        public Decl Type;
        public bool IsMutable;
        public byte Index { get { return mIndex; } }

        public Field(string name, Decl type, bool isMutable)
        {
            Name = name;
            Type = type;
            IsMutable = isMutable;
        }

        public Field(string name, Decl type, bool isMutable, byte index)
        {
            Name = name;
            Type = type;
            IsMutable = isMutable;
            mIndex = index;
        }

        public void SetIndex(byte index)
        {
            mIndex = index;
        }

        private byte mIndex;
    }
}
