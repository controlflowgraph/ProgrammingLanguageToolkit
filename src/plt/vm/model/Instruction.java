package plt.vm.model;

import java.util.Arrays;

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

    @Override
    public String toString()
    {
        return "Instruction{" +
                "name='" + name + '\'' +
                ", inputs=" + Arrays.toString(inputs) +
                ", output=" + output +
                ", data=" + data +
                '}';
    }
}
