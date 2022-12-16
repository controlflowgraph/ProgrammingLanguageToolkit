package plt.lexer;

public interface RegexLexerFactory<T, R extends BasicToken<T>>
{
    RegexLexer<T, R> create();
}
