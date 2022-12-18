package plt.parser.yard;

public class ShuntingYardFactoryBuilder<T>
{
    public static <T> ShuntingYardFactoryBuilder<T> create(Class<T> input)
    {
        return new ShuntingYardFactoryBuilder<>();
    }

    private ShuntingYard.Merger<T> merger;
    private ShuntingYard.Wrapper<T> wrapper;
    private ShuntingYard.PrecedenceCalculator calculator;

    public ShuntingYardFactoryBuilder<T> merger(ShuntingYard.Merger<T> merger)
    {
        this.merger = merger;
        return this;
    }

    public ShuntingYardFactoryBuilder<T> wrapper(ShuntingYard.Wrapper<T> wrapper)
    {
        this.wrapper = wrapper;
        return this;
    }

    public ShuntingYardFactoryBuilder<T> calculator(ShuntingYard.PrecedenceCalculator calculator)
    {
        this.calculator = calculator;
        return this;
    }

    public ShuntingYardFactory<T> build()
    {
        return () -> new ShuntingYard<>(this.merger, this.wrapper, this.calculator);
    }
}
