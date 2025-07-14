package io.canvasmc.canvas;

import io.canvasmc.canvas.chunk.FluidPostProcessingMode;
import io.canvasmc.canvas.config.*;
import io.canvasmc.canvas.config.annotation.Comment;
import io.canvasmc.canvas.config.internal.ConfigurationManager;
import io.canvasmc.canvas.util.YamlTextFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Configuration("canvas-server")
public class Config {
    public static boolean RUNNING_IN_IDE = Boolean.getBoolean("minecraft.running-in-ide");
    public static ComponentLogger LOGGER = ComponentLogger.logger("Canvas");
    public static Config INSTANCE;

    @Comment(value = {
        "Folia is incompatible with spark normally. Canvas fixes it and implements a new spark plugin for Folia support",
        "If this option is enabled, Canvas will use the modified spark plugin internally for Folia support, otherwise it",
        "will use Papers spark implementation, which may have issues."
    })
    public boolean useOurSparkPlugin = true;

    public Chunks chunks = new Chunks();
    public static class Chunks {
        @Comment("Use euclidean distance squared for chunk task ordering. Makes the world load in what appears a circle rather than a diamond")
        public boolean useEuclideanDistanceSquared = true;

        @Comment("The thread priority for Canvas' rewritten chunk system executor")
        public int threadPoolPriority = Thread.NORM_PRIORITY;

        @Comment(value = {
            "Determines the fluid post processing mode.",
            "The worldgen processes creates a lot of unnecessary fluid post-processing tasks,",
            "which can overload the server thread and cause stutters.",
            "There are 3 accepted values",
            " - VANILLA - just normal vanilla, no changes",
            " - DISABLED - disables fluid post processing completely",
            " - FILTERED - applies a rough filter to filter out fluids that are definitely not going to flow"
        })
        public FluidPostProcessingMode fluidPostProcessingMode = FluidPostProcessingMode.VANILLA;

        @Comment(value = {
            "Whether to turn fluid postprocessing into scheduled tick",
            "",
            "Fluid post-processing is very expensive when loading in new chunks, and this can affect",
            "MSPT significantly. This option delays fluid post-processing to scheduled tick to hopefully mitigate this issue."
        })
        public boolean fluidPostProcessingToScheduledTick = false;

        @Comment("Whether to enable aquifer optimizations to accelerate overworld worldgen")
        public boolean optimizeAquifer = false;

        @Comment("Whether to enable End Biome Cache to accelerate The End worldgen")
        public boolean useEndBiomeCache = false;

        @Comment("The cache capacity for the end biome cache. Only works with 'useEndBiomeCache' enabled")
        public int endBiomeCacheCapacity = 1024;

        @Comment("Whether to enable Beardifier optimizations to accelerate world generation")
        public boolean optimizeBeardifier = false;

        @Comment("Whether to enable optimizations to the noise based chunk generator")
        public boolean optimizeNoiseGeneration = false;

        public BiomeCache biomeCache = new BiomeCache();
        public static class BiomeCache {
            @Comment("Enables biome caching, which makes a biome lookup caching layer to reduce expensive biome calculations and queries")
            public boolean enabled = false;
            @Comment("Enables advancement-related biome checks for biome caching")
            public boolean advancement = false;
            @Comment("Enables biome caching for mob spawning biome lookups")
            public boolean mobSpawn = false;
        }

        @Comment(value = {
            "Once one task is completed then the next task starts immediately, to prevent blocking threads while waiting to complete all tasks",
            "WARNING: May cause the sequence of future compose disorder"
        })
        public boolean useFasterStructureGenFutureSequencing = false;

        @Comment(value = {
            "Makes chunk packet preparation and sending asynchronous to improve server performance.",
            "This can significantly reduce main thread load when many players are loading chunks."
        })
        public boolean asyncChunkSend = false;

        @Comment(value = {
            "Changes the maximum view distance for the server, allowing clients to have",
            "render distances higher than 32"
        })
        public int maxViewDistance = 32;
    }

    public Networking networking = new Networking();
    public static class Networking {
        @Comment(value = {
            "The clientbound set entity motion packet can often cause high network (Netty) usage and consumes (on larger production servers)",
            "up to 60% of your network usage. Disabling this has minimal side effects, such as squids and glow squids swimming upright until attacked."
        })
        public boolean disableClientboundSetEntityMotionPacket = false;
    }

