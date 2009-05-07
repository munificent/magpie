using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Methods to build the compiler-generated functions based on user-defined types.
    /// </summary>
    public class AutoFunctions
    {
        public AutoFunctions(Compiler compiler)
        {
            mCompiler = compiler;
        }

        public void BuildFunctions(Struct structure)
        {
            BuildConstructor(structure);

            foreach (Field field in structure.Fields)
            {
                BuildGetter(structure, field);
                if (field.IsMutable) BuildSetter(structure, field);
            }
        }

        public void BuildFunctions(Union union)
        {
            // constructors for each case
            foreach (UnionCase unionCase in union.Cases)
            {
                BuildConstructor(unionCase);
                BuildCaseCheck(unionCase);

                if (unionCase.ValueType != Decl.Unit)
                {
                    BuildValueAccessor(unionCase);
                }
            }
        }

        private void BuildConstructor(Struct structure)
        {
            // each field is an argument
            int index = 0;
            List<ParamDecl> args = new List<ParamDecl>();
            foreach (Field field in structure.Fields)
            {
                args.Add(new ParamDecl("arg" + index, field.Type));
                index++;
            }

            // build the type declaration
            FuncType funcType = new FuncType(args, structure.Type);

            var body = new ConstructExpr(structure);

            BuildFunction(structure, structure.Name, funcType, body);
        }

        private void BuildGetter(Struct structure, Field field)
        {
            // build the type declaration
            var arg = new ParamDecl[] { new ParamDecl("s", structure.Type) };
            var funcType = new FuncType(arg, field.Type);

            // load the struct argument
            var loadStruct = new LoadExpr(new LocalsExpr(), structure.Type, 0);
            var loadField  = new LoadExpr(loadStruct, field);

            BuildFunction(structure, field.Name, funcType, loadField);
        }

        //### bob: structs are all good now. just need unions.
        // single value unions coincidentally work.
        // tuple value unions are broke

        private void BuildSetter(Struct structure, Field field)
        {
            // build the type declaration
            ParamDecl[] args = new ParamDecl[] { new ParamDecl("s", structure.Type), new ParamDecl("value", field.Type) };
            FuncType funcType = new FuncType(args, Decl.Unit);

            //### opt: this is kind of tedious...
            // load the locals
            // load the arg
            // load the first arg element
            // load the locals
            // load the arg
            // load the second arg element
            // definitely some optimizations we can do there...

            // load the argument tuple
            var loadArg = new LoadExpr(new LocalsExpr(), Decl.Unit /* ignored */, 0);

            // load the struct argument
            var loadStruct = new LoadExpr(loadArg, structure.Type, 0);

            // load the new value
            var loadValue = new LoadExpr(loadArg, field.Type, 1);

            var body = new StoreExpr(loadStruct, field, loadValue);

            BuildFunction(structure, field.Name + "<-", funcType, body);
        }

        private void BuildConstructor(UnionCase unionCase)
        {
            // build the type declaration
            Decl[] argTypes = unionCase.ValueType.Expanded;
            IEnumerable<ParamDecl> args = argTypes.Select((decl, index) => new ParamDecl("arg" + index, decl));

            var funcType = new FuncType(args, unionCase.Union.Type);
            var body = new ConstructUnionExpr(unionCase);

            BuildFunction(unionCase.Union, unionCase.Name, funcType, body);
        }

        private void BuildCaseCheck(UnionCase unionCase)
        {
            FuncType funcType = new FuncType(new ParamDecl[] { new ParamDecl("u", unionCase.Union.Type) }, Decl.Bool);

            // a union in memory looks like:
            //
            // no value
            // +------------+      +------------+
            // | union      | ---> | case (int) |
            // +------------+      +------------+
            //
            // single value
            // +------------+      +------------+
            // | union      | ---> | case (int) |
            // +------------+      +------------+
            //                     | value      |
            //                     +------------+
            //
            // tuple or ref value
            // +------------+      +------------+
            // | union      | ---> | case (int) |
            // +------------+      +------------+      +------------+
            //                     | reference  | ---> | field ...  |
            //                     +------------+      +------------+
            //                                         | field ...  |
            //                                         +------------+

            //### opt: unions with no value don't need to be references
            //         could just put the value in place

            var loadUnion = new LoadExpr(new LocalsExpr(), unionCase.Union.Type, 0);
            var loadCase  = new LoadExpr(loadUnion, Decl.Int, 0);

            var compare = Intrinsic.EqualInt(loadCase, new IntExpr(unionCase.Index));

            BuildFunction(unionCase.Union, unionCase.Name + "?", funcType, compare);
        }

        //### bob: this should go away when we have pattern matching
        private void BuildValueAccessor(UnionCase unionCase)
        {
            FuncType funcType = new FuncType(new ParamDecl[] { new ParamDecl("u", unionCase.Union.Type) }, unionCase.ValueType);

            var loadUnion = new LoadExpr(new LocalsExpr(), unionCase.Union.Type, 0);
            var loadValue = new LoadExpr(loadUnion, unionCase.ValueType, 1);

            BuildFunction(unionCase.Union, unionCase.Name + "Value", funcType, loadValue);
        }

        private void BuildFunction(TypeDefinition parentType, string name, FuncType type, IBoundExpr body)
        {
            var function = new Function(name, parentType.TypeParameters, type, new WrapBoundExpr(body));
            function.Qualify(parentType);

            mCompiler.AddFunction(function);
        }

        private Compiler mCompiler;
    }
}
