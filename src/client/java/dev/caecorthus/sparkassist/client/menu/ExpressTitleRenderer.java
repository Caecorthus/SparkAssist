package dev.caecorthus.sparkassist.client.menu;

import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.*;

import dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPaint;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * Draws the SparkDLC title screen chrome over Wathe's own panorama: the hung station sign, the track along the bottom,
 * the two site links, the installed SparkDLC family list and the version strip below the rails.
 * 在 Wathe 自带的全景图之上绘制 SparkDLC 主菜单装饰：悬挂站牌、底部铁轨、两个官网链接、已安装的 SparkDLC 系列列表，
 * 以及铁轨下方的版本条。
 *
 * <p>The panorama is drawn by the caller through vanilla's {@code renderPanoramaBackground}, so the background stays the
 * rotating Wathe scene instead of a flat fill; this renderer only adds the scrim and the chrome.
 * 全景图由调用方通过原版 {@code renderPanoramaBackground} 绘制，因此背景仍是旋转的 Wathe 场景而非平涂；本渲染器只叠加
 * 压暗层与装饰。</p>
 */
public final class ExpressTitleRenderer {
    /** Heading scale on the station sign. 站牌标题的放大倍数。 */
    private static final float HEADING_SCALE = 2.0F;
    /** Clearance kept between the bottom of the sign and the first button row. 站牌底部与首行按钮之间保留的空隙。 */
    private static final int BUTTON_CLEARANCE = 18;
    /** Room reserved for the two site lines above the track. 铁轨上方留给两行官网信息的空间。 */
    private static final int SITE_BAND = 26;
    /** SparkDLC family mods, in the order they are listed. SparkDLC 系列模组，按显示顺序排列。 */
    private static final String[] FAMILY = {
            "sparkassist", "sparkfactionapi", "sparktraits", "sparkwitch", "sparkstrength"
    };
    /** Shown without the scheme so the block stays inside its half of the screen. 显示时去掉协议头，使整块不越出半屏。 */
    private static final String DLC_SITE = "express-dlc.tlspark.cn";
    private static final String MAIN_SITE = "express.tlspark.cn";
    private static final String DLC_URL = "https://express-dlc.tlspark.cn/";
    private static final String MAIN_URL = "https://express.tlspark.cn/";

    private static String cachedVersion;
    private static List<String> cachedFamily;

    private ExpressTitleRenderer() {
    }

    /**
     * Renders the title screen chrome over an already-painted panorama.
     * 在已绘制的全景图之上渲染主菜单装饰。
     *
     * @param gameVersion vanilla version line shown below the rails 铁轨下方显示的原版版本行
     */
    public static void render(DrawContext context, TextRenderer font, int width, int height, String gameVersion) {
        double seconds = Util.getMeasuringTimeMs() / 1000.0D;
        int cx = width / 2;
        int firstButtonY = height / 4 + 48;
        int signCy = Math.min(Math.round(height * 0.15F), firstButtonY - ExpressMenuPaint.SIGN_HEIGHT / 2
                - BUTTON_CLEARANCE);
        signCy = Math.max(signCy, ExpressMenuPaint.SIGN_HEIGHT / 2 + 6);
        int signTop = signCy - ExpressMenuPaint.SIGN_HEIGHT / 2;

        // Scrim only over the two bands the chrome occupies, so the panorama stays visible in the middle.
        // 仅在装饰占据的上下两条带上压暗，让中段全景保持可见。
        ExpressMenuPaint.scrim(context, width, height, Math.max(60, signTop + ExpressMenuPaint.SIGN_HEIGHT + 8));

        rod(context, cx - ExpressMenuPaint.SIGN_WIDTH / 2 + 26, signTop);
        rod(context, cx + ExpressMenuPaint.SIGN_WIDTH / 2 - 26, signTop);
        ExpressMenuPaint.stationSign(
                context,
                font,
                cx,
                signCy,
                MenuText.localized("screen.sparkassist.title.heading", "SPARK EXPRESS"),
                MenuText.localized("screen.sparkassist.title.subtitle", "快 运 特 急"),
                HEADING_SCALE
        );

        ExpressMenuPaint.track(context, width, height, seconds, false);
        int railTop = height - ExpressMenuPaint.TRACK_HEIGHT - ExpressMenuPaint.FOOTER_HEIGHT;

        // The two side blocks need horizontal room; on a narrow or short screen they would collide with the button
        // grid, so only the version strip stays.
        // 两侧信息块需要横向空间；在窄屏或矮屏上它们会与按钮阵列相撞，因此只保留版本条。
        if (width >= 460 && height >= 280) {
            siteLinks(context, font, 6, railTop - SITE_BAND + 4, width / 2 - 14);
            familyList(context, font, width - 6, railTop - SITE_BAND + 4);
        }

        int versionY = height - ExpressMenuPaint.FOOTER_HEIGHT + 3;
        ExpressMenuPaint.caption(context, font, MenuText.literal(gameVersion), 6, versionY, FAINT);
        ExpressMenuPaint.rightCaption(
                context,
                font,
                MenuText.localized("screen.sparkassist.title.copyright", "© Mojang AB"),
                width - 6,
                versionY,
                FAINT
        );
    }

