using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IPatternVisitor<TReturn>
    {
        TReturn Visit(AnyPattern expr);
        TReturn Visit(BoolPattern expr);
        TReturn Visit(IntPattern expr);
        TReturn Visit(StringPattern expr);
        TReturn Visit(UnionPattern expr);
        TReturn Visit(TuplePattern expr);
    }
}
