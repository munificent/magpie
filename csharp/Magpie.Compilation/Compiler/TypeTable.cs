using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A symbol table for types.
    /// </summary>
    public class TypeTable
    {
        public IEnumerable<Struct> Structs { get { return mTypes.Values.OfType<Struct>(); } }
        public IEnumerable<Union> Unions { get { return mTypes.Values.OfType<Union>(); } }

        public TypeTable()
        {
            // add the built-in types
            Add(Decl.Unit);
            Add(Decl.Bool);
            Add(Decl.Int);
            Add(Decl.String);
        }

        public void Add(INamedType type)
        {
            Add(type, null);
        }

        //### bob: get rid of type args param, get from INamedType
        public void Add(INamedType type, IEnumerable<IBoundDecl> typeArgs)
        {
            var uniqueName = GetUniqueName(type.Name, typeArgs);

            if (mTypes.ContainsKey(uniqueName)) throw new CompileException(type.Position, "There is already a type named " + uniqueName + ".");

            mTypes[uniqueName] = type;
        }

        public INamedType Find(string name, IEnumerable<IBoundDecl> typeArgs)
        {
            var uniqueName = GetUniqueName(name, typeArgs);

            INamedType type;
            if (mTypes.TryGetValue(uniqueName, out type)) return type;

            // not found
            return null;
        }

        private static string GetUniqueName(string name, IEnumerable<IBoundDecl> typeArgs)
        {
            var args = (typeArgs == null) ? new IBoundDecl[0] : typeArgs.ToArray();

            if (args.Length > 0)
            {
                // an instantiated generic type, so mangle it
                if (args.Length == 1)
                {
                    return name + "'" + args[0].ToString();
                }
                else
                {
                    return name + "'(" + args.JoinAll(", ") + ")";
                }
            }
            else
            {
                // not generic
                return name;
            }
        }

        private Dictionary<string, INamedType> mTypes = new Dictionary<string, INamedType>();
    }
}
