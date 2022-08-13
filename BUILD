# SummitTool is a binary to simply parse and translate Apex source files.
#
# Pass it a list of file paths or directories to walk. It will read any files
# with the .cls extension.
#
# $ bazel run :SummitTool -- <list of files or directory paths>
alias(
    name = "SummitTool",
    actual = "//src/main/java/com/google/summit:SummitTool",
)

#
# Assemble JARs, POM for publishing to Maven central
#

load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library")
load("@vaticle_bazel_distribution//maven:rules.bzl", "assemble_maven")

kt_jvm_library(
    name = "maven_lib",
    srcs = [
        "//src/main/java/com/google/summit:lib_sources",
        "//src/main/java/com/google/summit/ast:sources",
        "//src/main/java/com/google/summit/translation:sources",
        "//src/main/java/com/google/summit/serialization:sources",
    ],
    tags = ["maven_coordinates=com.google.summit:summit-ast:{pom_version}", "manual"],
    deps = [
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_flogger_flogger_system_backend",
        "@maven//:com_github_nawforce_apex_parser",
        "@maven//:com_google_flogger_flogger",
        "@maven//:org_danilopianini_gson_extras",
        "@maven//:org_apache_commons_commons_lang3",
        "@maven//:com_google_code_gson_gson",
    ],
)

assemble_maven(
    name = "maven_assemble",
    target = ":maven_lib",
    project_name = "Summit AST",
    project_description = "Summit - Apex Language Abstract Syntax Tree",
    project_url = "https://github.com/google/summit-ast",
    scm_url = "https://github.com/google/summit-ast.git",
    version_file = "//:VERSION",
    license = "apache",
    workspace_refs = "@maven_workspace_refs//:refs.json",
)
