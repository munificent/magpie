using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Expression visitor that compiles expressions down to bytecode.
    /// </summary>
    public class BytecodeGenerator : IBoundExprVisitor<bool>
    {
        public int Position { get { return (int)mWriter.BaseStream.Position; } }

        private BytecodeGenerator(CompileUnit unit, int numLocals, BinaryWriter writer,
            OffsetTable functionPatcher, StringTable stringTable)
        {
            mUnit = unit;
            mNumLocals = numLocals;
            mWriter = writer;
            mFunctionPatcher = functionPatcher;
            mStrings = stringTable;
            mJumpTable = new JumpTable(this);
        }

        public static void Generate(CompileUnit unit, BinaryWriter writer,
            OffsetTable functionPatcher, StringTable stringTable, BoundFunction function)
        {
            BytecodeGenerator generator = new BytecodeGenerator(unit, function.NumLocals,
                writer, functionPatcher, stringTable);

            functionPatcher.DefineOffset(function.Name);

            writer.Write(function.NumLocals);
            writer.Write(function.Type.Parameters.Count);

            function.Body.Accept(generator);

            generator.Write(OpCode.Return);
        }

        public void SeekTo(int position)
        {
            mWriter.Seek(position, SeekOrigin.Begin);
        }

        public void SeekToEnd()
        {
            mWriter.Seek(0, SeekOrigin.End);
        }

        #region IBoundExprVisitor Members

        public bool Visit(UnitExpr expr) { return true; } // do nothing
        public bool Visit(BoolExpr expr) { Write(OpCode.PushBool, expr.Value ? (byte)1 : (byte)0); return true; }
        public bool Visit(IntExpr expr)  { Write(OpCode.PushInt, expr.Value); return true; }

        public bool Visit(StringExpr expr)
        {
            Write(OpCode.PushString);
            mStrings.InsertOffset(expr.Value);

            return true;
        }

        public bool Visit(BoundFuncRefExpr expr)
        {
            Write(OpCode.PushInt);
            mFunctionPatcher.InsertOffset(expr.Function.Name);
            return true;
        }

        public bool Visit(ForeignCallExpr expr)
        {
            // evaluate the arg
            expr.Arg.Accept(this);

            // add the foreign call
            OpCode op;
            switch (expr.Function.FuncType.Parameters.Count)
            {
                case 0: op = OpCode.ForeignCall0; break;
                case 1: op = OpCode.ForeignCall1; break;
                default: op = OpCode.ForeignCallN; break;
            }

            Write(op, expr.Function.ID);

            return true;
        }

        public bool Visit(BoundTupleExpr tuple)
        {
            // must visit in forward order to ensure that function arguments are
            // evaluated left to right
            for (int i = 0; i < tuple.Fields.Count; i++)
            {
                tuple.Fields[i].Accept(this);
            }

            // create the structure
            Write(OpCode.Alloc, tuple.Fields.Count);

            return true;
        }

        public bool Visit(IntrinsicExpr expr)
        {
            expr.Arg.Accept(this);

            expr.Intrinsic.OpCodes.ForEach(op => Write(op));

            return true;
        }

        public bool Visit(BoundCallExpr expr)
        {
            expr.Arg.Accept(this);
            expr.Target.Accept(this);

            // add the call
            OpCode op;
            switch (expr.Arg.Type.Expanded.Length)
            {
                case 0: op = OpCode.Call0; break;
                case 1: op = OpCode.Call1; break;
                default: op = OpCode.CallN; break;
            }
            Write(op);

            return true;
        }

        public bool Visit(BoundArrayExpr expr)
        {
            // must visit in forward order to ensure that array elements are
            // evaluated left to right
            for (int i = 0; i < expr.Elements.Count; i++)
            {
                expr.Elements[i].Accept(this);
            }

            // create the structure
            Write(OpCode.Alloc, expr.Elements.Count);

            return true;
        }

        public bool Visit(BoundBlockExpr block)
        {
            block.Exprs.ForEach(expr => expr.Accept(this));

            return true;
        }

        public bool Visit(BoundIfDoExpr expr)
        {
            // evaluate the condition
            expr.Condition.Accept(this);
            mJumpTable.JumpIfFalse("end");

            // execute the body
            expr.Body.Accept(this);

            // jump past it
            mJumpTable.PatchJump("end");

            return true;
        }

        public bool Visit(BoundIfThenExpr expr)
        {
            // evaluate the condition
            expr.Condition.Accept(this);
            mJumpTable.JumpIfFalse("else");

            // thenBody
            expr.ThenBody.Accept(this);

            // jump to end
            mJumpTable.Jump("end");

            // elseBody
            mJumpTable.PatchJump("else");
            expr.ElseBody.Accept(this);

            // end
            mJumpTable.PatchJump("end");

            return true;
        }

        public bool Visit(BoundWhileExpr expr)
        {
            mJumpTable.PatchJumpBack("while");

            // evaluate the condition
            expr.Condition.Accept(this);
            mJumpTable.JumpIfFalse("end");

            // body
            expr.Body.Accept(this);

            // jump back to loop
            mJumpTable.JumpBack("while");

            // exit loop
            mJumpTable.PatchJump("end");

            return true;
        }

        public bool Visit(LoadExpr expr)
        {
            expr.Struct.Accept(this);

            Write(OpCode.Load, expr.Field.Index);

            return true;
        }

        public bool Visit(StoreExpr expr)
        {
            expr.Value.Accept(this);
            expr.Struct.Accept(this);

            Write(OpCode.Store, expr.Field.Index);

            return true;
        }

        public bool Visit(LocalsExpr expr)
        {
            //### bob: will need to handle other scopes at some point
            Write(OpCode.PushLocals);

            return true;
        }

        public bool Visit(ConstructExpr expr)
        {
            //### bob: there's some redundancy here. when we *call* the constructor, we create a tuple
            // with all of the arguments, which are the struct fields in order. that tuple is pushed
            // onto the stack and the constructor is called.
            // the interpreter than allocates a structure for its locals.
            // it pops the tuple off the stack, and assigns each of its fields to the locals, in order.
            // then the body of the constructor here creates a new structure, and initializes each field
            // by reading each of the locals. so we have:
            // - tuple arg passed to constructor
            // - constructor's local variables
            // - created structure
            // each of those (provided the structure has more than one field) are essentially identical.
            // kinda dumb.

            // load all of the locals
            for (int i = 0; i < mNumLocals; i++)
            {
                Write(OpCode.PushLocals);
                Write(OpCode.Load, (byte)i);
            }

            // create the structure
            Write(OpCode.Alloc, mNumLocals);

            return true;
        }

        public bool Visit(ConstructUnionExpr expr)
        {
            // load the case tag
            Write(OpCode.PushInt, expr.Case.Index);

            // load all of the locals
            for (int i = 0; i < mNumLocals; i++)
            {
                Write(OpCode.PushLocals);
                Write(OpCode.Load, (byte)i);
            }

            // create the structure, add one for the case tag
            Write(OpCode.Alloc, mNumLocals + 1);

            return true;
        }

        #endregion

        public void Write(OpCode op)
        {
            mWriter.Write((byte)op);
        }

        public void Write(byte value)
        {
            mWriter.Write(value);
        }

        public void Write(int value)
        {
            mWriter.Write(value);
        }

        public void Write(OpCode op, byte operand)
        {
            Write(op);
            Write(operand);
        }

        public void Write(OpCode op, int operand)
        {
            Write(op);
            Write(operand);
        }

        private int mNumLocals; //### bob: temp

        private CompileUnit mUnit;
        private BinaryWriter mWriter;
        private OffsetTable mFunctionPatcher;
        private StringTable mStrings;
        private JumpTable mJumpTable;
    }
}
