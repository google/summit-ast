load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

filegroup(
    name = "sources",
    srcs = glob([
        "*.kt",
        "*/*.kt",
    ]),
)

kt_jvm_library(
    name = "translation",
    srcs = [":sources"],
    deps = [
        "//src/main/java/com/google/summit/ast",
        "@maven//:io_github_apex_dev_tools_apex_parser",
        "@maven//:com_google_flogger_flogger",
    ],
)
