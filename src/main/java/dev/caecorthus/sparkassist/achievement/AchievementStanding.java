package dev.caecorthus.sparkassist.achievement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * How an achievement stands in the place the player is in now. Each place earns an achievement once: {@link #HERE}
 * when this place has, {@link #ELSEWHERE} when only other places have. 微小胜利 ({@link AchievementGalaxy#CENTER},
 * "earn every other achievement") can also be expired: an unlock of it expires once another achievement in the
 * catalog was first earned after it, or never (one added since), and stays expired until that place earns it again.
 * 成就在玩家当前所在地点的状态。每个地点各能获得一次同一成就：本地点获得过为 {@link #HERE}，只有别的地点获得过为
 * {@link #ELSEWHERE}。微小胜利（{@link AchievementGalaxy#CENTER}，“达成其他所有成就”）还会过期：只要目录里有其他成就在它之后
 * 才首次获得，或从未获得（之后新增的），这条解锁就过期了，直到该地点再次获得它为止。
 */
public enum AchievementStanding {
    NOT_EARNED,
    HERE,
    ELSEWHERE,
    EXPIRED_HERE,
    EXPIRED_ELSEWHERE;

    public boolean here() {
        return this == HERE || this == EXPIRED_HERE;
    }

    public boolean expired() {
        return this == EXPIRED_HERE || this == EXPIRED_ELSEWHERE;
    }

    /**
     * The standing of every achievement in {@code catalog} that was earned somewhere, by id, seen from {@code here}
     * (null when unknown).
     * 从 {@code here}（未知时为 null）看，{@code catalog} 中每个在某处获得过的成就的状态，按 id 索引。
     */
    public static Map<String, AchievementStanding> of(
            List<Achievement> catalog, AchievementLedger ledger, AchievementLedger.Place here) {
        Unlocks unlocks = new Unlocks(catalog, ledger);
        Map<String, AchievementStanding> standings = new LinkedHashMap<>();
        for (Achievement achievement : catalog) {
            AchievementStanding standing = unlocks.standing(achievement.id(), here);
            if (standing != NOT_EARNED) {
                standings.put(achievement.id(), standing);
            }
        }
        return standings;
    }

    /**
     * The achievements {@code here} has earned and that have not expired: those it cannot earn again.
     * {@code here} 已获得且未过期的成就，也就是在这里不能再获得的成就。
     */
    public static Set<String> earnedAt(List<Achievement> catalog, AchievementLedger ledger, AchievementLedger.Place here) {
        Set<String> earned = new HashSet<>();
        of(catalog, ledger, here).forEach((id, standing) -> {
            if (standing == HERE) {
                earned.add(id);
            }
        });
        return earned;
    }

    private static final class Unlocks {
        private final Map<String, List<AchievementLedger.Unlock>> byId = new HashMap<>();
        // When the last of the others was first earned; an unlock of the center older than this has expired.
        // 其他成就中最晚首次获得的时间；早于此时间的微小胜利解锁已过期。
        private final long othersCompleteAt;

        Unlocks(List<Achievement> catalog, AchievementLedger ledger) {
            for (AchievementLedger.Unlock unlock : ledger.unlocks()) {
                byId.computeIfAbsent(unlock.id(), id -> new ArrayList<>()).add(unlock);
            }
            long completeAt = Long.MIN_VALUE;
            for (Achievement achievement : catalog) {
                if (achievement.id().equals(AchievementGalaxy.CENTER)) {
                    continue;
                }
                List<AchievementLedger.Unlock> unlocks = byId.get(achievement.id());
                if (unlocks == null) {
                    completeAt = Long.MAX_VALUE;
                    break;
                }
                long first = unlocks.stream().mapToLong(AchievementLedger.Unlock::epochMillis).min().orElseThrow();
                completeAt = Math.max(completeAt, first);
            }
            othersCompleteAt = completeAt;
        }

        AchievementStanding standing(String id, AchievementLedger.Place here) {
            List<AchievementLedger.Unlock> unlocks = byId.get(id);
            if (unlocks == null) {
                return NOT_EARNED;
            }
            for (AchievementLedger.Unlock unlock : unlocks) {
                if (Objects.equals(unlock.place(), here)) {
                    return expired(unlock) ? EXPIRED_HERE : HERE;
                }
            }
            AchievementLedger.Unlock latest = unlocks.stream()
                    .max(Comparator.comparingLong(AchievementLedger.Unlock::epochMillis))
                    .orElseThrow();
            return expired(latest) ? EXPIRED_ELSEWHERE : ELSEWHERE;
        }

        private boolean expired(AchievementLedger.Unlock unlock) {
            return unlock.id().equals(AchievementGalaxy.CENTER) && unlock.epochMillis() < othersCompleteAt;
        }
    }
}
