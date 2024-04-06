package me.cortex.voxy.common.util;

public class Util {
    public static int compactIdMeta(short id, short meta) {
        return id << 16 | meta;
    }

    public static int computeIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }
}
