package plt.vm;

import plt.vm.context.FunctionContext;
import plt.vm.context.ProgramContext;
import plt.vm.model.Func;
import plt.vm.model.Instruction;
import plt.vm.model.Program;

import java.util.Deque;
import java.util.List;
import java.util.Map;

public class VirtualMachine
{
    private final Map<String, Functionality> functions;
    private final List<Processor<Program>> pp;
    private final List<Processor<Func>> fp;

    public VirtualMachine(Map<String, Functionality> functions, List<Processor<Program>> pp, List<Processor<Func>> fp)
    {
        this.functions = functions;
        this.pp = pp;
        this.fp = fp;
    }

    public void run(Program program)
    {
        this.pp.forEach(p -> p.process(program));
        this.fp.forEach(p -> program.functions().forEach(p::process));

        ProgramContext context = new ProgramContext(program);
        FunctionContext c = new FunctionContext(context, program.getFunction("main"), new Object[0]);
        context.stack().push(c);
        run(context);
    }

    private void run(ProgramContext context)
    {
        Deque<FunctionContext> stack = context.stack();
        while(!stack.isEmpty())
        {
            FunctionContext c = stack.peek();
            Instruction instruction = c.getInstruction();
            if(!this.functions.containsKey(instruction.name()))
                throw new RuntimeException("No callback for instruction '" + instruction.name() + "'!");
            Functionality functionality = this.functions.get(instruction.name());
            functionality.execute(c, instruction);
        }
    }
}
