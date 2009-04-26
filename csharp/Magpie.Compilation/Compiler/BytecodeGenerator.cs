using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Expression visitor that compiles expressions down to bytecode.
    /// </summary>
    public class BytecodeGenerator : IBoundExprVisitor<bool>
    {
        public BytecodeGenerator(CompileUnit unit, FunctionBlock function)
        {
            mUnit = unit;
            mFunction = function;
        }

        #region IBoundExprVisitor Members

        public bool Visit(UnitExpr expr)        { return true; } // do nothing
        public bool Visit(BoolExpr expr)        { mFunction.PushBool(expr.Value); return true;   }
        public bool Visit(IntExpr expr)         { mFunction.PushInt(expr.Value); return true;    }
        public bool Visit(BoundStringExpr expr) { mFunction.PushString(expr.Index); return true; }

        public bool Visit(BoundFuncRefExpr expr)
        {
            mFunction.PushInt(mUnit.Bound.IndexOf(expr.Function));
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
            mFunction.Alloc(tuple.Fields.Count);

            return true;
        }

        public bool Visit(IntrinsicExpr expr)
        {
            expr.Arg.Accept(this);

            expr.OpCodes.ForEach(op => mFunction.Add(op));

            return true;
        }

        public bool Visit(BoundApplyExpr expr)
        {
            expr.Arg.Accept(this);
            expr.Target.Accept(this);

            mFunction.Add(OpCode.Call);

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
            mFunction.Alloc(expr.Elements.Count);

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
            mFunction.JumpIfFalse("end");

            // execute the body
            expr.Body.Accept(this);

            // jump past it
            mFunction.PatchJump("end");

            return true;
        }

        public bool Visit(BoundIfThenExpr expr)
        {
            // evaluate the condition
            expr.Condition.Accept(this);
            mFunction.JumpIfFalse("else");

            // thenBody
            expr.ThenBody.Accept(this);

            // jump to end
            mFunction.Jump("end");

            // elseBody
            mFunction.PatchJump("else");
            expr.ElseBody.Accept(this);

            // end
            mFunction.PatchJump("end");

            return true;
        }

        public bool Visit(BoundWhileExpr expr)
        {
            mFunction.PatchJumpBack("while");

            // evaluate the condition
            expr.Condition.Accept(this);
            mFunction.JumpIfFalse("end");

            // body
            expr.Body.Accept(this);

            // jump back to loop
            mFunction.JumpBack("while");

            // exit loop
            mFunction.PatchJump("end");

            return true;
        }

        public bool Visit(LoadExpr expr)
        {
            expr.Struct.Accept(this);

            mFunction.Load(expr.Field.Index);

            return true;
        }

        public bool Visit(StoreExpr expr)
        {
            expr.Value.Accept(this);
            expr.Struct.Accept(this);

            mFunction.Store(expr.Field.Index);

            return true;
        }

        public bool Visit(LocalsExpr expr)
        {
            //### bob: will need to handle other scopes at some point
            mFunction.Add(OpCode.PushLocals);

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
            for (int i = 0; i < mFunction.NumLocals; i++)
            {
                mFunction.Add(OpCode.PushLocals);
                mFunction.Load((byte)i);
            }

            // create the structure
            mFunction.Alloc(mFunction.NumLocals);

            return true;
        }

        public bool Visit(ConstructUnionExpr expr)
        {
            // load the case tag
            mFunction.PushInt(expr.Case.Index);

            // load all of the locals
            for (int i = 0; i < mFunction.NumLocals; i++)
            {
                mFunction.Add(OpCode.PushLocals);
                mFunction.Load((byte)i);
            }

            // create the structure
            mFunction.Alloc(mFunction.NumLocals + 1);

            return true;
        }

        #endregion

        private CompileUnit mUnit;
        private FunctionBlock mFunction;
    }
}
