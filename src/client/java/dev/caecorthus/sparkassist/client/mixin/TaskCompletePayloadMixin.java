package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.achievement.AchievementClientState;
import dev.doctor4t.wathe.util.TaskCompletePayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts Wathe's task-complete notices for achievements. 为成就统计 Wathe 的任务完成通知。 */
@Mixin(value = TaskCompletePayload.Receiver.class, remap = false)
public abstract class TaskCompletePayloadMixin {
    @Inject(
            method = "receive(Ldev/doctor4t/wathe/util/TaskCompletePayload;Lnet/fabricmc/fabric/api/client/networking/v1/ClientPlayNetworking$Context;)V",
            at = @At("HEAD")
    )
    private void sparkassist$taskComplete(CallbackInfo ci) {
        AchievementClientState.onTaskComplete();
    }
}
