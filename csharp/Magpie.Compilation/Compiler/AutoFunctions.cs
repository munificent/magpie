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

            var structArg = new LoadExpr(new LocalsExpr(), new Field("s", structure.Type, false, 0));

            var body = new LoadExpr(structArg, field);

            BuildFunction(structure, field.Name, funcType, body);
        }

        private void BuildSetter(Struct structure, Field field)
        {
            // build the type declaration
            ParamDecl[] args = new ParamDecl[] { new ParamDecl("s", structure.Type), new ParamDecl("value", field.Type) };
            FuncType funcType = new FuncType(args, Decl.Unit);

            LoadExpr structArg = new LoadExpr(new LocalsExpr(), new Field("s", structure.Type, false, 0));
            LoadExpr valueArg = new LoadExpr(new LocalsExpr(), new Field("value", field.Type, false, 1));

            var body = new StoreExpr(structArg, field, valueArg);

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

            var argField = new Field("u", unionCase.Union.Type, false, 0);
            var tagField = new Field("tag", Decl.Int, false, 0);

            // get first field from union
            var body = IntrinsicExpr.EqualInt(
                new BoundTupleExpr(new IBoundExpr[] {
                        new IntExpr(unionCase.Index),
                        new LoadExpr(new LoadExpr(new LocalsExpr(), argField), tagField)}));

            BuildFunction(unionCase.Union, unionCase.Name + "?", funcType, body);
        }

        private void BuildValueAccessor(UnionCase unionCase)
        {
            FuncType funcType = new FuncType(new ParamDecl[] { new ParamDecl("u", unionCase.Union.Type) }, unionCase.ValueType);

            var argField = new Field("u", unionCase.Union.Type, false, 0);

            IBoundExpr body;
            TupleType tupleValue = unionCase.ValueType as TupleType;
            if (tupleValue != null)
            {
                // the tuple fields are split out in the union struct (unfortunately), so
                // we need to pack them back into a tuple.
                //### bob: if i change things so that a function receives the args as a tuple and
                // not unwrapped, then this can go away.
                List<IBoundExpr> fields = new List<IBoundExpr>();
                for (int i = 0; i < tupleValue.Fields.Count; i++)
                {
                    var field = new Field("arg" + i, tupleValue.Fields[i], false, (byte)(i + 1));
                    fields.Add(new LoadExpr(new LoadExpr(new LocalsExpr(), argField), field));
                }

                // tuple value
                body = new BoundTupleExpr(fields);
            }
            else
            {
                var valueField = new Field("value", Decl.Int, false, 1);

                // single value
                body = new LoadExpr(new LoadExpr(new LocalsExpr(), argField), valueField);
            }

            BuildFunction(unionCase.Union, unionCase.Name + "Value", funcType, body);
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
