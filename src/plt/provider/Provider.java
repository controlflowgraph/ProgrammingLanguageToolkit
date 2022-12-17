package plt.provider;

public interface Provider<T>
{
    boolean has();
    T next();
    T peek();
}