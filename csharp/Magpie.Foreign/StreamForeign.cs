using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

using Magpie.Compilation;
using Magpie.Interpreter;

namespace Magpie.Foreign
{
    public class StreamForeign : ForeignBase
    {
        /// <summary>
        /// Gets the last stream that was previously closed.
        /// </summary>
        public byte[] LastStream { get { return mLastStream; } }

        public StreamForeign()
        {
            var streamType = new ForeignType("Stream");

            Add("Stream", streamType, Stream);
            Add("<<", new BoundTupleType(new IBoundDecl[] { streamType, Decl.Int }), Decl.Unit, WriteInt);
            Add("<<Byte", new BoundTupleType(new IBoundDecl[] { streamType, Decl.Int }), Decl.Unit, WriteByte);
            Add("<<", new BoundTupleType(new IBoundDecl[] { streamType, Decl.String }), Decl.Unit, WriteString);
            Add("Seek", new BoundTupleType(new IBoundDecl[] { streamType, Decl.Int }), Decl.Unit, Seek);
            Add("Position", streamType, Decl.Int, StreamPosition);
            Add("Close", streamType, Decl.Unit, CloseStream);
            Add("ReadFile", Decl.String, Decl.String, ReadFile);
        }

        private Value Stream(Value[] args)
        {
            var stream = new MemoryStream();
            var writer = new BinaryWriter(stream);

            return new Value(writer);
        }

        private Value WriteInt(Value[] args)
        {
            var writer = (BinaryWriter)args[0].Object;
            var value = args[1].Int;

            writer.Write(value);
            return null;
        }

        private Value WriteByte(Value[] args)
        {
            var writer = (BinaryWriter)args[0].Object;
            var value = args[1].Int;

            writer.Write((byte)value);
            return null;
        }

        private Value WriteString(Value[] args)
        {
            var writer = (BinaryWriter)args[0].Object;
            var value = args[1].String;

            writer.Write(value);
            return null;
        }

        private Value Seek(Value[] args)
        {
            var writer = (BinaryWriter)args[0].Object;
            var value = args[1].Int;

            writer.Seek(value, SeekOrigin.Begin);
            return null;
        }

        private Value StreamPosition(Value[] args)
        {
            var writer = (BinaryWriter)args[0].Object;

            return new Value((int)writer.BaseStream.Position);
        }

        private Value CloseStream(Value[] args)
        {
            var writer = (BinaryWriter)args[0].Object;

            var stream = (MemoryStream)writer.BaseStream;
            mLastStream = stream.ToArray();

            writer.Close();

            return null;
        }

        private Value ReadFile(Value[] args)
        {
            var path = args[0].String;

            return new Value(File.ReadAllText(path));
        }

        private byte[] mLastStream;
    }
}
