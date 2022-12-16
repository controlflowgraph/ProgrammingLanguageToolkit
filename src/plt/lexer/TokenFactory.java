package plt.lexer;

public interface TokenFactory<T, V extends BasicToken<T>>
{
    V create(String text, T type, Region region);
}
