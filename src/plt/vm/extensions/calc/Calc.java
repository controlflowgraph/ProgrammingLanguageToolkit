package plt.vm.extensions.calc;

import plt.vm.Extension;
import plt.vm.context.FunctionContext;
import plt.vm.model.Instruction;

import java.util.function.BinaryOperator;

public class Calc<T> extends Extension
{
    private final Class<T> cls;

    public Calc(String name, Class<T> cls)
    {
        super(name);
        this.cls = cls;
    }

    protected void calc(FunctionContext context, Instruction instruction, BinaryOperator<T> op)
    {
        T v1 = context.get(instruction.inputs()[0], this.cls);
        T v2 = context.get(instruction.inputs()[1], this.cls);
        context.set(instruction.output(), op.apply(v1, v2));
    }
}
