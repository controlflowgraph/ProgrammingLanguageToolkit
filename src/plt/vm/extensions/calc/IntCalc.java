package plt.vm.extensions.calc;

import java.util.List;

public class IntCalc extends NumberCalc<Integer>
{
    public IntCalc()
    {
        super(
                "int",
                Integer.class,
                List.of(
                        Integer::sum,
                        (a, b) -> a - b,
                        (a, b) -> a * b,
                        (a, b) -> a / b,
                        (a, b) -> a % b
                ),
                Integer::compare
        );
    }
}
