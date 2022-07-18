load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

kt_jvm_library(
    name = "serialization",
    srcs = glob([
        "*.kt",
    ]),
    runtime_deps = [
        "@kotlin-reflect//jar",
    ],
    deps = [
        "//src/main/java/com/google/summit/ast",
        "//third_party/gson:extras",
        "@gson//jar",
    ],
)
