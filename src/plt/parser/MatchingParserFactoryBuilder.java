package plt.parser;

import plt.lexer.BasicToken;

import java.util.List;

public class MatchingParserFactoryBuilder<T, R extends BasicToken<T>, O>
{
    public static <T, R extends BasicToken<T>, O> MatchingParserFactoryBuilder<T, R, O> create(String name, Class<R> input, Class<O> output)
    {
        return new MatchingParserFactoryBuilder<>(name);
    }

    private final String name;
    private String separator;
    private String opening;
    private String closing;
    private Creator<T, R, O> creator;

    public MatchingParserFactoryBuilder(String name)
    {
        this.name = name;
    }

    public MatchingParserFactoryBuilder<T, R, O> separator(String separator)
    {
        this.separator = separator;
        return this;
    }

    public MatchingParserFactoryBuilder<T, R, O> opening(String opening)
    {
        this.opening = opening;
        return this;
    }

    public MatchingParserFactoryBuilder<T, R, O> closing(String closing)
    {
        this.closing = closing;
        return this;
    }

    public MatchingParserFactoryBuilder<T, R, O> creator(Creator<T, R, O> creator)
    {
        this.creator = creator;
        return this;
    }

    public ParserUnitFactory<T, R, List<O>> build()
    {
        return new ParserUnitFactory<>(this.name)
        {
            @Override
            public ParserUnit<T, R, List<O>> create()
            {
                return new MatchingParser<>(name, separator, opening, closing, creator);
            }
        };
    }
}
