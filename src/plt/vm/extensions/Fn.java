package plt.vm.extensions;

import plt.vm.Extension;
import plt.vm.context.FunctionContext;
import plt.vm.model.Func;

import java.util.Deque;

public class Fn extends Extension
{
    public Fn()
    {
        super("fn");
        function("call", (c, v) -> {
            Object[] args = new Object[v.inputs().length];
            int[] inputs = v.inputs();
            for (int i = 0; i < inputs.length; i++)
                args[i] = c.get(inputs[i]);
            String name = v.data(String.class);
            Func function = c.getContext().program().getFunction(name);
            FunctionContext functionContext = new FunctionContext(c.getContext(), function, args);
            c.getContext().stack().push(functionContext);
        });
        function("ret", (c, v) -> c.getContext().stack().pop());
        function("ret-val", (c, v) -> {
            Object val = c.get(v.inputs()[0]);
            Deque<FunctionContext> stack = c.getContext().stack();
            stack.pop();
            FunctionContext peek = stack.getFirst();
            int inst = peek.getInstructionPointer();
            int dest = peek.getInstruction(inst - 1).output();
            peek.set(dest, val);
        });
    }
}
