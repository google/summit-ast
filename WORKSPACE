load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

# -------------------------
# Java toolchain
# -------------------------

RULES_JAVA_VERSION = "7.6.2"
RULES_JAVA_SHA = "eb43f35e3498e6bb8253e2c1759f9a48e56a104e462a58f4163d8f900e7ee8d0"

http_archive(
    name = "rules_java",
    url = "https://github.com/bazelbuild/rules_java/releases/download/%s/rules_java-%s.tar.gz" % (RULES_JAVA_VERSION, RULES_JAVA_VERSION),
    sha256 = RULES_JAVA_SHA,
)

# -------------------------
# External rules
# -------------------------

RULES_JVM_EXTERNAL_TAG = "6.1"
RULES_JVM_EXTERNAL_SHA = "42a6d48eb2c08089961c715a813304f30dc434df48e371ebdd868fc3636f0e82"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

# -------------------------
# Kotlin toolchain
# -------------------------

rules_kotlin_version = "1.9.0"
rules_kotlin_sha = "5766f1e599acf551aa56f49dab9ab9108269b03c557496c54acaf41f98e2b8d6"

http_archive(
    name = "rules_kotlin",
    urls = ["https://github.com/bazelbuild/rules_kotlin/releases/download/v%s/rules_kotlin-v%s.tar.gz" % (rules_kotlin_version, rules_kotlin_version)],
    sha256 = rules_kotlin_sha,
)

load("@rules_kotlin//kotlin:dependencies.bzl", "kt_download_local_dev_dependencies")
kt_download_local_dev_dependencies()

load("@rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")
kotlin_repositories()

load("@rules_kotlin//kotlin:kotlin.bzl", "kt_register_toolchains")
kt_register_toolchains()

# -------------------------
# Maven publishing
# -------------------------

git_repository(
    name = "vaticle_bazel_distribution",
    remote = "https://github.com/vaticle/bazel-distribution",
    commit = "8767cdec452c14274493c576a2955059ff17f2e4",
    repo_mapping = {"@io_bazel_rules_kotlin" : "@rules_kotlin"}
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
        "org.apache.commons:commons-lang3:3.14.0",
        "com.google.flogger:flogger:0.8",
        "com.google.guava:guava:33.2.1-jre",
        "com.google.flogger:flogger-system-backend:0.8",
        "junit:junit:4.13.2",
        "io.github.apex-dev-tools:apex-parser:4.4.0",
        "com.google.truth:truth:1.4.2",
        "com.google.code.gson:gson:2.11.0",
        "org.jetbrains.kotlin:kotlin-reflect:2.0.0",
        # Unofficial version to reference in Maven dependencies
        "org.danilopianini:gson-extras:1.3.0",
    ] + maven_artifacts_with_versions,
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://maven.google.com",
    ]
)
