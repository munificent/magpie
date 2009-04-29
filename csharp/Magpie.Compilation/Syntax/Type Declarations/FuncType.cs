using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A function type declaration.
    /// </summary>
    public class FuncType : Decl
    {
        public static FuncType Create(Decl returnType)
        {
            return new FuncType(new ParamDecl[0], returnType);
        }

        public static FuncType Create(Decl arg, Decl returnType)
        {
            return new FuncType(new ParamDecl[] { new ParamDecl("a", arg) }, returnType);
        }

        public static FuncType Create(Decl arg1, Decl arg2, Decl returnType)
        {
            return new FuncType(new ParamDecl[] { new ParamDecl("a", arg1), new ParamDecl("b", arg2) }, returnType);
        }

        public static FuncType Create(Decl arg1, Decl arg2, Decl arg3, Decl returnType)
        {
            return new FuncType(new ParamDecl[] { new ParamDecl("a", arg1), new ParamDecl("b", arg2), new ParamDecl("c", arg3) }, returnType);
        }

        /// <summary>
        /// Gets the parameter types for the function.
        /// </summary>
        public Decl[] ParameterTypes
        {
            get { return Parameters.ConvertAll(arg => arg.Type).ToArray(); }
        }

        public readonly List<ParamDecl> Parameters = new List<ParamDecl>();
        public Decl Return;

        public FuncType(IEnumerable<ParamDecl> parameters, Decl returnType)
        {
            Parameters.AddRange(parameters);
            Return = returnType;
        }

        public override string ToString()
        {
            if ((Parameters.Count == 0) && (Return == Decl.Unit)) return "(->)";
            if (Parameters.Count == 0) return "(-> " + Return.ToString() + ")";

            string argString = Parameters.JoinAll(", ");

            if (Return == Decl.Unit) return "(" + argString + " ->)";

            return "(" + argString + " -> " + Return.ToString() + ")";
        }

        public override TReturn Accept<TReturn>(IDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    public class ParamDecl
    {
        public string Name;
        public Decl Type;

        public ParamDecl(string name, Decl type)
        {
            Name = name;
            Type = type;
        }

        public override string ToString()
        {
            //### bob: never include the arg name. this is so that function binding, which
            // uses the string representation of the args, works correctly when arguments
            // are function types
            //### bob: this is a hack!
            return Type.ToString();
        }
    }
}
