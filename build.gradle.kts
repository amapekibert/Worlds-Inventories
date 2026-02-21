plugins {
    id("java")
}

group = "org.titago.worldsinventories"
version = "1.0.0"

tasks.compileJava {
    options.release.set(21)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://repo.thenextlvl.net/releases") {
        name = "thenextlvl-releases"
    }
    maven("https://repo.thenextlvl.net/snapshots") {
        name = "thenextlvl-snapshots"
    }
}

dependencies {
    compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("net.thenextlvl:worlds:3.12.3")
    compileOnly("net.thenextlvl:per-worlds:1.3.5")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
