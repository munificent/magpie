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
            var scanner       = new Scanner(File.ReadAllText(filePath));
            var lineProcessor = new LineProcessor(scanner);
            var parser        = new MagpieParser(lineProcessor);

            return parser.SourceFile();
        }

        private MagpieParser(IEnumerable<Token> tokens) : base(tokens) {}

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
                usings.Add(Name().Item1);
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
            var name = Name().Item1;
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

            while (true)
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

            //### bob: position is temp
            return new TupleType(Tuple.Create((IEnumerable<IUnboundDecl>)args, Position.None));
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
        /// <param name="hasContinue"></param>
        /// <returns></returns>
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
            return DefineExpr();
        }

        // <-- (DEF | MUTABLE) NAME ASSIGN Block
        private IUnboundExpr DefineExpr()
        {
            bool? isMutable = null;

            Position position;
            if (ConsumeIf(TokenType.Def, out position))
            {
                isMutable = false;
            }
            else if (ConsumeIf(TokenType.Mutable, out position))
            {
                isMutable = true;
            }

            if (isMutable.HasValue)
            {
                string name = Consume(TokenType.Name).StringValue;
                Consume(TokenType.LeftArrow);
                IUnboundExpr body = Block();

                return new DefineExpr(position, name, body, isMutable.Value);
            }
            else return MatchExpr();
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
                    ICaseExpr caseExpr = CaseExpr();
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

        // <-- PrimaryCaseExpr+
        private ICaseExpr CaseExpr()
        {
            return OneOrMoreRight<ICaseExpr>(PrimaryCaseExpr, (left, right) => new CallCase(left, right));
        }

        // <-- BOOL
        //   | INT
        //   | STRING
        //   | NAME
        //   | LPAREN CaseExpr (COMMA CaseExpr)+ RPAREN )
        //   | <null>
        private ICaseExpr PrimaryCaseExpr()
        {
            if (CurrentIs(TokenType.Bool))        return new BoolCase(Consume(TokenType.Bool).BoolValue);
            else if (CurrentIs(TokenType.Int))    return new IntCase(Consume(TokenType.Int).IntValue);
            else if (CurrentIs(TokenType.String)) return new StringCase(Consume(TokenType.String).StringValue);
            else if (CurrentIs(TokenType.Name))
            {
                string name = Consume(TokenType.Name).StringValue;

                if (name == "_") return new AnyCase();
                else return new NameCase(name);
            }
            else if (ConsumeIf(TokenType.LeftParen))
            {
                List<ICaseExpr> fields = new List<ICaseExpr>();

                fields.Add(CaseExpr());
                while (ConsumeIf(TokenType.Comma))
                {
                    fields.Add(CaseExpr());
                }

                Consume(TokenType.RightParen);

                return new TupleCase(fields);
            }
            else return null;
        }

        // <-- (FOR Name <- OperatorExpr LINE?)+ DO Block
        //   | WHILE AssignExpr DO Block
        //   | IF AssignExpr THEN InnerBlock (ELSE Block)?
        //   | RETURN FlowExpr
        //   | AssignExpr
        private IUnboundExpr FlowExpr()
        {
            Position position;
            if (CurrentIs(TokenType.For))
            {
                List<NamedIterator> iterators = new List<NamedIterator>();

                while (ConsumeIf(TokenType.For, out position))
                {
                    string name = Consume(TokenType.Name).StringValue;
                    Consume(TokenType.LeftArrow);
                    IUnboundExpr iterator = OperatorExpr();

                    ConsumeIf(TokenType.Line);

                    iterators.Add(new NamedIterator(position, name, iterator));
                }

                position = Consume(TokenType.Do).Position;
                IUnboundExpr body = Block();

                return new ForExpr(position, iterators, body);
            }
            else if (ConsumeIf(TokenType.While, out position))
            {
                IUnboundExpr condition = AssignExpr();
                Consume(TokenType.Do);
                IUnboundExpr body = Block();

                return new WhileExpr(position, condition, body);
            }
            else if (ConsumeIf(TokenType.If, out position))
            {
                IUnboundExpr condition = AssignExpr();

                // then/do can optionally be on the next line
                ConsumeIf(TokenType.Line);

                if (ConsumeIf(TokenType.Then))
                {
                    IUnboundExpr thenBody = InnerBlock(TokenType.Else);

                    if (ConsumeIf(TokenType.Else))
                    {
                        IUnboundExpr elseBody = Block();
                        return new IfThenElseExpr(position, condition, thenBody, elseBody);
                    }
                    else
                    {
                        return new IfThenExpr(position, condition, thenBody);
                    }
                }
                else
                {
                    Consume(TokenType.Do);
                    IUnboundExpr thenBody = Block();
                    return new IfThenExpr(position, condition, thenBody);
                }
            }
            else if (ConsumeIf(TokenType.Return, out position))
            {
                if (CurrentIs(TokenType.Line))
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
                Position opPosition = null;

                if (ConsumeIf(TokenType.Dot)) isDot = true;
                else if (CurrentIs(TokenType.Operator))
                {
                    Token op = Consume(TokenType.Operator);
                    opName = op.StringValue;
                    opPosition = op.Position;
                }

                IUnboundExpr value = Block();

                if (isDot) expr = new AssignExpr(position, expr, new CallExpr(value, expr));
                else if (!String.IsNullOrEmpty(opName)) expr = new AssignExpr(position, expr, new OperatorExpr(opPosition, expr, opName, value));
                else expr = new AssignExpr(position, expr, value);
            }

            return expr;
        }

        // <-- OperatorExpr (COMMA OperatorExpr)*
        private IUnboundExpr TupleExpr()
        {
            var fields = new List<IUnboundExpr>();

            fields.Add(OperatorExpr());

            while (ConsumeIf(TokenType.Comma))
            {
                fields.Add(OperatorExpr());
            }

            if (fields.Count == 1) return fields[0];

            return new TupleExpr(fields);
        }

        // <-- ApplyExpr (OPERATOR ApplyExpr)*
        private IUnboundExpr OperatorExpr()
        {
            return OneOrMoreLeft(TokenType.Operator, ApplyExpr,
                (left, separator, right) => new OperatorExpr(separator.Position, left, separator.StringValue, right));
        }

        // <-- PrimaryExpr+
        private IUnboundExpr ApplyExpr()
        {
            return OneOrMoreRight<IUnboundExpr>(ReverseApplyExpr, (left, right) => new CallExpr(left, right));
        }

        // <-- ArrayExpr (DOT ArrayExpr)*
        private IUnboundExpr ReverseApplyExpr()
        {
            return OneOrMoreLeft(TokenType.Dot, ArrayExpr,
                (left, separator, right) => new CallExpr(right, left));
        }

        // <-- LBRACKET (RBRACKET PRIME TypeDecl |
        //              (OperatorExpr (COMMA OperatorExpr)* )? RBRACKET)
        //   | PrimaryExpr
        private IUnboundExpr ArrayExpr()
        {
            Position position;
            if (ConsumeIf(TokenType.LeftBracket, out position))
            {
                if (ConsumeIf(TokenType.RightBracketBang))
                {
                    // empty (explicitly typed) array
                    Consume(TokenType.Prime);
                    return new ArrayExpr(position, TypeDecl(), true);
                }
                else if (ConsumeIf(TokenType.RightBracket))
                {
                    // empty (explicitly typed) array
                    Consume(TokenType.Prime);
                    return new ArrayExpr(position, TypeDecl(), false);
                }
                else
                {
                    // non-empty array
                    var elements = new List<IUnboundExpr>();

                    // get the elements
                    do
                    {
                        elements.Add(OperatorExpr());
                    }
                    while (ConsumeIf(TokenType.Comma));

                    bool isMutable = false;
                    if (ConsumeIf(TokenType.RightBracketBang))
                    {
                        isMutable = true;
                    }
                    else
                    {
                        Consume(TokenType.RightBracket);
                    }

                    return new ArrayExpr(position, elements, isMutable);
                }
            }
            else return PrimaryExpr();
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

            if (CurrentIs(TokenType.Name)) expression = new NameExpr(Name(), TypeArgs());
            else if (CurrentIs(TokenType.Fn)) expression = FuncRefExpr();
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

        private IUnboundExpr FuncRefExpr()
        {
            Position position = Consume(TokenType.Fn).Position;

            NameExpr name;
            if (CurrentIs(TokenType.Operator))
            {
                Token op = Consume(TokenType.Operator);
                name = new NameExpr(op.Position, op.StringValue);
            }
            else
            {
                name = new NameExpr(Name(), TypeArgs());
            }

            var parameters = TupleType().Item1.ToArray();
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

        // <-- ((MUTABLE?) NAME TypeDecl LINE)*
        private List<Field> StructFields()
        {
            var fields = new List<Field>();

            while (CurrentIs(TokenType.Name) || CurrentIs(TokenType.Mutable))
            {
                var isMutable = ConsumeIf(TokenType.Mutable);
                var name = Consume(TokenType.Name).StringValue;
                var type = TypeDecl();
                Consume(TokenType.Line);

                fields.Add(new Field(name, type, isMutable));
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
        private Tuple<IEnumerable<IUnboundDecl>, Position> TupleType()
        {
            var decls = new List<IUnboundDecl>();

            var position = Consume(TokenType.LeftParen).Position;

            if (!CurrentIs(TokenType.RightParen))
            {
                decls.Add(TypeDecl());

                while (ConsumeIf(TokenType.Comma))
                {
                    decls.Add(TypeDecl());
                }
            }

            Consume(TokenType.RightParen);

            return Tuple.Create((IEnumerable<IUnboundDecl>)decls, position);
        }

        // <-- PRIME (TypeDecl | (LPAREN TypeDecl (COMMA TypeDecl)* RPAREN))
        //   | <nothing>
        private IEnumerable<IUnboundDecl> TypeArgs()
        {
            var decls = new List<IUnboundDecl>();

            // may not be any args
            if (ConsumeIf(TokenType.Prime))
            {
                if (ConsumeIf(TokenType.LeftParen))
                {
                    decls.Add(TypeDecl());

                    while (ConsumeIf(TokenType.Comma))
                    {
                        decls.Add(TypeDecl());
                    }

                    Consume(TokenType.RightParen);
                }
                else
                {
                    // no grouping, so just a single type declaration
                    decls.Add(TypeDecl());
                }
            }

            return decls;
        }

        // <-- PRIME (NAME | (LPAREN NAME (COMMA NAME)* RPAREN))
        //   | <nothing>
        private IList<string> TypeParams()
        {
            var parameters = new List<string>();

            // may not be any params
            if (ConsumeIf(TokenType.Prime))
            {
                if (ConsumeIf(TokenType.LeftParen))
                {
                    parameters.Add(Consume(TokenType.Name).StringValue);

                    while (ConsumeIf(TokenType.Comma))
                    {
                        parameters.Add(Consume(TokenType.Name).StringValue);
                    }

                    Consume(TokenType.RightParen);
                }
                else
                {
                    // no grouping, so just a single type declaration
                    parameters.Add(Consume(TokenType.Name).StringValue);
                }
            }

            return parameters;
        }

        // <-- ( LBRACKET RBRACKET PRIME ) * ( TupleType
        //                                   | FnTypeDecl
        //                                   | Name TypeArgs )
        private IUnboundDecl TypeDecl()
        {
            var arrays = new Stack<Tuple<bool, Position>>();
            while (ConsumeIf(TokenType.LeftBracket))
            {
                bool isMutable = false;
                if (ConsumeIf(TokenType.RightBracketBang))
                {
                    isMutable = true;
                }
                else
                {
                    Consume(TokenType.RightBracket);
                }

                var position = Consume(TokenType.Prime).Position;

                arrays.Push(Tuple.Create(isMutable, position));
            }

            // figure out the endmost type
            IUnboundDecl type;
            if (CurrentIs(TokenType.LeftParen)) type = new TupleType(TupleType());
            else if (ConsumeIf(TokenType.Fn)) type = FnArgsDecl(null);
            else type = new NamedType(Name(), TypeArgs());

            // wrap it in the array declarations
            while (arrays.Count > 0)
            {
                var array = arrays.Pop();
                type = new ArrayType(array.Item2, type, array.Item1);
            }

            return type;
        }

        // <-- NAME (COLON NAME)*
        private Tuple<string, Position> Name()
        {
            Token token = Consume(TokenType.Name);
            Position position = token.Position;
            string name = token.StringValue;

            while (ConsumeIf(TokenType.Colon))
            {
                token = Consume(TokenType.Name);
                name += ":" + token.StringValue;

                // combine all of the names into a single span
                position = new Position(position.Line, position.Column, name.Length);
            }

            return new Tuple<string, Position>(name, position);
        }
    }
}
