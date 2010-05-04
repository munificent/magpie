using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IUnboundDecl
    {
        Position Position { get; }
        TReturn Accept<TReturn>(IUnboundDeclVisitor<TReturn> visitor);
    }

    public static class UnboundDeclExtensions
    {
        public static IUnboundDecl[] Expand(this IUnboundDecl decl)
        {
            // the unit type expands to no values
            if (ReferenceEquals(decl, Decl.Unit)) return new IUnboundDecl[0];

            // a tuple expands to its fields
            TupleType tuple = decl as TupleType;
            if (tuple != null)
            {
                return tuple.Fields.ToArray();
            }

            // everything else expands to just itself
            return new IUnboundDecl[] { decl };
        }

        public static T Match<T>(this IUnboundDecl decl,
                                 Func<AtomicDecl, T> atomicCallback,
                                 Func<FuncType, T> funcCallback,
                                 Func<RecordType, T> recordCallback,
                                 Func<TupleType, T> tupleCallback,
                                 Func<NamedType, T> namedCallback)
        {
            if (decl == null) throw new ArgumentNullException("decl");

            var atomic = decl as AtomicDecl;
            if (atomic != null) return atomicCallback(atomic);

            var func = decl as FuncType;
            if (func != null) return funcCallback(func);

            var record = decl as RecordType;
            if (record != null) return recordCallback(record);

            var tuple = decl as TupleType;
            if (tuple != null) return tupleCallback(tuple);

            var named = decl as NamedType;
            if (named != null) return namedCallback(named);

            throw new ArgumentException("Unknown declaration type.");
        }

        public static IUnboundDecl Clone(this IUnboundDecl decl)
        {
            return decl.Match<IUnboundDecl>(
                            // atomic types are immutable, so we can reuse it
                atomic =>   atomic,
                func   =>   func.CloneFunc(),
                record =>   {
                                var fields = new Dictionary<string, IUnboundDecl>();
                                foreach (var field in record.Fields)
                                {
                                    fields.Add(field.Key, field.Value.Clone());
                                }

                                return new RecordType(fields);
                            },
                tuple  =>   new TupleType(tuple.Fields.Select(field => field.Clone())),
                            // named types are immutable, so we can reuse it
                named  =>   named);
        }
    }
}
