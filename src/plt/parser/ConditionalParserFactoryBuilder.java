package plt.parser;

import plt.lexer.BasicToken;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ConditionalParserFactoryBuilder<T, R extends BasicToken<T>, O>
{
    public static <T, R extends BasicToken<T>, O> ConditionalParserFactoryBuilder<T, R, O> create(
            String name,
            Class<R> input,
            Class<O> output
    )
    {
        return new ConditionalParserFactoryBuilder<>(name);
    }

    private final String name;
    private final List<Predicate<R>> conditions = new ArrayList<>();
    private final List<Creator<T, R, O>> creators = new ArrayList<>();

    public ConditionalParserFactoryBuilder(String name)
    {
        this.name = name;
    }

    public ConditionalParserFactoryBuilder<T, R, O> when(T value, Creator<T, R, O> creator)
    {
        return when(t -> t.isType(value), creator);
    }

    public ConditionalParserFactoryBuilder<T, R, O> when(String value, Creator<T, R, O> creator)
    {
        return when(t -> t.isText(value), creator);
    }

    public ConditionalParserFactoryBuilder<T, R, O> when(Predicate<R> condition, Creator<T, R, O> creator)
    {
        this.conditions.add(condition);
        this.creators.add(creator);
        return this;
    }

    public ParserUnitFactory<T, R, O> build()
    {
        return new ParserUnitFactory<>(this.name)
        {
            @Override
            public ParserUnit<T, R, O> create()
            {
                return new ConditionalParser<>(name, conditions, creators);
            }
        };
    }
}
