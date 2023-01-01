package plt.vm.model;

import java.util.List;

public record Program(List<Func> functions, Meta meta)
{
    public Func getFunction(String name)
    {
        for (Func function : this.functions)
        {
            if(function.name().equals(name))
                return function;
        }
        throw new RuntimeException("No function with name '" + name + "' found!");
    }
}
