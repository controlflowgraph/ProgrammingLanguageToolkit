package plt.vm.extensions;

import java.util.List;

public class LongCast extends NumberCast<Long>
{

    public LongCast()
    {
        super(
                "long",
                Long.class,
                List.of(
                        new Converter<>("int", l -> (int) (long) l),
                        new Converter<>("double", l -> (double) (long) l)
                )
        );
    }
}
