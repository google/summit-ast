load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

kt_jvm_library(
    name = "translation",
    srcs = glob([
        "*.kt",
    ]),
    deps = [
        "//src/main/java/com/google/summit/ast",
        "@maven//:com_github_nawforce_apex_parser",
        "@maven//:com_google_flogger_flogger",
    ],
)
