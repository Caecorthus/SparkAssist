package dev.caecorthus.sparkassist.client.ponder;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;

/**
 * Actor colours straight from the roles' own {@code Role.color()}, which every Spark mod registers through Wathe;
 * the fallback covers a role whose mod is missing. NoellesRoles colours carry an alpha byte, hence the mask.
 * 演员颜色直接取自各身份自己的 Role.color()（所有 Spark 模组都通过 Wathe 注册身份）；对应模组缺失时用备用色。
 * NoellesRoles 的颜色带透明度字节，所以要截掉。
 */
final class RoleColors {
    static final int KILLER = of("wathe:killer", 0xC13838);
    static final int CIVILIAN = of("wathe:civilian", 0x36E51B);
    static final int VIGILANTE = of("wathe:vigilante", 0x1B8AE5);
    static final int VETERAN = of("wathe:veteran", 0x4A7023);
    static final int LOOSE_END = of("wathe:loose_end", 0x9F0000);

    private RoleColors() {
    }

    static int of(String roleId, int fallback) {
        Role role = WatheRoles.getRole(Identifier.of(roleId));
        return role == null ? fallback : role.color() & 0xFFFFFF;
    }
}
