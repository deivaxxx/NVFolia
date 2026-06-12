package org.bxteam.divinemc.util.cache;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;

/**
 * @author 2No2Name, original implemenation by SuperCoder7979 and Gegy1000
 */
public class IterateOutwardsCache {
    public static final BlockPos POS_ZERO = new BlockPos(0, 0, 0);

    private final ConcurrentHashMap<Long, LongArrayList> table;
    private final int capacity;
    private final Random random;

    public IterateOutwardsCache(int capacity) {
        this.capacity = capacity;
        this.table = new ConcurrentHashMap<>(31);
        this.random = new Random();
    }

    private void fillPositionsWithIterateOutwards(LongList entry, int xRange, int yRange, int zRange) {
        for (BlockPos pos : BlockPos.withinManhattan(POS_ZERO, xRange, yRange, zRange)) {
            entry.add(pos.asLong());
        }
    }

    public LongList getOrCompute(int xRange, int yRange, int zRange) {
        long key = BlockPos.asLong(xRange, yRange, zRange);

        LongArrayList entry = this.table.get(key);
        if (entry != null) {
            return entry;
        }

        entry = new LongArrayList(128);

        this.fillPositionsWithIterateOutwards(entry, xRange, yRange, zRange);

        entry.trim();

        Object previousEntry = this.table.put(key, entry);

        if (previousEntry == null && this.table.size() > this.capacity) {
            final Iterator<Long> iterator = this.table.keySet().iterator();

            for (int i = -this.capacity; iterator.hasNext() && i < 5; i++) {
                Long key2 = iterator.next();

                if (this.random.nextInt(8) == 0 && key2 != key) {
                    iterator.remove();
                }
            }
        }

        return entry;
    }
}
