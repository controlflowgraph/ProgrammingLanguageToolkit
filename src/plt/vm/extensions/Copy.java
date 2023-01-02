package plt.vm.extensions;

import plt.vm.Extension;

public class Copy extends Extension
{
    public Copy()
    {
        super("copy");
        function("val", (c, v) -> c.set(v.output(), c.get(v.inputs()[0])));
    }
}
