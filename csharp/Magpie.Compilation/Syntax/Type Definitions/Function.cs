using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A function definition.
    /// </summary>
    public class Function : TypeDefinition
    {
        public IUnboundExpr Body { get { return mBody; } }

        public override Decl Type { get { return mType; } }
        public FuncType FuncType { get { return mType; } }

        public bool CanOmitTypeArgs { get { return mCanOmitTypeArgs; } }

        public Function(string name, IEnumerable<Decl> typeParams, FuncType type, IUnboundExpr body)
            : this(name, typeParams, type, body, false) { }

        public Function(string name, IEnumerable<Decl> typeParams, FuncType type, IUnboundExpr body, bool canOmitTypeArgs)
            : base(name, typeParams)
        {
            mType = type;
            mBody = body;

            mCanOmitTypeArgs = canOmitTypeArgs;
        }

        public override string ToString()
        {
            return String.Format("{0}{1} {2} {3}",
                Name,
                IsGeneric ? ("[" + TypeParameters.JoinAll(", ") + "]") : "",
                Type, Body);
        }

        public bool Matches(string uniqueName)
        {
            return (uniqueName == mUniqueName) || (uniqueName == mInferredName);
        }

        protected override void OnQualify()
        {
            mUniqueName = FunctionTable.GetUniqueName(FullName, TypeParameters, FuncType.ParameterTypes);

            if (mCanOmitTypeArgs)
            {
                mInferredName = FunctionTable.GetUniqueName(FullName, FuncType.ParameterTypes);
            }
            else
            {
                mInferredName = String.Empty;
            }
        }

        private FuncType mType;
        private IUnboundExpr mBody;

        private bool mCanOmitTypeArgs; // true if the type arguments can be inferred from the value args, so do not need to be explicitly passed in
        private string mUniqueName;
        private string mInferredName;
    }
}
