using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface ICaseExprVisitor<TReturn>
    {
        TReturn Visit(AnyCase expr);
        TReturn Visit(LiteralCase expr);
        /*
        TReturn Visit(BoolCase expr);
        TReturn Visit(IntCase expr);
        TReturn Visit(StringCase expr);
         */
        TReturn Visit(UnionCaseCase expr);
        TReturn Visit(TupleCase expr);
    }
}
