# Summit-AST

The Summit-AST library defines an abstract syntax tree (AST) data structure to
represent Salesforce
[Apex](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_intro_what_is_apex.htm)
source code, and it provides the translation of a parse tree into its AST
representation.

This is not an official Google product.

## Dependencies

This is built on top of the
[apex-parser](https://github.com/apex-dev-tools/apex-parser) Apex parser, which is a
compiled [ANTLR4](https://github.com/antlr/antlr4) grammar.

All dependencies are downloaded and managed through the build system.

## Build

This software is built using [bazel](https://bazel.build/).

```
$ bazel build ...
$ bazel test ...
```

It is tested and working with the version listed in the `.bazelversion` file.

If you use [Bazelisk to use
Bazel](https://bazel.build/install/bazelisk), it will download the
correct version.

## Running

The primary output is an in-memory AST data structure. The library is intended
to be integrated into other development tools.

There is a small `SummitTool` demonstration executable, which parses Apex source
files, builds the AST, and prints basic information. It can be executed by
running:

```
$ bazel run :SummitTool -- [-json] [files | directories ...]
```

Any directories will be recursively walked. The tool attempts to compile any
files with the extension `.cls` or `.trigger`.

If the optional argument `-json` is given, then the AST is serialized additionally
as json into a file. The file name will be the original Apex source file with
the extension `.json` added.
