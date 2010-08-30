using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IBoundDecl
    {
        TReturn Accept<TReturn>(IBoundDeclVisitor<TReturn> visitor);
    }

    public static class BoundDeclExtensions
    {
        public static IBoundDecl[] Expand(this IBoundDecl decl)
        {
            // the unit type expands to no values
            if (ReferenceEquals(decl, Decl.Unit)) return new IBoundDecl[0];

            // a tuple expands to its fields
            BoundTupleType tuple = decl as BoundTupleType;
            if (tuple != null)
            {
                return tuple.Fields.ToArray();
            }

            // everything else expands to just itself
            return new IBoundDecl[] { decl };
        }

        public static T Match<T>(this IBoundDecl decl,
                                 Func<AtomicDecl, T> atomicCallback,
                                 Func<BoundArrayType, T> arrayCallback,
                                 Func<FuncType, T> funcCallback,
                                 Func<BoundRecordType, T> recordCallback,
                                 Func<BoundTupleType, T> tupleCallback,
                                 Func<Struct, T> structCallback,
                                 Func<Union, T> unionCallback,
                                 Func<ForeignType, T> foreignCallback)
        {
            if (decl == null) throw new ArgumentNullException("decl");

            var atomic = decl as AtomicDecl;
            if (atomic != null) return atomicCallback(atomic);

            var array = decl as BoundArrayType;
            if (array != null) return arrayCallback(array);

            var func = decl as FuncType;
            if (func != null) return funcCallback(func);

            var record = decl as BoundRecordType;
            if (record != null) return recordCallback(record);

            var tuple = decl as BoundTupleType;
            if (tuple != null) return tupleCallback(tuple);

            var structType = decl as Struct;
            if (structType != null) return structCallback(structType);

            var union = decl as Union;
            if (union != null) return unionCallback(union);

            var foreign = decl as ForeignType;
            if (foreign != null) return foreignCallback(foreign);

            throw new ArgumentException("Unknown declaration type.");
        }
    }
}
