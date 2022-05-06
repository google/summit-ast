load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

kt_jvm_library(
    name = "ast",
    srcs = glob([
        "*.kt",
        "*/*.kt",
    ]),
    deps = [
      "//external:commons-lang3-jar"
    ],
)
