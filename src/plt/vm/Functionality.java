package plt.vm;

import plt.vm.context.FunctionContext;
import plt.vm.model.Instruction;

public interface Functionality
{
    void execute(FunctionContext context, Instruction instruction);
}
