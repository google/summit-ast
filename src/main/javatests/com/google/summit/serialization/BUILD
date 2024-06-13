load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "tests",
    srcs = glob([
        "*.kt",
    ]),
    data = [
        "//src/main/javatests/com/google/summit:testdata",
    ],
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
    deps = [
        "//src/main/java/com/google/summit/ast",
        "//src/main/java/com/google/summit/serialization",
        "//src/main/javatests/com/google/summit/testing",
        "@rules_kotlin//kotlin/compiler:kotlin-stdlib",
        "@rules_kotlin//kotlin/compiler:kotlin-test",
        "@maven//:junit_junit",
        "@maven//:com_google_truth_truth",
    ],
)

kt_jvm_test(
    name = "SerializationTest",
    test_class = "com.google.summit.serialization.SerializationTest",
    deps = [":tests"],
)
