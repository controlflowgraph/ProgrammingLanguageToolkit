package plt.vm;

import plt.vm.model.Func;
import plt.vm.model.Program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Extension
{
    private final String name;
    private final Map<String, Functionality> functions = new HashMap<>();
    private final List<Processor<Program>> programProcessors = new ArrayList<>();
    private final List<Processor<Func>> functionProcessors = new ArrayList<>();

    protected Extension(String name)
    {
        this.name = name;
    }

    protected void function(String name, Functionality function)
    {
        this.functions.put(name, function);
    }

    protected void func(Processor<Func> processor)
    {
        this.functionProcessors.add(processor);
    }

    protected void program(Processor<Program> processor)
    {
        this.programProcessors.add(processor);
    }

    public String getName()
    {
        return this.name;
    }

    public Map<String, Functionality> getFunctions()
    {
        return this.functions;
    }

    public List<Processor<Func>> getFunctionProcessors()
    {
        return this.functionProcessors;
    }

    public List<Processor<Program>> getProgramProcessors()
    {
        return this.programProcessors;
    }
}
