load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "tests",
    srcs = glob([
        "*.kt",
    ]),
    data = [
        "//src/main/javatests/com/google/summit:testdata",
    ],
    runtime_deps = [
        "@flogger-system-backend//jar",
    ],
    deps = [
        "//src/main/java/com/google/summit/ast",
        "//src/main/java/com/google/summit/serialization",
        "//src/main/javatests/com/google/summit/testing",
        "@com_github_jetbrains_kotlin//:kotlin-test",
        "@junit//jar",
        "@truth//jar",
    ],
)

kt_jvm_test(
    name = "SerializationTest",
    test_class = "com.google.summit.serialization.SerializationTest",
    deps = [":tests"],
)
