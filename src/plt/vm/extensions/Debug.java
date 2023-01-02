package plt.vm.extensions;

import plt.vm.Extension;

import java.util.Arrays;

public class Debug extends Extension
{
    public Debug()
    {
        super("debug");
        function("print", (c, v) -> {
            Object o = c.get(v.inputs()[0]);

            if(o instanceof Object[] a)
            {
                System.out.println(Arrays.toString(a));
            }
            else
            {
                System.out.println(o);
            }
        });
    }
}
