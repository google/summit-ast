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
    name = "ast",
    srcs = [":sources"],
    deps = [
      "@maven//:org_apache_commons_commons_lang3",
    ],
)
