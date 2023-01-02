package plt.vm.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public record Func(Meta meta, String name, List<Instruction> instructions, int count)
{
    public Func(String name, List<Instruction> instructions)
    {
        this(new Meta(), name, instructions, calculateCount(instructions));
    }

    private static int calculateCount(List<Instruction> instructions)
    {
        int outMax = instructions.stream()
                .mapToInt(Instruction::output)
                .max()
                .orElse(0);

        int inMax = instructions.stream()
                .map(Instruction::inputs)
                .map(Arrays::stream)
                .map(IntStream::max)
                .mapToInt(o -> o.orElse(0))
                .max()
                .orElse(0);
        return Math.max(outMax, inMax) + 1;
    }
}
