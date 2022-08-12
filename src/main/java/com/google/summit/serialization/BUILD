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
    name = "serialization",
    srcs = [":sources"],
    runtime_deps = [
        "@maven//:org_jetbrains_kotlin_kotlin_reflect",
    ],
    deps = [
        "//src/main/java/com/google/summit/ast",
        "//third_party/gson:extras",
        "@maven//:com_google_code_gson_gson",
    ],
)
