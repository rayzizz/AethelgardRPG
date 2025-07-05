plugins {
    kotlin("jvm") version "2.2.0-RC3"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
    `maven-publish`
}

group = "me.ray"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven(url = "https://mvn.lumine.io/repository/maven-public/")
    maven (url = "https://repo.dmulloy2.net/repository/public/" )

}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.6")
    paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")


    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation("com.google.code.gson:gson:2.10.1")
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21")
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}
tasks.assemble {
    paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

tasks.build {
    dependsOn("shadowJar")
}
tasks.jar {
  manifest {
    attributes["paperweight-mappings-namespace"] = "mojang"
  }
}

// Task para copiar o JAR gerado pela shadowJar para a pasta de plugins
val copyShadowJarToPlugins by tasks.registering(Copy::class) {
    dependsOn(tasks.shadowJar) // Garante que shadowJar seja executada primeiro

    from(tasks.shadowJar.get().archiveFile) // O JAR gerado pela shadowJar
    into("C:/Users/r/Documents/RPG SERVER/plugins") // Diretório de destino

    // Opcional: renomear o arquivo no destino
    // rename { fileName ->
    //    // Exemplo: "AethelgardRPG.jar"
    //    "${project.name}.jar"
    // }
}

// if you have shadowJar configured
tasks.shadowJar {
  manifest {
    attributes["paperweight-mappings-namespace"] = "mojang"
  }
  // Adicionar relocations para evitar conflitos de dependência
  relocate("com.zaxxer.hikari", "me.ray.aethelgardRPG.lib.hikaricp")
  relocate("com.google.gson", "me.ray.aethelgardRPG.lib.gson")
  // Se você adicionar outras bibliotecas que precisam ser empacotadas, adicione relocations para elas aqui também.
  finalizedBy(copyShadowJarToPlugins) // Executa a cópia após a shadowJar
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}
