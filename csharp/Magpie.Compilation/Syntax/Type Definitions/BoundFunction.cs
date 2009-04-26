using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class BoundFunction
    {
        //### bob: is this the best place for this?
        public static string GetUniqueName(string name, IEnumerable<Decl> typeArgs, IEnumerable<Decl> paramTypes)
        {
            string typeArgString = ((typeArgs != null) && typeArgs.Any()) ? "[" + typeArgs.JoinAll(", ") + "]" : "";
            string argTypes = "(" + paramTypes.JoinAll(", ") + ")";
            return name + "__" + typeArgString + argTypes;
        }

        /// <summary>
        /// A unique name for the function, including type arguments and value argument types.
        /// </summary>
        public string Name { get { return mName; } }

        public Scope Locals { get { return mLocals; } }

        public IBoundExpr Body { get { return mBody; } }
        
        public FuncType Type { get { return mType; } }

        public BoundFunction(string name, string inferredName, FuncType funcType, Scope locals)
        {
            mName = name;
            mInferredName = inferredName;

            mType = funcType;
            mLocals = locals;
        }

        /// <summary>
        /// Sets the bound body for the function. Happens after construction so that the bound function
        /// can be created before the body is bound, allowing the body to have bound recursive references
        /// to itself.
        /// </summary>
        /// <param name="body"></param>
        public void SetBody(IBoundExpr body)
        {
            mBody = body;
        }

        public bool Matches(string uniqueName)
        {
            return (uniqueName == mName) || (uniqueName == mInferredName);
        }

        public override string ToString()
        {
            return mName;
        }

        private readonly string mName;
        private readonly string mInferredName;
        private readonly FuncType mType;
        private readonly Scope mLocals;
        private IBoundExpr mBody;
    }
}
