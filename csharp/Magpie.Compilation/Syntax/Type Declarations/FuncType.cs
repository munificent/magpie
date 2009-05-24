using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A function type declaration.
    /// </summary>
    public class FuncType : IUnboundDecl, IBoundDecl
    {
        public static FuncType Create(params IBoundDecl[] paramsAndReturn)
        {
            if (paramsAndReturn.Length == 0) throw new ArgumentException("Must provide at least the return type.");

            // pull out the parameters
            var parameters = new List<ParamDecl>();
            if (paramsAndReturn.Length > 1)
            {
                for (int i = 0; i < paramsAndReturn.Length - 1; i++)
                {
                    parameters.Add(new ParamDecl("arg" + i, paramsAndReturn[i]));
                }
            }

            return new FuncType(parameters,
                paramsAndReturn[paramsAndReturn.Length - 1]);
        }

        public Position Position { get; private set; }

        public readonly List<ParamDecl> Parameters = new List<ParamDecl>();

        public readonly Decl Return;

        /// <summary>
        /// Gets the parameter types for the function.
        /// </summary>
        public IUnboundDecl[] UnboundParameterTypes
        {
            get { return Parameters.ConvertAll(arg => arg.Type.Unbound).ToArray(); }
        }

        /// <summary>
        /// Gets the parameter types for the function.
        /// </summary>
        public IBoundDecl[] ParameterTypes
        {
            get { return Parameters.ConvertAll(arg => arg.Type.Bound).ToArray(); }
        }

        public FuncType(Position position, IEnumerable<ParamDecl> parameters, IUnboundDecl returnType)
        {
            if (parameters == null) throw new ArgumentNullException("parameters");
            if (returnType == null) throw new ArgumentNullException("returnType");

            Position = position;
            Parameters.AddRange(parameters);
            Return = new Decl(returnType);
        }

        public FuncType(IEnumerable<ParamDecl> parameters, IUnboundDecl returnType)
            : this (Position.None, parameters, returnType)
        {
        }

        public FuncType(IEnumerable<ParamDecl> parameters, IBoundDecl returnType)
        {
            if (parameters == null) throw new ArgumentNullException("parameters");
            if (returnType == null) throw new ArgumentNullException("returnType");

            Parameters.AddRange(parameters);
            Return = new Decl(returnType);
        }

        public override string ToString()
        {
            if ((Parameters.Count == 0) && ReferenceEquals(Return, Decl.Unit)) return "(->)";
            if (Parameters.Count == 0) return "(-> " + Return.ToString() + ")";

            string argString = Parameters.JoinAll(", ");

            if (ReferenceEquals(Return, Decl.Unit)) return "(" + argString + " ->)";

            return "(" + argString + " -> " + Return.ToString() + ")";
        }

        /// <summary>
        /// Creates a copy of this FuncType in unbound form.
        /// </summary>
        /// <returns></returns>
        public FuncType Clone()
        {
            var parameters = Parameters.Select(p => new ParamDecl(p.Name, p.Type.Unbound));
            return new FuncType(Position, parameters, Return.Unbound);
        }

        #region IUnboundDecl Members

        TReturn IUnboundDecl.Accept<TReturn>(IUnboundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion

        #region IBoundDecl Members

        TReturn IBoundDecl.Accept<TReturn>(IBoundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }

    public class ParamDecl
    {
        public string Name { get; private set; }

        public readonly Decl Type;

        public ParamDecl(string name, Decl type)
        {
            if (type == null) throw new ArgumentNullException("type");

            Name = name;
            Type = type;
        }

        public ParamDecl(string name, IUnboundDecl type)
            : this(name, new Decl(type))
        {
        }

        public ParamDecl(string name, IBoundDecl type)
            : this(name, new Decl(type))
        {
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
