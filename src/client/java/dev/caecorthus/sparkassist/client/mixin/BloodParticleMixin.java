package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.config.SparkAssistClientSettings;
import dev.caecorthus.sparkassist.config.BloodAppearance;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Recolours Wathe Blood particles just before they are built. Its sprite is a plain white 16x16 square, so the particle's
 * own tint channels are the entire colour — overriding them yields any hue without shipping a texture.
 * 在 Wathe Blood 粒子构建前重新着色。它的精灵是 16x16 纯白方块，因此粒子自身的着色通道就是全部颜色——覆盖它们即可得到任意
 * 色相，无需附带纹理。
 *
 * <p>{@code @Pseudo} plus a string target keeps the mixin inert when Wathe Blood is absent: nothing here is required for
 * SparkAssist itself to work.
 * {@code @Pseudo} 加字符串目标使 Wathe Blood 缺席时本 mixin 形同不存在：SparkAssist 本身不依赖这里任何东西。</p>
 */
@Pseudo
@Mixin(targets = "wathe_blood.particle.BloodParticle")
public abstract class BloodParticleMixin {
    // The target is a mod class, so its override keeps the intermediary name from the parent it compiles against:
    // Particle#buildGeometry is method_3074 here. A Yarn name would never match at runtime.
    // 目标是模组类，其覆写方法保留着编译时父类的中介名：Particle#buildGeometry 在这里就是 method_3074。用 Yarn 名在运行时
    // 永远匹配不上。
    @Inject(method = "method_3074", at = @At("HEAD"), require = 0)
    private void sparkassist$tintBlood(VertexConsumer buffer, Camera camera, float tickDelta, CallbackInfo ci) {
        BloodAppearance appearance = SparkAssistClientSettings.bloodAppearance();
        float[] tint = appearance.tint();
        if (tint == null) {
            return;
        }
        ParticleAccessorMixin accessor = (ParticleAccessorMixin) (Object) this;
        accessor.sparkassist$setRed(tint[0]);
        accessor.sparkassist$setGreen(tint[1]);
        accessor.sparkassist$setBlue(tint[2]);
        if (appearance == BloodAppearance.OFF) {
            accessor.sparkassist$setAlpha(0.0F);
        }
    }
}
