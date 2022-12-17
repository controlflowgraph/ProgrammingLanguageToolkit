package plt.lexer;

import java.util.List;

public interface Lexer<T, R extends BasicToken<T>>
{
    List<R> lex(String text);
}
