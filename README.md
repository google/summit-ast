# Summit-AST

The Summit-AST library defines an abstract syntax tree (AST) data structure
to represent Apex source code, and it provides the translation of a parse tree
into its AST representation. 

This is not an official Google product.

## Dependencies

This is built on top of the [apex-parser](https://github.com/nawforce/apex-parser)
Apex parser, which is a compiled [ANTLR4](https://github.com/antlr/antlr4) grammar.

All dependencies are downloaded and managed through the build system.

## Build

This software is built using [bazel](https://bazel.build/).

```
$ bazel build ...
```

It is tested and working with version 5.2.

## Running

The primary output is an in-memory AST data structure. The library
is intended to be integrated into other development tools.

There is a small `SummitTool` demonstration executable, which parses
Apex source files, builds the AST, and prints basic information. It
can be executed by runnning:

```
$ bazel run src/main/java/com/google/summit/SummitTool [files | directories ...]
```

Any directories will be recursively walked. The tool attempts to compile
any files with the extension `.cls` or '.trigger'.
