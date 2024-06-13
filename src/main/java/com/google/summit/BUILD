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
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
    deps = [
        "//src/main/java/com/google/summit/ast",
        "//src/main/java/com/google/summit/serialization",
        "//src/main/java/com/google/summit/symbols",
        "//src/main/java/com/google/summit/translation",
        "@maven//:io_github_apex_dev_tools_apex_parser",
        "@maven//:com_google_flogger_flogger",
        "@maven//:com_google_guava_guava",
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
        "@io_bazel_rules_kotlin//kotlin/compiler:kotlin-stdlib",
    ],
)
