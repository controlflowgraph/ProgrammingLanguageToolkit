import plt.vm.VirtualMachine;
import plt.vm.VirtualMachineBuilder;
import plt.vm.extensions.Debug;
import plt.vm.extensions.DoubleCalc;
import plt.vm.extensions.Fn;
import plt.vm.extensions.LongCalc;
import plt.vm.model.Func;
import plt.vm.model.Instruction;
import plt.vm.model.Meta;
import plt.vm.model.Program;

import java.util.List;

public class VirtualMachineExample
{
    public static void main(String[] args)
    {
        VirtualMachine vm = VirtualMachineBuilder.builder()
                .add(new LongCalc())
                .add(new DoubleCalc())
                .add(new Fn())
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
                                new Instruction("fn-ret", -1, null)
                        )
                ),
                new Func(
                        "f",
                        List.of(
                                new Instruction("double-add", new int[]{0, 1}, 2, null),
                                new Instruction("fn-ret-val", new int[]{2}, -1, null)
                        )
                )
        ), new Meta());

        vm.run(program);
    }
}
