package plt.parser;

import plt.lexer.BasicToken;

import java.util.HashMap;
import java.util.Map;

public class ParserBuilder<O, T, S extends BasicToken<T>>
{
    public static <O, T, S extends BasicToken<T>> ParserBuilder<O, T, S> create(
            Class<S> input,
            Class<O> output
    )
    {
        return new ParserBuilder<>(input, output);
    }

    private final Class<S> input;
    private final Class<O> output;
    private final Map<String, ParserUnitFactory<T, S, ? extends O>> mapping = new HashMap<>();

    protected ParserBuilder(Class<S> input, Class<O> output)
    {
        this.input = input;
        this.output = output;
    }

    public ParserBuilder<O, T, S> add(ParserUnitFactory<T, S, ? extends O> factory)
    {
        if(this.mapping.containsKey(factory.getName()))
            throw new RuntimeException("Factory with name " + factory.getName() + " already registered!");
        this.mapping.put(factory.getName(), factory);
        return this;
    }

    public Creator<T, S, O> parser(String name)
    {
        return p -> {
            if(!this.mapping.containsKey(name))
                throw new RuntimeException("No parser with name " + name + " found!");
            return (O) this.mapping.get(name).create().parse(p);
        };
    }

    public ParserFactory<T, S, O> create(String name)
    {
        return () -> new Parser<>(parser(name));
    }
}