    /**
     * The two site lines: a label and a clickable URL each, so a player can open either site from the menu.
     * 两行官网信息：每行一个标签加一个可点击链接，玩家可直接从菜单打开任一站点。
     */
    private static void siteLinks(DrawContext context, TextRenderer font, int x, int y, int maxWidth) {
        Text dlcLabel = MenuText.localized("screen.sparkassist.title.site.dlc", "Spark Express DLC 官网");
        Text mainLabel = MenuText.localized("screen.sparkassist.title.site.main", "Spark Express 官网");
        int labelWidth = Math.max(font.getWidth(dlcLabel), font.getWidth(mainLabel));
        int urlWidth = Math.max(font.getWidth(DLC_SITE), font.getWidth(MAIN_SITE));
        int urlX = x + labelWidth + 6;
        // The plate is clipped to the left half so a wide face cannot push it under the button grid.
        // 底板裁到左半屏内，避免较宽字体把它推到按钮阵列下面。
        int plateWidth = Math.min(labelWidth + 6 + urlWidth + 8, maxWidth);
        ExpressMenuPaint.plate(context, x - 4, y - 3, plateWidth, 24);
        ExpressMenuPaint.caption(context, font, dlcLabel, x, y, MUTED);
        ExpressMenuPaint.caption(context, font, MenuText.literal(DLC_SITE), urlX, y, BRASS_HI);
        ExpressMenuPaint.caption(context, font, mainLabel, x, y + 11, MUTED);
        ExpressMenuPaint.caption(context, font, MenuText.literal(MAIN_SITE), urlX, y + 11, BRASS_HI);
    }

    /**
     * The installed SparkDLC family and their versions, right-aligned above the track.
     * 已安装的 SparkDLC 系列及其版本，右对齐排在铁轨上方。
     */
    private static void familyList(DrawContext context, TextRenderer font, int right, int y) {
        List<String> family = family();
        if (family.isEmpty()) {
            return;
        }
        int row = y + 11 - (family.size() + 1) * 10;
        int plateWidth = font.getWidth(MenuText.localized("screen.sparkassist.title.family", "SparkDLC 系列模组"));
        for (String entry : family) {
            plateWidth = Math.max(plateWidth, font.getWidth(entry));
        }
        ExpressMenuPaint.plate(context, right - plateWidth - 4, row - 3, plateWidth + 8,
                (family.size() + 1) * 10 + 6);
        ExpressMenuPaint.rightCaption(
                context,
                font,
                MenuText.localized("screen.sparkassist.title.family", "SparkDLC 系列模组"),
                right,
                row,
                BRASS_HI
        );
        for (int i = 0; i < family.size(); i++) {
            ExpressMenuPaint.rightCaption(
                    context,
                    font,
                    MenuText.literal(family.get(i)),
                    right,
                    row + 10 * (i + 1),
                    FAINT
            );
        }
    }

    /** One hanger rod: a lit brass edge and a shaded body. 单根吊杆：亮黄铜边与暗色杆身。 */
    private static void rod(DrawContext context, int x, int bottom) {
        context.fill(x, 0, x + 3, bottom, EDGE);
        context.fill(x + 1, 0, x + 2, bottom, BRASS_LO);
    }

    /**
     * The running SparkAssist version, resolved once. Returns an empty string when the loader does not expose it.
     * 运行中的 SparkAssist 版本，只解析一次。加载器未提供时返回空串。
     */
    static String modVersion() {
        if (cachedVersion == null) {
            cachedVersion = FabricLoader.getInstance()
                    .getModContainer("sparkassist")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("");
        }
        return cachedVersion;
    }

    /**
     * The installed SparkDLC family, name and version, resolved once on first use.
     * 已安装的 SparkDLC 系列及其名称与版本，首次使用时解析一次。
     */
    private static List<String> family() {
        if (cachedFamily == null) {
            List<String> rows = new ArrayList<>();
            for (String id : FAMILY) {
                FabricLoader.getInstance().getModContainer(id).ifPresent(container -> rows.add(label(container)));
            }
            cachedFamily = List.copyOf(rows);
        }
        return cachedFamily;
    }

    /** One list row: the mod's display name and its version. 一行列表：模组显示名与版本号。 */
    private static String label(ModContainer container) {
        String name = container.getMetadata().getName();
        String version = container.getMetadata().getVersion().getFriendlyString();
        return name + " " + version;
    }

    /**
     * Outlines a vanilla button with a brass edge so the button grid reads as part of the Express chrome without
     * replacing the widget itself. Callers pass the widget's own geometry, so focus, hover and clicks stay vanilla.
     * 用黄铜边描出原版按钮，使按钮阵列与特快装饰风格一致而不替换控件本身。调用方传入控件自身几何，因此焦点、悬停与
     * 点击仍由原版处理。
     */
    public static void outlineButton(DrawContext context, int x, int y, int width, int height) {
        // Two-tone rim: dark seat outside, lit brass outside that, so the plate reads as metal rather than a stroke.
        // 双色边框：外层暗座、内层亮黄铜，让底板读起来像金属而不是一道描边。
        ExpressPaint.roundedOutline(context, x - 2, y - 2, width + 4, height + 4, EDGE);
        ExpressPaint.roundedOutline(context, x - 1, y - 1, width + 2, height + 2, BRASS_LO);
        context.fill(x, y - 1, x + width, y, BRASS_HI);
        context.fill(x, y + height, x + width, y + height + 1, 0x88000000);
        // Four rivets, inset from the corners so they never sit on the label. 四角铆钉，向内缩进，不会压住文字。
        rivet(context, x + 2, y + 2);
        rivet(context, x + width - 6, y + 2);
        rivet(context, x + 2, y + height - 6);
        rivet(context, x + width - 6, y + height - 6);
    }

    /** One 4x4 brass rivet with a dark seat and a lit crown. 单颗 4x4 黄铜铆钉：暗座配亮帽。 */
    private static void rivet(DrawContext context, int x, int y) {
        context.fill(x, y, x + 4, y + 4, EDGE);
        context.fill(x + 1, y + 1, x + 3, y + 3, BRASS);
        context.fill(x + 1, y + 1, x + 2, y + 2, POLISHED);
    }
}
