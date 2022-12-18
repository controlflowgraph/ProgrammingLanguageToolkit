package plt.parser.conditional;

import plt.lexer.BasicToken;
import plt.parser.Creator;
import plt.parser.ParserUnit;
import plt.provider.TokenProvider;

import java.util.List;
import java.util.function.Predicate;

public class ConditionalParser<T, R extends BasicToken<T>, O> implements ParserUnit<T, R, O>
{
    private final String name;
    private final List<Predicate<R>> conditions;
    private final List<Creator<T, R, O>> creators;

    public ConditionalParser(String name, List<Predicate<R>> conditions, List<Creator<T, R, O>> creators)
    {
        this.name = name;
        this.conditions = conditions;
        this.creators = creators;
    }

    @Override
    public O parse(TokenProvider<T, R> provider)
    {
        if(!provider.has())
            throw new RuntimeException("Expected token for " + this.name + " but no tokens left!");

        R peek = provider.peek();
        for (int i = 0; i < this.conditions.size(); i++)
        {
            if (this.conditions.get(i).test(peek))
            {
                return this.creators.get(i).create(provider);
            }
        }
        throw new RuntimeException("Non exhaustive match for " + this.name + " and token " + peek + "!");
    }
}
