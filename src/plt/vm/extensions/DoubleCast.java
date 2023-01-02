package plt.vm.extensions;

import java.util.List;

public class DoubleCast extends NumberCast<Double>
{

    public DoubleCast()
    {
        super(
                "long",
                Double.class,
                List.of(
                        new Converter<>("int", l -> (int) (double) l),
                        new Converter<>("long", l -> (long) (double) l)
                )
        );
    }
}
