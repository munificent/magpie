using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class Namespace
    {
        public string Name;

        public readonly List<Namespace> Namespaces = new List<Namespace>();
        public readonly List<Function> Functions = new List<Function>();
        public readonly List<Struct> Structs = new List<Struct>();
        public readonly List<Union> Unions = new List<Union>();
        public readonly List<GenericFunction> GenericFunctions = new List<GenericFunction>();
        public readonly List<GenericStruct> GenericStructs = new List<GenericStruct>();
        public readonly List<GenericUnion> GenericUnions = new List<GenericUnion>();

        public Namespace(string name)
        {
            Name = name;
        }

        public override string ToString()
        {
            return Name + ":";
        }
    }
}
