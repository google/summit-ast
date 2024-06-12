load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "tests",
    srcs = glob(["*.kt"]),
    deps = [
        "//src/main/java/com/google/summit/ast",
        "//src/main/java/com/google/summit/translation",
        "//src/main/javatests/com/google/summit/testing",
        "@maven//:junit_junit",
        "@maven//:com_google_truth_truth",
        "@rules_kotlin//kotlin/compiler:kotlin-test",
    ],
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
)

kt_jvm_test(
    name = "ClassDeclarationTest",
    test_class = "com.google.summit.translation.ClassDeclarationTest",
    deps = [ ":tests" ],
)

kt_jvm_test(
    name = "CompilationUnitTest",
    test_class = "com.google.summit.translation.CompilationUnitTest",
    deps = [ ":tests" ],
)

kt_jvm_test(
    name = "ExpressionTest",
    test_class = "com.google.summit.translation.ExpressionTest",
    deps = [ ":tests" ],
)

kt_jvm_test(
    name = "InitializerTest",
    test_class = "com.google.summit.translation.InitializerTest",
    deps = [ ":tests" ],
)

kt_jvm_test(
    name = "InterfaceDeclarationTest",
    test_class = "com.google.summit.translation.InterfaceDeclarationTest",
    deps = [ ":tests" ],
)

kt_jvm_test(
    name = "LiteralExpressionTest",
    test_class = "com.google.summit.translation.LiteralExpressionTest",
    deps = [ ":tests" ],
)

kt_jvm_test(
    name = "MethodDeclarationTest",
    test_class = "com.google.summit.translation.MethodDeclarationTest",
    deps = [ ":tests" ],
)

kt_jvm_test(
    name = "ModifierTest",
    test_class = "com.google.summit.translation.ModifierTest",
    deps = [ ":tests" ],
)

kt_jvm_test(
    name = "SoqlAndSoslTest",
    test_class = "com.google.summit.translation.SoqlAndSoslTest",
    deps = [ ":tests" ],
)

kt_jvm_test(
    name = "SourceLocationTest",
    test_class = "com.google.summit.translation.SourceLocationTest",
    deps = [ ":tests" ],
)

kt_jvm_test(
    name = "StatementTest",
    test_class = "com.google.summit.translation.StatementTest",
    deps = [ ":tests" ],
)

kt_jvm_test(
    name = "TypeRefTest",
    test_class = "com.google.summit.translation.TypeRefTest",
    deps = [ ":tests" ],
)
