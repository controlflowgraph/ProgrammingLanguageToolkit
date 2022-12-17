package plt.provider;

import java.util.List;

public class ListProvider<T> implements Provider<T>
{
    private final List<T> elements;
    private int index;

    public ListProvider(List<T> elements)
    {
        this.elements = elements;
    }

    @Override
    public boolean has()
    {
        return this.index < this.elements.size();
    }

    @Override
    public T next()
    {
        return this.elements.get(this.index++);
    }

    @Override
    public T peek()
    {
        return this.elements.get(this.index);
    }
}
