package dev.caecorthus.sparkassist.client.charge;

import dev.caecorthus.sparkassist.client.charge.ChargeMeter.Window;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * The held weapons that have to be wound up, and how long each one takes. The indicator works from this list and from
 * nothing else: "is using an item" is also true while aiming a rifle, raising a shield or drinking, none of which is a
 * charge.
 * 需要蓄力的手持武器，以及各自要蓄多久。指示只依据这张清单：「正在使用物品」在举枪瞄准、举盾、喝酒时同样为真，而那些都不是
 * 蓄力。
 *
 * <p>{@code getMaxUseTime} is no help either — it is how long the item may be held (100 ticks for the knife, 7200 for
 * the shuriken, 72000 for the axe), not when a release starts to count. So every item is listed with the number its
 * own release check uses, read at run time from the owning mod's constant where it has one, with the value known today
 * as the fallback. A change in that mod then reaches the indicator without touching this file.
 * {@code getMaxUseTime} 同样帮不上忙——它只是物品最多能按住多久（刀 100 刻、手里剑 7200、飞斧 72000），而不是松手从何时起
 * 有效。因此每件道具都按其自身松手判定所用的数值列出：所属模组有常量时在运行时读取，读不到则用目前已知的数值。那个模组改了
 * 规则，指示会自动跟上，无需改动本文件。</p>
 */
public final class ChargeItems {
    /** Wathe's {@code KnifeItem.onStoppedUsing} needs more than 10 elapsed ticks, so tick 11 is the first that stabs. Wathe 的 {@code KnifeItem.onStoppedUsing} 要求已用刻数超过 10，因此第 11 刻是第一个能出刀的。 */
    private static final int KNIFE_WINDUP_TICKS = 11;
    /**
     * NoellesRoles' {@code ThrowingAxeItem} uses the bow curve over 20 ticks and refuses a throw under 0.25 power,
     * which is 7 ticks; the numbers are literals there, so there is nothing to read.
     * NoellesRoles 的 {@code ThrowingAxeItem} 在 20 刻上套用弓的曲线，并拒绝力度低于 0.25 的投掷，即 7 刻；这些数值在那边是
     * 字面量，没有常量可读。
     */
    private static final int AXE_MIN_TICKS = 7;
    private static final int AXE_FULL_TICKS = 20;

    private static final ModConstant VENDETTA_KNIFE = new ModConstant(
            "dev.caecorthus.sparkwitch.roles.civilian.vendetta.VendettaKnifeRules", "MINIMUM_HOLD_TICKS", 10);
    private static final ModConstant SWORDFISH = new ModConstant(
            "dev.caecorthus.sparkwitch.roles.civilian.fisher.FisherRules", "SWORDFISH_MIN_HOLD_TICKS", 11);
    private static final ModConstant CROSS_SWORD = new ModConstant(
            "dev.caecorthus.sparkwitch.roles.civilian.knight.CrossSwordItem", "MIN_HOLD_TICKS", 11);
    private static final ModConstant SHURIKEN = new ModConstant(
            "dev.caecorthus.sparkwitch.item.ninja.NinjaShurikenItem", "MIN_CHARGE_TICKS", 4);
    private static final ModConstant M67 = new ModConstant(
            "annina.sparkstrength.item.m67.M67Rules", "CHARGE_TICKS", 20);

    /** Looked up once per item instance; the map also remembers the items that are not on the list. 每个物品实例只查一次；不在清单上的物品同样被记住。 */
    private static final Map<Item, Optional<Kind>> KINDS = new IdentityHashMap<>();

    private ChargeItems() {
    }

    /** The charged weapon this item is, or {@code null} for everything else. 该物品对应的蓄力武器；其他物品返回 {@code null}。 */
    public static Kind kindOf(Item item) {
        return KINDS.computeIfAbsent(item, key -> Optional.ofNullable(Kind.byId(Registries.ITEM.getId(key))))
                .orElse(null);
    }

    /** How long {@code player} has to hold this weapon, traits included. {@code player} 需要按住这件武器多久，已计入词条。 */
    public static Window window(Kind kind, PlayerEntity player, ItemStack stack) {
        return switch (kind) {
            case KNIFE -> Window.of(SparkTraitsCharge.knifeWindup(player, stack, KNIFE_WINDUP_TICKS));
            case VENDETTA_KNIFE -> Window.of(VENDETTA_KNIFE.value());
            case SWORDFISH -> Window.of(SWORDFISH.value());
            case CROSS_SWORD -> Window.of(CROSS_SWORD.value());
            case SHURIKEN -> Window.of(serverTimed(SparkTraitsCharge.throwCharge(player, SHURIKEN.value())));
            case M67 -> Window.of(serverTimed(SparkTraitsCharge.throwCharge(player, M67.value())));
            case THROWING_AXE -> new Window(
                    serverTimed(SparkTraitsCharge.throwCharge(player, AXE_MIN_TICKS)),
                    serverTimed(SparkTraitsCharge.throwCharge(player, AXE_FULL_TICKS)));
        };
    }

    /**
     * One tick of slack for the items whose hold is counted on the server alone. The client's count can be a tick ahead
     * of the server's, and a release the server finds short is not free: the M67 answers with a 40-tick cooldown.
     * 为仅由服务端计时的道具留一刻余量。客户端的计数可能比服务端快一刻，而被服务端判定为不足的松手并非没有代价：M67 会因此
     * 进入 40 刻冷却。
     */
    private static int serverTimed(int ticks) {
        return ticks + 1;
    }

    /** The weapons the indicator covers. 指示所覆盖的武器。 */
    public enum Kind {
        KNIFE("wathe", "knife"),
        /** Extends Wathe's knife but replaces the release check with its own. 继承自 Wathe 的刀，但用自己的松手判定替换了原判定。 */
        VENDETTA_KNIFE("sparkwitch", "vendetta_knife"),
        SWORDFISH("sparkwitch", "swordfish"),
        CROSS_SWORD("sparkwitch", "cross_sword"),
        SHURIKEN("sparkwitch", "ninja_shuriken"),
        M67("sparkstrength", "m67"),
        THROWING_AXE("noellesroles", "throwing_axe");

        private final Identifier id;

        Kind(String namespace, String path) {
            this.id = Identifier.of(namespace, path);
        }

        public Identifier id() {
            return id;
        }

        static Kind byId(Identifier id) {
            for (Kind kind : values()) {
                if (kind.id.equals(id)) {
                    return kind;
                }
            }
            return null;
        }
    }
}
