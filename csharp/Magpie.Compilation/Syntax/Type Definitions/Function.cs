using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A function definition.
    /// </summary>
    public class Function : Definition, ICallable
    {
        public FuncType Type { get; private set; }

        public readonly Expr Body;
        public readonly List<string> ParamNames = new List<string>();

        public int NumLocals { get; private set; }

        public Function(Position position, string name, FuncType type, IEnumerable<string> paramNames, IUnboundExpr body)
            : this(position, name, type, paramNames, body, null, false) { }

        public Function(Position position, string name, FuncType type, IEnumerable<string> paramNames, IUnboundExpr body,
            IEnumerable<IBoundDecl> typeArgs, bool hasInferrableTypeArguments)
            : base(position, name, typeArgs)
        {
            if (position == null) throw new ArgumentNullException("position");
            if (type == null) throw new ArgumentNullException("type");

            if (paramNames != null)
            {
                ParamNames.AddRange(paramNames);
            }

            Body = new Expr(body);

            Type = type;
            HasInferrableTypeArguments = hasInferrableTypeArguments;
        }

        public void Bind(BindingContext context, FunctionBinder binder)
        {
            Body.Bind(context, binder);
            NumLocals = binder.Scope.NumVariables;
        }

        #region ICallable Members

        public IBoundExpr CreateCall(IBoundExpr arg)
        {
            return new BoundCallExpr(new BoundFuncRefExpr(this), arg);
        }

        public bool HasInferrableTypeArguments { get; private set; }

        public IBoundDecl ParameterType { get { return Type.Parameter.Bound; } }

        #endregion
    }
}
