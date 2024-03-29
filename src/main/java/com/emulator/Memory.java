package com.emulator;

import java.io.IOException;
import java.io.InputStream;

public class Memory {

    public static int START = 0x200; // (512) Start of most Chip-8 programs
    public static int END = 0xFFF; // (4095) End of Chip-8 RAM

    public static final int[] FONT_SPRITES = {
        0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
        0x20, 0x60, 0x20, 0x20, 0x70, // 1
        0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
        0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
        0x90, 0x90, 0xF0, 0x10, 0x10, // 4
        0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
        0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
        0xF0, 0x10, 0x20, 0x40, 0x50, // 7
        0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
        0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
        0xF0, 0x90, 0xF0, 0x90, 0x90, // A
        0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
        0xF0, 0x80, 0x80, 0x80, 0xF0, // C
        0xE0, 0x90, 0x90, 0x90, 0xE0, // D
        0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
        0xF0, 0x80, 0xF0, 0x80, 0x80  // F
    };

    private static final int FONT_SIZE = 5;

    private final int[] addressSpace = new int[4096];

    public Memory() {
        loadFonts();
    }

    public void loadRom(InputStream rom) throws IOException {
        int addr = START;
        int b;
        while ((b = rom.read()) != -1) {
            write(addr++, b);
        }
    }

    public int read(int addr) {
        return addressSpace[addr] & 0xFF;
    }

    public int write(int addr, int value) {
        assert START >= addr && addr <= END;
        assert 0x00 <= value && value <= 0xFF;

        return addressSpace[addr] = value;
    }

    public int getFontSpriteAddress(int font) {
        assert font >= 0x0 && 0xF >= font;
        return font * FONT_SIZE;
    }

    private void loadFonts() {
        for (int i = 0; i < FONT_SPRITES.length; i++) {
            addressSpace[i] = FONT_SPRITES[i];
        }
    }
}
