package plt.provider;

import plt.lexer.BasicToken;

import java.util.List;

public class TokenProvider<T, R extends BasicToken<T>> implements Provider<R>
{
    private final Provider<R> provider;

    public TokenProvider(List<R> tokens)
    {
        this(new ListProvider<>(tokens));
    }

    public TokenProvider(Provider<R> provider)
    {
        this.provider = provider;
    }

    @Override
    public boolean has()
    {
        return this.provider.has();
    }

    @Override
    public R next()
    {
        return this.provider.next();
    }

    @Override
    public R peek()
    {
        return this.provider.peek();
    }

    public boolean nextIs(T type)
    {
        return has() && peek().isType(type);
    }

    public R assertNextIs(T type)
    {
        if(!nextIs(type))
            throw new RuntimeException("Expected " + type + "!");
        return next();
    }

    public boolean nextIs(String text)
    {
        return has() && peek().isText(text);
    }

    public R assertNextIs(String text)
    {
        if(!nextIs(text))
            throw new RuntimeException("Expected " + text + "!");
        return next();
    }
}
