package plt.lexer;

import java.util.function.Function;
import java.util.function.Predicate;

public record Transformer<T, R extends BasicToken<T>>(Predicate<R> check, Function<R, R> transformer)
{
}
