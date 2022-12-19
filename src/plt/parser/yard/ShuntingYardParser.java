package plt.parser.yard;

import plt.lexer.BasicToken;
import plt.parser.Creator;
import plt.parser.ParserUnit;
import plt.provider.TokenProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class ShuntingYardParser<T, R extends BasicToken<T>, O> implements ParserUnit<T, R, O>
{
    private final String name;
    private final Predicate<R> operator;
    private final ShuntingYardFactory<O> factory;
    private final List<Predicate<R>> dependentPattern;
    private final List<Predicate<R>> independentPattern;
    private final List<Creator<T, R, O>> independent;
    private final List<BiFunction<O, TokenProvider<T, R>, O>> dependent;

    public ShuntingYardParser(String name, Predicate<R> operator, ShuntingYardFactory<O> factory, List<Predicate<R>> independentPattern, List<Creator<T, R, O>> independent, List<Predicate<R>> dependentPattern, List<BiFunction<O, TokenProvider<T, R>, O>> dependent)
    {
        this.name = name;
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
        boolean running = true;
        while(provider.has() && running)
        {
            if(this.operator.test(provider.peek()))
            {
                yard.pushOperator(provider.next().text());
            }
            else if(yard.hasValue())
            {
                O o1 = yard.popValue();
                Optional<O> o = parseDependent(o1, provider);
                if(o.isPresent())
                    yard.pushValue(o.get());
                else {
                    running = false;
                    yard.pushValue(o1);
                }
            }
            else
            {
                Optional<O> o = parseIndependent(provider);
                if(o.isPresent())
                    yard.pushValue(o.get());
                else running = false;
            }
        }
        return yard.finish();
    }

    private Optional<O> parseDependent(O value, TokenProvider<T, R> provider)
    {
        R c = provider.peek();
        for (int i = 0; i < dependent.size(); i++)
        {
            if(this.dependentPattern.get(i).test(c))
            {
                O apply = this.dependent.get(i).apply(value, provider);
                return Optional.of(apply);
            }
        }
        return Optional.empty();
    }

    private Optional<O> parseIndependent(TokenProvider<T, R> provider)
    {
        R c = provider.peek();
        for (int i = 0; i < this.independent.size(); i++)
        {
            if(this.independentPattern.get(i).test(c))
            {
                O o = this.independent.get(i).create(provider);
                return Optional.of(o);
            }
        }
        return Optional.empty();
    }
}
