package com.emulator;

public class Screen {

    public static final int WIDTH = 64;
    public static final int HEIGHT = 32;

    private final boolean[][] pixels = new boolean[WIDTH][HEIGHT];

    public Screen() {
        clear();
    }

    public void clear() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                pixels[x][y] = false;
            }
        }
    }

    public int getPixel(int x, int y) {
        assert 0 <= x && x < WIDTH;
        assert 0 <= y && y < HEIGHT;

        return pixels[x][y] ? 1 : 0;
    }

    public void setPixel(int x, int y) {
        assert 0 <= x && x < WIDTH;
        assert 0 <= y && y < HEIGHT;

        pixels[x][y] ^= pixels[x][y];
    }

}
