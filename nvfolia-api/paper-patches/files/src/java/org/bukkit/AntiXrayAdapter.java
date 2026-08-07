package dev.imanity.antixray.sdk;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public interface AntiXrayAdapter {
    /**
     * Call a block change to the AntiXray system
     * It should be at Level.setBlock(BlockPos, BlockState, flags, maxUpdateDepth)
     * Or World.setTypeAndData(BlockPosition, IBlockData, i) in legacy spigot versions
     *
     * @param world    the bukkit world
     * @param x        the x
     * @param y        the y
     * @param z        the z
     * @param material the bukkit material
     */
    void callBlockChange(World world, int x, int y, int z, Material material);

    /**
     * Call a player left click block to the AntiXray system
     * It should be at ServerPlayerGameMode.handleBlockBreakAction(BlockPos, ServerboundPlayerActionPacket.Action, Direction, worldHeight, sequence)
     * Or PlayerInteractManager.a(BlockPosition, EnumDirection) in legacy spigot versions
     *
     * @param world  the bukkit world
     * @param player the bukkit player
     * @param x      the x
     * @param y      the y
     * @param z      the z
     */
    void callPlayerLeftClickBlock(World world, Player player, int x, int y, int z);
}
