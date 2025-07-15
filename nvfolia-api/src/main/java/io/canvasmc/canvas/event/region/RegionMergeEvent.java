package io.canvasmc.canvas.event.region;

import io.canvasmc.canvas.region.Region;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerEvent;
import org.jetbrains.annotations.NotNull;

public class RegionMergeEvent extends ServerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final Region from;
    private final Region into;

    public RegionMergeEvent(@NotNull Region from, @NotNull Region into) {
        this.from = from;
        this.into = into;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public @NotNull Region getFrom() {
        return from;
    }

    public @NotNull Region getInto() {
        return into;
    }
}
