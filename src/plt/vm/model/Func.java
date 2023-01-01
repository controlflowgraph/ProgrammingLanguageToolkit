package plt.vm.model;

import java.util.List;

public record Func(Meta meta, String name, List<Instruction> instructions, int count)
{
    public Func(String name, List<Instruction> instructions)
    {
        this(new Meta(), name, instructions, instructions.stream().mapToInt(Instruction::output).max().orElse(0) + 1);
    }
}
