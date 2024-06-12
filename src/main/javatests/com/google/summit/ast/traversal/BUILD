load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "tests",
    srcs = glob(["*.kt"]),
    deps = [
        "//src/main/java/com/google/summit/ast",
        "@maven//:junit_junit",
        "@maven//:com_google_truth_truth",
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
