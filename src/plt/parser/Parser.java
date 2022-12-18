package plt.parser;

import plt.lexer.BasicToken;
import plt.provider.TokenProvider;

import java.util.ArrayList;
import java.util.List;

public class Parser<T, R extends BasicToken<T>, O>
{
    private final Creator<T, R, O> unit;

    public Parser(Creator<T, R, O> unit)
    {
        this.unit = unit;
    }

    public List<O> parseAll(TokenProvider<T, R> provider)
    {
        List<O> elements = new ArrayList<>();
        while (provider.has())
        {
            elements.add(parse(provider));
        }
        return elements;
    }

    public O parse(TokenProvider<T, R> provider)
    {
        return this.unit.create(provider);
    }
}
