import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask

plugins {
    id("idea")
    id("java")

    id("org.jetbrains.intellij.platform") // IntelliJ Platform Gradle Plugin
    id("org.jetbrains.grammarkit") // Gradle Grammar-Kit Plugin
    id("org.jetbrains.changelog") // Gradle Changelog Plugin
    id("org.jetbrains.qodana") // Gradle Qodana Plugin
    id("org.jetbrains.kotlinx.kover") // Gradle Kover Plugin
}

// Project repositories take precedence over settings repositories when the IntelliJ Platform plugin
// registers its own. Keep the recommended set here so installer and Marketplace artifacts resolve.
repositories {
    mavenCentral()

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

dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("io.sentry:sentry:8.38.0")
    implementation("org.apache.commons:commons-text:1.15.0")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        // 2025.3+ publishes a unified IntelliJ IDEA distribution. Ultimate is required for the PHP plugin.
        intellijIdeaUltimate("2026.3") {
            useInstaller = true
        }

        bundledPlugins(
            "com.intellij.xml",
            "com.intellij.css",
            "org.jetbrains.plugins.yaml",
            "com.intellij.modules.json",
            "com.intellij.java",
            "com.intellij.java-i18n",
            "com.intellij.properties",
        )
        bundledModules("intellij.platform.langInjection")

        plugins(
            "com.jetbrains.hackathon.indices.viewer:1.32",
            "PsiViewer:2026.3-EAP.6800.24",
            "com.jetbrains.php:263.7307.200",
        )

        testFramework(TestFrameworkType.Platform)

        // Add modules only accessible via IntelliJ Ultimate subscription - like the php module
        testBundledModule("com.intellij.modules.ultimate")
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
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

        ideaVersion {
            untilBuild = "263.*"
        }
    }
}

grammarKit {
    jflexRelease = "1.9.0"
    grammarKitRelease = "2022.3.2"
}

val generateEelLexer = tasks.register<GenerateLexerTask>("GenerateEelLexer") {
    sourceFile = file("src/main/grammars/EelLexer.flex")
    targetOutputDir = file("src/gen/de/vette/idea/neos/lang/eel/parser")
    purgeOldFiles = true
}

val generateAfxLexer = tasks.register<GenerateLexerTask>("GenerateAfxLexer") {
    sourceFile = file("src/main/grammars/AfxLexer.flex")
    targetOutputDir = file("src/gen/de/vette/idea/neos/lang/afx/parser")
    purgeOldFiles = true
}

val generateFusionLexer = tasks.register<GenerateLexerTask>("GenerateFusionLexer") {
    sourceFile = file("src/main/grammars/FusionLexer.flex")
    targetOutputDir = file("src/gen/de/vette/idea/neos/lang/fusion/parser")
    purgeOldFiles = true
}

val generateEelParser = tasks.register<GenerateParserTask>("GenerateEelParser") {
    // Lexer generation purges this package directory, so the parser must be written afterwards.
    dependsOn(generateEelLexer)
    sourceFile = file("src/main/grammars/EelParser.bnf")
    targetRootOutputDir = file("src/gen")
    pathToParser = "/de/vette/idea/neos/lang/eel/parser/EelParser.java"
    pathToPsiRoot = "/de/vette/idea/neos/lang/core/psi"
    purgeOldFiles = true
}

val generateFusionParser = tasks.register<GenerateParserTask>("GenerateFusionParser") {
    // Lexer generation purges this package directory, so the parser must be written afterwards.
    dependsOn(generateFusionLexer)
    sourceFile = file("src/main/grammars/FusionParser.bnf")
    targetRootOutputDir = file("src/gen")
    pathToParser = "/de/vette/idea/neos/lang/fusion/parser/FusionParser.java"
    pathToPsiRoot = "/de/vette/idea/neos/lang/core/psi"
    purgeOldFiles = true
}

tasks {
    withType<JavaCompile>().configureEach {
        dependsOn(generateEelLexer, generateAfxLexer, generateFusionLexer, generateEelParser, generateFusionParser)
    }

    initializeIntellijPlatformPlugin {
        dependsOn(generateEelLexer)
        dependsOn(generateAfxLexer)
        dependsOn(generateFusionLexer)
        dependsOn(generateEelParser)
        dependsOn(generateFusionParser)
    }
}
