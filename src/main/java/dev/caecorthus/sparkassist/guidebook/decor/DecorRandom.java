package dev.caecorthus.sparkassist.guidebook.decor;

/**
 * Deterministic 32-bit generator (mulberry32) for the decoration generators, plus the seed rules: a page's seed is
 * its entry id hashed with the round seed, so the same page looks the same all round and changes next round.
 * 点缀生成器使用的确定性 32 位随机数（mulberry32）与种子规则：页种子由条目 id 与本局种子哈希而成，
 * 同一局内同一页样子不变，下一局换一种。
 */
public final class DecorRandom {
    private int state;

    public DecorRandom(long seed) {
        this.state = (int) (seed ^ (seed >>> 32));
    }

    /** Uniform in [0, 1). 均匀分布于 [0, 1)。 */
    public double next() {
        state += 0x6D2B79F5;
        int t = state;
        t = (t ^ (t >>> 15)) * (t | 1);
        t ^= t + ((t ^ (t >>> 7)) * (t | 61));
        return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
    }

    /** Uniform integer in [0, bound). [0, bound) 内的均匀整数。 */
    public int nextInt(int bound) {
        return (int) (next() * bound);
    }

    /** Integer in [min, max] inclusive. [min, max] 闭区间内的整数。 */
    public int between(int min, int max) {
        return min + nextInt(max - min + 1);
    }

    public boolean chance(double probability) {
        return next() < probability;
    }

    /** FNV-1a, 32 bits, over the UTF-16 code units. FNV-1a 32 位哈希。 */
    public static int hash(String text) {
        int h = 0x811C9DC5;
        for (int i = 0; i < text.length(); i++) {
            h ^= text.charAt(i);
            h *= 0x01000193;
        }
        return h;
    }

    /** Seed of one page in one round. 某一页在某一局的种子。 */
    public static long pageSeed(String entryId, long roundSeed) {
        return (hash(entryId) & 0xFFFFFFFFL) ^ (roundSeed * 7919L);
    }

    /** Seed for the player's own surfaces (directory, card), salted so it never equals a page seed.
     * 玩家自身面板（目录、信息卡）的种子，加盐后不会与任何页种子相同。 */
    public static long ownerSeed(String key, long roundSeed) {
        return pageSeed(key, roundSeed) ^ 0x5BD1E995L;
    }
}
