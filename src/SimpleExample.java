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
import plt.parser.yard.ShuntingYard;
import plt.parser.yard.ShuntingYardFactoryBuilder;
import plt.parser.yard.ShuntingYardParserFactoryBuilder;
import plt.provider.TokenProvider;

import java.util.*;
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
                fn fac(v)
                {
                    if(v == 0)
                    {
                        ret 1;
                    }
                    ret v * fac(v - 1);
                }
                                
                print(fac(10));
                print([1, 2, 3][0]);
                print((1, 2, 3)[0]);
                """;
        List<Token> tokens = lex(code);
        List<Element> parse = parse(tokens);
        Environment generate = generate(parse);
        run(generate);
    }

    public static List<Token> lex(String text)
    {
        LexerFactory<TokenType, Token> factory = RegexLexerFactoryBuilder
                .create(Token::new)
                .category(TokenType.COMMENT, Pattern.compile("//[^\n]*"))
                .category(TokenType.IDENTIFIER, Pattern.compile("[_a-zA-Z]\\w*"))
                .category(TokenType.NUMBER, Pattern.compile("\\d+(\\.\\d+)?"))
                .category(TokenType.SYNTAX, Pattern.compile("[{(\\[.,;\\])}]"))
                .category(TokenType.OPERATOR, Pattern.compile("[+\\-*/%=<>!&|^]+"))
                .category(TokenType.UNKNOWN, Pattern.compile("[^ \t\r\n]"))
                .transformer(
                        TokenType.IDENTIFIER,
                        ifTextIsOneOf("true", "false"),
                        Token.replaceTypeBy(TokenType.BOOLEAN)
                )
                .transformer(
                        TokenType.IDENTIFIER,
                        ifTextIsOneOf("fn", "if", "else", "ret", "var"),
                        Token.replaceTypeBy(TokenType.KEYWORD)
                )
                .filter(ifTypeIs(TokenType.COMMENT))
                .fail(TokenType.UNKNOWN)
                .build();

        return factory.create().lex(text);
    }

    public interface Element
    {
        void generate(Environment env);
    }

    public record Parameter(String name)
    {
    }

    public record Func(String name, List<Parameter> parameters, Element body) implements Element
    {
        @Override
        public void generate(Environment env)
        {
            env.push(this.name);
            for (int i = 0; i < this.parameters.size(); i++)
            {
                env.add(new Instruction("arg", this.parameters.get(i).name, List.of(), "" + i));
            }
            this.body.generate(env);
            env.pop();
        }
    }

    public record Block(List<Element> elements) implements Element
    {
        @Override
        public void generate(Environment env)
        {
            for (Element element : elements)
            {
                element.generate(env);
            }
        }
    }

    public record Expression(Segment segment) implements Element
    {
        @Override
        public void generate(Environment env)
        {
            this.segment.generate(env);
        }
    }

    public interface Segment
    {
        String generate(Environment env);

        default String assign(Environment env, Segment right)
        {
            throw new RuntimeException("Unable to assign to " + getClass().getSimpleName() + "!");
        }
    }

    public record Num(String value) implements Segment
    {

        @Override
        public String generate(Environment env)
        {
            String next = env.current().counter.next();
            env.add(new Instruction("num", next, List.of(), this.value));
            return next;
        }
    }

    public record Bool(String value) implements Segment
    {

        @Override
        public String generate(Environment env)
        {
            String next = env.current().counter.next();
            env.add(new Instruction("bool", next, List.of(), this.value));
            return next;
        }
    }

    public record Id(String value) implements Segment
    {
        @Override
        public String assign(Environment env, Segment right)
        {
            String next = env.current().counter.next();
            String generate = right.generate(env);
            env.add(new Instruction("set-var", next, List.of(generate), this.value));
            return next;
        }

        @Override
        public String generate(Environment env)
        {
            String next = env.current().counter.next();
            env.add(new Instruction("get-var", next, List.of(), this.value));
            return next;
        }
    }

    public record Un(String op, Segment source) implements Segment
    {
        @Override
        public String generate(Environment env)
        {
            String generate = this.source.generate(env);
            String next = env.current().counter.next();
            String operation = switch (this.op)
                    {
                        case "!" -> "not";
                        case "-" -> "neg";
                        default -> throw new RuntimeException("Unknown " + this.op + "!");
                    };
            env.add(new Instruction(operation, next, List.of(generate), null));
            return next;
        }
    }

    public record Bin(String op, Segment left, Segment right) implements Segment
    {
        @Override
        public String generate(Environment env)
        {
            if (this.op.equals("="))
            {
                return this.left.assign(env, this.right);
            }
            String l = this.left.generate(env);
            String r = this.right.generate(env);
            String next = env.current().counter.next();
            String operation = switch (this.op)
                    {
                        case "+" -> "add";
                        case "-" -> "sub";
                        case "*" -> "mul";
                        case "/" -> "div";
                        case "==" -> "eq";
                        default -> throw new RuntimeException("Unknown " + this.op + "!");
                    };
            env.add(new Instruction(operation, next, List.of(l, r), null));
            return next;
        }
    }

    public record Tuple(List<Segment> segments) implements Segment
    {
        @Override
        public String generate(Environment env)
        {
            List<String> results = this.segments.stream()
                    .map(s -> s.generate(env))
                    .toList();
            String next = env.current().counter.next();
            env.add(new Instruction("tuple", next, results, null));
            return next;
        }
    }

    public record Invocation(Segment source, String name, List<Segment> arguments) implements Segment
    {
        @Override
        public String generate(Environment env)
        {
            String next = env.current().counter.next();
            List<String> args = new ArrayList<>();
            String generate = this.source.generate(env);
            args.add(generate);
            this.arguments.stream()
                    .map(a -> a.generate(env))
                    .forEach(args::add);
            env.add(new Instruction("invoke", next, args, this.name));
            return next;
        }
    }

    public record Selection(Segment source, String name) implements Segment
    {
        @Override
        public String assign(Environment env, Segment right)
        {
            String value = right.generate(env);
            String next = env.current().counter.next();
            String generate = this.source.generate(env);
            env.add(new Instruction("set-attr", next, List.of(generate, value), this.name));
            return next;
        }

        @Override
        public String generate(Environment env)
        {
            String next = env.current().counter.next();
            String generate = this.source.generate(env);
            env.add(new Instruction("get-attr", next, List.of(generate), this.name));
            return next;
        }
    }

    public record Index(Segment source, Segment index) implements Segment
    {
        @Override
        public String assign(Environment env, Segment right)
        {
            String value = right.generate(env);
            String dest = this.source.generate(env);
            String key = this.index.generate(env);
            String next = env.current().counter.next();
            env.add(new Instruction("set-index", next, List.of(dest, key, value), null));
            return next;
        }

        @Override
        public String generate(Environment env)
        {
            String generate = this.source.generate(env);
            String key = this.index.generate(env);
            String next = env.current().counter.next();
            env.add(new Instruction("get-index", next, List.of(generate, key), null));
            return next;
        }
    }

    public record Lst(List<Segment> segments) implements Segment
    {
        @Override
        public String generate(Environment env)
        {
            String next = env.current().counter.next();
            List<String> args = this.segments.stream()
                    .map(s -> s.generate(env))
                    .toList();
            env.add(new Instruction("list", next, args, null));
            return next;
        }
    }

    public record Return(Segment segment) implements Element
    {

        @Override
        public void generate(Environment env)
        {
            String result = this.segment.generate(env);
            env.add(new Instruction("ret", "_", List.of(result), null));
        }
    }

    public record Declaration(String name, Segment value) implements Element
    {
        @Override
        public void generate(Environment env)
        {
            env.add(new Instruction("def", this.name, List.of(), ""));
            String result = this.value.generate(env);
            env.add(new Instruction("get-var", this.name, List.of(result), ""));
        }
    }

    public record If(Segment condition, Element body, Element successor) implements Element
    {

        @Override
        public void generate(Environment env)
        {
            String check = this.condition.generate(env);
            String otherwise = env.current().labels.next();
            String after = env.current().labels.next();
            env.add(new Instruction("if", "_", List.of(check), otherwise));
            this.body.generate(env);
            env.add(new Instruction("jump", "_", List.of(), after));
            env.add(new Instruction("label", "_", List.of(), otherwise));
            if (this.successor != null)
            {
                this.successor.generate(env);
            }
            env.add(new Instruction("label", "_", List.of(), after));
        }
    }

    public static class Counter
    {
        private final String prefix;
        private int value;

        public Counter(String prefix)
        {
            this.prefix = prefix;
        }

        public String next()
        {
            return this.prefix + this.value++;
        }
    }

    public record Scope(List<Instruction> instructions, Counter counter, Counter labels)
    {

    }

    public record Environment(Map<String, Scope> functions, Deque<Scope> stack)
    {
        public Scope get(String s)
        {
            return this.functions.get(s);
        }

        public Scope current()
        {
            return this.stack.peek();
        }

        public void add(Instruction instruction)
        {
            current().instructions.add(instruction);
        }

        public void push(String name)
        {
            Scope s = new Scope(new ArrayList<>(), new Counter("$"), new Counter(":"));
            this.stack.push(s);
            this.functions.put(name, current());
        }

        public void pop()
        {
            this.stack.pop();
        }
    }

    public record Instruction(String name, String dest, List<String> arguments, String data)
    {
    }

    public record Call(String name, List<Segment> arguments) implements Segment
    {
        @Override
        public String generate(Environment env)
        {
            List<String> args = this.arguments.stream()
                    .map(a -> a.generate(env))
                    .toList();
            String next = env.current().counter.next();
            env.add(new Instruction("call", next, args, this.name));
            return next;
        }
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
            new Level(true, Set.of("=", "+=", "-=", "*=", "/=", "%="))
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
                .when("var", builder.parser("var"))
                .when("{", builder.parser("block"))
                .when(t -> !t.isType(TokenType.KEYWORD), builder.parser("expr"))
                .build()
        );

        builder.add(SimpleParserFactoryBuilder.create("var", Token.class, Declaration.class)
                .check("var")
                .check(TokenType.IDENTIFIER, Token::text)
                .check("=")
                .parse(p -> sb.parser("segment").create(p))
                .check(";")
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
                .when(t -> true, p -> new Block(List.of()))
                .build()
        );

        builder.add(SimpleParserFactoryBuilder.create("if", Token.class, If.class)
                .check("if")
                .parse(sb.parser("segment"))
                .parse(builder.parser("element"))
                .parse(builder.parser("else"))
                .build()
        );

        ParserUnitFactory<TokenType, Token, List<Parameter>> args = MatchingParserFactoryBuilder
                .create("args", Token.class, Parameter.class)
                .opening("(")
                .creator(c -> new Parameter(
                        c.assertNextIs(TokenType.IDENTIFIER).text()
                ))
                .separator(",")
                .closing(")")
                .build();

        builder.add(SimpleParserFactoryBuilder
                .create("func", Token.class, Func.class)
                .check("fn")
                .check(TokenType.IDENTIFIER, BasicToken::text)
                .parse(p -> args.create().parse(p))
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

        sb.add(paren.transform("tuple", l -> l.size() == 1 ? l.get(0) : new Tuple(l)));

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
                .when(t -> t.isType(TokenType.IDENTIFIER), p -> {
                    String text = p.next().text();
                    if(p.nextIs("(")) return new Call(text, paren.create().parse(p));
                    else return new Id(text);
                })
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
                    if (p.nextIs("(")) return new Invocation(c, text, paren.create().parse(p));
                    else return new Selection(c, text);
                })
                .operator(t -> t.isType(TokenType.OPERATOR))
                .build()
        );

        builder.add(new ParserUnitFactory<>("expr")
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

    private static Environment generate(List<Element> elements)
    {
        Environment environment = new Environment(new HashMap<>(), new ArrayDeque<>());
        environment.push("-global-");
        for (Element element : elements)
        {
            element.generate(environment);
        }
        environment.pop();
        return environment;
    }

    private static void run(Environment env)
    {
        run(env, "-global-", List.of());
    }

    private static Object run(Environment env, String str, List<Object> args)
    {
        if(str.equals("print"))
        {
            System.out.println(String.join("    ", args.stream().map(Objects::toString).toList()));
            return null;
        }
        Scope scope = env.get(str);
        Map<String, Integer> mapping = new HashMap<>();
        List<Instruction> instructions = scope.instructions;
        for (int i = 0; i < instructions.size(); i++)
        {
            if(instructions.get(i).name.equals("label"))
            {
                mapping.put(instructions.get(i).data, i - 1);
            }
        }
        Map<String, Object> values = new HashMap<>();
        int index = 0;
        while (0 <= index && index < instructions.size())
        {
            Instruction in = instructions.get(index);
            switch (in.name)
            {
                case "get-var" -> values.put(in.dest, values.get(in.data));
                case "def" -> values.put(in.dest, null);
                case "if" -> {
                    Object o = values.get(in.arguments.get(0));
                    if(!Objects.equals(o, true))
                    {
                        index = mapping.get(in.data);
                    }
                }
                case "arg" -> values.put(in.dest, args.get(Integer.parseInt(in.data)));
                case "num" -> values.put(in.dest, Double.parseDouble(in.data));
                case "bool" -> values.put(in.dest, in.data.equals("true"));
                case "call" -> values.put(in.dest, run(env, in.data, in.arguments.stream().map(values::get).toList()));
                case "ret" -> { return values.get(in.arguments.get(0)); }
                case "eq" -> {
                    Object v1 = values.get(in.arguments.get(0));
                    Object v2 = values.get(in.arguments.get(1));
                    boolean cond = Objects.equals(v1, v2);
                    values.put(in.dest, cond);
                }
                case "sub" -> values.put(in.dest, (double) values.get(in.arguments.get(0)) - (double) values.get(in.arguments.get(1)));
                case "mul" -> values.put(in.dest, (double) values.get(in.arguments.get(0)) * (double) values.get(in.arguments.get(1)));
                case "label" -> {}
                case "jump" -> index = mapping.get(in.data);
                case "tuple", "list" -> values.put(in.dest, in.arguments.stream().map(values::get).toList());
                case "get-index" -> {
                    List<?> lst = (List<?>) values.get(in.arguments.get(0));
                    int i = (int)(double) values.get(in.arguments.get(1));
                    values.put(in.dest, lst.get(i));
                }
                default -> throw new RuntimeException("Unknown " + in + "!");
            }
            index++;
        }
        return null;
    }

}
