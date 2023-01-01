package plt.vm.model;

import plt.vm.Extension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Jump extends Extension
{
    private record Labels(Map<String, Integer> labels)
    {
        public int get(String name)
        {
            if(!this.labels.containsKey(name))
                throw new RuntimeException("No label '" + name + "' is defined!");
            return this.labels.get(name);
        }
    }

    public Jump()
    {
        super("jump");
        String key = "labels";
        func(f -> {
            Map<String, Integer> labels = new HashMap<>();
            List<Instruction> instructions = f.instructions();
            for (int i = 0; i < instructions.size(); i++)
            {
                Instruction instruction = instructions.get(i);
                if(instruction.name().equals("jump-label"))
                {
                    labels.put(instruction.data(String.class), i);
                }
            }
            f.meta().add(key, new Labels(labels));
        });

        function("if", (c, v) -> {
            boolean condition = c.get(v.inputs()[0], Boolean.class);
            if(condition)
            {
                Labels labels = c.getFunction().meta().get(key, Labels.class);
                c.setInstruction(labels.get(v.data(String.class)));
            }
        });

        function("to", (c, v) -> {
            Labels labels = c.getFunction().meta().get(key, Labels.class);
            c.setInstruction(labels.get(v.data(String.class)));
        });

        function("label", (c, v) -> {});
    }
}
