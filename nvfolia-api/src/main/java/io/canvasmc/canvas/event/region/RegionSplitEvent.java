package io.canvasmc.canvas.event.region;

import io.canvasmc.canvas.region.Region;
import java.util.List;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerEvent;
import org.jetbrains.annotations.NotNull;

public class RegionSplitEvent extends ServerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final Region from;
    private final List<Region> into;

    public RegionSplitEvent(@NotNull Region from, @NotNull List<Region> into) {
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

    public @NotNull List<Region> getInto() {
        return into;
    }
}
