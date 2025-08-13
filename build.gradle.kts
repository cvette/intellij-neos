import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask

plugins {
    id("idea")
    id("java")
    id("java-library")

    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.grammarKit) // Gradle Grammar-Kit Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

idea {
    module {
        generatedSourceDirs.add(file("src/gen"))
    }
}

sourceSets {
    main {
        java.srcDirs("src/gen")
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    testImplementation(libs.junit)
    implementation("io.sentry:sentry:8.19.1")
    implementation("org.apache.commons:commons-text:1.14.0")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

grammarKit {
    jflexRelease = providers.gradleProperty("jFlexVersion")
    grammarKitRelease = providers.gradleProperty("grammarKitVersion")
}

val generateEelLexer = task<GenerateLexerTask>("GenerateEelLexer") {
    sourceFile = file("src/main/grammars/EelLexer.flex")
    targetOutputDir = file("src/gen/de/vette/idea/neos/lang/eel/parser")
    purgeOldFiles = true
}

val generateAfxLexer = task<GenerateLexerTask>("GenerateAfxLexer") {
    sourceFile = file("src/main/grammars/AfxLexer.flex")
    targetOutputDir = file("src/gen/de/vette/idea/neos/lang/afx/parser")
    purgeOldFiles = true
}

val generateFusionLexer = task<GenerateLexerTask>("GenerateFusionLexer") {
    sourceFile = file("src/main/grammars/FusionLexer.flex")
    targetOutputDir = file("src/gen/de/vette/idea/neos/lang/fusion/parser")
    purgeOldFiles = true
}

val generateEelParser = task<GenerateParserTask>("GenerateEelParser") {
    sourceFile = file("src/main/grammars/EelParser.bnf")
    targetRootOutputDir = file("src/gen")
    pathToParser = "/de/vette/idea/neos/lang/eel/parser/EelParser.java"
    pathToPsiRoot = "/de/vette/idea/neos/lang/core/psi"
    purgeOldFiles = true
}

val generateFusionParser = task<GenerateParserTask>("GenerateFusionParser") {
    sourceFile = file("src/main/grammars/FusionParser.bnf")
    targetRootOutputDir = file("src/gen")
    pathToParser = "/de/vette/idea/neos/lang/fusion/parser/FusionParser.java"
    pathToPsiRoot = "/de/vette/idea/neos/lang/core/psi"
    purgeOldFiles = true
}

tasks {
    // Set the JVM compatibility versions
    providers.gradleProperty("javaVersion").let {
        withType<JavaCompile> {
            sourceCompatibility = it.get()
            targetCompatibility = it.get()
            dependsOn(generateEelLexer, generateAfxLexer, generateFusionLexer, generateEelParser, generateFusionParser)
        }
    }

    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }

    initializeIntellijPlatformPlugin {
        dependsOn(generateEelLexer)
        dependsOn(generateAfxLexer)
        dependsOn(generateFusionLexer)
        dependsOn(generateEelParser)
        dependsOn(generateFusionParser)
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}