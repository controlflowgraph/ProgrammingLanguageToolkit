package plt.lexer;

public abstract class BasicToken<T>
{
    private final String text;
    private final T type;
    private final Region region;

    protected BasicToken(String text, T type, Region region)
    {
        this.text = text;
        this.type = type;
        this.region = region;
    }

    public String text()
    {
        return this.text;
    }

    public T type()
    {
        return this.type;
    }

    public Region region()
    {
        return this.region;
    }
}
