load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

filegroup(
    name = "lib_sources",
    srcs = [
        "SummitAST.kt",
    ],
)

kt_jvm_library(
    name = "SummitAST",
    srcs = [":lib_sources"],
    deps = [
        "//src/main/java/com/google/summit/ast",
        "//src/main/java/com/google/summit/translation",
        "@maven//:com_github_nawforce_apex_parser",
        "@maven//:com_google_flogger_flogger",
        "@maven//:com_google_guava_guava",
    ],
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
)

kt_jvm_binary(
    name = "SummitTool",
    srcs = [
        "SummitTool.kt",
    ],
    main_class = "com.google.summit.SummitTool",
    deps = [
        ":SummitAST",
    ],
)
