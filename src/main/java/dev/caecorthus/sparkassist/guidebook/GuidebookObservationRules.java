package dev.caecorthus.sparkassist.guidebook;

public final class GuidebookObservationRules {
    private GuidebookObservationRules() {
    }

    /**
     * Spectator sync can contain information the living owner never saw, so polling stops at death.
     * 旁观同步可能包含生前未向本人揭示的信息，因此死亡后必须停止读取。
     */
    public static boolean shouldPollOwnerTraits(
            boolean roundActive,
            boolean dead,
            boolean spectator,
            boolean creative
    ) {
        return roundActive && !dead && !spectator && !creative;
    }
}
