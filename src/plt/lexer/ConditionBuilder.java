package plt.lexer;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class ConditionBuilder
{
    private ConditionBuilder() { }

    public static <T, R extends BasicToken<T>> Predicate<R> ifTextIs(String text)
    {
        return t -> t.isText(text);
    }

    public static <T, R extends BasicToken<T>> Predicate<R> ifTextIsOneOf(String ... text)
    {
        Set<String> s = new HashSet<>(Arrays.asList(text));
        return t -> s.contains(t.text());
    }

    public static <T, R extends BasicToken<T>> Predicate<R> ifTypeIs(T text)
    {
        return t -> t.isType(text);
    }

    public static <T, R extends BasicToken<T>> Predicate<R> ifTypeIsOneOf(T ... text)
    {
        Set<T> s = new HashSet<>(Arrays.asList(text));
        return t -> s.contains(t.type());
    }
}
