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

        public IEnumerable<GenericStruct> GenericStructs { get { return mGenericStructs; } }
        public IEnumerable<GenericUnion> GenericUnions { get { return mGenericUnions; } }

        public TypeTable()
        {
            // add the built-in types
            Add(Decl.Unit);
            Add(Decl.Bool);
            Add(Decl.Int);
            Add(Decl.String);
        }

        public void BindAll(Compiler compiler)
        {
            // copy the types to a queue because binding a type may cause
            // other generic types to be instantiated, adding to the type
            // table.
            mToBind = new Queue<INamedType>(mTypes.Values.Where(type => (type is Struct) || (type is Union)));

            while (mToBind.Count > 0)
            {
                INamedType type = mToBind.Dequeue();

                //### bob: call the appropriate bind function. this is gross.
                // should use an overridden method or something.
                Struct structure = type as Struct;
                if (structure != null)
                {
                    TypeBinder.Bind(compiler, structure);
                }
                else
                {
                    TypeBinder.Bind(compiler, (Union)type);
                }
            }

            mToBind = null;
        }

        public void Add(INamedType type)
        {
            Add(type, null);
        }

        public void Add(INamedType type, IEnumerable<IBoundDecl> typeArgs)
        {
            var uniqueName = GetUniqueName(type.Name, typeArgs);

            if (mTypes.ContainsKey(uniqueName)) throw new CompileException(type.Position, "There is already a type named " + uniqueName + ".");

            mTypes[uniqueName] = type;

            /*
            // if we are in the middle of binding, make sure we bind this type too
            if (mToBind != null)
            {
                mToBind.Enqueue(type);
            }*/
        }

        public void Add(GenericStruct generic)
        {
            mGenericStructs.Add(generic);
        }

        public void Add(GenericUnion generic)
        {
            mGenericUnions.Add(generic);
        }

        public Struct FindStruct(string name, IEnumerable<IBoundDecl> typeArgs)
        {
            var uniqueName = GetUniqueName(name, typeArgs);

            INamedType type;
            if (mTypes.TryGetValue(uniqueName, out type))
            {
                // may be a union, so cast using "as"
                return type as Struct;
            }

            return null;
        }

        //### bob: copy/paste from struct :(
        public Union FindUnion(string name, IEnumerable<IBoundDecl> typeArgs)
        {
            var uniqueName = GetUniqueName(name, typeArgs);

            INamedType type;
            if (mTypes.TryGetValue(uniqueName, out type))
            {
                // may be a struct, so cast using "as"
                return type as Union;
            }

            return null;
        }

        public INamedType Find(Compiler compiler, NameSearchSpace searchSpace, Position position,
            string name, IEnumerable<IBoundDecl> typeArgs)
        {
            // look through the namespaces
            foreach (var potentialName in searchSpace.SearchFor(name))
            {
                // look for a concrete type
                var type = Find(potentialName, typeArgs);
                if (type != null) return type;

                // look for a generic
                foreach (var structure in mGenericStructs)
                {
                    // names must match
                    if (structure.Name != potentialName) continue;

                    // number of type args must match
                    if (typeArgs.Count() != structure.TypeParameters.Count) continue;

                    var instance = structure.Instantiate(compiler, typeArgs);

                    // only instantiate once
                    //Add(instance, typeArgs);

                    return instance;
                }

                //### bob: gross copy/paste of above
                // look for a generic
                foreach (var union in mGenericUnions)
                {
                    // names must match
                    if (union.Name != potentialName) continue;

                    // number of type args must match
                    if (typeArgs.Count() != union.TypeParameters.Count) continue;

                    var instance = union.Instantiate(compiler, typeArgs);

                    // only instantiate once
                    //Add(instance, typeArgs);

                    return instance;
                }
            }

            // not found
            throw new CompileException(position, "Could not find a type named " + name + ".");
        }

        private INamedType Find(string name, IEnumerable<IBoundDecl> typeArgs)
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

        private readonly Dictionary<string, INamedType> mTypes = new Dictionary<string, INamedType>();
        private readonly List<GenericStruct> mGenericStructs = new List<GenericStruct>();
        private readonly List<GenericUnion> mGenericUnions = new List<GenericUnion>();

        private Queue<INamedType> mToBind;
    }
}
