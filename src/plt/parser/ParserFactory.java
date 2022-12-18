package plt.parser;

import plt.lexer.BasicToken;

public interface ParserFactory<T, R extends BasicToken<T>, O>
{
    Parser<T, R, O> create();
}
