package plt.parser;

import plt.lexer.BasicToken;
import plt.provider.TokenProvider;

public interface ParserUnit<T, R extends BasicToken<T>, O>
{
    O parse(TokenProvider<T, R> provider);
}
