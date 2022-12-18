package plt.parser.simple;

import plt.lexer.BasicToken;
import plt.parser.ParserUnit;
import plt.provider.TokenProvider;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class SimpleParser<T, R extends BasicToken<T>, O> implements ParserUnit<T, R, O>
{
    private final String name;
    private final Class<O> output;
    private final List<Function<TokenProvider<T, R>, Optional<?>>> list;

    public SimpleParser(String name, Class<O> output, List<Function<TokenProvider<T, R>, Optional<?>>> list)
    {
        this.name = name;
        this.output = output;
        this.list = list;
    }

    @Override
    public O parse(TokenProvider<T, R> provider)
    {
        List<Object> arguments = list.stream()
                .map(f -> f.apply(provider))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Object.class::cast)
                .toList();
        Class<?>[] cls = arguments.stream()
                .map(Object::getClass)
                .toArray(Class[]::new);
        try
        {
            Constructor<?> constructor = output.getDeclaredConstructors()[0];
            return (O) constructor.newInstance(arguments.toArray());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("Unable to find constructor matching " + Arrays.toString(cls) + "!");
        }
    }
}
