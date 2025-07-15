package io.canvasmc.canvas;

import io.canvasmc.canvas.region.Region;
import io.canvasmc.canvas.scheduler.MultithreadedTickScheduler;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <h2>ThreadedBukkitServer</h2>
 * <b>The main interface into Canvas threaded internals.</b>
 * <br><br>
 * This includes access to the tick task scheduler, fetching regions, threaded world access, and checks
 * for if the current thread owns a specific object.
 * <br><br>
 * <h3>Thread Ownership</h3>
 * The {@code isTickThreadFor} methods are used to determine if the current thread owns an object at
 * a certain location, like an {@link Entity} or a {@link Chunk}. This can assist in thread-safety
 * in the new multithreaded environment.
 * <p><b>Note:</b> During server shutdown, the thread performing shutdown is treated as the owning
 * thread for all regions and locations. This is usually the main thread</p>
 */
public interface ThreadedBukkitServer {

    /**
     * Returns the {@link ThreadedBukkitServer} instance for the runtime
     */
    @Contract(pure = true)
    static ThreadedBukkitServer getInstance() {
        return InstanceHolder.instance;
    }

    @ApiStatus.Internal // don't use, this is for internal use only.
    static void setInstance(ThreadedBukkitServer server) {
        if (server == null) throw new IllegalArgumentException("ThreadedServer instance cannot be null");
        synchronized (InstanceHolder.class) {
            if (InstanceHolder.instance != null) {
                throw new IllegalStateException("ThreadedServer instance already set");
            }
            InstanceHolder.instance = server;
        }
    }

    /**
     * Gets the level access, which allows accessing internals of the tick task specifically tied to the world
     * like scheduling, and the world-independent scheduler.
     *
     * @param world the bukkit world
     * @return the world's level access
     */
    @NotNull
    LevelAccess getLevelAccess(@NotNull World world);

    /**
     * Gets the tick scheduler for Canvas, allowing scheduling and fetching of tick tasks
     *
     * @return the scheduler
     */
    @NotNull
    MultithreadedTickScheduler getScheduler();

    /**
     * Schedules a task on the main thread
     *
     * @param runnable task
     */
    @Deprecated(forRemoval = true)
    void scheduleOnMain(@NotNull Runnable runnable);

    /**
     * Gets the region at the specified chunk. If regionizing is disabled, this returns null, or if the chunk is not loaded.
     *
     * @param chunkX x coord
     * @param chunkZ z coord
     * @return the region
     */
    @Nullable
    Region getRegionAtChunk(@NotNull World world, int chunkX, int chunkZ);

    /**
     * Returns true if the current thread is the owner of the specified bounding box
     * @param world the world
     * @param aabb the bounding box
     * @return if the aabb is owned by the current thread
     */
    boolean isTickThreadFor(final @NotNull World world, final @NotNull BoundingBox aabb);

    /**
     * Returns true if the current thread is the owner of the specified block position
     * @param world the world
     * @param blockX the blocks X coordinate
     * @param blockZ the blocks Z coordinate
     * @return if the block position is owned by the current thread
     */
    boolean isTickThreadFor(final @NotNull World world, final double blockX, final double blockZ);

    /**
     * Returns true if the current thread is the owner of from the location to the location after the velocity, <b>deltaMovement</b>, is applied, with a specified buffer
     * <br><br>
     * The calculated <b>from</b> && <b>to</b> chunk positions are created as such:
     * <pre>{@code
     * final int fromChunkX = CoordinateUtils.getChunkX(position);
     * final int fromChunkZ = CoordinateUtils.getChunkZ(position);
     *
     * final int toChunkX = CoordinateUtils.getChunkCoordinate(position.x + deltaMovement.x);
     * final int toChunkZ = CoordinateUtils.getChunkCoordinate(position.z + deltaMovement.z);
     * }</pre>
     * <br>
     * This is used internally to make sure entities do not move into regions they do not own
     * @param world the world
     * @param position initial position
     * @param deltaMovement the velocity
     * @param buffer the buffer
     * @return if the start location to the end location with the buffer are owned by the current thread
     */
    boolean isTickThreadFor(final @NotNull World world, final Location position, final @NotNull Vector deltaMovement, final int buffer);

    /**
     * Returns true if the current thread is the owner of the location provided
     * @param world the world
     * @param pos the location
     * @return if the location is owned by this thread
     */
    boolean isTickThreadFor(final @NotNull World world, final @NotNull Location pos);

    /**
     * Returns true if the current thread owns <b>all</b> the blocks within the radius of the center
     * @param world the world
     * @param pos the center location
     * @param blockRadius the radius
     * @return if the current thread owns all the blocks within the radius of the center
     */
    boolean isTickThreadFor(final @NotNull World world, final @NotNull Location pos, final int blockRadius);

    /**
     * Returns true if the current thread owns the chunk provided
     * @param world the world
     * @param chunk the chunk
     * @return if the chunk is owned by the current thread
     */
    boolean isTickThreadFor(final @NotNull World world, final @NotNull Chunk chunk);

    /**
     * Returns true if the current thread owns the entity
     * @param entity the entity
     * @return if the entity is owned by the current thread
     */
    boolean isTickThreadFor(final @NotNull Entity entity);

    /**
     * Returns true if the current thread owns the chunk at the specified chunk X and Z positions
     * @param world the world
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return if the chunk at the X/Z pos is owned by the current thread
     */
    boolean isTickThreadFor(final @NotNull World world, final int chunkX, final int chunkZ);

    /**
     * Returns true if <b>all</b> the chunks within the radius of the center chunk are owned by the current thread
     * <br><br>
     * <b>Note: during shutdown, whichever thread is conducting shutdown is the owning thread. This is normally the main thread</b>
     * @param world the world
     * @param chunkX the center chunk X coordinate
     * @param chunkZ the center chunk Z coordinate
     * @param radius the radius
     * @return if the current thread owns all the chunks within the radius
     */
    boolean isTickThreadFor(final @NotNull World world, final int chunkX, final int chunkZ, final int radius);

    /**
     * Returns true if <b>all</b> the chunks from the corner chunk <b>"from"</b> to the corner chunk <b>"to"</b> are owned by the current thread
     * @param world the world
     * @param fromChunkX the chunk X coordinate at the <b>"from"</b> corner
     * @param fromChunkZ the chunk Z coordinate at the <b>"from"</b> corner
     * @param toChunkX the chunk X coordinate at the <b>"to"</b> corner
     * @param toChunkZ the chunk Z coordinate at the <b>"to"</b> corner
     * @return if all chunks within the corners are owned by the current thread
     */
    boolean isTickThreadFor(final @NotNull World world, final int fromChunkX, final int fromChunkZ, final int toChunkX, final int toChunkZ);

    /**
     * Returns if the 2 locations provided are in the same region. When regionizing is disabled, the entire world is 1 region, so it just compares the world
     * @param location1 the first location
     * @param location2 the second location
     * @return if the locations are owned by the same region
     */
    boolean isSameRegion(@NotNull Location location1, @NotNull Location location2);

    class InstanceHolder {
        private static volatile ThreadedBukkitServer instance;
    }
}
