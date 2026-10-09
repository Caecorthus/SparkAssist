package dev.caecorthus.sparkassist.achievement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The local player's owner-visible traits over one round, in round ticks. Each change holds the full set from its
 * tick until the next change; before the first change and after death the set is empty. The client polls traits
 * about once a second, so a change may show up to 20 ticks late.
 * 本地玩家在一局中自己可见的词条随时间的变化，以对局 tick 计。每次变化记录从该 tick 起直到下一次变化的完整集合；
 * 第一次变化之前和死亡之后为空集合。客户端大约每秒读取一次词条，所以变化最多可能晚 20 tick 才出现。
 */
public record TraitTimeline(List<Change> changes) {
    public TraitTimeline {
        changes = List.copyOf(changes);
        for (int i = 1; i < changes.size(); i++) {
            if (changes.get(i).tick() < changes.get(i - 1).tick()) {
                throw new IllegalArgumentException("Trait changes must be in tick order");
            }
        }
    }

    public static TraitTimeline empty() {
        return new TraitTimeline(List.of());
    }

    /** The traits held at round tick {@code tick}. 对局 tick {@code tick} 时持有的词条。 */
    public Set<String> at(long tick) {
        Set<String> held = Set.of();
        for (Change change : changes) {
            if (change.tick() > tick) {
                break;
            }
            held = change.traitIds();
        }
        return held;
    }

    /** Whether the trait was held at any point. 是否在任何时刻持有过该词条。 */
    public boolean everHeld(String traitId) {
        return changes.stream().anyMatch(change -> change.traitIds().contains(traitId));
    }

    /**
     * Whether the trait was held at any moment from {@code fromTick} to {@code toTick}, both included.
     * 从 {@code fromTick} 到 {@code toTick}（含两端）之间的任何时刻是否持有该词条。
     */
    public boolean heldBetween(String traitId, long fromTick, long toTick) {
        if (at(fromTick).contains(traitId)) {
            return true;
        }
        return changes.stream().anyMatch(change -> change.tick() > fromTick && change.tick() <= toTick
                && change.traitIds().contains(traitId));
    }

    /** Every trait held at some point, in first-seen order. 曾经持有过的所有词条，按首次出现顺序。 */
    public Set<String> everHeldIds() {
        Set<String> all = new LinkedHashSet<>();
        changes.forEach(change -> all.addAll(change.traitIds()));
        return Set.copyOf(all);
    }

    /** From {@code tick} on, the player held exactly {@code traitIds}. 从 {@code tick} 起，玩家恰好持有 {@code traitIds}。 */
    public record Change(long tick, Set<String> traitIds) {
        public Change {
            traitIds = Set.copyOf(traitIds);
        }
    }

    /** Collects changes from repeated observations, skipping repeats. 从重复的观察中收集变化，跳过未变化的观察。 */
    public static final class Builder {
        private final List<Change> changes = new ArrayList<>();

        public void observe(long tick, Set<String> traitIds) {
            Set<String> held = traitIds == null ? Set.of() : Set.copyOf(traitIds);
            Set<String> previous = changes.isEmpty() ? Set.of() : changes.getLast().traitIds();
            if (!Objects.equals(previous, held) && (changes.isEmpty() || tick >= changes.getLast().tick())) {
                changes.add(new Change(tick, held));
            }
        }

        public TraitTimeline build() {
            return new TraitTimeline(changes);
        }
    }
}
