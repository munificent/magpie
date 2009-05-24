using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A named type declaration.
    /// </summary>
    public class NamedType : IUnboundDecl
    {
        public Position Position { get; private set; }

        public string Name { get; private set; }
        public IUnboundDecl[] TypeArgs { get { return mTypeArgs; } }

        public bool IsGeneric { get { return mTypeArgs.Length > 0; } }

        public NamedType(string name)
        {
            Position = Position.None;
            Name = name;
            mTypeArgs = new IUnboundDecl[0];
        }

        public NamedType(string name, IEnumerable<IUnboundDecl> typeArgs)
        {
            Position = Position.None;
            Name = name;
            mTypeArgs = typeArgs.ToArray();
        }

        public NamedType(Tuple<string, Position> args)
        {
            Position = args.Item2;
            Name = args.Item1;
            mTypeArgs = new IUnboundDecl[0];
        }

        public NamedType(Tuple<string, Position> args, IEnumerable<IUnboundDecl> typeArgs)
            : this(args)
        {
            if (typeArgs != null)
            {
                mTypeArgs = typeArgs.ToArray();
            }
        }

        public override string ToString()
        {
            if (mTypeArgs.Length > 0) return Name + "[" + mTypeArgs.JoinAll(", ") + "]";

            return Name;
        }

        #region IUnboundDecl Members

        TReturn IUnboundDecl.Accept<TReturn>(IUnboundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion

        private IUnboundDecl[] mTypeArgs;
    }
}