    @Comment("Configurations for enabling virtual threads for different thread pool executors")
    public VirtualThreads virtualThreads = new VirtualThreads();
    public static class VirtualThreads {
        @Comment("Enables virtual thread usage for the async scheduler executor")
        public boolean asyncScheduler = false;

        @Comment("Enables virtual thread usage for the chat executor")
        public boolean chatExecutor = false;

        @Comment("Enables virtual thread usage for the authenticator pool")
        public boolean authenticatorPool = false;

        @Comment("Enables virtual thread usage for the text filter executor")
        public boolean serverTextFilter = false;

        @Comment("Enables virtual thread usage for the text filter executor")
        public boolean tabCompleteExecutor = false;

        @Comment("Enables virtual thread usage for the profile lookup executor")
        public boolean profileLookupExecutor = false;
    }

    public Entities entities = new Entities();
    public static class Entities {
        @Comment("When enabled, hides flames on entities with fire resistance")
        public boolean hideFlamesOnEntitiesWithFireResistance = false;

        @Comment("Filters entity movement packets to reduce the amount of useless move packets sent")
        public boolean reduceUselessMovePackets = false;
    }

    @Comment("Check if a cactus can survive before growing. Heavily optimizes cacti farms")
    public boolean cactusCheckSurvivalBeforeGrowth = false;

    @Comment("Whether to cache expensive CraftEntityType#minecraftToBukkit call")
    public boolean enableCachedMTBEntityTypeConvert = false;

    @Comment("Enables creation of tile entity snapshots on retrieving blockstates")
    public boolean tileEntitySnapshotCreation = false;

    @Comment("Determines if end crystals should explode in a chain reaction, similar to how tnt works when exploded")
    public boolean chainEndCrystalExplosions = false;

    @Comment("Disables falling on farmland turning it back to dirt")
    public boolean disableFarmlandTrampling = false;

    @Comment("Makes farmland always moist, never drying out, even if it isn't near water")
    public boolean farmlandAlwaysMoist = false;

    public AsyncLocator asyncLocator = new AsyncLocator();
    public static class AsyncLocator {
        @Comment("The amount of threads allocated to the async locator")
        public int threads = 1;

        @Comment("The keepalive time in seconds for the async locator")
        public int keepalive = 60;
    }

    private static <T extends Config> @NotNull ConfigSerializer<T> buildSerializer(Configuration config, Class<T> configClass) {
        ConfigurationUtils.extractKeys(configClass);
        Set<String> changes = new LinkedHashSet<>();
        return new AnnotationBasedYamlSerializer<>(SerializationBuilder.<T>newBuilder()
            .header(new String[]{
                "This is the main Canvas configuration file",
                "All configuration options here are made for vanilla-compatibility",
                "and not for performance. Settings must be configured specific",
                "to your hardware and server type. If you have questions",
                "join our discord at https://canvasmc.io/discord",
                "As a general rule of thumb, do NOT change a setting if",
                "you don't know what it does! If you don't know, ask!"
            })
            .handler(ConfigHandlers.ExperimentalProcessor::new)
            .handler(ConfigHandlers.CommentProcessor::new)
            .validator(ConfigHandlers.RangeProcessor::new)
            .validator(ConfigHandlers.NegativeProcessor::new)
            .validator(ConfigHandlers.PositiveProcessor::new)
            .validator(ConfigHandlers.NonNegativeProcessor::new)
            .validator(ConfigHandlers.NonPositiveProcessor::new)
            .validator(ConfigHandlers.PatternProcessor::new)
            .runtimeModifier("debug.*", new RuntimeModifier<>(boolean.class, (original) -> RUNNING_IN_IDE || original))
            .post(context -> {
                INSTANCE = context.configuration();
                // build and print config tree.
                YamlTextFormatter formatter = new YamlTextFormatter(4);
                LOGGER.info(Component.text("Printing configuration tree:").appendNewline().append(formatter.apply(context.contents())));
                if (RUNNING_IN_IDE) {
                    LOGGER.info("Running Minecraft development server in IDE.");
                }
                for (final String change : changes) {
                    LOGGER.info(change);
                }
            })
            .build(config, configClass), changes::add
        );
    }

    public static Config init() {
        long startNanos = System.nanoTime();
        ConfigurationManager.register(Config.class, Config::buildSerializer);
        LOGGER.info("Finished Canvas config init in {}ms", TimeUnit.MILLISECONDS.convert(Util.getNanos() - startNanos, TimeUnit.NANOSECONDS));
        return INSTANCE;
    }
}
