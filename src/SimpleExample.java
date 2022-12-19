import plt.lexer.BasicToken;
import plt.lexer.LexerFactory;
import plt.lexer.RegexLexerFactoryBuilder;
import plt.lexer.Region;
import plt.parser.ParserBuilder;
import plt.parser.ParserUnit;
import plt.parser.ParserUnitFactory;
import plt.parser.conditional.ConditionalParserFactoryBuilder;
import plt.parser.matching.MatchingParserFactoryBuilder;
import plt.parser.simple.SimpleParserFactoryBuilder;
import plt.parser.yard.ShuntingYardParserFactoryBuilder;
import plt.parser.yard.ShuntingYard;
import plt.parser.yard.ShuntingYardFactoryBuilder;
import plt.provider.TokenProvider;

import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static plt.lexer.ConditionBuilder.ifTextIsOneOf;
import static plt.lexer.ConditionBuilder.ifTypeIs;

public class SimpleExample
{
    public enum TokenType
    {
        COMMENT,
        KEYWORD,
        IDENTIFIER,
        NUMBER,
        BOOLEAN,
        SYNTAX,
        OPERATOR,
        UNKNOWN,
    }

    public static class Token extends BasicToken<TokenType>
    {
        protected Token(String text, TokenType type, Region region)
        {
            super(text, type, region);
        }

        public static UnaryOperator<Token> replaceTypeBy(TokenType type)
        {
            return t -> new Token(t.text(), type, t.region());
        }

        @Override
        public String toString()
        {
            return type() + " " + text() + " " + region();
        }
    }

    public static void main(String[] args)
    {
        String code = """
                fn some(p1, p2)
                {
                    if(true)
                    {
                        a.print(10);
                    }
                    else
                    {
                        b.print(20);
                    }
                    (10, 20, 30 + (123, 456, 789));
                    10 + 20 * 30;
                    [1, 2, 3][0];
                    a[123];
                    a.b(b.c);
                    ret 10 + 20;
                }
                """;
        List<Token> tokens = lex(code);
        List<Element> parse = parse(tokens);
        parse.forEach(System.out::println);
    }

    public static List<Token> lex(String text)
    {
        LexerFactory<TokenType, Token> factory = RegexLexerFactoryBuilder
                .create(Token::new)
                .category(TokenType.COMMENT, Pattern.compile("//[^\n]*"))
                .category(TokenType.IDENTIFIER, Pattern.compile("[_a-zA-Z]\\w*"))
                .category(TokenType.NUMBER, Pattern.compile("\\d+(\\.\\d+)?"))
                .category(TokenType.SYNTAX, Pattern.compile("[{(\\[.,;\\])}]"))
                .category(TokenType.OPERATOR, Pattern.compile("[+\\-*/%]"))
                .category(TokenType.UNKNOWN, Pattern.compile("[^ \t\r\n]"))
                .transformer(
                        TokenType.IDENTIFIER,
                        ifTextIsOneOf("true", "false"),
                        Token.replaceTypeBy(TokenType.BOOLEAN)
                )
                .transformer(
                        TokenType.IDENTIFIER,
                        ifTextIsOneOf("fn", "if", "ret"),
                        Token.replaceTypeBy(TokenType.KEYWORD)
                )
                .filter(ifTypeIs(TokenType.COMMENT))
                .fail(TokenType.UNKNOWN)
                .build();

        return factory.create().lex(text);
    }

    public interface Element
    {
    }

    public record Parameter(String name)
    {
    }

    public record Func(String name, List<Parameter> parameters, Element body) implements Element
    {
    }

    public record Block(List<Element> elements) implements Element
    {
    }

    public record Expression(Segment segment) implements Element
    {

    }

    public interface Segment
    {

    }

    public record Num(String value) implements Segment
    {

    }

    public record Bool(String value) implements Segment
    {

    }

    public record Id(String value) implements Segment
    {

    }

    public record Un(String op, Segment source) implements Segment
    {
    }

    public record Bin(String op, Segment left, Segment right) implements Segment
    {
    }

    public record Tuple(List<Segment> segments) implements Segment
    {
    }

    public record Invocation(Segment source, String name, List<Segment> arguments) implements Segment
    {
    }

    public record Selection(Segment source, String name) implements Segment
    {
    }

    public record Index(Segment source, Segment index) implements Segment
    {
    }

    public record Lst(List<Segment> segments) implements Segment
    {
    }

    public record Return(Segment segment) implements Element
    {
    }

    public record If(Segment condition, Element body, Element successor) implements Element
    {

    }

    private record Level(boolean binary, Set<String> operators)
    {

    }

    private static final List<Level> PRECEDENCES = List.of(
            new Level(true, Set.of(".", "[]")),
            new Level(false, Set.of("+", "-", "!")),
            new Level(true, Set.of("<<", ">>", "<<<", ">>>")),
            new Level(true, Set.of("&")),
            new Level(true, Set.of("^")),
            new Level(true, Set.of("|")),
            new Level(true, Set.of("*", "/", "%")),
            new Level(true, Set.of("+", "-")),
            new Level(true, Set.of("<", ">", "<=", ">=")),
            new Level(true, Set.of("==", "!=")),
            new Level(true, Set.of("&&")),
            new Level(true, Set.of("^^")),
            new Level(true, Set.of("||")),
            new Level(true, Set.of("=", "+=", "-="))
    );

