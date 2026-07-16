package me.biquaternions.fish.async.thread.factory;

import net.minecraft.server.level.ServerLevel;
import me.biquaternions.fish.async.thread.WorldTickThread;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import java.util.concurrent.ThreadFactory;

public class WorldExecutorThreadFactory implements ThreadFactory {

    private final Logger logger;
    private final ServerLevel world;

    public WorldExecutorThreadFactory(@NonNull ServerLevel world) {
        this.world = world;
        this.logger = LogManager.getLogger(String.format("World %s", this.world.serverLevelData.getLevelName()));
    }

    @Override
    public Thread newThread(@NonNull final Runnable r) {
        Thread thread = new WorldTickThread(r, this.world);
        thread.setDaemon(false);
        thread.setPriority(Thread.NORM_PRIORITY + 1);
        thread.setUncaughtExceptionHandler((t, e) -> this.logger.fatal("An exception was thrown while ticking {}", t.getName(), e));
        return thread;
    }

}
