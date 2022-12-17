package plt.lexer;

public interface LexerFactory<T, R extends BasicToken<T>>
{
    Lexer<T, R> create();
}
