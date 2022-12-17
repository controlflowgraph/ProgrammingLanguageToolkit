package plt.lexer;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class RegexLexerFactoryBuilder<T, R extends BasicToken<T>>
{
    public static <T, R extends BasicToken<T>> RegexLexerFactoryBuilder<T, R> create()
    {
        return new RegexLexerFactoryBuilder<>();
    }
    private TokenFactory<T, R> factory;
    private final List<Category<T>> categories = new ArrayList<>();
    private final Map<T, List<Transformer<T, R>>> transformer = new HashMap<>();
    private final List<Predicate<R>> filters = new ArrayList<>();
    private Predicate<T> fail = t -> false;

    public RegexLexerFactoryBuilder<T, R> factory(TokenFactory<T, R> factory)
    {
        this.factory = factory;
        return this;
    }

    public RegexLexerFactoryBuilder<T, R> category(T type, Pattern pattern)
    {
        return category(new Category<>(type, pattern));
    }

    public RegexLexerFactoryBuilder<T, R> category(T type, String pattern)
    {
        return category(new Category<>(type, pattern));
    }

    public RegexLexerFactoryBuilder<T, R> category(Category<T> category)
    {
        this.categories.add(category);
        return this;
    }

    public RegexLexerFactoryBuilder<T, R> transformer(T type, UnaryOperator<R> transformer)
    {
        return transformer(type, v -> true, transformer);
    }

    public RegexLexerFactoryBuilder<T, R> transformer(T type, Predicate<R> check, UnaryOperator<R> transformer)
    {
        this.transformer.computeIfAbsent(type, k -> new ArrayList<>()).add(new Transformer<>(check, transformer));
        return this;
    }

    public RegexLexerFactoryBuilder<T, R> filter(Predicate<R> check)
    {
        this.filters.add(check);
        return this;
    }

    public RegexLexerFactoryBuilder<T, R> fail(T type)
    {
        this.fail = t -> Objects.equals(t, type);
        return this;
    }

    public LexerFactory<T, R> build()
    {
        if(this.factory == null)
            throw new RuntimeException("Token factory is required!");
        return () -> new RegexLexer<>(this.factory, this.categories, this.transformer, this.filters, this.fail);
    }
}
