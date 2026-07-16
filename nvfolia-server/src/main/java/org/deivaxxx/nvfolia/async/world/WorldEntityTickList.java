package me.biquaternions.fish.async.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import org.jspecify.annotations.NonNull;
import java.util.function.Consumer;

public class WorldEntityTickList extends EntityTickList {

    private final ServerLevel level;

    public WorldEntityTickList(ServerLevel level) {
        this.level = level;
    }

    @Override
    public void add(@NonNull Entity entity) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(entity, "Asynchronous entity ticklist addition"); // Paper // SparklyPaper - parallel world ticking (additional concurrency issues logs)
        super.add(entity);
    }

    @Override
    public void remove(@NonNull Entity entity) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(entity, "Asynchronous entity ticklist addition"); // Paper // SparklyPaper - parallel world ticking (additional concurrency issues logs)
        super.remove(entity);
    }

    @Override
    public void forEach(@NonNull Consumer<Entity> entity) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.level, "Asynchronous entity ticklist iteration"); // SparklyPaper - parallel world ticking (additional concurrency issues logs)
        super.forEach(entity);
    }

}
