package plt.parser.yard;

import plt.lexer.BasicToken;
import plt.parser.Creator;
import plt.parser.ParserUnit;
import plt.provider.TokenProvider;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class ShuntingYardParser<T, R extends BasicToken<T>, O> implements ParserUnit<T, R, O>
{
    private final String name;
    private final Predicate<R> stop;
    private final Predicate<R> operator;
    private final ShuntingYardFactory<O> factory;
    private final List<Predicate<R>> dependentPattern;
    private final List<Predicate<R>> independentPattern;
    private final List<Creator<T, R, O>> independent;
    private final List<BiFunction<O, TokenProvider<T, R>, O>> dependent;

    public ShuntingYardParser(String name, Predicate<R> stop, Predicate<R> operator, ShuntingYardFactory<O> factory, List<Predicate<R>> independentPattern, List<Creator<T, R, O>> independent, List<Predicate<R>> dependentPattern, List<BiFunction<O, TokenProvider<T, R>, O>> dependent)
    {
        this.name = name;
        this.stop = stop;
        this.operator = operator;
        this.factory = factory;
        this.independentPattern = independentPattern;
        this.independent = independent;
        this.dependentPattern = dependentPattern;
        this.dependent = dependent;
    }

    @Override
    public O parse(TokenProvider<T, R> provider)
    {
        ShuntingYard<O> yard = this.factory.create();
        while(provider.has() && !this.stop.test(provider.peek()))
        {
            if(this.operator.test(provider.peek()))
            {
                yard.pushOperator(provider.next().text());
            }
            else if(yard.hasValue())
            {
                O o1 = yard.popValue();
                O o = parseDependent(o1, provider);
                yard.pushValue(o);
            }
            else
            {
                O o = parseIndependent(provider);
                yard.pushValue(o);
            }
        }
        if(!provider.has())
            throw new RuntimeException("Expected token but EOF found!");
        if(!this.stop.test(provider.next()))
            throw new RuntimeException("Unexpected token found!");

        return yard.finish();
    }

    private O parseDependent(O value, TokenProvider<T, R> provider)
    {
        R c = provider.peek();
        for (int i = 0; i < dependent.size(); i++)
        {
            if(this.dependentPattern.get(i).test(c))
            {
                return this.dependent.get(i).apply(value, provider);
            }
        }
        throw new RuntimeException("Non exhaustive pattern! " + c);
    }

    private O parseIndependent(TokenProvider<T, R> provider)
    {
        R c = provider.peek();
        for (int i = 0; i < this.independent.size(); i++)
        {
            if(this.independentPattern.get(i).test(c))
            {
                return this.independent.get(i).create(provider);
            }
        }
        throw new RuntimeException("Non exhaustive pattern! " + c);
    }
}
