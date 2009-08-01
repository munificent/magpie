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
        TReturn Visit(NameCase expr);
        TReturn Visit(CallCase expr);
        TReturn Visit(TupleCase expr);
    }
}
