using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public static class IEnumerableExtensions
    {
        public static IEnumerable<string> AllToString<T>(this IEnumerable<T> enumerable)
        {
            return enumerable.Select(item => item.ToString());
        }

        public static string JoinAll<T>(this IEnumerable<T> enumerable, string separator)
        {
            return String.Join(separator, enumerable.AllToString().ToArray());
        }

        public static IEnumerable<TReturn> Accept<T, TReturn>(this IEnumerable<T> enumerable, IBoundExprVisitor<TReturn> visitor)
            where T : IBoundExpr
        {
            return enumerable.Select(item => item.Accept(visitor));
        }

        public static IEnumerable<TReturn> Accept<T, TReturn>(this IEnumerable<T> enumerable, IDeclVisitor<TReturn> visitor)
            where T : Decl
        {
            return enumerable.Select(item => item.Accept(visitor));
        }

        public static IEnumerable<TReturn> Accept<T, TReturn>(this IEnumerable<T> enumerable, IUnboundExprVisitor<TReturn> visitor)
            where T : IUnboundExpr
        {
            return enumerable.Select(item => item.Accept(visitor));
        }
    }
}
