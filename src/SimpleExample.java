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
import plt.vm.VirtualMachine;
import plt.vm.VirtualMachineBuilder;
import plt.vm.extensions.*;
import plt.vm.extensions.calc.BoolCalc;
import plt.vm.extensions.calc.DoubleCalc;
import plt.vm.extensions.calc.IntCalc;
import plt.vm.extensions.cast.DoubleCast;
import plt.vm.extensions.cast.IntCast;
import plt.vm.model.Instruction;
import plt.vm.model.Meta;
import plt.vm.model.Program;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

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
                                
                fn LstAdd(l, e)
                {
                    if(l.size == double(l.elements.length))
                    {
                        var a = Array(l.size * 2.0);
                        for(var i = 0.0; i < l.size; i = i + 1.0)
                        {
                            a[i] = l.elements[i];
                        }
                        l.elements = a;
                    }
                    l.elements[l.size] = e;
                    l.size = l.size + 1.0;
                }
                
                var l = [1, 2, 3, 4];
                LstAdd(l, 1234);
                print(l.elements.elements);
                                
                print((1, 2, 3)._0);
                                
                var t = [1, 2, 3];
                t.add(69);
                t.add(420);
                t[0] = 123;
                print(t);
                print(fac(10));
                print([1, 2, 3][0]);
                var a = true;
                print(a);
                                
                for(var i = 0; i < 10; i = i + 1)
                {
                    print(i);
                }
                                
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
                        ifTextIsOneOf("fn", "if", "else", "ret", "var", "for"),
                        Token.replaceTypeBy(TokenType.KEYWORD)
                )
                .filter(ifTypeIs(TokenType.COMMENT))
                .fail(TokenType.UNKNOWN)
                .build();

        return factory.create().lex(text);
    }

    public static int[] args(int... args)
    {
        return args;
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
            for (Parameter parameter : this.parameters)
            {
                env.current().define(parameter.name);
            }
            this.body.generate(env);
            env.add(new Instruction("fn-ret", args(), -1, null));
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
        int generate(Environment env);

        default int assign(Environment env, Segment right)
        {
            throw new RuntimeException("Unable to assign to " + getClass().getSimpleName() + "!");
        }
    }

    public record Num(String value) implements Segment
    {

        @Override
        public int generate(Environment env)
        {
            int next = env.current().counter.next();
            env.add(new Instruction("double-val", args(), next, Double.parseDouble(this.value)));
            return next;
        }
    }

    public record Bool(String value) implements Segment
    {

        @Override
        public int generate(Environment env)
        {
            int next = env.current().counter.next();
            env.add(new Instruction("bool-val", args(), next, this.value));
            return next;
        }
    }

    public record Id(String value) implements Segment
    {
        @Override
        public int assign(Environment env, Segment right)
        {
            int index = env.current().index(this.value);
            int generate = right.generate(env);
            env.add(new Instruction("copy-val", args(generate), index, null));
            return index;
        }

        @Override
        public int generate(Environment env)
        {
            int next = env.current().counter.next();
            int index = env.current().index(this.value);
            env.add(new Instruction("copy-val", args(index), next, null));
            return next;
        }
    }

    public record Un(String op, Segment source) implements Segment
    {
        @Override
        public int generate(Environment env)
        {
            int generate = this.source.generate(env);
            int next = env.current().counter.next();
            String operation = switch (this.op)
                    {
                        case "!" -> "bool-not";
                        case "-" -> "neg";
                        default -> throw new RuntimeException("Unknown " + this.op + "!");
                    };
            env.add(new Instruction(operation, args(generate), next, null));
            return next;
        }
    }

    public record Bin(String op, Segment left, Segment right) implements Segment
    {
        @Override
        public int generate(Environment env)
        {
            if (this.op.equals("="))
            {
                return this.left.assign(env, this.right);
            }
            int l = this.left.generate(env);
            int r = this.right.generate(env);
            int next = env.current().counter.next();
            String operation = switch (this.op)
                    {
                        case "+" -> "double-add";
                        case "-" -> "double-sub";
                        case "*" -> "double-mul";
                        case "/" -> "double-div";
                        case "==" -> "double-equal";
                        case "<" -> "double-less";
                        default -> throw new RuntimeException("Unknown " + this.op + "!");
                    };
            env.add(new Instruction(operation, args(l, r), next, null));
            return next;
        }
    }

    public record Tuple(List<Segment> segments) implements Segment
    {
        @Override
        public int generate(Environment env)
        {
            int next = env.current().counter.next();

            env.add(new Instruction("obj-create", args(), next, new Obj.Descriptor(
                    "Tuple",
                    IntStream.range(0, this.segments.size())
                            .mapToObj(i -> "_" + i)
                            .toList()
            )));
            List<Segment> segmentList = this.segments;
            for (int i = 0; i < segmentList.size(); i++)
            {
                Segment segment = segmentList.get(i);
                int generate = segment.generate(env);
                env.add(new Instruction("obj-set", args(next, generate), -1, "_" + i));
            }
            return next;
        }
    }

    public record Invocation(Segment source, String name, List<Segment> arguments) implements Segment
    {
        @Override
        public int generate(Environment env)
        {
            int next = env.current().counter.next();
            int[] args = new int[this.arguments.size() + 1];
            args[0] = this.source.generate(env);
            for (int i = 0; i < this.arguments.size(); i++)
            {
                args[i + 1] = this.arguments.get(i).generate(env);
            }
            env.add(new Instruction("obj-invoke", args, next, this.name));
            return next;
        }
    }

    public record Selection(Segment source, String name) implements Segment
    {
        @Override
        public int assign(Environment env, Segment right)
        {
            int value = right.generate(env);
            int next = env.current().counter.next();
            int generate = this.source.generate(env);
            env.add(new Instruction("obj-set", args(generate, value), next, this.name));
            return next;
        }

        @Override
        public int generate(Environment env)
        {
            int next = env.current().counter.next();
            int generate = this.source.generate(env);
            env.add(new Instruction("obj-get", args(generate), next, this.name));
            return next;
        }
    }

    public record Index(Segment source, Segment index) implements Segment
    {
        @Override
        public int assign(Environment env, Segment right)
        {
            int value = right.generate(env);
            int dest = this.source.generate(env);
            int key = this.index.generate(env);
            int next = env.current().counter.next();
            env.add(new Instruction("obj-invoke", args(dest, key, value), -1, "set"));
            return next;
        }

        @Override
        public int generate(Environment env)
        {
            int generate = this.source.generate(env);
            int key = this.index.generate(env);
            int next = env.current().counter.next();
            env.add(new Instruction("obj-invoke", args(generate, key), next, "get"));
            return next;
        }
    }

    public record Lst(List<Segment> segments) implements Segment
    {
        @Override
        public int generate(Environment env)
        {
            int len = env.current().counter.next();
            int ind = env.current().counter.next();
            int arr = env.current().counter.next();
            int lst = env.current().counter.next();

            env.add(new Instruction("obj-create", lst, new Obj.Descriptor(
                    "List",
                    List.of(
                            "size",
                            "elements"
                    )
            )));
            int count = count(this.segments.size());
            // create the array
            env.add(new Instruction("int-val", args(), len, count));
            env.add(new Instruction("arr-create", args(len), arr, null));
            List<Segment> segmentList = this.segments;
            for (int i = 0; i < segmentList.size(); i++)
            {
                Segment segment = segmentList.get(i);
                int generate = segment.generate(env);
                env.add(new Instruction("int-val", args(), ind, i));
                env.add(new Instruction("arr-set", args(arr, ind, generate), -1, null));
            }

            int s = env.current().counter().next();

            env.add(new Instruction("double-val", args(), s, (double) this.segments.size()));
            env.add(new Instruction("obj-set", args(lst, s), -1, "size"));
            env.add(new Instruction("obj-set", args(lst, arr), -1, "elements"));

            return lst;
        }

        private int count(int min)
        {
            int size = 1;
            while (min > size)
                size <<= 1;
            return size;
        }
    }

    public record Return(Segment segment) implements Element
    {

        @Override
        public void generate(Environment env)
        {
            int result = this.segment.generate(env);
            env.add(new Instruction("fn-ret-val", args(result), -1, null));
        }
    }

    public record For(Declaration declaration, Segment condition, Segment inc, Element body) implements Element
    {
        @Override
        public void generate(Environment env)
        {
            this.declaration.generate(env);
            String cond = "lab" + env.current().labels().next();
            String end = "lab" + env.current().labels().next();
            int neg = env.current().counter().next();
            env.add(new Instruction("jump-label", -1, cond));
            int con = this.condition.generate(env);
            env.add(new Instruction("bool-not", args(con), neg, null));
            env.add(new Instruction("jump-if", args(neg), -1, end));
            this.body.generate(env);
            this.inc.generate(env);
            env.add(new Instruction("jump-to", -1, cond));
            env.add(new Instruction("jump-label", -1, end));
        }
    }

    public record Declaration(String name, Segment value) implements Element
    {
        @Override
        public void generate(Environment env)
        {
            int define = env.current().define(this.name);
            int result = this.value.generate(env);
            env.add(new Instruction("copy-val", args(result), define, null));
        }
    }

    public record If(Segment condition, Element body, Element successor) implements Element
    {

        @Override
        public void generate(Environment env)
        {
            int check = this.condition.generate(env);
            int neg = env.current().counter.next();
            String otherwise = "lab" + env.current().labels.next();
            String after = "lab" + env.current().labels.next();
            env.add(new Instruction("bool-not", args(check), neg, null));
            env.add(new Instruction("jump-if", args(neg), -1, otherwise));
            this.body.generate(env);
            env.add(new Instruction("jump-to", args(), -1, after));
            env.add(new Instruction("jump-label", args(), -1, otherwise));
            if (this.successor != null)
            {
                this.successor.generate(env);
            }
            env.add(new Instruction("jump-label", args(), -1, after));
        }
    }

    public static class Counter
    {
        private int value;

        public int next()
        {
            return this.value++;
        }
    }

    public record Scope(List<Instruction> instructions, Counter counter, Counter labels, Map<String, Integer> variables)
    {
        public Scope()
        {
            this(new ArrayList<>(), new Counter(), new Counter(), new HashMap<>());
        }

        public int define(String name)
        {
            int number = this.counter.next();
            this.variables.put(name, number);
            return number;
        }

        public int index(String value)
        {
            if (!this.variables.containsKey(value))
                throw new RuntimeException("No variable with name '" + value + "' defined!");
            return this.variables.get(value);
        }
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
            Scope s = new Scope();
            this.stack.push(s);
            this.functions.put(name, current());
        }

        public void pop()
        {
            this.stack.pop();
        }
    }

    public record Call(String name, List<Segment> arguments) implements Segment
    {
        @Override
        public int generate(Environment env)
        {
            int[] args = this.arguments.stream()
                    .mapToInt(a -> a.generate(env))
                    .toArray();
            int next = env.current().counter.next();
            env.add(new Instruction("fn-call", args, next, this.name));
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
                .when("for", builder.parser("for"))
                .when("ret", builder.parser("ret"))
                .when("var", builder.parser("var"))
                .when("{", builder.parser("block"))
                .when(t -> !t.isType(TokenType.KEYWORD), builder.parser("expr"))
                .build()
        );

        builder.add(SimpleParserFactoryBuilder.create("for", Token.class, For.class)
                .check("for")
                .check("(")
                .parse(builder.parser("var"))
                .parse(p -> sb.parser("segment").create(p))
                .check(";")
                .parse(p -> sb.parser("segment").create(p))
                .check(")")
                .parse(builder.parser("element"))
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
                    if (p.nextIs("(")) return new Call(text, paren.create().parse(p));
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
        environment.push("main");
        for (Element element : elements)
        {
            element.generate(environment);
        }
        environment.pop();
        return environment;
    }

    private static void run(Environment env)
    {
        VirtualMachine vm = VirtualMachineBuilder.builder()
                .add(new IntCalc())
                .add(new DoubleCalc())
                .add(new Fn())
                .add(new Jump())
                .add(new Obj())
                .add(new Arr())
                .add(new DoubleCast())
                .add(new IntCast())
                .add(new Copy())
                .add(new Debug())
                .add(new BoolCalc())
                .build();

        env.get("main").instructions().add(new Instruction("fn-ret", args(), -1, null));

        List<plt.vm.model.Func> v = env.functions.entrySet()
                .stream()
                .map(e -> new plt.vm.model.Func(e.getKey(), e.getValue().instructions()))
                .collect(ArrayList::new, List::add, ArrayList::addAll);

        v.add(new plt.vm.model.Func(
                "print",
                List.of(
                        new Instruction("debug-print", args(0), -1, null),
                        new Instruction("fn-ret", args(), -1, null)
                )
        ));

        v.add(new plt.vm.model.Func(
                "List$add",
                List.of(
                        new Instruction("copy-val", args(0), 3, null),
                        new Instruction("obj-get", args(3), 2, "size"),
                        new Instruction("copy-val", args(0), 6, null),
                        new Instruction("obj-get", args(6), 5, "elements"),
                        new Instruction("obj-get", args(5), 4, "length"),
                        new Instruction("fn-call", args(4), 7, "double"),
                        new Instruction("double-equal", args(2, 7), 8, null),
                        new Instruction("bool-not", args(8), 9, null),
                        new Instruction("jump-if", args(9), -1, "lab0"),
                        new Instruction("copy-val", args(0), 12, null),
                        new Instruction("obj-get", args(12), 11, "size"),
                        new Instruction("double-val", args(), 13, 2.0),
                        new Instruction("double-mul", args(11, 13), 14, null),
                        new Instruction("fn-call", args(14), 15, "Array"),
                        new Instruction("copy-val", args(15), 10, null),
                        new Instruction("double-val", args(), 17, 0.0),
                        new Instruction("copy-val", args(17), 16, null),
                        new Instruction("jump-label", args(), -1, "lab2"),
                        new Instruction("copy-val", args(16), 19, null),
                        new Instruction("copy-val", args(0), 21, null),
                        new Instruction("obj-get", args(21), 20, "size"),
                        new Instruction("double-less", args(19, 20), 22, null),
                        new Instruction("bool-not", args(22), 18, null),
                        new Instruction("jump-if", args(18), -1, "lab3"),
                        new Instruction("copy-val", args(0), 24, null),
                        new Instruction("obj-get", args(24), 23, "elements"),
                        new Instruction("copy-val", args(16), 25, null),
                        new Instruction("obj-invoke", args(23, 25), 26, "get"),
                        new Instruction("copy-val", args(10), 27, null),
                        new Instruction("copy-val", args(16), 28, null),
                        new Instruction("obj-invoke", args(27, 28, 26), -1, "set"),
                        new Instruction("copy-val", args(16), 30, null),
                        new Instruction("double-val", args(), 31, 1.0),
                        new Instruction("double-add", args(30, 31), 32, null),
                        new Instruction("copy-val", args(32), 16, null),
                        new Instruction("jump-to", args(), -1, "lab2"),
                        new Instruction("jump-label", args(), -1, "lab3"),
                        new Instruction("copy-val", args(10), 33, null),
                        new Instruction("copy-val", args(0), 35, null),
                        new Instruction("obj-set", args(35, 33), 34, "elements"),
                        new Instruction("jump-to", args(), -1, "lab1"),
                        new Instruction("jump-label", args(), -1, "lab0"),
                        new Instruction("jump-label", args(), -1, "lab1"),
                        new Instruction("copy-val", args(1), 36, null),
                        new Instruction("copy-val", args(0), 38, null),
                        new Instruction("obj-get", args(38), 37, "elements"),
                        new Instruction("copy-val", args(0), 40, null),
                        new Instruction("obj-get", args(40), 39, "size"),
                        new Instruction("obj-invoke", args(37, 39, 36), -1, "set"),
                        new Instruction("copy-val", args(0), 43, null),
                        new Instruction("obj-get", args(43), 42, "size"),
                        new Instruction("double-val", args(), 44, 1.0),
                        new Instruction("double-add", args(42, 44), 45, null),
                        new Instruction("copy-val", args(0), 47, null),
                        new Instruction("obj-set", args(47, 45), 46, "size"),
                        new Instruction("fn-ret", args(), -1, null)
                )
        ));

        v.add(new plt.vm.model.Func(
                "List$get",
                List.of(
                        new Instruction("obj-get", args(0), 2, "elements"),
                        new Instruction("cast-double-to-int", args(1), 3, null),
                        new Instruction("arr-get", args(2, 3), 4, null),
                        new Instruction("fn-ret-val", args(4), -1, null)
                )
        ));

        v.add(new plt.vm.model.Func(
                "List$set",
                List.of(
                        new Instruction("obj-get", args(0), 3, "elements"),
                        new Instruction("cast-double-to-int", args(1), 4, null),
                        new Instruction("arr-set", args(3, 4, 2), -1, null),
                        new Instruction("fn-ret", args(), -1, null)
                )
        ));

        v.add(new plt.vm.model.Func(
                "Array",
                List.of(
                        new Instruction("cast-double-to-int", args(0), 1, null),
                        new Instruction("arr-create", args(1), 2, null),
                        new Instruction("fn-ret-val", args(2), -1, null)
                )
        ));

        v.add(new plt.vm.model.Func(
                "Array$get",
                List.of(
                        new Instruction("cast-double-to-int", args(1), 2, null),
                        new Instruction("arr-get", args(0, 2), 3, null),
                        new Instruction("fn-ret-val", args(3), -1, null)
                )
        ));

        v.add(new plt.vm.model.Func(
                "Array$set",
                List.of(
                        new Instruction("cast-double-to-int", args(1), 3, null),
                        new Instruction("arr-set", args(0, 3, 2), -1, null),
                        new Instruction("fn-ret", args(), -1, null)
                )
        ));

        v.add(new plt.vm.model.Func(
                "double",
                List.of(
                        new Instruction("cast-int-to-double", args(0), 1, null),
                        new Instruction("fn-ret-val", args(1), -1, null)
                )
        ));

        Program program = new Program(v, new Meta());
        vm.run(program);
    }
}
