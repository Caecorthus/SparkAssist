package dev.caecorthus.sparkassist.client.mixin;

import java.util.Map;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.client.network.ClientAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the achievement page give its entries an obtained progress. 让成就页为其条目写入已完成的进度。 */
@Mixin(ClientAdvancementManager.class)
public interface ClientAdvancementManagerAccessor {
    @Accessor("advancementProgresses")
    Map<AdvancementEntry, AdvancementProgress> sparkassist$progresses();

    @Accessor("listener")
    ClientAdvancementManager.Listener sparkassist$listener();
}
