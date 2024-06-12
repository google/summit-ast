load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

filegroup(
    name = "testdata",
    srcs = glob(["testdata/*"]),
    visibility = ["//visibility:public"],
)

kt_jvm_library(
    name = "tests",
    srcs = glob(["*.kt"]),
    data = [
        "//src/main/javatests/com/google/summit:testdata",
    ],
    deps = [
        "//src/main/java/com/google/summit:SummitAST",
        "@maven//:junit_junit",
        "@maven//:com_google_truth_truth",
    ],
)

kt_jvm_test(
    name = "SummitASTTest",
    test_class = "com.google.summit.SummitASTTest",
    deps = [":tests"],
)
