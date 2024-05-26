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

rules_kotlin_version = "v1.7.1"
rules_kotlin_sha = "fd92a98bd8a8f0e1cdcb490b93f5acef1f1727ed992571232d33de42395ca9b3"

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
# Maven publishing
# -------------------------

git_repository(
    name = "vaticle_bazel_distribution",
    remote = "https://github.com/vaticle/bazel-distribution",
    commit = "e61daa787bc77d97e36df944e7223821cab309ea"
)

# Load //common
load("@vaticle_bazel_distribution//common:deps.bzl", "rules_pkg")
rules_pkg()
load("@rules_pkg//:deps.bzl", "rules_pkg_dependencies")
rules_pkg_dependencies()

# Load //maven
load("@vaticle_bazel_distribution//maven:deps.bzl", "maven_artifacts_with_versions")

# Workspace refs
load("@vaticle_bazel_distribution//common:rules.bzl", "workspace_refs")
workspace_refs(
    name = "maven_workspace_refs"
)

# -------------------------
# Third-party libaries
# -------------------------

maven_install(
    artifacts = [
        "org.apache.commons:commons-lang3:3.6",
        "com.google.flogger:flogger:0.7.4",
        "com.google.guava:guava:31.1-jre",
        "com.google.flogger:flogger-system-backend:0.7.4",
        "junit:junit:4.13.2",
        "io.github.apex-dev-tools:apex-parser:4.1.0",
        "com.google.truth:truth:1.1.3",
        "com.google.code.gson:gson:2.9.0",
        "org.jetbrains.kotlin:kotlin-reflect:1.7.0",
        # Unofficial version to reference in Maven dependencies
        "org.danilopianini:gson-extras:1.0.0",
    ] + maven_artifacts_with_versions,
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://maven.google.com",
    ]
)
