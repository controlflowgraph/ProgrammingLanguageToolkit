package plt.vm.extensions.calc;

public class BoolCalc extends Calc<Boolean>
{
    public BoolCalc()
    {
        super("bool", Boolean.class);
        function("and", (c, v) -> calc(c, v, (a, b) -> a && b));
        function("or", (c, v) -> calc(c, v, (a, b) -> a || b));
        function("xor", (c, v) -> calc(c, v, (a, b) -> a ^ b));
    }
}
