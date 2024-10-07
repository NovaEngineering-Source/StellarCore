import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.RunConfigurationContainer
import java.util.*

plugins {
    id("java-library")
    id("maven-publish")
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.7"
    id("eclipse")
    id("com.gtnewhorizons.retrofuturagradle") version "1.3.19"
}

// Project properties
group = "github.kasuminova.stellarcore"
version = "1.5.3"

// Set the toolchain version to decouple the Java we run Gradle with from the Java used to compile and run the mod
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        // Azul covers the most platforms for Java 8 toolchains, crucially including MacOS arm64
        vendor.set(JvmVendorSpec.AZUL)
    }
    // Generate sources and javadocs jars when building and publishing
    withSourcesJar()
    withJavadocJar()
}

// Most RFG configuration lives here, see the JavaDoc for com.gtnewhorizons.retrofuturagradle.MinecraftExtension
minecraft {
    mcVersion.set("1.12.2")

    // Username for client run configurations
    username.set("Kasumi_Nova")

    // Generate a field named VERSION with the mod version in the injected Tags class
    injectedTags.put("VERSION", project.version)

    // If you need the old replaceIn mechanism, prefer the injectTags task because it doesn't inject a javac plugin.
    // tagReplacementFiles.add("RfgExampleMod.java")

    // Enable assertions in the mod's package when running the client or server
    val args = mutableListOf("-ea:${project.group}")

    // Mixin args
    args.add("-Dmixin.hotSwap=true")
    args.add("-Dmixin.checks.interfaces=true")
    args.add("-Dmixin.debug.export=true")
    extraRunJvmArguments.addAll(args)

    // If needed, add extra tweaker classes like for mixins.
    // extraTweakClasses.add("org.spongepowered.asm.launch.MixinTweaker")

    // Exclude some Maven dependency groups from being automatically included in the reobfuscated runs
    groupsToExcludeFromAutoReobfMapping.addAll("com.diffplug", "com.diffplug.durian", "net.industrial-craft")
}

// Generates a class named rfg.examplemod.Tags with the mod version in it, you can find it at
tasks.injectTags.configure {
    outputClassName.set("${project.group}.Tags")
}

// Put the version from gradle into mcmod.info
tasks.processResources.configure {
//    inputs.property("version", project.version)
//
//    filesMatching("mcmod.info") {
//        expand(mapOf("version" to project.version))
//    }
}

