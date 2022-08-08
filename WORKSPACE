load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

# -------------------------
# External rules
# -------------------------

RULES_JVM_EXTERNAL_TAG = "4.2"
RULES_JVM_EXTERNAL_SHA = "cd1a77b7b02e8e008439ca76fd34f5b07aecb8c752961f9640dea15e9e5ba1ca"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

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

http_archive(
  name = "apex_parser",
  urls = ["https://github.com/nawforce/apex-parser/archive/v2.13.0.tar.gz"],
  sha256 = "c239a2176067269e926d8944502a817aca7344029fde305c68b3a8e50889d7c0",
  build_file = "BUILD.apex_parser",
  strip_prefix = "apex-parser-2.13.0",
)

maven_install(
    artifacts = [
        "org.apache.commons:commons-lang3:3.6",
        "org.antlr:antlr4:4.10.1",
        "org.antlr:antlr4-runtime:4.10.1",
        "org.antlr:antlr-runtime:3.5.3",
        "org.antlr:ST4:4.3.3",
        "com.google.flogger:flogger:0.7.4",
        "com.google.guava:guava:31.1-jre",
        "com.google.flogger:flogger-system-backend:0.7.4",
        "junit:junit:4.13.2",
        "com.google.truth:truth:1.1.3",
        "com.google.code.gson:gson:2.9.0",
        "org.jetbrains.kotlin:kotlin-reflect:1.7.0",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://maven.google.com",
    ]
)
