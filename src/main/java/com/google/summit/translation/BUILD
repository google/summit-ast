load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

kt_jvm_library(
    name = "translation",
    srcs = glob([
        "*.kt",
    ]),
    deps = [
       "//src/main/java/com/google/summit/ast",
       "@apex_parser//:apex_parser",
       "@flogger//jar",
    ],
)
