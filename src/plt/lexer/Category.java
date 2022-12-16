package plt.lexer;

import java.util.regex.Pattern;

public record Category <T> (T type, Pattern pattern)
{
    public Category(T type, String pattern)
    {
        this(type, Pattern.compile(pattern));
    }
}
