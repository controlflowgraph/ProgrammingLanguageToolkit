package plt.parser;

import plt.lexer.BasicToken;
import plt.provider.TokenProvider;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Function;

public class SimpleParserFactoryBuilder<T, R extends BasicToken<T>, O>
{
    public static <T, R extends BasicToken<T>, O> SimpleParserFactoryBuilder<T, R, O> create(
            String name,
            Class<R> input,
            Class<O> output
    )
    {
        return new SimpleParserFactoryBuilder<>(name, output);
    }

    private final String name;
    private final Class<O> output;
    private final List<Function<TokenProvider<T, R>, Optional<?>>> list = new ArrayList<>();

    private SimpleParserFactoryBuilder(String name, Class<O> output)
    {
        this.name = name;
        this.output = output;
    }

    public SimpleParserFactoryBuilder<T, R, O> check(String name)
    {
        this.list.add(p -> {
            p.assertNextIs(name);
            return Optional.empty();
        });
        return this;
    }

    public SimpleParserFactoryBuilder<T, R, O> check(String name, Function<R, ?> transformer)
    {
        this.list.add(p -> {
            R r = p.assertNextIs(name);
            return Optional.of(transformer.apply(r));
        });
        return this;
    }

    public SimpleParserFactoryBuilder<T, R, O> check(T type)
    {
        this.list.add(p -> {
            p.assertNextIs(type);
            return Optional.empty();
        });
        return this;
    }

    public SimpleParserFactoryBuilder<T, R, O> check(T type, Function<R, ?> transformer)
    {
        this.list.add(p -> {
            R r = p.assertNextIs(type);
            return Optional.of(transformer.apply(r));
        });
        return this;
    }

    public SimpleParserFactoryBuilder<T, R, O> parse(Creator<T, R, ?> creator)
    {
        this.list.add(p -> Optional.of(creator.create(p)));
        return this;
    }

    public ParserUnitFactory<T, R, O> build()
    {
        return new ParserUnitFactory<>(this.name)
        {
            @Override
            public ParserUnit<T, R, O> create()
            {
                return new SimpleParser<>(name, output, list);
            }
        };
    }
}
