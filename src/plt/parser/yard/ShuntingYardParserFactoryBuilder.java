package plt.parser.yard;

import plt.lexer.BasicToken;
import plt.parser.Creator;
import plt.parser.ParserUnit;
import plt.parser.ParserUnitFactory;
import plt.provider.TokenProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class ShuntingYardParserFactoryBuilder<T, R extends BasicToken<T>, O>
{
    public static <T, R extends BasicToken<T>, O> ShuntingYardParserFactoryBuilder<T, R, O> create(String name, Class<R> input, Class<O> output)
    {
        return new ShuntingYardParserFactoryBuilder<>(name);
    }

    private final String name;
    private Predicate<R> stop;
    private Predicate<R> operator;
    private ShuntingYardFactory<O> factory;
    private final List<Predicate<R>> independentPatterns = new ArrayList<>();
    private final List<Predicate<R>> dependentPatterns = new ArrayList<>();
    private final List<Creator<T, R, O>> independent = new ArrayList<>();
    private final List<BiFunction<O, TokenProvider<T, R>, O>> dependent = new ArrayList<>();

    public ShuntingYardParserFactoryBuilder(String name)
    {
        this.name = name;
    }

    public ShuntingYardParserFactoryBuilder<T, R, O> stop(Predicate<R> stop)
    {
        this.stop = stop;
        return this;
    }

    public ShuntingYardParserFactoryBuilder<T, R, O> when(Predicate<R> check, Creator<T, R, O> creator)
    {
        this.independentPatterns.add(check);
        this.independent.add(creator);
        return this;
    }

    public ShuntingYardParserFactoryBuilder<T, R, O> when(Predicate<R> check, BiFunction<O, TokenProvider<T, R>, O> creator)
    {
        this.dependentPatterns.add(check);
        this.dependent.add(creator);
        return this;
    }

    public ParserUnitFactory<T, R, O> build()
    {
        return new ParserUnitFactory<>(this.name)
        {
            @Override
            public ParserUnit<T, R, O> create()
            {
                return new ShuntingYardParser<>(name, stop, operator, factory, independentPatterns, independent, dependentPatterns, dependent);
            }
        };
    }

    public ShuntingYardParserFactoryBuilder<T, R, O> operator(Predicate<R> check)
    {
        this.operator = check;
        return this;
    }

    public ShuntingYardParserFactoryBuilder<T, R, O> factory(ShuntingYardFactory<O> factory)
    {
        this.factory = factory;
        return this;
    }
}
