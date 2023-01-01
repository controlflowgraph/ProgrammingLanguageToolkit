package plt.vm.context;

import plt.vm.model.Func;
import plt.vm.model.Instruction;

public class FunctionContext
{
    private final Object[] variables;
    private final ProgramContext context;
    private final Func func;
    private int instruction;
    private Object value;

    public FunctionContext(ProgramContext context, Func func, Object[] args)
    {
        this.context = context;
        this.func = func;
        this.variables = new Object[func.count()];
        System.arraycopy(args, 0, this.variables, 0, args.length);
    }

    public Object get(int index)
    {
        return this.variables[index];
    }

    public <T> T get(int index, Class<T> cls)
    {
        return cls.cast(get(index));
    }

    public void set(int index, Object object)
    {
        this.variables[index] = object;
    }

    public boolean hasInstruction()
    {
        return 0 <= this.instruction && this.instruction < this.func.instructions().size();
    }

    public Instruction getInstruction()
    {
        return getInstruction(this.instruction++);
    }

    public Instruction getInstruction(int index)
    {
        return this.func.instructions().get(index);
    }

    public int getInstructionPointer()
    {
        return this.instruction;
    }

    public void setInstruction(int index)
    {
        this.instruction = index;
    }

    public Object getReturnValue()
    {
        return this.value;
    }

    public Func getFunction()
    {
        return this.func;
    }

    public void setValue(Object value)
    {
        this.value = value;
    }

    public ProgramContext getContext()
    {
        return this.context;
    }
}
