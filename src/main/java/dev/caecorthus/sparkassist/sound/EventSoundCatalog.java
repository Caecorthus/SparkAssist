package dev.caecorthus.sparkassist.sound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Identifier;

/**
 * Catalog Implementation for optional sibling-mod event sounds.
 * 可选同级 Mod 事件声音的目录实现。
 *
 * Dotted event ids and slash resource ids stay separate here, while
 * EventSoundVolumeRules remains the stable public lookup Interface.
 * 点号事件 id 和斜杠资源 id 在这里分列维护，EventSoundVolumeRules 保持稳定的公开查询接口。
 */
final class EventSoundCatalog {
    private final Map<Identifier, EventSoundGroup> eventIdGroups;
    private final Map<Identifier, EventSoundGroup> resourceIdGroups;

    private EventSoundCatalog(List<CatalogGroup> groups) {
        Map<Identifier, EventSoundGroup> eventIdGroups = new HashMap<>();
        Map<Identifier, EventSoundGroup> resourceIdGroups = new HashMap<>();
        for (CatalogGroup group : groups) {
            group.indexInto(eventIdGroups, resourceIdGroups);
        }
        this.eventIdGroups = Map.copyOf(eventIdGroups);
        this.resourceIdGroups = Map.copyOf(resourceIdGroups);
    }

    static EventSoundCatalog createDefault() {
        return new EventSoundCatalog(List.of(
                group(EventSoundGroup.PSYCHO_MODE,
                        eventIds("wathe",
                                "ambient.psycho_drone"
                        ),
                        resourceIds("wathe",
                                "ambient/psycho_drone"
                        )
                ),
                group(EventSoundGroup.CORRUPT_COP_MOMENT,
                        eventIds("noellesroles",
                                "ambient.corrupt_cop_execution",
                                "music.corrupt_cop_moment_1",
                                "music.corrupt_cop_moment_2"
                        ),
                        resourceIds("noellesroles",
                                "ambient/manba_out",
                                "ambient/kemiao",
                                "ambient/boyi",
                                "ambient/donk"
                        )
                ),
                group(EventSoundGroup.JESTER_MOMENT,
                        eventIds("noellesroles",
                                "ambient.jester_laugh",
                                "music.jester_moment"
                        ),
                        resourceIds("noellesroles",
                                "ambient/jester_laugh",
                                "ambient/dbd_jester"
                        )
                ),
                group(EventSoundGroup.TRAIN_OUTSIDE,
                        eventIds("wathe",
                                "ambient.train.outside"
                        ),
                        resourceIds("wathe",
                                "ambient/train_outside"
                        )
                ),
                group(EventSoundGroup.TRAIN_HORN,
                        eventIds("wathe",
                                "ambient.train.horn",
                                "ambient.ship.outside"
                        ),
                        resourceIds("wathe",
                                "ambient/train_horn",
                                "ambient/ship_outside"
                        )
                ),
                group(EventSoundGroup.PIG_CHASE,
                        eventIds("sparkwitch",
                                "skill.pig_chase"
                        ),
                        resourceIds("sparkwitch",
                                "skill/pig_chase"
                        )
                ),
                group(EventSoundGroup.ARROGANT_ASF_MUSIC,
                        eventIds("sparktraits",
                                "music.takediskrush"
                        ),
                        resourceIds("sparktraits",
                                "music/takediskrush"
                        )
                ),
                group(EventSoundGroup.GRAND_WITCH_CEREMONIAL_SWORD_BGM,
                        eventIds("sparkwitch",
                                "ambient.grand_witch_ceremonial_sword_bgm"
                        ),
                        resourceIds("sparkwitch",
                                "ambient/grand_witch_ceremonial_sword_bgm"
                        )
                ),
                group(EventSoundGroup.DEPRESSION_PSYCHO_RANGE,
                        eventIds("sparktraits",
                                "depression.docile_to_rage",
                                "depression.rage_loop",
                                "depression.melee_kill_1",
                                "depression.melee_kill_2",
                                "depression.rage_to_docile",
                                "depression.shyguy_killed"
                        ),
                        resourceIds("sparktraits",
                                "depression/docile_to_rage",
                                "depression/rage_loop",
                                "depression/melee_kill_1",
                                "depression/melee_kill_2",
                                "depression/rage_to_docile",
                                "depression/shyguy_killed"
                        )
                ),
                group(EventSoundGroup.DEPRESSION_PSYCHO_MUSIC,
                        eventIds("sparktraits",
                                "depression.blind_rage_enrage",
                                "depression.blind_rage_chase"
                        ),
                        resourceIds("sparktraits",
                                "depression/blind_rage_enrage",
                                "depression/blind_rage_chase"
                        )
                ),
                group(EventSoundGroup.DEPRESSION_PSYCHO_ALERT,
                        eventIds("sparktraits",
                                "depression.player_was_seen"
                        ),
                        resourceIds("sparktraits",
                                "depression/player_was_seen"
                        )
                )
        ));
    }

    boolean contains(Identifier id) {
        return groupFor(id) != null;
    }

    EventSoundGroup groupFor(Identifier id) {
        if (id == null) {
            return null;
        }
        EventSoundGroup eventGroup = eventIdGroups.get(id);
        if (eventGroup != null) {
            return eventGroup;
        }
        return resourceIdGroups.get(id);
    }

    private static CatalogGroup group(
            EventSoundGroup group,
            List<Identifier> eventIds,
            List<Identifier> resourceIds
    ) {
        return new CatalogGroup(group, eventIds, resourceIds);
    }

    private static List<Identifier> eventIds(String namespace, String... paths) {
        return identifiers(namespace, paths);
    }

    private static List<Identifier> resourceIds(String namespace, String... paths) {
        return identifiers(namespace, paths);
    }

    private static List<Identifier> identifiers(String namespace, String... paths) {
        List<Identifier> ids = new ArrayList<>(paths.length);
        for (String path : paths) {
            ids.add(Identifier.of(namespace, path));
        }
        return List.copyOf(ids);
    }

    private record CatalogGroup(
            EventSoundGroup group,
            List<Identifier> eventIds,
            List<Identifier> resourceIds
    ) {
        private CatalogGroup {
            eventIds = List.copyOf(eventIds);
            resourceIds = List.copyOf(resourceIds);
        }

        private void indexInto(
                Map<Identifier, EventSoundGroup> eventIdGroups,
                Map<Identifier, EventSoundGroup> resourceIdGroups
        ) {
            for (Identifier eventId : eventIds) {
                eventIdGroups.put(eventId, group);
            }
            for (Identifier resourceId : resourceIds) {
                resourceIdGroups.put(resourceId, group);
            }
        }
    }
}
