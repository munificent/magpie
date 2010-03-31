using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class Cover
    {
        public const int InfiniteSpan = -1;

        public static IEnumerable<int> GetDimensions(IBoundDecl type)
        {
            if (type == Decl.Unit)
            {
                yield return 1;
            }
            else if (type == Decl.Bool)
            {
                yield return 2;
            }
            else if (type == Decl.Int)
            {
                yield return InfiniteSpan;
            }
            else if (type == Decl.String)
            {
                yield return InfiniteSpan;
            }
            else if (type is Union)
            {
                var union = (Union)type;
                yield return union.Cases.Count;
            }
            else if (type is BoundTupleType)
            {
                var tuple = (BoundTupleType)type;

                // flatten out nested tuples
                foreach (var field in tuple.Fields)
                {
                    foreach (var inner in GetDimensions(field))
                    {
                        yield return inner;
                    }
                }
            }
            else
            {
                throw new ArgumentException("Unknown declaration type.");
            }
        }

        public static Cover Create(IBoundDecl valueType, IPattern pattern)
        {
            var cover = new Cover(valueType);

            if (pattern is AnyPattern)
            {
                cover.mIsFull = true;
            }
            else if (pattern is BoolPattern)
            {
                var boolPattern = (BoolPattern)pattern;
                int[] value = new int[] { boolPattern.Value ? 1 : 0 };
                cover.Insert(value, Cover.Full);
            }
            else if (pattern is IntPattern)
            {
                var intPattern = (IntPattern)pattern;
                int[] value = new int[] { intPattern.Value };
                cover.Insert(value, Cover.Full);
            }
            else if (pattern is StringPattern)
            {
                var stringPattern = (StringPattern)pattern;
                throw new NotImplementedException();
            }
            else if (pattern is TuplePattern)
            {
                throw new NotImplementedException();
            }
            else if (pattern is UnionPattern)
            {
                var unionPattern = (UnionPattern)pattern;
                var matchedCase = ((Union)valueType).Cases.First(unionCase => unionCase.Name == unionPattern.Name);
                int[] value = new int[] { matchedCase.Index };

                var valueCover = Cover.Full;
                if (unionPattern.Value != null)
                {
                    valueCover = Create(matchedCase.ValueType.Bound, unionPattern.Value);
                }

                cover.Insert(value, valueCover);
            }
            else if (pattern is VariablePattern)
            {
                cover.mIsFull = true;
            }
            else
            {
                throw new ArgumentException("Unexpected pattern type.");
            }

            return cover;
        }

        public static Cover Full { get { return sFull; } }

        public Cover(IBoundDecl type)
            : this(GetDimensions(type).ToArray())
        {
        }

        public bool IsFull { get { return mIsFull; } }

        public bool IsCovered(int[] coordinate)
        {
            return mPoints.ContainsKey(GetKey(coordinate));
        }

        public bool IsCovered(Cover other)
        {
            if (other.IsFull && !IsFull) return false;

            //### bob: this isn't complete. needs to handle filled dimensions

            foreach (var point in other.mPoints)
            {
                if (!mPoints.ContainsKey(point.Key)) return false;
            }

            return true;
        }

        public void Merge(Cover other)
        {
            if (other.IsFull)
            {
                mIsFull = true;
                return;
            }

            foreach (var point in other.mPoints)
            {
                Insert(point.Key, point.Value);
            }
        }

        private Cover(int[] dimensions)
        {
            mDimensions = dimensions;
        }

        private string GetKey(int[] coordinate)
        {
            return String.Join(",", coordinate.Select(it => it == -1 ? "*" : it.ToString())
                                              .ToArray());
        }

        private void Insert(int[] coordinate, Cover value)
        {
            Insert(GetKey(coordinate), value);
        }

        private void Insert(string key, Cover value)
        {
            bool filledCell = false;

            Cover oldValue;
            if (mPoints.TryGetValue(key, out oldValue))
            {
                bool wasFull = oldValue.IsFull;
                oldValue.Merge(value);
                filledCell = !wasFull && oldValue.IsFull;
            }
            else
            {
                mPoints[key] = value;
                filledCell = value.IsFull;
            }

            // cascade to see if any entire spans were filled
            if (filledCell)
            {
                //### bob: hack. tuples aren't implemented yet
                if (mDimensions.Length > 1) throw new NotImplementedException();

                if (mDimensions[0] != InfiniteSpan)
                {
                    bool filledCoordinate = true;

                    // see if we have a value for each possible coordinate
                    for (int i = 0; i < mDimensions[0]; i++)
                    {
                        string thisKey = GetKey(new int[] { i });
                        if (!mPoints.ContainsKey(thisKey) ||
                            !mPoints[thisKey].IsFull)
                        {
                            filledCoordinate = false;
                            break;
                        }
                    }

                    if (filledCoordinate)
                    {
                        var spannedKey = GetKey(new int[] { InfiniteSpan });
                        mPoints[spannedKey] = Cover.Full;
                    }
                }
            }

            // see if the entire coordinate space is filled
            var fullSpan = GetKey(mDimensions.Select(it => InfiniteSpan).ToArray());
            mIsFull = mPoints.ContainsKey(fullSpan);
        }

        static Cover()
        {
            sFull = new Cover(new int[] { 1 });
            sFull.mIsFull = true;
        }

        private static Cover sFull;

        private int[] mDimensions;
        private bool mIsFull;
        private readonly Dictionary<string, Cover> mPoints = new Dictionary<string, Cover>();
    }
}
