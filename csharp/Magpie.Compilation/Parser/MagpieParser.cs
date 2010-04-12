using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class MagpieParser : LlParser
    {
        public static SourceFile ParseSourceFile(string filePath)
        {
            // chain the parsing passes together
            var scanner       = new Scanner(filePath, File.ReadAllText(filePath));
            var lineProcessor = new LineProcessor(scanner);
            var parser        = new MagpieParser(lineProcessor);

            return parser.SourceFile();
        }

        private MagpieParser(IEnumerable<Token> tokens) : base(tokens)
        {
            mC = new CodeBuilder(null);
        }

        // <-- Usings NamespaceContents
        private SourceFile SourceFile()
        {
            var usings = Usings();

            var sourceFile = new SourceFile(usings);
            var contents = NamespaceContents(sourceFile);

            Consume(TokenType.Eof);

            return sourceFile;
        }

        // <-- (USING Name LINE)*
        private IEnumerable<string> Usings()
        {
            var usings = new List<string>();

            while (ConsumeIf(TokenType.Using))
            {
                usings.Add(Consume(TokenType.Name).StringValue);
                Consume(TokenType.Line);
            }

            return usings;
        }

        // <-- ((Namespace | Function | Struct | Union) LINE)*
        private List<object> NamespaceContents(Namespace namespaceObj)
        {
            List<object> contents = new List<object>();

            while (true)
            {
                if (CurrentIs(TokenType.Namespace))     namespaceObj.Namespaces.Add(Namespace());
                else if (CurrentIs(TokenType.Name))     Function(namespaceObj);
                else if (CurrentIs(TokenType.Operator)) Operator(namespaceObj);
                else if (CurrentIs(TokenType.Struct))   Struct(namespaceObj);
                else if (CurrentIs(TokenType.Union))    Union(namespaceObj);
                else break;

                if (!ConsumeIf(TokenType.Line)) break;
            }

            return contents;
        }

        // <-- NAMESPACE Name LINE NamespaceBody END
        private Namespace Namespace()
        {
            Consume(TokenType.Namespace);
            var name = Consume(TokenType.Name).StringValue;
            Consume(TokenType.Line);

            var namespaceObj = new Namespace(name);
            var contents = NamespaceContents(namespaceObj);
            Consume(TokenType.End);

            return namespaceObj;
        }

        // <-- NAME FunctionDefinition
        private void Function(Namespace namespaceObj)
        {
            var token = Consume(TokenType.Name);
            FunctionDefinition(namespaceObj, token.StringValue, token.Position);
        }

        // <-- OPERATOR FunctionDefinition
        private void Operator(Namespace namespaceObj)
        {
            var token = Consume(TokenType.Operator);
            FunctionDefinition(namespaceObj, token.StringValue, token.Position);
        }

        // <-- GenericDecl FnArgsDecl Block
        private void FunctionDefinition(Namespace namespaceObj, string name, Position position)
        {
            var typeParams = TypeParams();

            var paramNames = new List<string>();
            var funcType = FnArgsDecl(paramNames);
            var body = Block();

            var function = new Function(position, name, funcType, paramNames, body);

            if (typeParams.Count == 0)
            {
                namespaceObj.Functions.Add(function);
            }
            else
            {
                namespaceObj.GenericFunctions.Add(new GenericFunction(function, typeParams));
            }
        }

        // <-- LPAREN ParamDecl? ARROW TypeDecl? RPAREN
        private FuncType FnArgsDecl(List<string> paramNames)
        {
            Consume(TokenType.LeftParen);

            var arg = CurrentIs(TokenType.RightArrow) ? Decl.Unit : ParamDecl(paramNames);

            var position = Consume(TokenType.RightArrow).Position;

            IUnboundDecl returnType = CurrentIs(TokenType.RightParen) ? Decl.Unit : TypeDecl();

            Consume(TokenType.RightParen);

            return new FuncType(position, arg, returnType);
        }

        // <-- NAME TypeDecl (COMMA NAME TypeDecl)*
        private IUnboundDecl ParamDecl(List<string> paramNames)
        {
            var args = new List<IUnboundDecl>();

            while (!CurrentIs(TokenType.RightParen))
            {
                if (paramNames != null)
                {
                    paramNames.Add(Consume(TokenType.Name).StringValue);
                }

                args.Add(TypeDecl());

                if (!ConsumeIf(TokenType.Comma)) break;
            }

            if (args.Count == 0) return Decl.Unit;
            if (args.Count == 1) return args[0];

            return new TupleType(args);
        }

        // <-- Expression | LINE (Expression LINE)+ END
        private IUnboundExpr Block()
        {
            if (ConsumeIf(TokenType.Line))
            {
                var expressions = new List<IUnboundExpr>();

                do
                {
                    expressions.Add(Expression());
                    Consume(TokenType.Line);
                }
                while (!ConsumeIf(TokenType.End));

                return new BlockExpr(expressions);
            }
            else
            {
                return Expression();
            }
        }

        // <-- Expression LINE? | LINE (Expression LINE)+ <terminator>
        /// <summary>
        /// Parses a block terminated by "end" or a continue terminator. Used for blocks
        /// that may end or be followed by something else (such as an "else" clause).
        /// </summary>
        /// <param name="continueTerminator"></param>
        private IUnboundExpr InnerBlock(TokenType continueTerminator)
        {
            if (ConsumeIf(TokenType.Line))
            {
                var expressions = new List<IUnboundExpr>();

                bool inBlock = true;
                while (inBlock)
                {
                    expressions.Add(Expression());
                    Consume(TokenType.Line);

                    if (CurrentIs(continueTerminator)) // don't consume
                    {
                        inBlock = false;
                    }
                    else if (ConsumeIf(TokenType.End))
                    {
                        inBlock = false;
                    }
                }

                return new BlockExpr(expressions);
            }
            else
            {
                IUnboundExpr expr = Expression();

                if (CurrentIs(TokenType.Else))
                {
                    // don't consume
                }
                else if (CurrentIs(TokenType.Line, TokenType.Else))
                {
                    // just consume the line
                    Consume(TokenType.Line);
                }
                // for inner blocks, allow a line at the end. this is for cases like:
                // if foo then bar
                // else bang
                //
                // only do this if there is an "else" after the line so that we don't
                // eat the line after an "if/then"

                return expr;
            }
        }

        private IUnboundExpr Expression()
        {
            return SyntaxExpr();
        }

        private IUnboundExpr SyntaxExpr()
        {
            Position position;
            if (ConsumeIf(TokenType.LeftCurly, out position))
            {
                // ignore a leading line
                ConsumeIf(TokenType.Line);

                var expressions = new List<IUnboundExpr>();

                while (true)
                {
                    expressions.Add(Expression());

                    if (ConsumeIf(TokenType.RightCurly)) break;
                    Consume(TokenType.Line);
                    if (ConsumeIf(TokenType.RightCurly)) break;
                }

                IUnboundExpr expr;
                if (expressions.Count > 1)
                {
                    expr = new BlockExpr(expressions);
                }
                else
                {
                    expr = expressions[0];
                }

                return new SyntaxExpr(position, expr);
            }
            else return DefineExpr();
        }

        private IUnboundExpr DefineExpr()
        {
            bool? isMutable = null;

            Position position;
            if (ConsumeIf(TokenType.Def, out position))
            {
                isMutable = false;
            }
            else if (ConsumeIf(TokenType.Var, out position))
            {
                isMutable = true;
            }

            if (isMutable.HasValue)
            {
                var definitions = new List<Define>();

                // check for multi-line define
                if (ConsumeIf(TokenType.Line))
                {
                    while (true)
                    {
                        definitions.Add(Define());
                        Consume(TokenType.Line);
                        if (ConsumeIf(TokenType.End)) break;
                    }
                }
                else
                {
                    // single line define
                    definitions.Add(Define());
                }

                return new DefineExpr(isMutable.Value, definitions);
            }
            else return MatchExpr();
        }

        private Define Define()
        {
            var names = new List<string>();
            var position = CurrentPosition;

            do
            {
                names.Add(Consume(TokenType.Name).StringValue);
            }
            while (ConsumeIf(TokenType.Comma));

            Consume(TokenType.LeftArrow);
            IUnboundExpr body = Block();

            return new Define(position, names, body);
        }

        // <-- MATCH FlowExpr LINE? (CASE CaseExpr THEN Block)+ END
        //   | FlowExpr
        private IUnboundExpr MatchExpr()
        {
            Position position;
            if (ConsumeIf(TokenType.Match, out position))
            {
                IUnboundExpr matchExpr = FlowExpr();
                ConsumeIf(TokenType.Line);

                var cases = new List<MatchCase>();
                Position casePosition;
                while (ConsumeIf(TokenType.Case, out casePosition))
                {
                    IPattern caseExpr = CaseExpr();
                    Consume(TokenType.Then);
                    IUnboundExpr bodyExpr = InnerBlock(TokenType.Case);

                    cases.Add(new MatchCase(casePosition, caseExpr, bodyExpr));

                    // optional line after case
                    ConsumeIf(TokenType.Line);
                }
                Consume(TokenType.End);

                return new MatchExpr(position, matchExpr, cases);
            }
            else return FlowExpr();
        }

        // <-- BOOL
        //   | INT
        //   | STRING
        //   | "_"
        //   | NAME CaseExpr?
        //   | LPAREN CaseExpr (COMMA CaseExpr)+ RPAREN )
        //   | <null>
        private IPattern CaseExpr()
        {
            if (CurrentIs(TokenType.Bool))        return new BoolPattern(Consume(TokenType.Bool));
            else if (CurrentIs(TokenType.Int))    return new IntPattern(Consume(TokenType.Int));
            else if (CurrentIs(TokenType.String)) return new StringPattern(Consume(TokenType.String));
            else if (CurrentIs(TokenType.Name))
            {
                var token = Consume(TokenType.Name);

                if (token.StringValue == "_") return new AnyPattern(token.Position);

                // lower-case names are variables
                if (Char.IsLower(token.StringValue[0])) return new VariablePattern(token.Position, token.StringValue);

                // a union case may match the subsequent value
                var value = CaseExpr();

                return new UnionPattern(token.Position, token.StringValue, value);
            }
            else if (ConsumeIf(TokenType.LeftParen))
            {
                List<IPattern> fields = new List<IPattern>();

                fields.Add(CaseExpr());
                while (ConsumeIf(TokenType.Comma))
                {
                    fields.Add(CaseExpr());
                }

                Consume(TokenType.RightParen);

                return new TuplePattern(fields);
            }
            else return null;
        }

        private IUnboundExpr FlowExpr()
        {
            Position position;
            if (CurrentIs(TokenType.For) ||
                CurrentIs(TokenType.While))
            {
                var clauses = new List<LoopClause>();

                while (true)
                {
                    if (ConsumeIf(TokenType.For, out position))
                    {
                        var name = Consume(TokenType.Name).StringValue;
                        Consume(TokenType.LeftArrow);
                        var iterator = OperatorExpr();

                        ConsumeIf(TokenType.Line);

                        clauses.Add(new LoopClause(position, name, iterator));
                    }
                    else if (ConsumeIf(TokenType.While, out position))
                    {
                        var condition = AssignExpr();

                        ConsumeIf(TokenType.Line);

                        clauses.Add(new LoopClause(position, String.Empty, condition));
                    }
                    else
                    {
                        // no more clauses
                        break;
                    }
                }

                position = Consume(TokenType.Do).Position;
                IUnboundExpr body = Block();

                return new LoopExpr(position, clauses, body);
            }
            else if (ConsumeIf(TokenType.If, out position))
            {
                var condition = InnerBlock(TokenType.Then);

                // then can optionally be on the next line
                ConsumeIf(TokenType.Line);

                Consume(TokenType.Then);

                var thenBody = InnerBlock(TokenType.Else);
                var elseBody = (IUnboundExpr)null;

                if (ConsumeIf(TokenType.Else))
                {
                    elseBody = Block();
                }

                return new IfExpr(position, condition, thenBody, elseBody);
            }
            else if (ConsumeIf(TokenType.Let, out position))
            {
                var names = new List<string>();

                do
                {
                    names.Add(Consume(TokenType.Name).StringValue);
                }
                while (ConsumeIf(TokenType.Comma));

                Consume(TokenType.LeftArrow);

                var condition = InnerBlock(TokenType.Then);

                // then can optionally be on the next line
                ConsumeIf(TokenType.Line);

                Consume(TokenType.Then);

                var thenBody = InnerBlock(TokenType.Else);
                var elseBody = (IUnboundExpr)null;

                if (ConsumeIf(TokenType.Else))
                {
                    elseBody = Block();
                }

                return new LetExpr(position, names, condition, thenBody, elseBody);
            }
            else if (ConsumeIf(TokenType.Return, out position))
            {
                if (CurrentIs(TokenType.Line) | CurrentIs(TokenType.RightCurly))
                {
                    // infer () if there is no value
                    return new ReturnExpr(position, new UnitExpr(position));
                }
                else
                {
                    // no line after return, so parse the expression
                    return new ReturnExpr(position, FlowExpr());
                }
            }
            else return AssignExpr();
        }

        // <-- TupleExpr (ASSIGN (DOT | OPERATOR | e) Block)?
        private IUnboundExpr AssignExpr()
        {
            IUnboundExpr expr = TupleExpr();

            Position position;
            if (ConsumeIf(TokenType.LeftArrow, out position))
            {
                bool isDot = false;
                string opName = String.Empty;

                if (ConsumeIf(TokenType.Dot)) isDot = true;
                else if (CurrentIs(TokenType.Operator))
                {
                    Token op = Consume(TokenType.Operator);
                    opName = op.StringValue;
                }

                IUnboundExpr value = Block();

                mC.SetPosition(position);

                if (isDot)
                {
                    expr = mC.Assign(expr, mC.Call(value, expr));
                }
                else if (!String.IsNullOrEmpty(opName))
                {
                    expr = mC.Assign(expr, mC.Op(expr, opName, value));
                }
                else
                {
                    expr = mC.Assign(expr, value);
                }
            }

            return expr;
        }

        // <-- RecordExpr (COMMA RecordExpr)*
        private IUnboundExpr TupleExpr()
        {
            var fields = new List<IUnboundExpr>();

            fields.Add(RecordExpr());

            while (ConsumeIf(TokenType.Comma))
            {
                fields.Add(RecordExpr());
            }

            if (fields.Count == 1) return fields[0];

            return new TupleExpr(fields);
        }

        // <-- KEYWORD OperatorExpr (COMMA KEYWORD OperatorExpr)*
        private IUnboundExpr RecordExpr()
        {
            if (CurrentIs(TokenType.Keyword))
            {
                var fields = new Dictionary<string, IUnboundExpr>();
                do
                {
                    var keyword = Consume(TokenType.Keyword).StringValue;

                    // strip the trailing :
                    keyword = keyword.Substring(0, keyword.Length - 1);

                    fields.Add(keyword, OperatorExpr());
                }
                while (CurrentIs(TokenType.Keyword));

                return new RecordExpr(fields);
            }
            else return OperatorExpr();
        }

        // <-- ApplyExpr (OPERATOR ApplyExpr)*
        private IUnboundExpr OperatorExpr()
        {
            return OneOrMoreLeft(TokenType.Operator, ApplyExpr,
                (left, separator, right) => mC.Call(new NameExpr(separator.Position, separator.StringValue), mC.Tuple(left, right)));
        }

        // <-- PrimaryExpr+
        private IUnboundExpr ApplyExpr()
        {
            return OneOrMoreRight<IUnboundExpr>(ReverseApplyExpr, (left, right) => mC.Call(left, right));
        }

        // <-- ArrayExpr (DOT ArrayExpr)*
        private IUnboundExpr ReverseApplyExpr()
        {
            return OneOrMoreLeft(TokenType.Dot, PrimaryExpr,
                (left, separator, right) => mC.Call(right, left));
        }

        // <-- Name
        //   | FN Name TupleType
        //   | BOOL_LITERAL
        //   | INT_LITERAL
        //   | STRING_LITERAL
        //   | LPAREN (Expression)? RPAREN
        private IUnboundExpr PrimaryExpr()
        {
            IUnboundExpr expression;

            if (CurrentIs(TokenType.Name))
            {
                var name = Consume();
                expression = new NameExpr(name.Position, name.StringValue, TypeArgs());
            }
            else if (CurrentIs(TokenType.Fn)) expression = FuncExpr();
            else if (CurrentIs(TokenType.Bool)) expression = new BoolExpr(Consume(TokenType.Bool));
            else if (CurrentIs(TokenType.Int)) expression = new IntExpr(Consume(TokenType.Int));
            else if (CurrentIs(TokenType.String)) expression = new StringExpr(Consume(TokenType.String));
            else if (CurrentIs(TokenType.LeftParen))
            {
                Token leftParen = Consume(TokenType.LeftParen);

                if (CurrentIs(TokenType.RightParen))
                {
                    // () -> unit
                    expression = new UnitExpr(leftParen.Position);
                }
                else if (CurrentIs(TokenType.Operator))
                {
                    // ( OPERATOR ) -> an operator in prefix form
                    Token op = Consume(TokenType.Operator);
                    expression = new NameExpr(op.Position, op.StringValue);
                }
                else
                {
                    // anything else is a regular parenthesized expression
                    expression = Expression();
                }

                Consume(TokenType.RightParen);
            }
            else expression = null;

            return expression;
        }

        // <-- FN ((Name | Operator) TupleType)
        //      | (LPAREN ParamDecl RPAREN)
        private IUnboundExpr FuncExpr()
        {
            Position position = Consume(TokenType.Fn).Position;

            if (CurrentIs(TokenType.LeftParen))
            {
                // local function
                var paramNames = new List<string>();
                var funcType = FnArgsDecl(paramNames);
                var body = Block();

                return new LocalFuncExpr(position, paramNames, funcType, body);
            }
            else
            {
                // function reference
                NameExpr name;
                if (CurrentIs(TokenType.Operator))
                {
                    Token op = Consume(TokenType.Operator);
                    name = new NameExpr(op.Position, op.StringValue);
                }
                else
                {
                    var token = Consume(TokenType.Name);
                    name = new NameExpr(token.Position, token.StringValue, TypeArgs());
                }

                var parameters = TupleType().ToArray();
                IUnboundDecl parameter;
                if (parameters.Length == 0)
                {
                    parameter = Decl.Unit;
                }
                else if (parameters.Length == 1)
                {
                    parameter = parameters[0];
                }
                else
                {
                    parameter = new TupleType(parameters);
                }

                return new FuncRefExpr(position, name, parameter);
            }
        }

        // <-- STRUCT NAME GenericDecl LINE StructBody END
        private void Struct(Namespace namespaceObj)
        {
            Consume(TokenType.Struct);
            var name = Consume(TokenType.Name);

            var typeParams = TypeParams();
            Consume(TokenType.Line);

            var fields = StructFields();
            Consume(TokenType.End);

            var structure = new Struct(name.Position, name.StringValue, fields);

            if (typeParams.Count == 0)
            {
                namespaceObj.Structs.Add(structure);
            }
            else
            {
                namespaceObj.GenericStructs.Add(new GenericStruct(structure, typeParams));
            }
        }

        // <-- (NAME TypeDecl LINE)*
        private List<Field> StructFields()
        {
            var fields = new List<Field>();

            while (CurrentIs(TokenType.Name))
            {
                var name = Consume(TokenType.Name).StringValue;
                var type = TypeDecl();
                Consume(TokenType.Line);

                fields.Add(new Field(name, type));
            }

            return fields;
        }

        // <-- UNION NAME GenericDecl LINE UnionBody END
        private void Union(Namespace namespaceObj)
        {
            Consume(TokenType.Union);
            var name = Consume(TokenType.Name);

            var typeParams = TypeParams();
            Consume(TokenType.Line);

            var cases = UnionCases();
            Consume(TokenType.End);

            var union = new Union(name.Position, name.StringValue, cases);

            if (typeParams.Count == 0)
            {
                namespaceObj.Unions.Add(union);
            }
            else
            {
                namespaceObj.GenericUnions.Add(new GenericUnion(union, typeParams));
            }
        }

        // <-- (NAME (TypeDecl)? LINE)*
        private List<UnionCase> UnionCases()
        {
            var cases = new List<UnionCase>();

            while (CurrentIs(TokenType.Name))
            {
                var name = Consume(TokenType.Name).StringValue;

                IUnboundDecl type = Decl.Unit;
                if (!CurrentIs(TokenType.Line))
                {
                    type = TypeDecl();
                }
                Consume(TokenType.Line);

                cases.Add(new UnionCase(name, type, cases.Count));
            }

            return cases;
        }

        // <-- LPAREN (TypeDecl (COMMA TypeDecl)*)? RPAREN
        private IEnumerable<IUnboundDecl> TupleType()
        {
            var decls = new List<IUnboundDecl>();

            Consume(TokenType.LeftParen);

            if (!CurrentIs(TokenType.RightParen))
            {
                decls.Add(TypeDecl());

                while (ConsumeIf(TokenType.Comma))
                {
                    decls.Add(TypeDecl());
                }
            }

            Consume(TokenType.RightParen);

            return decls;
        }

        // <-- LBRACKET (TypeDecl (COMMA TypeDecl)*) RBRACKET
        //   | <nothing>
        private IEnumerable<IUnboundDecl> TypeArgs()
        {
            var decls = new List<IUnboundDecl>();

            // using [] for generics
            // may not be any args
            if (ConsumeIf(TokenType.LeftBracket))
            {
                do
                {
                    decls.Add(TypeDecl());
                }
                while (ConsumeIf(TokenType.Comma));

                Consume(TokenType.RightBracket);
            }

            return decls;
        }

        // <-- LBRACKET NAME (COMMA NAME)* RBRACKET
        //   | <nothing>
        private IList<string> TypeParams()
        {
            var parameters = new List<string>();

            // may not be any params
            if (ConsumeIf(TokenType.LeftBracket))
            {
                do
                {
                    parameters.Add(Consume(TokenType.Name).StringValue);
                }
                while (ConsumeIf(TokenType.Comma));

                Consume(TokenType.RightBracket);
            }

            return parameters;
        }

        // <-- LPAREN KEYWORD TypeDecl (COMMA KEYWORD TypeDecl)* RPAREN
        private IUnboundDecl RecordType()
        {
            var fields = new Dictionary<string, IUnboundDecl>();

            Consume(TokenType.LeftParen);

            do
            {
                var keyword = Consume(TokenType.Keyword).StringValue;

                // strip the trailing :
                keyword = keyword.Substring(0, keyword.Length - 1);

                fields.Add(keyword, TypeDecl());
            }
            while (CurrentIs(TokenType.Keyword));

            Consume(TokenType.RightParen);

            return new RecordType(fields);
        }

        // <-- TupleType
        //   | FN FnArgsDecl
        //   | NAME TypeArgs
        private IUnboundDecl TypeDecl()
        {
            if (CurrentIs(TokenType.LeftParen, TokenType.Keyword)) return RecordType();
            else if (CurrentIs(TokenType.LeftParen)) return new TupleType(TupleType());
            else if (ConsumeIf(TokenType.Fn)) return FnArgsDecl(null);
            else
            {
                var name = Consume(TokenType.Name);
                return new NamedType(name.Position, name.StringValue, TypeArgs());
            }
        }

        private CodeBuilder mC;
    }
}