    private static ShuntingYard.Operator get(String op, boolean bin)
    {
        for (int i = 0; i < PRECEDENCES.size(); i++)
        {
            if (PRECEDENCES.get(i).binary == bin && PRECEDENCES.get(i).operators.contains(op))
            {
                return new ShuntingYard.Operator(i, bin, op);
            }
        }
        throw new RuntimeException((bin ? "Binary" : "Unary") + " operator '" + op + "' not recognized!");
    }

    private static List<Element> parse(List<Token> tokens)
    {
        ParserBuilder<Element, TokenType, Token> builder = ParserBuilder.create(Token.class, Element.class);
        ParserBuilder<Segment, TokenType, Token> sb = ParserBuilder.create(Token.class, Segment.class);

        builder.add(ConditionalParserFactoryBuilder
                .create("element", Token.class, Element.class)
                .when("fn", builder.parser("func"))
                .when("if", builder.parser("if"))
                .when("ret", builder.parser("ret"))
                .when("{", builder.parser("block"))
                .when(t -> !t.isType(TokenType.KEYWORD), builder.parser("expr"))
                .build()
        );

        builder.add(SimpleParserFactoryBuilder.create("ret", Token.class, Return.class)
                .check("ret")
                .parse(p -> sb.parser("segment").create(p))
                .check(";")
                .build()
        );

        builder.add(ConditionalParserFactoryBuilder.create("else", Token.class, Element.class)
                .when("else", p -> {
                    p.assertNextIs("else");
                    return builder.parser("element").create(p);
                })
                .when(t -> true, p -> null)
                .build()
        );

        builder.add(SimpleParserFactoryBuilder.create("if", Token.class, If.class)
                .check("if")
                .parse(sb.parser("segment"))
                .parse(builder.parser("element"))
                .parse(builder.parser("else"))
                .build()
        );

        builder.add(MatchingParserFactoryBuilder
                .create("args", Token.class, Parameter.class)
                .opening("(")
                .creator(c -> new Parameter(
                        c.assertNextIs(TokenType.IDENTIFIER).text()
                ))
                .separator(",")
                .closing(")")
                .build()
        );

        builder.add(SimpleParserFactoryBuilder
                .create("func", Token.class, Func.class)
                .check("fn")
                .check(TokenType.IDENTIFIER, BasicToken::text)
                .parse(builder.parser("args"))
                .parse(builder.parser("element"))
                .build()
        );

        builder.add(MatchingParserFactoryBuilder
                .create("block", Token.class, Element.class)
                .opening("{")
                .creator(builder.parser("element"))
                .closing("}")
                .build()
                .transform(Block::new)
        );

        ParserUnitFactory<TokenType, Token, List<Segment>> paren = MatchingParserFactoryBuilder
                .create("paren", Token.class, Segment.class)
                .opening("(")
                .creator(sb.parser("segment"))
                .separator(",")
                .closing(")")
                .build();

        sb.add(paren.transform("tuple", Tuple::new));

        ParserUnitFactory<TokenType, Token, List<Segment>> brack = MatchingParserFactoryBuilder
                .create("brack", Token.class, Segment.class)
                .opening("[")
                .creator(sb.parser("segment"))
                .separator(",")
                .closing("]")
                .build();

        sb.add(brack.transform("list", Lst::new));


        sb.add(ShuntingYardParserFactoryBuilder
                .create("segment", Token.class, Segment.class)
                .factory(ShuntingYardFactoryBuilder.create(Segment.class)
                        .calculator(SimpleExample::get)
                        .merger(Bin::new)
                        .wrapper(Un::new)
                        .build()
                )
                .when(t -> t.isType(TokenType.NUMBER), p -> new Num(p.next().text()))
                .when(t -> t.isType(TokenType.BOOLEAN), p -> new Bool(p.next().text()))
                .when(t -> t.isType(TokenType.IDENTIFIER), p -> new Id(p.next().text()))
                .when(t -> t.isText("["), p -> sb.parser("list").create(p))
                .when(t -> t.isText("("), p -> sb.parser("tuple").create(p))
                .when(t -> t.isText("["), (c, p) -> {
                    List<Segment> keys = brack.create().parse(p);
                    if (keys.size() != 1)
                        throw new RuntimeException("Expected exactly one key in index!");
                    return new Index(c, keys.get(0));
                })
                .when(t -> t.isText("."), (c, p) -> {
                    p.assertNextIs(".");
                    String text = p.assertNextIs(TokenType.IDENTIFIER).text();
                    if (p.nextIs("("))
                    {
                        List<Segment> arguments = paren.create().parse(p);
                        return new Invocation(c, text, arguments);
                    }
                    else
                    {
                        return new Selection(c, text);
                    }
                })
                .operator(t -> t.isType(TokenType.OPERATOR))
                .build()
        );

        builder.add(new ParserUnitFactory<TokenType, Token, Element>("expr")
        {
            @Override
            public ParserUnit<TokenType, Token, Element> create()
            {
                return p -> {
                    Expression segment = new Expression(sb.parser("segment").create(p));
                    p.assertNextIs(";");
                    return segment;
                };
            }
        });

        return builder.create("element").create().parseAll(new TokenProvider<>(tokens));
    }
}
