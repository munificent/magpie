using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public static class IEnumerableExtensions
    {
        public static bool IsEmpty<T>(this IEnumerable<T> enumerable)
        {
            foreach (var item in enumerable)
            {
                return false;
            }

            return true;
        }

        public static IEnumerable<string> AllToString<T>(this IEnumerable<T> enumerable)
        {
            return enumerable.Select(item => item.ToString());
        }

        public static string JoinAll<T>(this IEnumerable<T> enumerable, string separator)
        {
            return String.Join(separator, enumerable.AllToString().ToArray());
        }

        public static IEnumerable<TReturn> Accept<T, TReturn>(this IEnumerable<T> enumerable, IUnboundExprVisitor<TReturn> visitor)
            where T : IUnboundExpr
        {
            // ToArray() is to force select to eager evaluate. makes it much easier to see what's going on in the debugger
            return enumerable.Select(item => item.Accept(visitor)).ToArray();
        }

        public static IEnumerable<TReturn> Accept<T, TReturn>(this IEnumerable<T> enumerable, IBoundExprVisitor<TReturn> visitor)
            where T : IBoundExpr
        {
            // ToArray() is to force select to eager evaluate. makes it much easier to see what's going on in the debugger
            return enumerable.Select(item => item.Accept(visitor)).ToArray();
        }

        public static IEnumerable<TReturn> Accept<T, TReturn>(this IEnumerable<T> enumerable, IBoundDeclVisitor<TReturn> visitor)
            where T : IBoundDecl
        {
            // ToArray() is to force select to eager evaluate. makes it much easier to see what's going on in the debugger
            return enumerable.Select(item => item.Accept(visitor)).ToArray();
        }

        public static IEnumerable<Tuple<T, U>> Zip<T, U>(this IEnumerable<T> enumerable, IEnumerable<U> other)
        {
            var enum1 = enumerable.GetEnumerator();
            var enum2 = other.GetEnumerator();

            while (enum1.MoveNext() && enum2.MoveNext())
            {
                yield return new Tuple<T, U>(enum1.Current, enum2.Current);
            }
        }
    }

    public static class Tuple
    {
        public static Tuple<T, U> Create<T, U>(T item1, U item2)
        {
            return new Tuple<T, U>(item1, item2);
        }

        public static bool All<T, U>(this IEnumerable<Tuple<T, U>> collection, Func<T, U, bool> predicate)
        {
            return collection.All(tuple => predicate(tuple.Item1, tuple.Item2));
        }
    }

    public class Tuple<T, U>
    {
        public T Item1 { get; private set; }
        public U Item2 { get; private set; }

        public Tuple(T item1, U item2)
        {
            Item1 = item1;
            Item2 = item2;
        }
    }
}
