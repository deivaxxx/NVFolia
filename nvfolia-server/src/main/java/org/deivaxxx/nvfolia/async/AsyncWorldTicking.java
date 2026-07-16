package me.biquaternions.fish.async;

import ca.spottedleaf.common.time.TickData;
import ca.spottedleaf.common.time.TickTime;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.util.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerLevel;
import me.biquaternions.fish.FishConfig;
import me.biquaternions.fish.threadedregions.scheduler.WorldRegionScheduler;
import me.biquaternions.fish.util.CallableWrapper;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;

@NullMarked
public class AsyncWorldTicking {

    private static final Logger LOGGER = LoggerFactory.getLogger("Fish World Ticking");
    private static final Semaphore SEMAPHORE = new Semaphore(FishConfig.ASYNC.WORLD_TICKING._THREADS);
    private static final CompletableFuture<?>[] EMPTY_ARRAY = new CompletableFuture[0];
    private static final Queue<Runnable> END_OF_TICK_TASKS = new ConcurrentLinkedQueue<>();
    private static final ServerTickRateManager TICK_RATE_MANAGER = MinecraftServer.getServer().tickRateManager();

    public static void tickWorlds(Iterable<ServerLevel> worlds, BooleanSupplier hasTimeLeft) {

        long tickInterval = TICK_RATE_MANAGER.isSprinting() ? 0 : TICK_RATE_MANAGER.nanosecondsPerTick();
        final Queue<CompletableFuture<Void>> tasks = new ArrayDeque<>();
        try {
            for (ServerLevel serverLevel : worlds) {
                serverLevel.hasPhysicsEvent = org.bukkit.event.block.BlockPhysicsEvent.getHandlerList().getRegisteredListeners().length > 0; // Paper - BlockPhysicsEvent
                serverLevel.hasEntityMoveEvent = io.papermc.paper.event.entity.EntityMoveEvent.getHandlerList().getRegisteredListeners().length > 0; // Paper - Add EntityMoveEvent
                serverLevel.updateLagCompensationTick(); // Paper - lag compensation
                net.minecraft.world.level.block.entity.HopperBlockEntity.skipHopperEvents = serverLevel.paperConfig().hopper.disableMoveEvent || org.bukkit.event.inventory.InventoryMoveItemEvent.getHandlerList().getRegisteredListeners().length == 0; // Paper - Perf: Optimize Hoppers

                SEMAPHORE.acquire();
                tasks.offer(CompletableFuture.runAsync(() -> {
                    serverLevel.fish$lock.writeLock().lock();
                    try {
                        serverLevel.fish$currentTickStart = System.nanoTime();
                        serverLevel.fish$tickSchedule.setNextPeriod(serverLevel.fish$currentTickStart, tickInterval);
                        serverLevel.fish$nextTickTimeNanos = serverLevel.fish$tickSchedule.getDeadline(tickInterval);

                        serverLevel.tick(hasTimeLeft);
                        ((WorldRegionScheduler) Bukkit.getRegionScheduler()).tickWorld(serverLevel);
                        AsyncWorldTicking.recordEndOfTick(serverLevel);

                    } catch (Throwable var7) {
                        CrashReport crashReport = CrashReport.forThrowable(var7, "Exception ticking world [" + serverLevel.getWorld().getName() + "]");
                        serverLevel.fillReportDetails(crashReport);
                        throw new ReportedException(crashReport);
                    } finally {
                        serverLevel.fish$lock.writeLock().unlock();
                        SEMAPHORE.release();
                    }
                }, Objects.requireNonNull(serverLevel.fish$tickExecutor)));
                serverLevel.explosionDensityCache.clear(); // Paper - Optimize explosions
            }
            CompletableFuture.allOf(tasks.toArray(EMPTY_ARRAY)).join();
            AsyncWorldTicking.processScheduledTasks();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private static void recordEndOfTick(final ServerLevel level) {
        final long prevStart = level.fish$lastTickStart;
        final long currStart = level.fish$currentTickStart;
        level.fish$lastTickStart = level.fish$currentTickStart;
        final long scheduledStart = level.fish$scheduledTickStart;
        level.fish$scheduledTickStart = level.fish$nextTickTimeNanos; // set scheduledStart for next tick

        final long now = Util.getNanos();

        final TickTime time = new TickTime(
            prevStart,
            scheduledStart,
            currStart,
            0L,
            now,
            0L,
            level.fish$taskExecutionTime,
            0L,
            false
        );
        level.fish$taskExecutionTime = 0L;

        AsyncWorldTicking.addTickTime(level, time);
    }

    private static void addTickTime(final ServerLevel level, final TickTime time) {
        synchronized (level.fish$statsLock) {
            level.fish$tickTimes5s.addDataFrom(time);
            level.fish$tickTimes10s.addDataFrom(time);
            level.fish$tickTimes15s.addDataFrom(time);
            level.fish$tickTimes60s.addDataFrom(time);
            AsyncWorldTicking.clearTickTimeStatistics(level);
        }
    }

    private static void clearTickTimeStatistics(final ServerLevel level) {
        level.fish$msptData5s = null;
    }

    public static TickData.@Nullable MSPTData getMSPTData5s(final ServerLevel level) {
        synchronized (level.fish$statsLock) {
            if (level.fish$msptData5s == null) {
                level.fish$msptData5s = level.fish$tickTimes5s.getMSPTData(null, TICK_RATE_MANAGER.nanosecondsPerTick());
            }
            return level.fish$msptData5s;
        }
    }

    private static void processScheduledTasks() {
        Runnable task;
        while ((task = END_OF_TICK_TASKS.poll()) != null) {
            task.run();
        }
    }

    public static <T> T scheduleForEndOfWorldTick(ServerLevel level, Callable<T> callable) {
        if (FishConfig.ASYNC.WORLD_TICKING.LOG_ASYNC_ACCESSES) AsyncWorldTicking.logAsyncAccess();
        if (level.fish$lock.readLock().tryLock()) {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                level.fish$lock.readLock().unlock();
            }
        } else {
            CallableWrapper<T> task = new CallableWrapper<>(callable);
            level.fish$scheduler.schedule(task);
            return task.get();
        }
    }

    public static void scheduleVoidForEndOfWorldTick(ServerLevel level, Runnable runnable) {
        if (FishConfig.ASYNC.WORLD_TICKING.LOG_ASYNC_ACCESSES) AsyncWorldTicking.logAsyncAccess();
        if (level.fish$lock.readLock().tryLock()) {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                level.fish$lock.readLock().unlock();
            }
        } else {
            level.fish$scheduler.schedule(runnable);
        }
    }

