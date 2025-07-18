package io.canvasmc.canvas.chunk;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * <h2>DynamicChunkPosLongSet</h2>
 * A dynamically resizing set optimized for storing unique long values with extremely fast lookups.
 *
 * <p> This implementation uses a **fixed-size open-addressing hash table** with **quadratic probing**
 * to resolve collisions efficiently. It is designed for scenarios where **only unique long values**
 * need to be stored, and retrieval is not required (only containment checks). </p>
 *
 * <p> Optimized for extremely fast lookups, O(1) worst-case time due to quadratic probing. Resizes dynamically
 * to reduce wasted space. All modification-related methods are synchronzied, and we cache for faster
 * consecutive lookups to avoid redundant computations. </p>
 *
 * <h2>Implementation Details:</h2>
 * <ul>
 *     <li><h6>Quadratic Probing</h6> If a collision occurs, the algorithm searches using
 *         <code>index = (index + probe * probe) & mask</code>, minimizing clustering.</li>
 *     <li><h6>Resizing Strategy</h6> If the load factor exceeds 88%, the table size is doubled.</li>
 *     <li><h6>Cache Mechanism</h6> The last checked key is stored for faster consecutive queries.</li>
 *     <li><h6>Open Addressing</h6> No linked structures; all data is stored directly in an array.</li>
 * </ul>
 *
 * <p> Used primarily for caching the long value of ChunkPos objects to optimize lookup times with
 * servers that have high render distances</p>
 */
public class DynamicChunkPosLongSet implements Set<Long> {
    private static final long EMPTY_KEY = Long.MIN_VALUE;

    private long[] table;
    private int mask;
    private int size;
    private int threshold;
    private long lastChecked = EMPTY_KEY;
    private boolean lastReturn = false;

    public DynamicChunkPosLongSet() {
        this(10);
    }

    public DynamicChunkPosLongSet(int initialCapacity) {
        int capacity = Integer.highestOneBit(Math.max(2, initialCapacity)) << 1;
        this.mask = capacity - 1;
        this.table = new long[capacity];
        Arrays.fill(this.table, EMPTY_KEY);
        this.size = 0;
        this.threshold = capacity >> 1;
    }

    private int indexFor(long key) {
        return (int) (key & mask);
    }

    public boolean containsLong(long key) {
        if (lastChecked == key) {
            return lastReturn;
        }

        lastChecked = key;
        int index = indexFor(key);
        int probe = 0;

        while (true) {
            long entry = table[index];
            if (entry == EMPTY_KEY) {
                lastReturn = false;
                return false;
            }
            if (entry == key) {
                lastReturn = true;
                return true;
            }

            probe++;
            index = (index + probe * probe) & mask;
        }
    }

    public void add(long key) {
        if (size >= threshold) {
            resize(table.length << 1);
        }

        int index = indexFor(key);
        int probe = 0;

        while (true) {
            long entry = table[index];

            if (entry == EMPTY_KEY) {
                table[index] = key;
                size++;
                lastChecked = EMPTY_KEY;
                return;
            }

            if (entry == key) {
                lastChecked = EMPTY_KEY;
                return;
            }

            probe++;
            index = (index + probe * probe) & mask;
        }
    }

    public void remove(long key) {
        int index = indexFor(key);
        int probe = 0;

        while (true) {
            long entry = table[index];
            if (entry == EMPTY_KEY) return;
            if (entry == key) {
                table[index] = EMPTY_KEY;
                size--;
                lastChecked = EMPTY_KEY;
                return;
            }

            probe++;
            index = (index + probe * probe) & mask;
        }
    }

    private void resize(int newCapacity) {
        long[] oldTable = table;
        this.table = new long[newCapacity];
        Arrays.fill(this.table, EMPTY_KEY);
        this.mask = newCapacity - 1;
        this.threshold = newCapacity >> 2;
        this.size = 0;

        lastChecked = EMPTY_KEY;

        for (long key : oldTable) {
            if (key != EMPTY_KEY) {
                add(key);
            }
        }
    }

    public int count() {
        return size;
    }

    @Override
    public int size() {
        return count();
    }

    @Override
    public boolean isEmpty() {
        return count() == 0;
    }

    @Override
    public boolean contains(final Object o) {
        return o instanceof Long l && containsLong(l);
    }

    @Override
    public @NotNull Iterator<Long> iterator() {
        return new Iterator<>() {
            private int index = 0;
            private int lastReturnedIndex = -1;

            private void advance() {
                while (index < table.length && table[index] == EMPTY_KEY) {
                    index++;
                }
            }

            @Override
            public boolean hasNext() {
                advance();
                return index < table.length;
            }

            @Override
            public Long next() {
                advance();
                if (index >= table.length) {
                    throw new NoSuchElementException();
                }
                lastReturnedIndex = index;
                return table[index++];
            }

            @Override
            public void remove() {
                if (lastReturnedIndex < 0) {
                    throw new IllegalStateException("remove() called before next()");
                }
                table[lastReturnedIndex] = EMPTY_KEY;
                size--;
                lastChecked = EMPTY_KEY;
                lastReturnedIndex = -1;
            }
        };
    }

    @Override
    public boolean add(final Long aLong) {
        add((long) aLong);
        return true;
    }

    @Override
    public boolean remove(final Object o) {
        if (o instanceof Long l) {
            remove((long) l);
        }
        return false;
    }

    @Override
    public boolean containsAll(@NotNull final Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(@NotNull final Collection<? extends Long> c) {
        boolean changed = false;
        for (Long l : c) {
            if (!contains(l)) {
                add(l);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(@NotNull final Collection<?> c) {
        boolean changed = false;
        for (int i = 0; i < table.length; i++) {
            long key = table[i];
            if (key != EMPTY_KEY && !c.contains(key)) {
                table[i] = EMPTY_KEY;
                size--;
                changed = true;
            }
        }
        lastChecked = EMPTY_KEY;
        return changed;
    }

    @Override
    public boolean removeAll(@NotNull final Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            if (o instanceof Long l && containsLong(l)) {
                remove(l);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        Arrays.fill(this.table, EMPTY_KEY);
        this.size = 0;
        lastChecked = EMPTY_KEY;
    }

    // unsupported

    @Override
    public @NotNull Object @NotNull [] toArray() {
        throw new UnsupportedOperationException("toArray not allowed in DCPLS");
    }

    @Override
    public @NotNull <T> T @NotNull [] toArray(@NotNull final T @NotNull [] a) {
        throw new UnsupportedOperationException("toArray not allowed in DCPLS");
    }

}