tasks.compileJava.configure {
    sourceCompatibility = "17"
    options.release = 8
    options.encoding = "UTF-8" // Use the UTF-8 charset for Java compilation

    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.compileTestJava.configure {
    sourceCompatibility = "17"
    options.release = 8
    options.encoding = "UTF-8" // Use the UTF-8 charset for Java compilation

    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.javadoc.configure {
    // No need for JavaDoc.
    actions = Collections.emptyList()
}


tasks.jar.configure {
    manifest {
        val attributes = manifest.attributes
        attributes["FMLCorePlugin"] = "github.kasuminova.stellarcore.mixin.StellarCoreEarlyMixinLoader"
        attributes["FMLCorePluginContainsFMLMod"] = true
//        attributes["FMLAT"] = "stellar_core_at.cfg"
    }
}

//tasks.deobfuscateMergedJarToSrg.configure {
//    accessTransformerFiles.from("src/main/resources/META-INF/stellar_core_at.cfg")
//}
//tasks.srgifyBinpatchedJar.configure {
//    accessTransformerFiles.from("src/main/resources/META-INF/stellar_core_at.cfg")
//}

// Create a new dependency type for runtime-only dependencies that don't get included in the maven publication
val runtimeOnlyNonPublishable: Configuration by configurations.creating {
    description = "Runtime only dependencies that are not published alongside the jar"
    isCanBeConsumed = false
    isCanBeResolved = false
}
listOf(configurations.runtimeClasspath, configurations.testRuntimeClasspath).forEach {
    it.configure {
        extendsFrom(
                runtimeOnlyNonPublishable
        )
    }
}

// Dependencies
repositories {
    flatDir {
        dirs("lib")
    }
    maven {
        url = uri("https://maven.aliyun.com/nexus/content/groups/public/")
    }
    maven {
        url = uri("https://maven.aliyun.com/nexus/content/repositories/jcenter")
    }
    maven {
        url = uri("https://maven.cleanroommc.com")
    }
    maven {
        url = uri("https://cfa2.cursemaven.com")
    }
    maven {
        url = uri("https://cursemaven.com")
    }
    maven {
        url = uri("https://maven.blamejared.com/")
    }
    maven {
        url = uri("https://maven.tterrag.com") // AutoSave, AutoConfig
    }
    maven {
        url = uri("https://repo.spongepowered.org/maven")
    }
    maven {
        name = "GeckoLib"
        url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    }
    maven {
        name = "OvermindDL1 Maven"
        url = uri("https://gregtech.overminddl1.com/")
        mavenContent {
            excludeGroup("net.minecraftforge") // missing the `universal` artefact
        }
    }
    maven {
        name = "GTNH Maven"
        url = uri("http://jenkins.usrv.eu:8081/nexus/content/groups/public/")
        isAllowInsecureProtocol = true
    }
}

dependencies {
    annotationProcessor("com.github.bsideup.jabel:jabel-javac-plugin:0.4.2")
    compileOnly("com.github.bsideup.jabel:jabel-javac-plugin:0.4.2")
    // workaround for https://github.com/bsideup/jabel/issues/174
    annotationProcessor("net.java.dev.jna:jna-platform:5.13.0")
    // Allow jdk.unsupported classes like sun.misc.Unsafe, workaround for JDK-8206937 and fixes Forge crashes in tests.
    patchedMinecraft("me.eigenraven.java8unsupported:java-8-unsupported-shim:1.0.0")
    // allow Jabel to work in tests
    testAnnotationProcessor("com.github.bsideup.jabel:jabel-javac-plugin:1.0.0")
    testCompileOnly("com.github.bsideup.jabel:jabel-javac-plugin:1.0.0") {
        isTransitive = false // We only care about the 1 annotation class
    }
    testCompileOnly("me.eigenraven.java8unsupported:java-8-unsupported-shim:1.0.0")

    // Mixins
//    implementation("zone.rong:mixinbooter:7.1")
    val mixin : String = modUtils.enableMixins("zone.rong:mixinbooter:9.3", "mixins.stellar_core.refmap.json").toString()
    api (mixin) {
        isTransitive = false
    }
    annotationProcessor("org.ow2.asm:asm-debug-all:5.2")
    annotationProcessor("com.google.guava:guava:30.0-jre")
    annotationProcessor("com.google.code.gson:gson:2.8.9")
    annotationProcessor (mixin) {
        isTransitive = false
    }

    // Mod Dependencies
    implementation("com.cleanroommc:configanytime:2.0")
    compileOnly("CraftTweaker2:CraftTweaker2-MC1120-Main:1.12-4.+")
    compileOnly(rfg.deobf("curse.maven:modularmachinery-community-edition-817377:5375642"))
    implementation(rfg.deobf("curse.maven:had-enough-items-557549:5210315"))
    compileOnly(rfg.deobf("curse.maven:jei-utilities-616190:4630499"))
    implementation(rfg.deobf("curse.maven:the-one-probe-245211:2667280"))
    implementation(rfg.deobf("curse.maven:ae2-extended-life-570458:5147702"))
    compileOnly(rfg.deobf("curse.maven:CodeChickenLib-242818:2779848"))
    compileOnly(rfg.deobf("curse.maven:nuclearcraft-overhauled-336895:3862197"))
    implementation(rfg.deobf("curse.maven:industrialcraft-2-242638:3838713"))
    compileOnly(rfg.deobf("curse.maven:mekanism-ce-unofficial-840735:5130458"))
//    compileOnly(rfg.deobf("curse.maven:mekanism-unofficial-edition-v10-edition-840735:4464199"))
    compileOnly(rfg.deobf("curse.maven:RedstoneFlux-270789:2920436"))
    compileOnly(rfg.deobf("curse.maven:cofh-core-69162:2920433"))
    compileOnly(rfg.deobf("curse.maven:cofh-world-271384:2920434"))
    compileOnly(rfg.deobf("curse.maven:thermal-foundation-222880:2926428"))
    compileOnly(rfg.deobf("curse.maven:thermal-innovation-291737:2920441"))
    compileOnly(rfg.deobf("curse.maven:thermal-expansion-69163:2926431"))
    compileOnly(rfg.deobf("curse.maven:botania-225643:3330934"))
    compileOnly(rfg.deobf("curse.maven:astral-sorcery-241721:3044416"))
    implementation(rfg.deobf("curse.maven:baubles-227083:2518667"))
    compileOnly(rfg.deobf("curse.maven:zenutil-401178:4394263"))
    compileOnly(rfg.deobf("curse.maven:immersive-engineering-231951:2974106"))
    compileOnly(rfg.deobf("curse.maven:immersive-petroleum-268250:3382321"))
    compileOnly(rfg.deobf("curse.maven:smooth-font-285742:3944565"))
    compileOnly(rfg.deobf("curse.maven:athenaeum-284350:4633750"))
    compileOnly(rfg.deobf("curse.maven:artisan-worktables-284351:3205284"))
    compileOnly(rfg.deobf("curse.maven:artisan-automation-373329:2994098"))
    compileOnly(rfg.deobf("curse.maven:touhou-little-maid-355044:3576415"))
    compileOnly(rfg.deobf("curse.maven:ingame-info-xml-225604:2489566"))
    compileOnly(rfg.deobf("curse.maven:lunatriuscore-225605:2489549"))
    compileOnly(rfg.deobf("curse.maven:rgb-chat-702720:4092100"))
    compileOnly(rfg.deobf("curse.maven:endercore-231868:4671384"))
    compileOnly(rfg.deobf("curse.maven:ender-io-64578:4674244"))
//    compileOnly("info.loenwind.autosave:AutoSave:1.12.2:1.0.11") // EnderIO Dependency
//    compileOnly("info.loenwind.autoconfig:AutoConfig:1.12.2:1.0.2") // EnderIO Dependency
    compileOnly(rfg.deobf("curse.maven:tinkers-evolution-384589:4941753"))
    compileOnly(rfg.deobf("curse.maven:ore-excavation-250898:2897369"))
    compileOnly(rfg.deobf("curse.maven:techguns-244201:2958103"))
    compileOnly(rfg.deobf("curse.maven:biomes-o-plenty-220318:3558882"))
    compileOnly(rfg.deobf("curse.maven:more-electric-tools-366298:3491973"))
    compileOnly(rfg.deobf("curse.maven:brandonscore-231382:3051539"))
    compileOnly(rfg.deobf("curse.maven:draconicevolution-223565:3051542"))
    compileOnly(rfg.deobf("curse.maven:mantle-74924:2713386"))
    compileOnly(rfg.deobf("curse.maven:tinkers-construct-74072:2902483"))
    compileOnly(rfg.deobf("curse.maven:thermal-dynamics-227443:2920505"))
    compileOnly(rfg.deobf("curse.maven:armourers-workshop-229523:3101995"))
    compileOnly(rfg.deobf("curse.maven:avaritia-1-10-261348:3143349"))
    compileOnly(rfg.deobf("curse.maven:blood-magic-224791:2822288"))
    compileOnly(rfg.deobf("curse.maven:legendary-tooltips-532127:4499615"))
    compileOnly(rfg.deobf("curse.maven:ftb-quests-forge-289412:3156637"))
    compileOnly(rfg.deobf("curse.maven:flux-networks-248020:3178199"))
    compileOnly(rfg.deobf("curse.maven:scalingguis-319656:2716334"))
    compileOnly(rfg.deobf("curse.maven:extrabotany-299086:3112313"))
    compileOnly(rfg.deobf("curse.maven:better-loading-screen-229302:3769828"))
    compileOnly(rfg.deobf("curse.maven:better-chat-363860:3048407"))
    compileOnly(rfg.deobf("curse.maven:mrcrayfish-furniture-mod-55438:3865259"))
    compileOnly(rfg.deobf("curse.maven:cucumber-272335:2645867"))
    compileOnly(rfg.deobf("curse.maven:sync-229090:2682824"))
    compileOnly(rfg.deobf("curse.maven:libvulpes-236541:3801015"))
    compileOnly(rfg.deobf("curse.maven:advanced-rocketry-236542:4671856"))
    compileOnly(rfg.deobf("curse.maven:gugu-utils-530919:3652765"))
    compileOnly(rfg.deobf("curse.maven:not-enough-energistics-515565:4690660"))
    compileOnly(rfg.deobf("curse.maven:avaritiaddons-248873:4745387"))
    compileOnly(rfg.deobf("curse.maven:custom-starter-gear-253735:2514705"))
    compileOnly(rfg.deobf("curse.maven:modular-routers-250294:2954953"))
    compileOnly(rfg.deobf("curse.maven:ftb-library-legacy-forge-237167:2985811"))
    compileOnly(rfg.deobf("curse.maven:item-filters-309674:3003364"))
    compileOnly(rfg.deobf("curse.maven:ftb-quests-forge-289412:3156637"))
    compileOnly(rfg.deobf("curse.maven:ftb-utilities-forge-237102:3157548"))
    compileOnly(rfg.deobf("curse.maven:tinkers-evolution-384589:4941753"))
    compileOnly(rfg.deobf("curse.maven:foamfix-optimization-mod-278494:3973967"))
    compileOnly(rfg.deobf("curse.maven:neverenoughanimation-1062347:5531863"))
    compileOnly(rfg.deobf("curse.maven:ctm-267602:2915363"))
    compileOnly(rfg.deobf("curse.maven:chisel-235279:2915375"))
    compileOnly(rfg.deobf("curse.maven:libnine-322344:3509087"))
    compileOnly(rfg.deobf("curse.maven:lazy-ae2-322347:3254160"))
    compileOnly(rfg.deobf("curse.maven:electroblobs-wizardry-265642:5354477"))
    implementation(rfg.deobf("curse.maven:ender-utilities-224320:2977010"))
    compileOnly(rfg.deobf("curse.maven:ancient-spellcraft-358124:5413256"))
    compileOnly(rfg.deobf("curse.maven:random-psideas-302313:3215550"))
    compileOnly(rfg.deobf("curse.maven:journeymap-32274:5172461"))
    compileOnly(rfg.deobf("curse.maven:abyssalcraft-53686:5330323"))
    compileOnly(rfg.deobf("curse.maven:vintagefix-871198:5536276"))
    implementation(rfg.deobf("curse.maven:lolasm-460609:5257348")) // ASM Compat
    compileOnly(rfg.deobf("curse.maven:fermiumasm-971247:5346789")) // or ASM Compat
    compileOnly(rfg.deobf("curse.maven:blahajasm-1081696:5623595")) // or ASM Compat?
    runtimeOnly(rfg.deobf("curse.maven:spark-361579:3245793"))
    compileOnly(rfg.deobf("curse.maven:dme-737252:5043404"))
    compileOnly(rfg.deobf("curse.maven:bountifulbaubles-313536:3568240"))
}

// IDE Settings
//eclipse {
//    classpath {
//        isDownloadSources = true
//        isDownloadJavadoc = true
//    }
//}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
        inheritOutputDirs = true // Fix resources in IJ-Native runs
    }
    project {
        this.withGroovyBuilder {
            "settings" {
                "runConfigurations" {
                    val self = this.delegate as RunConfigurationContainer
                    self.add(Gradle("1. Run Client").apply {
                        setProperty("taskNames", listOf("runClient"))
                    })
                    self.add(Gradle("2. Run Server").apply {
                        setProperty("taskNames", listOf("runServer"))
                    })
                    self.add(Gradle("3. Run Obfuscated Client").apply {
                        setProperty("taskNames", listOf("runObfClient"))
                    })
                    self.add(Gradle("4. Run Obfuscated Server").apply {
                        setProperty("taskNames", listOf("runObfServer"))
                    })
                }
                "compiler" {
                    val self = this.delegate as org.jetbrains.gradle.ext.IdeaCompilerConfiguration
                    afterEvaluate {
                        self.javac.moduleJavacAdditionalOptions = mapOf(
                                (project.name + ".main") to
                                        tasks.compileJava.get().options.compilerArgs.map { '"' + it + '"' }.joinToString(" ")
                        )
                    }
                }
            }
        }
    }
}

tasks.processIdeaSettings.configure {
    dependsOn(tasks.injectTags)
}