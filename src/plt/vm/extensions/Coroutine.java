package plt.vm.extensions;

import plt.vm.Extension;
import plt.vm.context.FunctionContext;
import plt.vm.model.Func;

import java.util.Deque;

public class Coroutine extends Extension
{
    public Coroutine()
    {
        super("co");

        function("crash", (c, v) -> {
            throw new RuntimeException("Reached the end of the routine!");
        });

        function("yield", (c, v) -> {
            Object val = c.get(v.inputs()[0]);
            Deque<FunctionContext> stack = c.getContext().stack();
            stack.pop();
            FunctionContext peek = stack.getFirst();
            int inst = peek.getInstructionPointer();
            int dest = peek.getInstruction(inst - 1).output();
            peek.set(dest, val);
        });

        function("create", (c, v) -> {
            Func function = c.getContext().program().getFunction(v.data(String.class));
            FunctionContext context = new FunctionContext(c.getContext(), function, new Object[0]);
            c.set(v.output(), context);
        });

        function("invoke", (c, v) -> {
            FunctionContext routine = c.get(v.inputs()[0], FunctionContext.class);
            c.getContext().stack().push(routine);
        });
    }
}
