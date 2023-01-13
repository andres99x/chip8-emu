package com.emulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Main {

    public static void main(String[] args) {
        Memory memory = new Memory();

        File f = new File("roms/IBM");
        try (InputStream is = new FileInputStream(f)) {
            memory.loadRom(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Screen screen = new Screen();
        Cpu cpu = new Cpu(memory, screen);

        int i = 0;
        while (i < 1_000_000) {
            int opcode = cpu.fetch();
            cpu.decodeAndExecute(opcode);
            i++;
        }

        for (int y = 0; y < Screen.HEIGHT; y++) {
            String s = "";
            for (int x = 0; x < Screen.WIDTH; x++) {
                s += screen.getPixel(x, y) == 1 ? "X" : " ";
            }
            System.out.println(s);
        }

    }

}

