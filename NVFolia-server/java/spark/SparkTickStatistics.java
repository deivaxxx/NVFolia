package me.lucko.spark.paper.common.monitor.tick;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import com.google.common.base.Suppliers;
import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.threadedregions.TickData;
import io.papermc.paper.threadedregions.TickRegions;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.paper.common.tick.TickHook;
import me.lucko.spark.paper.common.tick.TickReporter;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

public class SparkTickStatistics implements TickHook.Callback, TickReporter.Callback, TickStatistics {
    // Canvas start - rewrite spark tick statistics
    private final Supplier<List<ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData>>> regionSupplier;

    public SparkTickStatistics() {
        this.regionSupplier = Suppliers.memoizeWithExpiration(() -> getRegions(Bukkit.getServer()), 5, TimeUnit.MILLISECONDS);
    }

    @Override
    public double tps5Sec() {
        return tps(StatisticWindow.TicksPerSecond.SECONDS_5);
    }

    @Override
    public double tps10Sec() {
        return tps(StatisticWindow.TicksPerSecond.SECONDS_10);
    }

    @Override
    public double tps1Min() {
        return tps(StatisticWindow.TicksPerSecond.MINUTES_1);
    }

    @Override
    public double tps5Min() {
        return tps(StatisticWindow.TicksPerSecond.MINUTES_5);
    }

    @Override
    public double tps15Min() {
        return tps(StatisticWindow.TicksPerSecond.MINUTES_15);
    }

    @Override
    public boolean isDurationSupported() {
        return true;
    }

    @Override
    public DoubleAverageInfo duration10Sec() {
        return mspt(StatisticWindow.MillisPerTick.SECONDS_10);
    }

    @Override
    public DoubleAverageInfo duration1Min() {
        return mspt(StatisticWindow.MillisPerTick.MINUTES_1);
    }

    @Override
    public DoubleAverageInfo duration5Min() {
        return mspt(StatisticWindow.MillisPerTick.MINUTES_5);
    }

    private static List<ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData>> getRegions(Server server) {
        List<ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData>> regions = new ArrayList<>();
        for (World world : server.getWorlds()) {
            ThreadedRegionizer<TickRegions.TickRegionData, TickRegions.TickRegionSectionData> regionizer = ((CraftWorld) world).getHandle().regioniser;
            regionizer.computeForAllRegions(regions::add);
        }
        return regions;
    }

    public double tps(StatisticWindow.TicksPerSecond window) {
        long nanoTime = System.nanoTime();
        return this.regionSupplier.get().stream()
            .map(region -> region.getData().getRegionSchedulingHandle())
            .map(handle -> switch (window) {
                case SECONDS_5 -> handle.getTickReport5s(nanoTime);
                case SECONDS_10 -> handle.getTickReport15s(nanoTime); // close enough!
                case MINUTES_1 -> handle.getTickReport1m(nanoTime);
                case MINUTES_5 -> handle.getTickReport5m(nanoTime);
                case MINUTES_15 -> handle.getTickReport15m(nanoTime);
            })
            .filter(Objects::nonNull)
            .mapToDouble(data -> data.tpsData().segmentAll().average())
            .average()
            .orElse(20.0);
    }

    public DoubleAverageInfo mspt(StatisticWindow.MillisPerTick window) {
        long nanoTime = System.nanoTime();
        List<TickData.SegmentedAverage> averages = this.regionSupplier.get().stream()
            .map(region -> region.getData().getRegionSchedulingHandle())
            .map(handle -> switch (window) {
                case SECONDS_10 -> handle.getTickReport15s(nanoTime); // close enough!
                case MINUTES_1 -> handle.getTickReport1m(nanoTime);
                case MINUTES_5 -> handle.getTickReport5m(nanoTime);
            })
            .filter(Objects::nonNull)
            .map(TickData.TickReportData::timePerTickData)
            .toList();
        return new SegmentedDoubleAverageInfo(averages);
    }

    @Override
    public void onTick(final int i) {

    }

    @Override
    public void onTick(final double v) {

    }

    private record SegmentedDoubleAverageInfo(List<TickData.SegmentedAverage> averages) implements DoubleAverageInfo {

        @Override
        public double mean() {
            return this.averages.stream()
                .mapToDouble(avg -> avg.segmentAll().average() / 1.0E6)
                .average()
                .orElse(0);
        }

        @Override
        public double max() {
            return this.averages.stream()
                .mapToDouble(avg -> avg.segmentAll().greatest() / 1.0E6)
                .max()
                .orElse(0);
        }

        @Override
        public double min() {
            return this.averages.stream()
                .mapToDouble(avg -> avg.segmentAll().least() / 1.0E6)
                .min()
                .orElse(0);
        }

        @Override
        public double percentile(double percentile) {
            if (percentile == 0.50d) {
                // median
                return this.averages.stream()
                    .mapToDouble(avg -> avg.segmentAll().median() / 1.0E6)
                    .average()
                    .orElse(0);
            } else if (percentile == 0.95d) {
                // 95th percentile
                return this.averages.stream()
                    .mapToDouble(avg -> avg.segment5PercentWorst().average() / 1.0E6)
                    .average()
                    .orElse(0);
            }

            throw new UnsupportedOperationException("Unsupported percentile: " + percentile);
        }
    }

    // Canvas end
}