    /*
     * Used to schedule tasks after all worlds are done ticking.
     * Useful if an async task involves multiple worlds.
     * Possible use for teams plugins.
     */
    public static <T> T scheduleForEndOfTick(Callable<T> callable) {
        if (FishConfig.ASYNC.WORLD_TICKING.LOG_ASYNC_ACCESSES) AsyncWorldTicking.logAsyncAccess();
        CallableWrapper<T> task = new CallableWrapper<>(callable);
        END_OF_TICK_TASKS.offer(task);
        return task.get();
    }

    /*
     * Used to schedule tasks after all worlds are done ticking.
     * Useful if an async task involves multiple worlds.
     * Possible use for async respawn in practice plugins (those calls shouldn't be async imo, but IDK).
     */
    public static void scheduleVoidForEndOfTick(Runnable runnable) {
        if (FishConfig.ASYNC.WORLD_TICKING.LOG_ASYNC_ACCESSES) AsyncWorldTicking.logAsyncAccess();
        END_OF_TICK_TASKS.offer(runnable);
    }

    /*
     * Some tasks call CraftBlock#getNMS which has a chance of scheduling sync Chunk load, which therefore
     *   has a chance of causing a deadlock (at it will be loaded in the next tick, but will never leave this tick
     *   due to holding the read lock).
     * To prevent this scenario, these tasks will be enqueued directly.
     * Other tasks call events that are meant to be sync anyway so they will also be enqueued.
     * And some others may result in try to spawn entities (xp orbs) async.
     *
     * Known scenarios:
     *   1. CraftBlock#setBlockState <- Internally calls Level#setBlock.
     *   2. CraftBlock#breakNaturally <- Internally calls Block#dropResources and Level#setBlock.
     *   3. CraftBlock#applyBoneMeal <- Calls StructureGrowEvent, BlockFertilizeEvent and also could use ThreadLocal
     *        variables on which PWT depends. This one could be directly blocked instead of enqueued.
     *
     */
    public static <T> T scheduleForEndOfWorldTickDirect(ServerLevel level, Callable<T> callable) {
        if (FishConfig.ASYNC.WORLD_TICKING.LOG_ASYNC_ACCESSES) AsyncWorldTicking.logAsyncAccess();
        CallableWrapper<T> task = new CallableWrapper<>(callable);
        level.fish$scheduler.schedule(task);
        return task.get();
    }

    /*
     * Some tasks call NMS functions that have been protected from async reads.
     * Internally also call the chunk system, which means this one has to be scheduled directly to
     *   prevent deadlocks.
     *
     * Known scenarios:
     *   1. CraftBlock#setData <- Internally calls Level#setBlock.
     *
     */
    public static void scheduleVoidForEndOfWorldTickDirect(ServerLevel level, Runnable runnable) {
        if (FishConfig.ASYNC.WORLD_TICKING.LOG_ASYNC_ACCESSES) AsyncWorldTicking.logAsyncAccess();
        level.fish$scheduler.schedule(runnable);
    }

    /**
     * Prints a stacktrace of the asynchronous access, but does not prevent it
     * <br>
     * The idea of this function is to help server designing by minimizing the number of async accesses if those cannot be avoided.
     * Once the server is done and ready for production, this can be safely disabled.
     * The server owner should be aware of the consequences of the async accesses left on the server.
     * <br>
     * A typical scenario where an async access cannot be avoided is on Random Teleport plugins, unless you're developing your own.
     *
     */
    private static void logAsyncAccess() {
        Thread thread = Thread.currentThread();
        LOGGER.warn("A plugin accessed world/block data asynchronously from thread \"{}\".", thread.getName());
        for (StackTraceElement stackTraceElement : thread.getStackTrace()) {
            LOGGER.warn("\tat {}", stackTraceElement);
        }
    }

}
