package io.canvasmc.canvas.scheduler;

import java.math.BigDecimal;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An interface into Canvas' tick scheduler
 */
public interface MultithreadedTickScheduler {
    int SAMPLE_RATE = 20;

    /**
     * Creates a new "full tick", which contains and manages logic ticking
     * in parallel with the server at the server tick-rate ({@link MultithreadedTickScheduler#getTickRate()}). This contains logic for the following automatically:
     * <ui>
     * <li>Appends the tick loop to the <b>/tps</b> command</li>
     * <li>Registers it to Watchdog</li>
     * <li>Implements tick times handling</li>
     * <li>Implements tick sleeping</li>
     * <li>Implements "task scheduling"</li>
     * </ui>
     * <br>
     * The first tick is scheduled for {@link MultithreadedTickScheduler#getTimeBetweenTicks()} nanoseconds
     * in the after constructing the loop.
     * <br><br>
     * Example:
     * <pre>{@code
     * ThreadedBukkitServer bukkitServer = ThreadedBukkitServer.getInstance();
     * bukkitServer.getScheduler().scheduleWrapped(new WrappedTickLoop.WrappedTick() {
     *       @Override
     *       public void blockTick(final WrappedTickLoop loop, final BooleanSupplier hasTimeLeft, final int tickCount) {
     *             if (tickCount % 20 == 0) {
     *                 loop.getLogger().info("Hello World! The tick count is: {}", loop.getTickCount());
     *             }
     *       }
     *
     *       @Override
     *       public Component debugInfo() {
     *            return Component.text("Hello World!", TextColor.color(200, 20, 100), TextDecoration.OBFUSCATED);
     *       }
     * }, NamespacedKey.fromString("canvas:example"));
     * }</pre>
     *
     * @param tick       the tick
     * @param identifier the identifier for your tick-loop. recommended to be unique
     */
    @NotNull WrappedTickLoop scheduleWrapped(@NotNull WrappedTickLoop.WrappedTick tick, @NotNull NamespacedKey identifier);

    /**
     * Gets the tick loop from the internal registry. The loop MUST be active(meaning it's ticking) to be locatable.
     * <br>
     * The identifier for tick loops is not forced to be unique. Within Canvas internals, this is always unique, with plugins, it is unpredictable.
     *
     * @param identifier the String identifier that defines the tick loop.
     * @return the located loop
     */
    @Nullable
    WrappedTickLoop getTickLoop(@NotNull NamespacedKey identifier);

    /**
     * Returns the amount of threads allocated to the scheduler
     *
     * @return thread count
     */
    int getThreadCount();

    /**
     * Returns an array of the tick runner threads
     *
     * @return the runner threads
     */
    Thread @NotNull [] getThreads();

    /**
     * Returns the tick rate for the scheduler. This can be changed at any time and is not final.
     * This is due to the `/tick` command existing, which can affect the tick rate.
     *
     * @return the tick rate of the server
     */
    int getTickRate();

    /**
     * Sets the tick rate for the server
     *
     * @param tickRate the tick rate
     */
    void setTickRate(int tickRate);

    /**
     * Returns the time in nanoseconds between ticks. This follows the same rules as
     * the tick rate, where it can change at any time. This is directly influenced by the tick rate
     *
     * @return the time between ticks
     */
    long getTimeBetweenTicks();

    /**
     * Returns the tps base, which is used for calculating tps
     *
     * @return tps base
     */
    @NotNull BigDecimal getTpsBase();

    /**
     * Gets the "sample rate" which is used for how often we tick TPS
     * and other data
     *
     * @return the sample rate
     */
    default int getSampleRate() {
        return SAMPLE_RATE;
    }
}
