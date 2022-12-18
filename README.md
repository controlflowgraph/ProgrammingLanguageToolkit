# Programming Language Toolkit (PLT)
The PLT is a first attempt to allow quick prototyping of programming languages.
The main focus of the library is on the lexer and parser for programming languages.
It is designed to be used for imperative languages and simple syntax.
This allows a fast mockup of a language in order to deal with the interesting parts like type checking or optimization.

## Getting started
In the `src/SimpleExample.java` file is a simple example on how to lex and parse a super simple (currently incomplete) language.

## Future goals
- add support more complex grammars
- add parsing of expressions (e.g. using a shunting yard algorithm)
- add more examples for simple languages (e.g. with significant white space)
- add better support for regions by being able to look at the last token as well