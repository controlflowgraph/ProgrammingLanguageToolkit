package plt.vm.extensions;

import plt.vm.Extension;
import plt.vm.context.FunctionContext;
import plt.vm.model.Instruction;

import java.util.List;
import java.util.function.BinaryOperator;

public class NumberCalc<T> extends Extension
{
    private final Class<T> cls;
    private static final List<String> OPERATIONS = List.of(
            "add",
            "sub",
            "mul",
            "div",
            "mod"
    );

    protected NumberCalc(String name, Class<T> cls, List<BinaryOperator<T>> operators)
    {
        super(name);
        this.cls = cls;
        for (int i = 0; i < OPERATIONS.size(); i++)
        {
            String operation = OPERATIONS.get(i);
            BinaryOperator<T> op = operators.get(i);
            function(operation, (c, v) -> calc(c, v, op));
        }

        function("val", (c, v) -> c.set(v.output(), v.data()));
    }

    private void calc(FunctionContext context, Instruction instruction, BinaryOperator<T> op)
    {
        T v1 = context.get(instruction.inputs()[0], this.cls);
        T v2 = context.get(instruction.inputs()[1], this.cls);
        context.set(instruction.output(), op.apply(v1, v2));
    }
}