package plt.parser;

import plt.lexer.BasicToken;

import java.util.function.Function;

public abstract class ParserUnitFactory<T, R extends BasicToken<T>, O>
{
    private final String name;

    protected ParserUnitFactory(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public abstract ParserUnit<T, R, O> create();

    public <A> ParserUnitFactory<T, R, A> transform(Function<O, A> value)
    {
        ParserUnitFactory<T, R, O> c = this;
        return new ParserUnitFactory<>(this.name)
        {
            @Override
            public ParserUnit<T, R, A> create()
            {
                ParserUnit<T, R, O> traParserUnit = c.create();
                return p -> {
                    O parse = traParserUnit.parse(p);
                    return value.apply(parse);
                };
            }
        };
    }
}
