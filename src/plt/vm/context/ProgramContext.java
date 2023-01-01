package plt.vm.context;

import plt.vm.model.Program;

import java.util.ArrayDeque;
import java.util.Deque;

public record ProgramContext(Program program, Deque<FunctionContext> stack)
{
    public ProgramContext(Program program)
    {
        this(program, new ArrayDeque<>());
    }
}
