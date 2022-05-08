load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")

package(default_visibility = ["//visibility:public"])

kt_jvm_binary(
    name = "SummitTool",
    main_class = "com.google.summit.SummitTool",
    srcs = [
        "SummitTool.kt",
    ],
    deps = [
       "//src/main/java/com/google/summit/ast",
       "//src/main/java/com/google/summit/translation",
       "@apex_parser//:apex_parser",
       "@flogger//jar",
    ],
    runtime_deps = [
       "@flogger-system-backend//jar",
    ],
)
