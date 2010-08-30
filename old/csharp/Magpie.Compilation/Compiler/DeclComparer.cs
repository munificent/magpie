using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class DeclComparer
    {
        public static bool TypesMatch(IBoundDecl parameter, IBoundDecl argument)
        {
            // if they're the same object, they must match
            if (ReferenceEquals(parameter, argument)) return true;

            // if they're different types, they can't match
            if (!parameter.GetType().Equals(argument.GetType())) return false;

            // types that return false here do so because if they did match,
            // the above ReferenceEquals check should have been true. since
            // we got here, they must not match.
            return parameter.Match(
                atomic      =>  false,
                array       =>  TypesMatch(array.ElementType, ((BoundArrayType)argument).ElementType),
                func        =>  {
                                    var argFunc = (FuncType)argument;
                                    return TypesMatch(func.Parameter.Bound, argFunc.Parameter.Bound) &&
                                           TypesMatch(func.Return.Bound, argFunc.Return.Bound);
                                },
                record      =>  {
                                    var argRecord = (BoundRecordType)argument;
                                    if (record.Fields.Count != argRecord.Fields.Count) return false;

                                    // fields must match
                                    foreach (var pair in record.Fields.Zip(argRecord.Fields))
                                    {
                                        if (pair.Item1.Key != pair.Item2.Key) return false;
                                        if (!TypesMatch(pair.Item1.Value, pair.Item2.Value)) return false;
                                    }

                                    return true;
                                },
                tuple       =>  {
                                    var argTuple = (BoundTupleType)argument;
                                    if (tuple.Fields.Count != argTuple.Fields.Count) return false;

                                    // fields must match
                                    return tuple.Fields.Zip(argTuple.Fields).All(TypesMatch);
                                },
                structType  =>  false,
                union       =>  false,
                foreign     =>  foreign.Name == ((ForeignType)argument).Name);
        }
    }
}
