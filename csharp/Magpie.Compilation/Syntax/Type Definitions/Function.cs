using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A function definition.
    /// </summary>
    public class Function : TypeDefinition, ICallable
    {
        public FuncType Type { get; private set; }

        public readonly Expr Body;

        public int NumLocals { get; private set; }

        public Function(TokenPosition position, string name, FuncType type, IUnboundExpr body)
            : this(position, name, type, body, null, false) { }

        public Function(TokenPosition position, string name, FuncType type, IUnboundExpr body,
            IEnumerable<IBoundDecl> typeArgs, bool hasInferrableTypeArguments)
            : base(position, name, typeArgs)
        {
            if (position == null) throw new ArgumentNullException("position");
            if (type == null) throw new ArgumentNullException("type");

            Body = new Expr(body);

            Type = type;
            HasInferrableTypeArguments = hasInferrableTypeArguments;
        }

        public void Bind(IBoundExpr body, int numLocals)
        {
            Body.Bind(body);
            NumLocals = numLocals;
        }

        #region ICallable Members

        public IBoundExpr CreateCall(IBoundExpr arg)
        {
            return new BoundCallExpr(new BoundFuncRefExpr(this), arg);
        }

        public bool HasInferrableTypeArguments { get; private set; }

        public IBoundDecl[] ParameterTypes { get { return Type.ParameterTypes; } }

        #endregion
    }
}
