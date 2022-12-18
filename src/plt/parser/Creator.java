package plt.parser;

import plt.lexer.BasicToken;
import plt.provider.TokenProvider;

public interface Creator<T, R extends BasicToken<T>, O>
{
    O create(TokenProvider<T, R> provider);
}
