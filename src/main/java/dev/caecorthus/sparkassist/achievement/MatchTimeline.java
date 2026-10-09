package dev.caecorthus.sparkassist.achievement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Queries over one settled round's match record, built once per round. Events keep record order (Wathe's sequence
 * order) and record ticks (world ticks since the server started the match). Every query tolerates a missing record
 * (old servers send none, so the timeline is simply empty) and missing fields (servers without a given mod).
 * <p>
 * Roles come from Wathe's {@code role_assigned} snapshot plus SparkFactionAPI's {@code role_changed} events, so
 * {@link #roleAt} is the role a player held when an event happened, not their final role. Factions are effective
 * factions where the record carries them (a {@code death} has {@code victim_faction} / {@code killer_faction}),
 * otherwise the base faction of the role held at that time.
 * <p>
 * 一局已结算对局的记录查询，每局只构建一次。事件保持记录顺序（Wathe 序号顺序）和记录 tick（服务端开始对局后的世界 tick）。
 * 所有查询都能容忍没有记录（旧服务器不发送，时间线为空）和缺字段（服务器没装对应模组）。
 * 身份取自 Wathe 的 {@code role_assigned} 快照加上 SparkFactionAPI 的 {@code role_changed} 事件，所以 {@link #roleAt}
 * 是事件发生时玩家的身份，而不是最终身份。阵营在记录带有时取实际阵营（{@code death} 带 {@code victim_faction} /
 * {@code killer_faction}），否则取当时身份的基础阵营。
 */
public final class MatchTimeline {
    private final List<MatchEvent> events;
    private final Map<MatchEvent, Integer> positions = new IdentityHashMap<>();
    private final Function<String, String> baseFactionOfRole;
    private final Map<String, String> resolvedFactions = new HashMap<>();
    private final Map<String, String> watheFactions = new HashMap<>();
    private final Map<UUID, String> openingRoles = new LinkedHashMap<>();
    private final Map<UUID, List<RoleChange>> roleChanges = new HashMap<>();
    private final Map<UUID, List<TickWindow>> psychoWindows = new HashMap<>();
    private final int endTick;

    private MatchTimeline(List<MatchEvent> events, Function<String, String> baseFactionOfRole) {
        this.events = List.copyOf(events);
        this.baseFactionOfRole = Objects.requireNonNull(baseFactionOfRole, "baseFactionOfRole");
        int last = 0;
        for (int i = 0; i < this.events.size(); i++) {
            MatchEvent event = this.events.get(i);
            positions.putIfAbsent(event, i);
            last = Math.max(last, event.tick());
            readRoles(event, i);
        }
        this.endTick = last;
        readPsycho();
        psychoWindows.replaceAll((player, windows) -> List.copyOf(windows));
    }

    /**
     * Builds the timeline of {@code events} (in record order). {@code baseFactionOfRole} maps a role id to its base
     * faction id, or null when unknown; Wathe's faction names in {@code role_assigned} are the fallback.
     * 根据 {@code events}（按记录顺序）构建时间线。{@code baseFactionOfRole} 把身份 id 映射为基础阵营 id，未知时返回 null；
     * {@code role_assigned} 中 Wathe 的阵营名作为后备。
     */
    public static MatchTimeline of(List<MatchEvent> events, Function<String, String> baseFactionOfRole) {
        return new MatchTimeline(events, baseFactionOfRole);
    }

    /** A round without a match record. 没有对局记录的一局。 */
    public static MatchTimeline empty() {
        return new MatchTimeline(List.of(), role -> null);
    }

    // ---- Events 事件 ----

    /** Every event, in record order. 全部事件，按记录顺序。 */
    public List<MatchEvent> events() {
        return events;
    }

    /** Whether the round has no match record. 本局是否没有对局记录。 */
    public boolean isEmpty() {
        return events.isEmpty();
    }

    /** The latest record tick, 0 without a record. 最晚的记录 tick；没有记录时为 0。 */
    public int endTick() {
        return endTick;
    }

    public List<MatchEvent> events(String type) {
        return events.stream().filter(event -> event.is(type)).toList();
    }

    /** Events of {@code type} done by {@code actor}. {@code actor} 执行的 {@code type} 事件。 */
    public List<MatchEvent> eventsBy(String type, UUID actor) {
        return events.stream().filter(event -> event.is(type) && Objects.equals(actor, event.actor())).toList();
    }

    /** Events of {@code type} done to {@code target}. 作用于 {@code target} 的 {@code type} 事件。 */
    public List<MatchEvent> eventsOn(String type, UUID target) {
        return events.stream().filter(event -> event.is(type) && Objects.equals(target, event.target())).toList();
    }

    /**
     * Splits {@code events} into groups that share a tick, in first-appearance order, e.g. deaths that happened at
     * the same moment.
     * 把 {@code events} 按相同 tick 分组，按首次出现顺序排列，例如同一时刻发生的多起死亡。
     */
    public static List<List<MatchEvent>> groupByTick(List<MatchEvent> events) {
        Map<Integer, List<MatchEvent>> groups = new LinkedHashMap<>();
        for (MatchEvent event : events) {
            groups.computeIfAbsent(event.tick(), tick -> new ArrayList<>()).add(event);
        }
        return groups.values().stream().map(List::copyOf).toList();
    }

    // ---- Roles and factions 身份与阵营 ----

    /** The role from Wathe's opening snapshot, or null. Wathe 开局快照中的身份，没有则为 null。 */
    public String openingRole(UUID player) {
        String opening = openingRoles.get(player);
        if (opening != null) {
            return opening;
        }
        List<RoleChange> changes = roleChanges.getOrDefault(player, List.of());
        return changes.isEmpty() ? null : changes.getFirst().from();
    }

    /** The role after every recorded change, or null. 所有记录的变化之后的身份，没有则为 null。 */
    public String finalRole(UUID player) {
        return roleBefore(player, Integer.MAX_VALUE);
    }

    /** The opening role followed by each new role, without repeats in a row. 开局身份及之后每次变化的新身份，相邻不重复。 */
    public List<String> roleHistory(UUID player) {
        List<String> history = new ArrayList<>();
        addRole(history, openingRole(player));
        for (RoleChange change : roleChanges.getOrDefault(player, List.of())) {
            addRole(history, change.to());
        }
        return List.copyOf(history);
    }

    /**
     * The role {@code player} held when {@code event} happened: for a death, the recorded {@code victim_role} /
     * {@code killer_role} of that player when present, otherwise the role just before the event (a
     * {@code role_changed} event itself still shows its "from" role). Null when unknown.
     * {@code event} 发生时 {@code player} 的身份：死亡事件优先取该玩家记录的 {@code victim_role} / {@code killer_role}，
     * 否则取事件发生前一刻的身份（{@code role_changed} 事件本身仍显示变化前的身份）。未知时为 null。
     *
     * @throws IllegalArgumentException when {@code event} is not part of this timeline 事件不属于本时间线时
     */
    public String roleAt(UUID player, MatchEvent event) {
        int position = position(event);
        if (player == null) {
            return null;
        }
        if (event.is(MatchEvent.DEATH)) {
            String recorded = player.equals(event.target()) ? event.value("victim_role", null)
                    : player.equals(event.actor()) ? event.value("killer_role", null)
                    : null;
            if (recorded != null) {
                return recorded;
            }
        }
        return roleBefore(player, position);
    }

    /**
     * {@code player}'s faction when {@code event} happened: for a death, the recorded effective
     * {@code victim_faction} / {@code killer_faction} when present, otherwise the base faction of
     * {@link #roleAt}. Null when unknown.
     * {@code event} 发生时 {@code player} 的阵营：死亡事件优先取记录的实际阵营 {@code victim_faction} / {@code killer_faction}，
     * 否则取 {@link #roleAt} 身份的基础阵营。未知时为 null。
     */
    public String factionAt(UUID player, MatchEvent event) {
        if (player != null && event.is(MatchEvent.DEATH)) {
            String recorded = player.equals(event.target()) ? event.value("victim_faction", null)
                    : player.equals(event.actor()) ? event.value("killer_faction", null)
                    : null;
            if (recorded != null) {
                return recorded;
            }
        }
        return baseFaction(roleAt(player, event));
    }

    /** The base faction of a role id, or null when unknown. 身份 id 的基础阵营，未知时为 null。 */
    public String baseFaction(String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return null;
        }
        if (!resolvedFactions.containsKey(roleId)) {
            String faction = baseFactionOfRole.apply(roleId);
            resolvedFactions.put(roleId, faction == null || faction.isBlank() ? watheFactions.get(roleId) : faction);
        }
        return resolvedFactions.get(roleId);
    }

    // ---- Deaths 死亡 ----

    public List<MatchEvent> deaths() {
        return events(MatchEvent.DEATH);
    }

    /** The first recorded death of {@code player}. {@code player} 第一次被记录的死亡。 */
    public Optional<MatchEvent> deathOf(UUID player) {
        return events.stream()
                .filter(event -> event.is(MatchEvent.DEATH) && Objects.equals(player, event.target()))
                .findFirst();
    }

    /**
     * Deaths {@code killer} caused, in order; dying by your own hand (e.g. a gun backfire, actor = target) is not a
     * kill.
     * {@code killer} 造成的死亡，按时间顺序；死于自己之手（例如枪械走火，actor = target）不算击杀。
     */
    public List<MatchEvent> killsBy(UUID killer) {
        return events.stream()
                .filter(event -> event.is(MatchEvent.DEATH) && killer != null
                        && killer.equals(event.actor()) && !killer.equals(event.target()))
                .toList();
    }

    /** The victim's role at this death. 死者在这次死亡时的身份。 */
    public String victimRole(MatchEvent death) {
        return roleAt(death.target(), death);
    }

    /** The victim's effective faction at this death. 死者在这次死亡时的实际阵营。 */
    public String victimFaction(MatchEvent death) {
        return death.target() == null ? null : factionAt(death.target(), death);
    }

    /** The killer's role at this death, or null without a killer. 击杀者在这次死亡时的身份；没有击杀者时为 null。 */
    public String killerRole(MatchEvent death) {
        return roleAt(death.actor(), death);
    }

    /** The killer's effective faction at this death, or null. 击杀者在这次死亡时的实际阵营，没有则为 null。 */
    public String killerFaction(MatchEvent death) {
        return death.actor() == null ? null : factionAt(death.actor(), death);
    }

    // ---- Psycho mode 疯魔模式 ----

    /**
     * When {@code player} was in psycho mode (any source), from {@code sparkfactionapi:psycho} events. A window that
     * never ended closes at the player's death or at the end of the record.
     * {@code player} 处于疯魔模式（任何来源）的时段，取自 {@code sparkfactionapi:psycho} 事件。没有结束的时段在玩家死亡或记录结束时关闭。
     */
    public List<TickWindow> psychoWindows(UUID player) {
        return psychoWindows.getOrDefault(player, List.of());
    }

    /** Whether {@code player} was in psycho mode at record tick {@code tick}. {@code player} 在记录 tick {@code tick} 时是否处于疯魔模式。 */
    public boolean inPsycho(UUID player, int tick) {
        return psychoWindows(player).stream().anyMatch(window -> window.contains(tick));
    }

    /**
     * An inclusive span of record ticks. 一段记录 tick（含两端）。
     */
    public record TickWindow(int fromTick, int toTick) {
        public boolean contains(int tick) {
            return tick >= fromTick && tick <= toTick;
        }

        public int length() {
            return toTick - fromTick;
        }
    }

    // ---- Building 构建 ----

    private record RoleChange(int position, String from, String to) {
    }

    private void readRoles(MatchEvent event, int position) {
        if (event.is(MatchEvent.ROLE_ASSIGNED)) {
            UUID player = event.uuid("player.uuid");
            String role = event.value("player.role", null);
            if (player != null && role != null) {
                openingRoles.putIfAbsent(player, role);
                String faction = Factions.fromWatheName(event.value("player.faction"));
                if (faction != null) {
                    watheFactions.putIfAbsent(role, faction);
                }
            }
        } else if (event.is(MatchEvent.ROLE_CHANGED)) {
            UUID player = event.uuid("player");
            if (player == null) {
                player = event.actor();
            }
            String to = event.value("to", null);
            if (player != null && to != null) {
                roleChanges.computeIfAbsent(player, key -> new ArrayList<>())
                        .add(new RoleChange(position, event.value("from", null), to));
            }
        }
    }

    private void readPsycho() {
        Map<UUID, Integer> openSince = new LinkedHashMap<>();
        for (MatchEvent event : events) {
            if (event.is(MatchEvent.PSYCHO) && event.actor() != null && event.has("active")) {
                if (event.bool("active")) {
                    openSince.putIfAbsent(event.actor(), event.tick());
                } else {
                    close(openSince, event.actor(), event.tick());
                }
            } else if (event.is(MatchEvent.DEATH) && event.target() != null) {
                close(openSince, event.target(), event.tick());
            }
        }
        for (UUID player : List.copyOf(openSince.keySet())) {
            close(openSince, player, endTick);
        }
    }

    private void close(Map<UUID, Integer> openSince, UUID player, int tick) {
        Integer from = openSince.remove(player);
        if (from != null) {
            psychoWindows.computeIfAbsent(player, key -> new ArrayList<>()).add(new TickWindow(from, Math.max(from, tick)));
        }
    }

    private String roleBefore(UUID player, int position) {
        String role = openingRole(player);
        for (RoleChange change : roleChanges.getOrDefault(player, List.of())) {
            if (change.position() >= position) {
                break;
            }
            role = change.to();
        }
        return role;
    }

    private int position(MatchEvent event) {
        Integer position = positions.get(Objects.requireNonNull(event, "event"));
        if (position != null) {
            return position;
        }
        int index = events.indexOf(event);
        if (index < 0) {
            throw new IllegalArgumentException("Event is not part of this match timeline: " + event.type());
        }
        return index;
    }

    private static void addRole(List<String> history, String role) {
        if (role != null && (history.isEmpty() || !history.getLast().equals(role))) {
            history.add(role);
        }
    }
}
