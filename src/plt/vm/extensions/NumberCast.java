package plt.vm.extensions;

import plt.vm.Extension;

import java.util.List;
import java.util.function.Function;

public abstract class NumberCast<T> extends Extension
{
    protected record Converter<T> (String target, Function<T, ?> converter)
    {

    }

    protected NumberCast(String name, Class<T> cls, List<Converter<T>> converter)
    {
        super("cast");
        for (Converter<T> conv : converter)
        {
            function(name + "-to-" + conv.target(), (c, v) -> {
                T value = cls.cast(c.get(v.inputs()[0]));
                Object result = conv.converter.apply(value);
                c.set(v.output(), result);
            });
        }
    }
}
