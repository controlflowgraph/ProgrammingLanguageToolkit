package plt.vm.extensions.calc;

import java.util.List;

public class DoubleCalc extends NumberCalc<Double>
{
    public DoubleCalc()
    {
        super(
                "double",
                Double.class,
                List.of(
                        Double::sum,
                        (a, b) -> a - b,
                        (a, b) -> a * b,
                        (a, b) -> a / b,
                        (a, b) -> a % b
                ),
                Double::compare
        );
    }
}
