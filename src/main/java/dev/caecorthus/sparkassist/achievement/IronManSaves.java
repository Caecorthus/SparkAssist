package dev.caecorthus.sparkassist.achievement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lethal hits the Professor's Iron Man Vial blocked for other players. Noelle's Roles cancels the kill, writes
 * {@code iron_man_activated} (actor = the protected player) and removes the buff, so each block is one player saved
 * from death. A block is credited to whoever last used a vial on that player, if it was me as the Professor; a block
 * the victim dies from in the same tick (the Ceremonial Sword or the AXMC piercing it) saved nobody.
 * 教授的铁人药剂为其他玩家挡下的致命攻击。Noelle's Roles 会取消这次击杀、写下 {@code iron_man_activated}
 * （actor 为被保护者）并移除增益，因此每次格挡就是让一名玩家免于一死。格挡记在最后对该玩家使用药剂的人名下，
 * 前提是那是以教授身份的我；同一 tick 内被保护者仍然死亡的格挡（被仪礼剑或 AXMC 穿透）没有救下任何人。
 */
final class IronManSaves {
    static final String VIAL = "noellesroles:iron_man_vial";
    static final String ACTIVATED = "iron_man_activated";
    static final String PROFESSOR = "noellesroles:professor";

    private IronManSaves() {
    }

    /** How many players my vials saved this round. 本局我的药剂救下的玩家次数。 */
    static int byMe(MatchTimeline timeline, UUID self) {
        if (self == null) {
            return 0;
        }
        Map<UUID, MatchEvent> latestVial = new HashMap<>();
        List<MatchEvent> deaths = timeline.deaths();
        int saves = 0;
        for (MatchEvent event : timeline.events()) {
            if (event.is(MatchEvent.ITEM_USE) && VIAL.equals(event.value("item")) && event.target() != null) {
                latestVial.put(event.target(), event);
            } else if (event.is(ACTIVATED) && event.actor() != null && !event.actor().equals(self)) {
                MatchEvent vial = latestVial.get(event.actor());
                boolean pierced = deaths.stream().anyMatch(death -> death.tick() == event.tick()
                        && event.actor().equals(death.target()));
                if (vial != null && self.equals(vial.actor()) && PROFESSOR.equals(timeline.roleAt(self, vial))
                        && !pierced) {
                    saves++;
                }
            }
        }
        return saves;
    }
}
