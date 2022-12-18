package plt.parser.yard;

import java.util.*;
import java.util.function.Predicate;

public class ShuntingYard<T>
{

    private enum Kind
    {
        SEGMENT,
        BINARY,
        UNARY,
    }

    public record Operator(int precedence, boolean binary, String symbol)
    {

    }

    private final Merger<T> merger;
    private final Wrapper<T> wrapper;
    private final PrecedenceCalculator factory;

    private final Deque<Kind> kinds = new ArrayDeque<>();
    private final Deque<Operator> operators = new ArrayDeque<>();
    private final Deque<T> segments = new ArrayDeque<>();

    public ShuntingYard(Merger<T> merger, Wrapper<T> wrapper, PrecedenceCalculator factory)
    {
        this.merger = merger;
        this.wrapper = wrapper;
        this.factory = factory;
    }

    public T finish()
    {
        combine(o -> true);
        if(this.segments.size() != 1)
            throw new RuntimeException("Unable to resolve expression '" + this.segments + "' remaining!");
        return this.segments.pop();
    }

    public interface Merger<T>
    {
        T combine(String op, T left, T right);
    }

    public interface Wrapper<T>
    {
        T wrap(String op, T source);
    }

    public interface PrecedenceCalculator
    {
        Operator create(String text, boolean binary);
    }

    public void pushOperator(String text)
    {
        if(this.kinds.isEmpty() || this.kinds.peek() != ShuntingYard.Kind.SEGMENT)
        {
            ShuntingYard.Operator operator = this.factory.create(text, false);
            this.operators.push(operator);
            this.kinds.push(ShuntingYard.Kind.UNARY);
        }
        else
        {
            ShuntingYard.Operator operator = this.factory.create(text, true);
            combine(o -> o.precedence() <= operator.precedence());
            this.operators.push(operator);
            this.kinds.push(ShuntingYard.Kind.BINARY);
        }
    }

    public void pushValue(T value)
    {
        this.segments.push(value);
        this.kinds.push(Kind.SEGMENT);
    }

    public T popValue()
    {
        if(this.kinds.isEmpty())
            throw new RuntimeException("Unable to pop non existing element!");
        if(this.kinds.peek() != Kind.SEGMENT)
            throw new RuntimeException("Unexpected type on stack " + this.kinds.peek() + "!");
        this.kinds.pop();
        return this.segments.pop();
    }

    public boolean hasValue()
    {
        return !this.kinds.isEmpty() && this.kinds.peek() == Kind.SEGMENT;
    }

    private void combine(Predicate<Operator> check)
    {
        while (!this.operators.isEmpty() && check.test(operators.peek()))
        {
            Operator required = this.operators.pop();
            this.kinds.pop();
            this.kinds.pop();
            if(required.binary())
            {
                T right = segments.pop();
                T left = segments.pop();
                T operation = this.merger.combine(
                        required.symbol(),
                        left,
                        right
                );
                this.segments.push(operation);
            }
            else
            {
                T value = this.segments.pop();
                T operation = this.wrapper.wrap(
                        required.symbol(),
                        value
                );
                this.segments.push(operation);
                this.kinds.push(Kind.SEGMENT);
            }
        }
    }
}