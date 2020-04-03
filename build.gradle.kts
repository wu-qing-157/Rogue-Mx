plugins {
    id("java")
    id("application")
    id("org.jetbrains.kotlin.jvm").version("1.3.70")
    id("antlr")
}

group = "personal.wuqing"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClassName = "personal.wuqing.rogue.MainKt"
}

// force IDEA to consider it as source root
sourceSets.getByName("main").allJava.srcDir("src/main/antlr")

dependencies {
    antlr(group = "org.antlr", name = "antlr4", version = "4.8")
    implementation(group = "commons-cli", name = "commons-cli", version = "1.4")
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8", version = "1.3.70")
}

tasks.generateGrammarSource {
    arguments = listOf("-no-listener", "-visitor", "-package", "personal.wuqing.rogue.parser")
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
    dependsOn += tasks.generateGrammarSource
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
    dependsOn += tasks.generateTestGrammarSource
}

tasks.startScripts {
    applicationName = "mxc"
}

tasks.register<Exec>("generateBuiltin") {
    dependsOn += tasks.processResources
    workingDir = File("build/resources/main")
    commandLine = listOf(
        "riscv32-unknown-linux-gnu-gcc",
        "-S", "-std=c99", "-fno-section-anchors",
        "builtin.c",
        "-o", "builtin.s"
    )
}
