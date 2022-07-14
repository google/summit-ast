load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "testing",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//src/main/java/com/google/summit/ast",
        "//src/main/java/com/google/summit/translation",
        "@truth//jar",
    ],
)
