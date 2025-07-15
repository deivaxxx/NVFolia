package io.canvasmc.canvas;

import io.canvasmc.canvas.region.Region;
import io.canvasmc.canvas.scheduler.WrappedTickLoop;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public interface LevelAccess extends WrappedTickLoop {
    /**
     * Gets the Bukkit {@link World}
     *
     * @return the world associated with this {@link LevelAccess} instance
     */
    @NotNull
    World getWorld();

    /**
     * If the level is actively ticking
     *
     * @return true if the level is processing ticks
     */
    boolean isTicking();

    // threaded regions

    /**
     * Gets all regions currently in this world
     * <br>
     * If {@link Server#isRegionized()} is false, this will be empty
     *
     * @return regions in the world
     */
    @Unmodifiable
    @NotNull
    List<Region> getAllRegions();

    /**
     * Runs a consumer on each region currently in the world
     * <br>
     * If {@link Server#isRegionized()} is false, this will do nothing
     *
     * @param forEach the consumer
     */
    void forEachRegion(@NotNull Consumer<Region> forEach);
}
