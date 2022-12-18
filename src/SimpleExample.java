import plt.lexer.BasicToken;
import plt.lexer.LexerFactory;
import plt.lexer.RegexLexerFactoryBuilder;
import plt.lexer.Region;
import plt.parser.conditional.ConditionalParserFactoryBuilder;
import plt.parser.matching.MatchingParserFactoryBuilder;
import plt.parser.ParserBuilder;
import plt.parser.simple.SimpleParserFactoryBuilder;
import plt.provider.TokenProvider;

import java.util.List;
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
                    fn name() { }
                }
                """;
        List<Token> tokens = lex(code);
        tokens.forEach(System.out::println);

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
                .category(TokenType.SYNTAX, Pattern.compile("[{(,)}]"))
                .category(TokenType.UNKNOWN, Pattern.compile("[^ \t\r\n]"))
                .transformer(
                        TokenType.IDENTIFIER,
                        ifTextIsOneOf("true", "false"),
                        Token.replaceTypeBy(TokenType.BOOLEAN)
                )
                .transformer(
                        TokenType.IDENTIFIER,
                        ifTextIsOneOf("fn", "if"),
                        Token.replaceTypeBy(TokenType.KEYWORD)
                )
                .filter(ifTypeIs(TokenType.COMMENT))
                .fail(TokenType.UNKNOWN)
                .build();

        return factory.create().lex(text);
    }

    public interface Element { }
    public record Parameter(String name) { }
    public record Func(String name, List<Parameter> parameters, Element body) implements Element { }
    public record Block(List<Element> elements) implements Element { }

    private static List<Element> parse(List<Token> tokens)
    {
        ParserBuilder<Element, TokenType, Token> builder = ParserBuilder.create(Token.class, Element.class);

        builder.add(ConditionalParserFactoryBuilder
                .create("element", Token.class, Element.class)
                .when("fn", builder.parser("func"))
                .when("{", builder.parser("block"))
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

        return builder.create("element").create().parseAll(new TokenProvider<>(tokens));
    }
}
