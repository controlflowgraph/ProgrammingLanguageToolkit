package plt.vm.extensions;

import plt.vm.Extension;
import plt.vm.context.FunctionContext;
import plt.vm.model.Func;

import java.util.Arrays;
import java.util.List;

public class Obj extends Extension
{
    public record Descriptor(String name, List<String> fields)
    {

    }

    public record Instance(Descriptor descriptor, Object[] fields)
    {
        public void setField(String name, Object value)
        {
            this.fields[getFieldIndex(name)] = value;
        }

        public <T> T getField(String name, Class<T> cls)
        {
            return cls.cast(getField(name));
        }

        public Object getField(String name)
        {
            return this.fields[getFieldIndex(name)];
        }

        private int getFieldIndex(String name)
        {
            List<String> strings = this.descriptor.fields();
            for (int i = 0; i < strings.size(); i++)
            {
                String field = strings.get(i);
                if (field.equals(name))
                {
                    return i;
                }
            }
            throw new RuntimeException("No field with name '" + name + "' on type '" + this.descriptor.name() + "'!");
        }
    }

    public Obj()
    {
        super("obj");

        function("create", (c, v) -> {
            Descriptor descriptor = v.data(Descriptor.class);
            int size = descriptor.fields().size();
            Object[] elements = new Object[size];
            c.set(v.output(), new Instance(descriptor, elements));
        });

        function("get", (c, v) -> {
            Instance inst = c.get(v.inputs()[0], Instance.class);
            String field = v.data(String.class);
            Object value = inst.getField(field);
            c.set(v.output(), value);
        });

        function("set", (c, v) -> {
            Instance inst = c.get(v.inputs()[0], Instance.class);
            String field = v.data(String.class);
            Object value = c.get(v.inputs()[1]);
            inst.setField(field, value);
        });

        function("invoke", (c, v) -> {
            Object[] args = new Object[v.inputs().length];
            int[] inputs = v.inputs();
            for (int i = 0; i < inputs.length; i++)
                args[i] = c.get(inputs[i]);
            Instance o = c.get(v.inputs()[0], Instance.class);
            String name = o.descriptor().name() + "$" + v.data(String.class);
            Func function = c.getContext().program().getFunction(name);
            FunctionContext functionContext = new FunctionContext(c.getContext(), function, args);
            c.getContext().stack().push(functionContext);
        });
    }
}
