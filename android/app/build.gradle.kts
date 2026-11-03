import com.android.build.api.artifact.SingleArtifact
import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.application)
}

// The version and the repo URL live in package.json only (see CLAUDE.md).
val rootPackage = JsonSlurper().parse(file("../../package.json")) as Map<*, *>
val appVersion = rootPackage["version"] as String
val repoUrl = (rootPackage["repository"] as Map<*, *>)["url"] as String

val (repoOwner, repoName) = Regex("""^https://github\.com/([^/]+)/([^/]+?)(?:\.git)?/?$""")
    .matchEntire(repoUrl)?.destructured
    ?: error("package.json repository.url is not a github.com repository URL: $repoUrl")
val defaultRulesBaseUrl = "https://raw.githubusercontent.com/$repoOwner/$repoName/main/rules"

val (versionMajor, versionMinor, versionPatch) = appVersion.split(".").map { it.toInt() }

// Copies the top-level JSON files of the repo's rules folder (not schema/) into the assets under rules/.
abstract class CopyRulesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val ruleFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val target = outputDir.get().asFile.resolve("rules")
        target.deleteRecursively()
        target.mkdirs()
        ruleFiles.files.forEach { it.copyTo(target.resolve(it.name)) }
    }
}

// Fails when a merged manifest asks for the INTERNET permission.
abstract class VerifyNoInternetTask : DefaultTask() {
    @get:InputFile
    abstract val manifest: RegularFileProperty

    @TaskAction
    fun verify() {
        check(!manifest.get().asFile.readText().contains("android.permission.INTERNET")) {
            "The offline flavor must not request the INTERNET permission"
        }
    }
}

android {
    namespace = "io.github.lucafregona.unscroll"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.lucafregona.unscroll"
        minSdk = 26
        targetSdk = 36
        versionCode = versionMajor * 10_000 + versionMinor * 100 + versionPatch
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("offline") {
            dimension = "distribution"
            buildConfigField("boolean", "ONLINE", "false")
            buildConfigField("String", "DEFAULT_RULES_BASE_URL", "\"\"")
        }
        create("online") {
            dimension = "distribution"
            buildConfigField("boolean", "ONLINE", "true")
            buildConfigField("String", "DEFAULT_RULES_BASE_URL", "\"$defaultRulesBaseUrl\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        abortOnError = true
    }
}

fun String.capitalized() = replaceFirstChar { it.uppercase() }

androidComponents {
    onVariants { variant ->
        val copyRules = tasks.register<CopyRulesTask>("copy${variant.name.capitalized()}Rules") {
            ruleFiles.from(fileTree("../../rules") { include("*.json") })
            outputDir.set(layout.buildDirectory.dir("generated/rules/${variant.name}"))
        }
        variant.sources.assets!!.addGeneratedSourceDirectory(copyRules, CopyRulesTask::outputDir)
    }

    onVariants(selector().withFlavor("distribution" to "offline")) { variant ->
        val verify = tasks.register<VerifyNoInternetTask>("verify${variant.name.capitalized()}Manifest") {
            manifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
        }
        tasks.named("check") { dependsOn(verify) }
    }
}

dependencies {
    testImplementation(libs.junit)
}
