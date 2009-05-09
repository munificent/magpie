using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class TypeInstancer : IDeclVisitor<Decl>
    {
        public static FuncType Instance(TypeArgApplicator applicator, FuncType type)
        {
            return (FuncType)type.Accept(new TypeInstancer(applicator));
        }

        #region IDeclVisitor Members

        public Decl Visit(AnyType decl)
        {
            return decl;
        }

        public Decl Visit(ArrayType decl)
        {
            return new ArrayType(decl.ElementType.Accept(this), decl.IsMutable);
        }

        public Decl Visit(AtomicDecl decl)
        {
            return decl;
        }

        public Decl Visit(FuncType decl)
        {
            // translate the args and return
            return new FuncType(decl.Parameters.Select(arg => new ParamDecl(arg.Name, arg.Type.Accept(this))),
                decl.Return.Accept(this));
        }

        public Decl Visit(NamedType decl)
        {
            return mApplicator.ApplyType(decl);
        }

        public Decl Visit(TupleType decl)
        {
            // translate the fields
            return new TupleType(decl.Fields.Accept(this));
        }

        #endregion

        private TypeInstancer(TypeArgApplicator applicator)
        {
            mApplicator = applicator;
        }

        private TypeArgApplicator mApplicator;
    }
}
