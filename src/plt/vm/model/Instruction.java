package plt.vm.model;

public record Instruction(String name, int[] inputs, int output, Object data)
{
    public Instruction(String name, int output, Object data)
    {
        this(name, new int[0], output, data);
    }

    public <T> T data(Class<T> cls)
    {
        return cls.cast(data());
    }
}
