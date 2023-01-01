import plt.vm.VirtualMachine;
import plt.vm.VirtualMachineBuilder;
import plt.vm.extensions.*;
import plt.vm.model.*;

import java.util.List;

public class VirtualMachineExample
{
    public static void main(String[] args)
    {
        VirtualMachine vm = VirtualMachineBuilder.builder()
                .add(new LongCalc())
                .add(new DoubleCalc())
                .add(new Fn())
                .add(new Jump())
                .add(new Debug())
                .build();

        Program program = new Program(List.of(
                new Func(
                        "main",
                        List.of(
                                new Instruction("double-val", 0, 1.0),
                                new Instruction("double-val", 1, 2.0),
                                new Instruction("fn-call", new int[]{0, 1}, 2, "f"),
                                new Instruction("debug-print", new int[]{2}, -1, null),
                                new Instruction("long-val", 3, 10L),
                                new Instruction("fn-call", new int[]{3}, 4, "factorial"),
                                new Instruction("debug-print", new int[]{4}, -1, null),
                                new Instruction("fn-ret", -1, null)
                        )
                ),
                new Func(
                        "f",
                        List.of(
                                new Instruction("double-add", new int[]{0, 1}, 2, null),
                                new Instruction("fn-ret-val", new int[]{2}, -1, null)
                        )
                ),
                new Func(
                        "factorial",
                        List.of(
                                new Instruction("long-val", 1, 1L),
                                new Instruction("long-less", new int[]{0, 1}, 2, null),
                                new Instruction("jump-if", new int[]{2}, -1, "other"),
                                new Instruction("long-sub", new int[]{0, 1}, 1, -1),
                                new Instruction("fn-call", new int[]{1}, 1, "factorial"),
                                new Instruction("long-mul", new int[]{0, 1}, 1, null),
                                new Instruction("jump-label", -1, "other"),
                                new Instruction("fn-ret-val", new int[]{1}, -1, null)
                        )
                )
        ), new Meta());

        vm.run(program);
    }
}
