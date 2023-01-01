package plt.vm.extensions;

import plt.vm.Extension;

public class Debug extends Extension
{
    public Debug()
    {
        super("debug");
        function("print", (c, v) -> System.out.println(c.get(v.inputs()[0])));
    }
}
