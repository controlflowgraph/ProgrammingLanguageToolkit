package plt.parser;

import plt.lexer.BasicToken;
import plt.provider.TokenProvider;

import java.util.ArrayList;
import java.util.List;

public class MatchingParser<T, R extends BasicToken<T>, O> implements ParserUnit<T, R, List<O>>
{
    private final String name;
    private final String separator;
    private final String opening;
    private final String closing;
    private final Creator<T, R, O> creator;

    public MatchingParser(String name, String separator, String opening, String closing, Creator<T, R, O> creator)
    {
        this.name = name;
        this.separator = separator;
        this.opening = opening;
        this.closing = closing;
        this.creator = creator;
    }

    @Override
    public List<O> parse(TokenProvider<T, R> provider)
    {
        List<O> parsed = new ArrayList<>();
        provider.assertNextIs(this.opening);
        boolean required = false;
        while (provider.has() && !provider.nextIs(this.closing))
        {
            parsed.add(this.creator.create(provider));
            if(this.separator != null)
            {
                required = provider.nextIs(this.separator);
                if(required)
                    provider.assertNextIs(this.separator);
            }
        }

        if(required)
            throw new RuntimeException("Expected another " + this.name + "!");

        provider.assertNextIs(this.closing);
        return parsed;
    }
}
