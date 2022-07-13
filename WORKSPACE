load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:maven_rules.bzl", "maven_jar")
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

# -------------------------
# Kotlin toolchain
# -------------------------

rules_kotlin_version = "v1.5.0"
rules_kotlin_sha = "12d22a3d9cbcf00f2e2d8f0683ba87d3823cb8c7f6837568dd7e48846e023307"

http_archive(
    name = "io_bazel_rules_kotlin",
    sha256 = rules_kotlin_sha,
    urls = [
        "https://github.com/bazelbuild/rules_kotlin/releases/download/%s/rules_kotlin_release.tgz" % rules_kotlin_version,
    ],
)

load("@io_bazel_rules_kotlin//kotlin:dependencies.bzl", "kt_download_local_dev_dependencies")
kt_download_local_dev_dependencies()

load("@io_bazel_rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")
kotlin_repositories()

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_register_toolchains")
kt_register_toolchains()

# -------------------------
# Third-party libaries
# -------------------------

maven_jar(
    name = "commons-lang3",
    artifact = "org.apache.commons:commons-lang3:3.6",
)

maven_jar(
    name = "antlr4",
    artifact = "org.antlr:antlr4:4.10.1",
)

maven_jar(
    name = "antlr4-runtime",
    artifact = "org.antlr:antlr4-runtime:4.10.1",
)

maven_jar(
    name = "antlr-runtime",
    artifact = "org.antlr:antlr-runtime:3.5.3",
)

maven_jar(
    name = "antlr-st4",
    artifact = "org.antlr:ST4:4.3.3",
)

maven_jar(
    name = "flogger",
    artifact = "com.google.flogger:flogger:0.7.4",
)

maven_jar(
    name = "guava",
    artifact = "com.google.guava:guava:31.1-jre",
)

maven_jar(
    name = "flogger-system-backend",
    artifact = "com.google.flogger:flogger-system-backend:0.7.4",
)

maven_jar(
    name = "junit",
    artifact = "junit:junit:4.13.2",
)

maven_jar(
    name = "truth",
    artifact = "com.google.truth:truth:1.1.3",
)

http_archive(
  name = "apex_parser",
  urls = ["https://github.com/nawforce/apex-parser/archive/v2.13.0.tar.gz"],
  sha256 = "c239a2176067269e926d8944502a817aca7344029fde305c68b3a8e50889d7c0",
  build_file = "BUILD.apex_parser",
  strip_prefix = "apex-parser-2.13.0",
)
