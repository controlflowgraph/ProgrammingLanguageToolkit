import plt.vm.VirtualMachine;
import plt.vm.VirtualMachineBuilder;
import plt.vm.extensions.*;
import plt.vm.extensions.calc.DoubleCalc;
import plt.vm.extensions.calc.LongCalc;
import plt.vm.extensions.cast.LongCast;
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
                .add(new Coroutine())
                .add(new Obj())
                .add(new Arr())
                .add(new LongCast())
                .build();

        Obj.Descriptor descriptor = new Obj.Descriptor(
                "Test",
                List.of(
                        "t"
                )
        );

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

                                new Instruction("co-create", 5, "nat"),
                                new Instruction("co-invoke", new int[]{5}, 6, null),
                                new Instruction("debug-print", new int[]{6}, -1, null),
                                new Instruction("co-invoke", new int[]{5}, 6, null),
                                new Instruction("debug-print", new int[]{6}, -1, null),


                                new Instruction("obj-create", 7, descriptor),
                                new Instruction("long-val", 8, 123L),
                                new Instruction("fn-call", new int[]{7, 8}, 7, "Test-init"),
                                new Instruction("obj-get", new int[]{7}, 8, "t"),
                                new Instruction("debug-print", new int[]{8}, -1, null),

                                new Instruction("long-val", 9, 3),
                                new Instruction("arr-create", new int[]{9}, 9, null),
                                new Instruction("long-val", 10, 2),
                                new Instruction("long-val", 11, 12345),
                                new Instruction("arr-set", new int[]{9, 10, 11}, -1, null),
                                new Instruction("arr-get", new int[]{9, 10}, 11, null),
                                new Instruction("debug-print", new int[]{11}, -1, null),

                                new Instruction("long-val", 12, 0xff00000000L),
                                new Instruction("cast-long-to-int", new int[]{12}, 12, null),
                                new Instruction("debug-print", new int[]{12}, -1, null),

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
                ),
                new Func(
                        "nat",
                        List.of(
                                new Instruction("long-val", 0, 1L),
                                new Instruction("jump-label", -1, "loop"),
                                new Instruction("co-yield", new int[]{0}, -1, null),
                                new Instruction("long-val", 1, 1L),
                                new Instruction("long-add", new int[]{0, 1}, 0, null),
                                new Instruction("jump-to", -1, "loop"),
                                new Instruction("co-crash",-1, null)
                        )
                ),
                new Func(
                        "Test-init",
                        List.of(
                                new Instruction("obj-set", new int[]{0, 1}, 1, "t"),
                                new Instruction("fn-ret-val", new int[]{0}, -1, null)
                        )
                )
        ), new Meta());

        vm.run(program);
    }
}
