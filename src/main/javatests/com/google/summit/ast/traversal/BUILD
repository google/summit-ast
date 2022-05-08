load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")

kt_jvm_library(
    name = "tests",
    srcs = glob(["*.kt"]),
    deps = [
        "//src/main/java/com/google/summit/ast",
        "@junit//jar",
        "@truth//jar",
    ],
)

kt_jvm_test(
    name = "DfsWalkerTest",
    test_class = "com.google.summit.ast.traversal.DfsWalkerTest",
    deps = [ ":tests" ],
)

kt_jvm_test(
    name = "VisitorTest",
    test_class = "com.google.summit.ast.traversal.VisitorTest",
    deps = [ ":tests" ],
)
