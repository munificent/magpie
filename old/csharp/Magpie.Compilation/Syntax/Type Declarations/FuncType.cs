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

            // no parameters
            if (paramsAndReturn.Length == 1)
            {
                return new FuncType(Position.None, Decl.Unit, paramsAndReturn[0]);
            }

            // one parameter
            if (paramsAndReturn.Length == 2)
            {
                return new FuncType(Position.None, paramsAndReturn[0], paramsAndReturn[1]);
            }

            // tuple parameter
            var parameters = new List<IBoundDecl>();
            if (paramsAndReturn.Length > 1)
            {
                for (int i = 0; i < paramsAndReturn.Length - 1; i++)
                {
                    parameters.Add(paramsAndReturn[i]);
                }
            }

            return new FuncType(Position.None, new BoundTupleType(parameters),
                paramsAndReturn[paramsAndReturn.Length - 1]);
        }

        public Position Position { get; private set; }

        public readonly Decl Parameter;

        public readonly Decl Return;

        public FuncType(Position position, IUnboundDecl parameter, IUnboundDecl returnType)
        {
            if (parameter == null) throw new ArgumentNullException("parameter");
            if (returnType == null) throw new ArgumentNullException("returnType");

            Position = position;

            Parameter = new Decl(parameter);
            Return = new Decl(returnType);
        }

        public FuncType(Position position, IBoundDecl parameter, IBoundDecl returnType)
        {
            if (parameter == null) throw new ArgumentNullException("parameter");
            if (returnType == null) throw new ArgumentNullException("returnType");

            Position = position;

            Parameter = new Decl(parameter);
            Return = new Decl(returnType);
        }

        public override string ToString()
        {
            return "(" + Parameter.ToString() + "->" + Return.ToString() + ")";
        }

        /// <summary>
        /// Creates a copy of this FuncType in unbound form.
        /// </summary>
        /// <returns></returns>
        public FuncType CloneFunc()
        {
            return new FuncType(Position, Parameter.Unbound.Clone(), Return.Unbound.Clone());
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
}
