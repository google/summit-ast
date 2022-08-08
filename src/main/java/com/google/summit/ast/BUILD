load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

kt_jvm_library(
    name = "ast",
    srcs = glob([
        "*.kt",
        "*/*.kt",
    ]),
    deps = [
      "@maven//:org_apache_commons_commons_lang3",
    ],
)
