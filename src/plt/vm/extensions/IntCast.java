package plt.vm.extensions;

import java.util.List;

public class IntCast extends NumberCast<Integer>
{

    public IntCast()
    {
        super(
                "long",
                Integer.class,
                List.of(
                        new Converter<>("long", l -> (long) l),
                        new Converter<>("double", l -> (double) l)
                )
        );
    }
}