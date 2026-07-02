plugins {
    id("com.gradleup.shadow")
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

dependencies {
    implementation(project(":kronos-common"))
    implementation(project(":kronos-economy"))
    implementation(project(":kronos-players"))
    implementation(project(":kronos-timers"))
    implementation(project(":kronos-factions"))
    implementation(project(":kronos-claims"))
    implementation(project(":kronos-koth"))
    implementation(project(":kronos-classes"))
    implementation(project(":kronos-api"))
    implementation(project(":kronos-scoreboard"))
    implementation(project(":kronos-spawn"))
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    implementation("com.google.inject:guice:5.1.0")
    implementation("org.mongodb:mongodb-driver-sync:4.11.1")
    implementation("io.lettuce:lettuce-core:6.3.2.RELEASE")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.guava:guava:32.1.3-jre")
}

bukkit {
    main = "dev.alexissdev.kronos.plugin.HCFPlugin"
    name = "KronosHCF"
    version = "${project.version}"
    description = "Plugin HCF completo para Spigot 1.8.8"
    authors = listOf("alexissdev")
    depend = listOf("Vault")
    softDepend = listOf("PlaceholderAPI", "Apolo")
    website = "https://github.com/alexissdev/kronos"

    commands {
        register("f") {
            description = "Comandos de facción"
            usage = "/f <create|disband|invite|accept|leave|kick|info|top|ally|enemy|deposit|withdraw|map|claim|unclaim|overclaim>"
            aliases = listOf("faction")
        }
        register("koth") {
            description = "Comandos de KOTH"
            usage = "/koth <start|end|list|create|delete>"
            permission = "hcf.koth.admin"
        }
        register("money") {
            description = "Comandos de economía"
            usage = "/money [jugador|pay|top]"
            aliases = listOf("balance", "bal")
        }
        register("hcf") {
            description = "Comandos administrativos HCF"
            usage = "/hcf <reload|give-money|set-money>"
            permission = "hcf.admin"
        }
        register("pvptimer") {
            description = "Gestionar PvP Timer de jugadores"
            usage = "/pvptimer <give|remove> <jugador>"
            permission = "hcf.admin"
        }
        register("spawn") {
            description = "Gestión de la zona de spawn"
            usage = "/spawn <setzone|info|remove>"
            permission = "hcf.spawn.admin"
        }
        register("stats") {
            description = "Ver estadísticas de un jugador"
            usage = "/stats [jugador]"
        }
        register("near") {
            description = "Ver jugadores cercanos"
            usage = "/near [radio]"
        }
        register("fix") {
            description = "Reparar inventario"
            usage = "/fix"
            permission = "hcf.admin"
        }
        register("baltop") {
            description = "Ver el ranking de balance"
            usage = "/baltop"
        }
        register("stuck") {
            description = "Teletransportarse al spawn si estás atrapado"
            usage = "/stuck"
        }
        register("kit") {
            description = "Recibir un kit según tu clase"
            usage = "/kit"
        }
    }

    permissions {
        register("hcf.admin") {
            description = "Acceso completo a comandos administrativos de HCF"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
        }
        register("hcf.koth.admin") {
            description = "Administrar eventos KOTH"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
        }
        register("hcf.bypass.pvptimer") {
            description = "Bypass del PvP Timer"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
        }
        register("hcf.bypass.combattag") {
            description = "Bypass del Combat Tag"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
        }
        register("hcf.bypass.claimprotection") {
            description = "Construir en cualquier claim"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
        }
        register("hcf.spawn.admin") {
            description = "Gestionar zona de spawn"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
        }
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    mergeServiceFiles()

    relocate("com.google.inject", "dev.alexissdev.kronos.libs.inject")
    relocate("com.mongodb", "dev.alexissdev.kronos.libs.mongodb")
    relocate("io.lettuce", "dev.alexissdev.kronos.libs.lettuce")
    relocate("com.google.gson", "dev.alexissdev.kronos.libs.gson")
    relocate("com.google.common", "dev.alexissdev.kronos.libs.guava")
    relocate("com.google.thirdparty", "dev.alexissdev.kronos.libs.guava.thirdparty")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
