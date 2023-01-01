package plt.vm.model;

import java.util.HashMap;
import java.util.Map;

public class Meta
{
    private final Map<String, Object> data = new HashMap<>();

    public void add(String name, Object data)
    {
        if(this.data.containsKey(name))
            throw new RuntimeException("Metadata '" + name + "' already defined!");
        this.data.put(name, data);
    }

    public Object get(String name)
    {
        return this.data.get(name);
    }

    public <T> T get(String name, Class<T> cls)
    {
        return cls.cast(get(name));
    }
}
