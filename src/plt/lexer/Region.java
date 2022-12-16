package plt.lexer;

public record Region(String file, Position start, Position end)
{
    public Region merge(Region region)
    {
        Position first;
        if(this.start.line() < region.start.line()) first = this.start;
        else if(this.start.line() > region.start.line()) first = region.start;
        else first = new Position(this.start.line(), Math.min(this.start.offset(), region.start.offset()));

        Position second;
        if(this.end.line() < region.end.line()) second = region.end;
        else if(this.end.line() > region.end.line()) second = this.end;
        else second = new Position(this.end.line(), Math.max(this.end.offset(), region.end.offset()));

        return new Region(this.file, first, second);
    }
}
