package io.canvasmc.canvas.event;

import io.canvasmc.canvas.ThreadedBukkitServer;
import io.canvasmc.canvas.scheduler.MultithreadedTickScheduler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerEvent;
import org.jetbrains.annotations.NotNull;

public class TickSchedulerStartEvent extends ServerEvent {
    private static final HandlerList handlers = new HandlerList();

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public @NotNull MultithreadedTickScheduler getScheduler() {
        return ThreadedBukkitServer.getInstance().getScheduler();
    }
}
