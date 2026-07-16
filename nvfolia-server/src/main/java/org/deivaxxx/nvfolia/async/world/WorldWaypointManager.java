package me.biquaternions.fish.async.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.waypoints.WaypointTransmitter;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class WorldWaypointManager extends ServerWaypointManager {

    private final ServerLevel level;

    public WorldWaypointManager(ServerLevel level) {
        super(level);
        this.level = level;
    }

    @Override
    public final void trackWaypoint(WaypointTransmitter waypoint) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.level, "Cannot track waypoints off-main");
        super.trackWaypoint(waypoint);
    }

    @Override
    public final void updateWaypoint(WaypointTransmitter waypoint) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.level, "Cannot update waypoints off-main");
        super.updateWaypoint(waypoint);
    }

    @Override
    public final void untrackWaypoint(WaypointTransmitter waypoint) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.level, "Cannot untrack waypoints off-main");
        super.untrackWaypoint(waypoint);
    }

    @Override
    public final void addPlayer(ServerPlayer player) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.level, "Cannot add player to waypoints off-main");
        super.addPlayer(player);
    }

    @Override
    public final void updatePlayer(ServerPlayer player) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.level, "Cannot update player for waypoints off-main");
        super.updatePlayer(player);
    }

    @Override
    public final void removePlayer(ServerPlayer player) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.level, "Cannot remove player from waypoints off-main");
        super.removePlayer(player);
    }

    @Override
    public final void breakAllConnections() {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.level, "Cannot break all waypoint connections off-main");
        super.breakAllConnections();
    }

    @Override
    protected final void createConnection(ServerPlayer player, WaypointTransmitter waypoint) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.level, "Cannot create waypoint connections off-main");
        super.createConnection(player, waypoint);
    }

    @Override
    protected final void updateConnection(ServerPlayer player, WaypointTransmitter waypoint, WaypointTransmitter.Connection connection) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.level, "Cannot update waypoint connection off-main");
        super.updateConnection(player, waypoint, connection);
    }

}
