using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;

namespace Magpie.Compilation
{
    public static class ObjectExtensions
    {
        public static TReturn Match<TReturn>(this object obj, IPattern pattern,
            Func<AnyPattern, TReturn> anyCallback,
            Func<BoolPattern, TReturn> boolCallback,
            Func<IntPattern, TReturn> intCallback,
            Func<StringPattern, TReturn> stringCallback,
            Func<UnionPattern, TReturn> unionCallback,
            Func<TuplePattern, TReturn> tupleCallback)
        {
            return pattern.Accept(new PatternDelegates<TReturn>(
                anyCallback,
                boolCallback,
                intCallback,
                stringCallback,
                unionCallback,
                tupleCallback));
        }

        public static void Match(this object obj, IPattern pattern,
            Action<AnyPattern> anyCallback,
            Action<BoolPattern> boolCallback,
            Action<IntPattern> intCallback,
            Action<StringPattern> stringCallback,
            Action<UnionPattern> unionCallback,
            Action<TuplePattern> tupleCallback)
        {
            // dummy bool return values
            Match(obj, pattern,
                a => { anyCallback(a); return false; },
                a => { boolCallback(a); return false; },
                a => { intCallback(a); return false; },
                a => { stringCallback(a); return false; },
                a => { unionCallback(a); return false; },
                a => { tupleCallback(a); return false; });
        }

        private class PatternDelegates<TReturn> : IPatternVisitor<TReturn>
        {
            public PatternDelegates(
                Func<AnyPattern, TReturn> anyCallback,
                Func<BoolPattern, TReturn> boolCallback,
                Func<IntPattern, TReturn> intCallback,
                Func<StringPattern, TReturn> stringCallback,
                Func<UnionPattern, TReturn> unionCallback,
                Func<TuplePattern, TReturn> tupleCallback)
            {
                mAnyCallback = anyCallback;
                mBoolCallback = boolCallback;
                mIntCallback = intCallback;
                mStringCallback = stringCallback;
                mUnionCallback = unionCallback;
                mTupleCallback = tupleCallback;
            }

            #region IPatternVisitor<TReturn> Members

            TReturn IPatternVisitor<TReturn>.Visit(AnyPattern expr) { return mAnyCallback(expr); }
            TReturn IPatternVisitor<TReturn>.Visit(BoolPattern expr) { return mBoolCallback(expr); }
            TReturn IPatternVisitor<TReturn>.Visit(IntPattern expr) { return mIntCallback(expr); }
            TReturn IPatternVisitor<TReturn>.Visit(StringPattern expr) { return mStringCallback(expr); }
            TReturn IPatternVisitor<TReturn>.Visit(UnionPattern expr) { return mUnionCallback(expr); }
            TReturn IPatternVisitor<TReturn>.Visit(TuplePattern expr) { return mTupleCallback(expr); }

            #endregion

            private Func<AnyPattern, TReturn> mAnyCallback;
            private Func<BoolPattern, TReturn> mBoolCallback;
            private Func<IntPattern, TReturn> mIntCallback;
            private Func<StringPattern, TReturn> mStringCallback;
            private Func<UnionPattern, TReturn> mUnionCallback;
            private Func<TuplePattern, TReturn> mTupleCallback;
        }
    }
}
