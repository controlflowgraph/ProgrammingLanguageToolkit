package plt.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;

public class RegexLexer<T, R extends BasicToken<T>>
{
    private final TokenFactory<T, R> factory;
    private final List<Category<T>> categories;
    private final Map<T, List<Transformer<T, R>>> transformers;
    private final List<Predicate<R>> filters;
    private final Predicate<T> fail;

    public RegexLexer(TokenFactory<T, R> factory, List<Category<T>> categories, Map<T, List<Transformer<T, R>>> transformers, List<Predicate<R>> filters, Predicate<T> fail)
    {
        this.factory = factory;
        this.categories = categories;
        this.transformers = transformers;
        this.filters = filters;
        this.fail = fail;
    }

    private record Positioning(Region region, Location location)
    {
    }

    public List<R> lex(String text)
    {
        return filter(transform(process(text)));
    }

    private List<R> filter(List<R> tokens)
    {
        List<R> filtered = new ArrayList<>();
        for (R token : tokens)
        {
            boolean removed = this.filters.stream()
                    .map(f -> f.test(token))
                    .reduce(false, (a, b) -> a || b);
            if(!removed)
                filtered.add(token);
        }
        return filtered;
    }

    private List<R> transform(List<R> tokens)
    {
        List<R> transformed = new ArrayList<>();
        for (R token : tokens)
        {
            List<Transformer<T, R>> specific = this.transformers.getOrDefault(token.type(), List.of());
            boolean changed = false;
            for (Transformer<T, R> transformer : specific)
            {
                if(!changed)
                {
                    boolean test = transformer.check().test(token);
                    if(test)
                        token = transformer.transformer().apply(token);
                    changed = test;
                }
            }
            transformed.add(token);
        }
        return transformed;
    }

    private List<R> process(String text)
    {
        List<R> tokens = new ArrayList<>();
        Positioning positioning = getStartPositioning();
        List<Matcher> matchers = getMatchers(text);
        boolean running = true;
        while (running && positioning.location.global < text.length())
        {
            int best = getBestMatcherIndex(positioning.location.global, matchers);
            running = best != -1;
            if(running)
            {
                Matcher matcher = matchers.get(best);
                String t = matcher.group();
                T type = this.categories.get(best).type();
                int start = matcher.start();
                int end = matcher.end();
                Positioning location = getLocation(text, start, end, positioning);
                tokens.add(this.factory.create(t, type, location.region));
                positioning = location;
            }
        }
        return tokens;
    }

    private int getBestMatcherIndex(int start, List<Matcher> matchers)
    {
        int bestStart = Integer.MAX_VALUE;
        int index = -1;
        for(int i = 0; i < matchers.size(); i++)
        {
            Matcher matcher = matchers.get(i);
            if(matcher.find(start) && matcher.start() < bestStart)
            {
                bestStart = matcher.start();
                index = i;
            }
        }
        return index;
    }

    private record Location(int global, Position local)
    {
    }

    private Positioning getLocation(String text, int start, int end, Positioning positioning)
    {
        Location s = getLocation(text, positioning.location.global, start, positioning.location.local);
        Location e = getLocation(text, s.global, end, s.local);
        Region region = new Region(null, s.local, e.local);
        return new Positioning(region, e);
    }

    private Location getLocation(String text, int current, int pos, Position local)
    {
        int line = local.line();
        int offset = local.offset();
        while (current < pos)
        {
            if (text.charAt(current) == '\n')
            {
                offset = 0;
                line++;
            }
            offset++;
            current++;
        }
        return new Location(current, new Position(line, offset));
    }

    private Positioning getStartPositioning()
    {
        return new Positioning(
                null,
                new Location(
                        0,
                        new Position(1, 0)
                )
        );
    }

    private List<Matcher> getMatchers(String text)
    {
        return this.categories.stream()
                .map(Category::pattern)
                .map(p -> p.matcher(text))
                .toList();
    }
}