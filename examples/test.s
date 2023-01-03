fn fac(v)
{
    if(v == 0)
    {
        ret 1;
    }
    ret v * fac(v - 1);
}

fn LstAdd(l, e)
{
    if(l.size == double(l.elements.length))
    {
        var a = Array(l.size * 2.0);
        for(var i = 0.0; i < l.size; i = i + 1.0)
        {
            a[i] = l.elements[i];
        }
        l.elements = a;
    }
    l.elements[l.size] = e;
    l.size = l.size + 1.0;
}

var l = [1, 2, 3, 4];
LstAdd(l, 1234);
print(l.elements.elements);

print((1, 2, 3)._0);

var t = [1, 2, 3];
t.add(69);
t.add(420);
t[0] = 123;
print(t);
print(fac(10));
print([1, 2, 3][0]);
var a = true;
print(a);

for(var i = 0; i < 10; i = i + 1)
{
    print(i);
}