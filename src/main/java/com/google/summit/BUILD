load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

kt_jvm_library(
    name = "SummitAST",
    srcs = [
        "SummitAST.kt",
    ],
    deps = [
        "//src/main/java/com/google/summit/ast",
        "//src/main/java/com/google/summit/translation",
        "@apex_parser",
        "@flogger//jar",
        "@guava//jar",
    ],
    runtime_deps = [
        "@flogger-system-backend//jar",
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
