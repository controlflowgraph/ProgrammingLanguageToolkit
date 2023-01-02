package plt.vm.extensions.calc;

import java.util.List;

public class LongCalc extends NumberCalc<Long>
{

    public LongCalc()
    {
        super(
                "long",
                Long.class,
                List.of(
                        Long::sum,
                        (a, b) -> a - b,
                        (a, b) -> a * b,
                        (a, b) -> a / b,
                        (a, b) -> a % b
                ),
                Long::compare
        );
    }
}
