package com.emulator;

import java.util.Random;

public class Cpu {

    private static final Random RANDOM = new Random(); // Generates random bytes

    private int[] v = new int[16]; // 16 general purpose 8-bit registers, usually referred to as Vx

    private int i; // 16-bit register called I. Used to store memory addresses, so only the lowest 12 bits are usually used.

    private int pc; // The program counter (PC) should be 16-bit, and is used to store the currently executing address.

    private int sp; // The stack pointer (SP) can be 8-bit, it is used to point to the topmost level of the stack.

    private int[] stack = new int[16]; // The stack is an array of 16 16-bit values, used to store the address that the
                                        // interpreter should return to when finished with a subroutine.

    private int dt; // The delay timer register (DT) does nothing more than subtract 1 from the value of DT at a rate of
                    // 60Hz. When DT reaches 0, it deactivates.
    private int st; // The sound timer register (ST) also decrements at a rate of 60Hz, however, as long as ST's value
                    // is greater than zero, the Chip-8 buzzer will sound.

    private final Memory memory;

    public Cpu(Memory memory) {
        this.memory = memory;
        pc = Memory.START;
    }

    public int fetch() {
        return (memory.read(pc) << 8) | memory.read(pc + 1);
    }

    public void decodeAndExecute(int opcode) {
        assert 0x0000 >= opcode && opcode <= 0xFFFF;

        switch (opcode & 0xF000) {
            case 0x0000 -> {
                switch (opcode) {
                    case 0x00E0 -> {
                        // 00E0 - CLS
                        // Clear the display.
                    }
                    case 0x00EE -> { // 00EE - RET - Return from a subroutine.
                        pc = stack[sp--];
                        pc += 2;
                    }
                    default -> { // 0nnn - SYS addr - Jump to a machine code routine at nnn.
                        // This instruction is only used on the old computers on which Chip-8 was originally implemented.
                        // It is ignored by modern interpreters.
                    }
                }
            }
            case 0x1000 -> { // 1nnn - JP addr - Jump to location nnn
                pc = opcode & 0x0FFF;
            }
            case 0x2000 -> { // 2nnn - CALL addr - Call subroutine at nnn.
                stack[++sp] = pc;
                pc = opcode & 0x0FFF;
            }
            case 0x3000 -> { // 3xkk - SE Vx, byte - Skip next instruction if Vx = kk.
                if (v[(opcode & 0x0F00) >>> 8] == (opcode & 0x00FF)) {
                    pc += 2;
                }
                pc += 2;
            }
            case 0x4000 -> { // 4xkk - SNE Vx, byte - Skip next instruction if Vx != kk.
                if (v[(opcode & 0x0F00) >>> 8] != (opcode & 0x00FF)) {
                    pc += 2;
                }
                pc += 2;
            }
            case 0x5000 -> { // 5xy0 - SE Vx, Vy - Skip next instruction if Vx = Vy.
                if (v[(opcode & 0x0F00) >>> 8] == v[(opcode & 0x00F0) >>> 4]) {
                    pc += 2;
                }
                pc += 2;
            }
            case 0x6000 -> { // 6xkk - LD Vx, byte - Set Vx = kk.
                v[(opcode & 0x0F00) >>> 8] = (opcode & 0x00FF);
                pc += 2;
            }
            case 0x7000 -> { // 7xkk - ADD Vx, byte - Set Vx = Vx + kk.
                int x = (opcode & 0x0F00) >>> 8;
                int kk = opcode & 0x00FF;
                v[x] = (v[x] + kk) & 0xFF; // & 0xFF handles Overflow
                pc += 2;
            }
            case 0x8000 -> {
                switch (opcode & 0xF00F) {
                    case 0x8000 -> { // 8xy0 - LD Vx, Vy - Set Vx = Vy.
                        v[(opcode & 0x0F00) >>> 8] = (opcode & 0x00F0) >>> 4;
                        pc += 2;
                    }
                    case 0x8001 -> { // 8xy1 - OR Vx, Vy - Set Vx = Vx OR Vy.
                        v[(opcode & 0x0F00) >>> 8] = ((opcode & 0x0F00) >>> 8) | ((opcode & 0x00F0) >>> 4);
                        pc += 2;
                    }
                    case 0x8002 -> { // 8xy2 - AND Vx, Vy - Set Vx = Vx AND Vy.
                        v[(opcode & 0x0F00) >>> 8] = ((opcode & 0x0F00) >>> 8) & ((opcode & 0x00F0) >>> 4);
                        pc += 2;
                    }
                    case 0x8003 -> { // 8xy3 - XOR Vx, Vy - Set Vx = Vx XOR Vy.
                        v[(opcode & 0x0F00) >>> 8] = ((opcode & 0x0F00) >>> 8) ^ ((opcode & 0x00F0) >>> 4);
                        pc += 2;
                    }
                    case 0x8004 -> { // 8xy4 - ADD Vx, Vy - Set Vx = Vx + Vy, set VF = carry.
                        int x = (opcode & 0x0F00) >>> 8;
                        int y = (opcode & 0x00F0) >>> 4;
                        int res = v[x] + v[y];
                        v[0xF] = res > 0xFF ? 1 : 0; // carry
                        v[x] = res & 0xFF; // & 0xFF handles Overflow
                        pc += 2;
                    }
                    case 0x8005 -> { // 8xy5 - SUB Vx, Vy - Set Vx = Vx - Vy, set VF = NOT borrow.
                        int x = (opcode & 0x0F00) >>> 8;
                        int y = (opcode & 0x00F0) >>> 4;
                        v[0xF] = v[x] > v[y] ? 1 : 0; // NOT borrow
                        v[x] = (v[x] - v[y]) & 0xFF; // & 0xFF handles Underflow
                        pc += 2;
                    }
                    case 0x8006 -> { // 8xy6 - SHR Vx {, Vy} - Set Vx = Vx SHR 1.
                        // If the least-significant bit of Vx is 1, then VF is set to 1, otherwise 0. Then Vx is divided by 2.
                        int x = (opcode & 0x0F00) >>> 8;
                        v[0xF] = (v[x] & 0x1) == 0x1 ? 1 : 0;
                        v[x] = v[x] >>> 1;
                        pc += 2;
                    }
                    case 0x8007 -> { // 8xy7 - SUBN Vx, Vy - Set Vx = Vy - Vx, set VF = NOT borrow.
                        int x = (opcode & 0x0F00) >>> 8;
                        int y = (opcode & 0x00F0) >>> 4;
                        v[0xF] = v[y] > v[x] ? 1 : 0; // NOT borrow
                        v[x] = (v[y] - v[x]) & 0xFF; // & 0xFF handles Underflow
                        pc += 2;
                    }
                    case 0x800E -> { // 8xyE - SHL Vx {, Vy} - Set Vx = Vx SHL 1.
                        // If the most-significant bit of Vx is 1, then VF is set to 1, otherwise to 0. Then Vx is multiplied by 2.
                        int x = (opcode & 0x0F00) >>> 8;
                        v[0xF] = (v[x] >>> 7) == 0x1 ? 1 : 0;
                        v[x] = (v[x] << 1) & 0xFF; // & 0xFF handles Overflow;
                        pc += 2;
                    }
                }
            }
            case 0x9000 -> { // 9xy0 - SNE Vx, Vy - Skip next instruction if Vx != Vy.
                if (v[(opcode & 0x0F00) >>> 8] != v[(opcode & 0x00F0) >>> 4]) {
                    pc += 2;
                }
                pc += 2;
            }
            case 0xA000 -> { // Annn - LD I, addr - Set I = nnn.
                i = opcode & 0x0FFF;
                pc += 2;
            }
            case 0xB000 -> { // Bnnn - JP V0, addr - Jump to location nnn + V0.
                pc = (opcode & 0x0FFF) + v[0x0];
            }
            case 0xC000 -> { // Cxkk - RND Vx, byte - Set Vx = random byte AND kk.
                v[(opcode & 0x0F00) >>> 8] = getRandomByte() & (opcode & 0x00FF);
                pc += 2;
            }
            case 0xD000 -> { // Dxyn - DRW Vx, Vy, nibble - Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision.
                // The interpreter reads n bytes from memory, starting at the address stored in I.
                // These bytes are then displayed as sprites on screen at coordinates (Vx, Vy). Sprites are XORed onto the existing screen.
                // If this causes any pixels to be erased, VF is set to 1, otherwise it is set to 0.
                // If the sprite is positioned so part of it is outside the coordinates of the display, it wraps around to the opposite side of the screen.
                // See instruction 8xy3 for more information on XOR, and section 2.4, Display, for more information on the Chip-8 screen and sprites.
            }
            case 0xE000 -> {
                switch (opcode & 0xF0FF) {
                    case 0xE09E -> { // Ex9E - SKP Vx - Skip next instruction if key with the value of Vx is pressed.
                        // Checks the keyboard, and if the key corresponding to the value of Vx is currently in the down position, PC is increased by 2.
                    }
                    case 0xE0A1 -> { // ExA1 - SKNP Vx - Skip next instruction if key with the value of Vx is not pressed.
                        // Checks the keyboard, and if the key corresponding to the value of Vx is currently in the up position, PC is increased by 2.
                    }
                }
            }
            case 0xF000 -> {
                switch (opcode & 0xF0FF) {
                    case 0xF007 -> { // Fx07 - LD Vx, DT - Set Vx = delay timer value.
                        v[(opcode & 0x0F00) >>> 8] = dt;
                        pc += 2;
                    }
                    case 0xF00A -> { // Fx0A - LD Vx, K - Wait for a key press, store the value of the key in Vx.
                        // All execution stops until a key is pressed, then the value of that key is stored in Vx.
                    }
                    case 0xF015 -> { // Fx15 - LD DT, Vx - Set delay timer = Vx.
                        dt = v[(opcode & 0x0F00) >>> 8];
                        pc += 2;
                    }
                    case 0xF018 -> { // Fx18 - LD ST, Vx - Set sound timer = Vx.
                        st = v[(opcode & 0x0F00) >> 8];
                        pc += 2;
                    }
                    case 0xF01E -> { // Fx1E - ADD I, Vx - Set I = I + Vx.
                        i = (i + v[(opcode & 0x0F00) >>> 8]) & 0xFFF; // & 0xFFF handles Overflow;
                        pc += 2;
                    }
                    case 0xF029 -> { // Fx29 - LD F, Vx - Set I = location of sprite for digit Vx.
                        i = memory.getFontSpriteAddress(v[(opcode & 0x0F00) >>> 8]);
                        pc += 2;
                    }
                    case 0xF033 -> { // Fx33 - LD B, Vx - Store BCD (binary-coded decimal) representation of Vx in memory locations I, I+1, and I+2.
                        int x = v[(opcode & 0x0F00) >>> 8];
                        memory.write(i, x / 100);
                        memory.write(i + 1, x / 10 % 10);
                        memory.write(i + 2, x % 10);
                        pc += 2;
                    }
                    case 0xF055 -> { // Fx55 - LD [I], Vx - Store registers V0 through Vx in memory starting at location I.
                        int x = (opcode & 0x0F00) >>> 8;
                        for (int j = 0; j < x; j++) {
                            memory.write(i + j, v[j]);
                        }
                        pc += 2;
                    }
                    case 0xF065 -> { // Fx65 - LD Vx, [I] - Read registers V0 through Vx from memory starting at location I.
                        int x = (opcode & 0x0F00) >>> 8;
                        for (int j = 0; j < x; j++) {
                            v[j] = memory.read(i + j);
                        }
                        pc += 2;
                    }
                }
            }
        }
    }

    private int getRandomByte() {
        return RANDOM.nextInt(256);
    }

}
