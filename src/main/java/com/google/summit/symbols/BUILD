load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

filegroup(
    name = "sources",
    srcs = glob([
        "*.kt",
        "*/*.kt",
    ]),
)

kt_jvm_library(
    name = "symbols",
    srcs = [":sources"],
    deps = [
        "//src/main/java/com/google/summit/ast",
        "@maven//:org_apache_commons_commons_lang3",
    ],
)
