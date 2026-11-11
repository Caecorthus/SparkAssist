package dev.caecorthus.sparkassist.client.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the tint channels of every particle so the blood mixin can recolour a subclass whose own fields are not
 * reachable. Accessors avoid touching the render path.
 * 暴露所有粒子的着色通道，使血液 mixin 能给无法直接访问字段的子类重新上色。访问器不触碰渲染路径。
 */
@Mixin(Particle.class)
public interface ParticleAccessorMixin {
    @Accessor("red")
    float sparkassist$getRed();

    @Accessor("green")
    float sparkassist$getGreen();

    @Accessor("blue")
    float sparkassist$getBlue();

    @Accessor("red")
    void sparkassist$setRed(float red);

    @Accessor("green")
    void sparkassist$setGreen(float green);

    @Accessor("blue")
    void sparkassist$setBlue(float blue);
}
