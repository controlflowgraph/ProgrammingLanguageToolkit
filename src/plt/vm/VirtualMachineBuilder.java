package plt.vm;

import plt.vm.model.Func;
import plt.vm.model.Program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualMachineBuilder
{
    private final List<Extension> extensions = new ArrayList<>();

    public static VirtualMachineBuilder builder()
    {
        return new VirtualMachineBuilder();
    }

    private VirtualMachineBuilder()
    {

    }

    public VirtualMachineBuilder add(Extension extension)
    {
        this.extensions.add(extension);
        return this;
    }

    public VirtualMachine build()
    {
        Map<String, Functionality> merged = new HashMap<>();
        List<Processor<Program>> pp = new ArrayList<>();
        List<Processor<Func>> fp = new ArrayList<>();
        for (Extension extension : this.extensions)
        {
            String prefix = extension.getName();
            Map<String, Functionality> functions = extension.getFunctions();
            for (String name : functions.keySet())
            {
                if(merged.containsKey(name))
                    throw new RuntimeException("Instruction '" + name + "' is defined multiple times!");
                merged.put(prefix + "-" + name, functions.get(name));
            }

            pp.addAll(extension.getProgramProcessors());
            fp.addAll(extension.getFunctionProcessors());
        }
        return new VirtualMachine(merged, pp, fp);
    }
}
