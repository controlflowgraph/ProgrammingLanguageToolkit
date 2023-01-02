package plt.vm.extensions;

import plt.vm.Extension;

import java.util.List;

public class Arr extends Extension
{
    private static final Obj.Descriptor ARRAY = new Obj.Descriptor(
            "Array",
            List.of(
                    "length",
                    "elements"
            )
    );

    public Arr()
    {
        super("arr");

        function("create", (c, v) -> {
            Obj.Instance instance = new Obj.Instance(ARRAY, new Object[2]);
            int size = c.get(v.inputs()[0], Integer.class);
            instance.setField("length", size);
            Object[] elements = new Object[size];
            instance.setField("elements", elements);
            c.set(v.output(), instance);
        });

        function("get", (c, v) -> {
            Obj.Instance instance = c.get(v.inputs()[0], Obj.Instance.class);
            Object[] elements = instance.getField("elements", Object[].class);
            int index = c.get(v.inputs()[1], Integer.class);
            Object value = elements[index];
            c.set(v.output(), value);
        });

        function("set", (c, v) -> {
            Obj.Instance instance = c.get(v.inputs()[0], Obj.Instance.class);
            Object[] elements = instance.getField("elements", Object[].class);
            Object value = c.get(v.inputs()[2]);
            int index = c.get(v.inputs()[1], Integer.class);
            elements[index] = value;
        });
    }
}
