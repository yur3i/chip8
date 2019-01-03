package chip8;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class Chip
{
    // memory; Program begins at 0x200
    private char[] memory;

    // 16 registers
    private char[] V;
    // Points to a specific point in memory
    private char I;
    // Points to current operation
    private char pc;
    // Can hold up to 16 layers of nesting
    private char stack[];
    // Points to next free slot in stack
    private int stackPointer;

    // For delaying events in games
    private int delay_timer;
    // For making beep sounds
    private int sound_timer;

    // Keyboard state
    private byte[] keys;

    // 64x32 monochrome display
    private byte[] display;
    private boolean needsRedraw;

    public Chip()
    {
	memory = new char[4096];
	V = new char[16];
	I = 0x0;
	pc = 0x200;
	
	stack = new char[16];
	stackPointer = 0;

	delay_timer = 0;
	sound_timer = 0;

	keys = new byte[16];

	display = new byte[64 * 32];
	needsRedraw = true;

	loadFontSet();
    }

public void run() {
		//fetch Opcode
		char opcode = (char)((memory[pc] << 8) | memory[pc + 1]);
		System.out.print(Integer.toHexString(opcode).toUpperCase() + ": ");
		//decode opcode
		switch(opcode & 0xF000) {
		
		case 0x0000: //Multi-case
			switch(opcode & 0x00FF) {
			case 0x00E0: //00E0: Clear Screen
				System.err.println("Unsupported Opcode!");
				System.exit(0);
				break;
				
			case 0x00EE: //00EE: Returns from subroutine
				stackPointer--;
				pc = (char)(stack[stackPointer] + 2);
				System.out.println("Returning to " + Integer.toHexString(pc).toUpperCase());
				break;
				
			default: //0NNN: Calls RCA 1802 Program at address NNN
				System.err.println("Unsupported Opcode!");
				System.exit(0);
				break;
			}
			break;
		
		case 0x1000: //1NNN: Jumps to address NNN
			int nnn = opcode & 0x0FFF;
			pc = (char)nnn;
			System.out.println("Jumping to " + Integer.toHexString(pc).toUpperCase());
			break;
			
		case 0x2000: //2NNN: Calls subroutine at NNN
			stack[stackPointer] = pc;
			stackPointer++;
			pc = (char)(opcode & 0x0FFF);
			System.out.println("Calling " + Integer.toHexString(pc).toUpperCase() + " from " + Integer.toHexString(stack[stackPointer - 1]).toUpperCase());
			break;
			
		case 0x3000: { //3XNN: Skips the next instruction if VX equals NN
			int x = (opcode & 0x0F00) >> 8;
			int nn = (opcode & 0x00FF);
			if(V[x] == nn) {
				pc += 4;
				System.out.println("Skipping next instruction (V[" + x +"] == " + nn + ")");
			} else {
				pc += 2;
				System.out.println("Not skipping next instruction (V[" + x +"] != " + nn + ")");
			}
			break;
		}
		
		case 0x4000: { //4XNN: Skip the next instruction if VX != NN
			int x = (opcode & 0x0F00) >> 8;
			int nn = opcode & 0x00FF;
			if(V[x] != nn) {
				System.out.println("Skipping next instruction V[" + x + "] = " + (int)V[x] + " != " + nn);
				pc += 4;
			} else {
				System.out.println("Not skipping next instruction V[" + x + "] = " + (int)V[x] + " == " + nn);
				pc += 2;
			}
			break;
		}
			
		case 0x6000: { //6XNN: Set VX to NN
			int x = (opcode & 0x0F00) >> 8;
			V[x] = (char)(opcode & 0x00FF);
			pc += 2;
			System.out.println("Setting V[" + x + "] to " + (int)V[x]);
			break;
		}
			
		case 0x7000: { //7XNN: Adds NN to VX
			int x = (opcode & 0x0F00) >> 8;
			int nn = (opcode & 0x00FF);
			V[x] = (char)((V[x] + nn) & 0xFF);
			pc += 2;
			System.out.println("Adding " + nn + " to V["+ x + "] = " + (int)V[x]);
			break;
		}
		
		case 0x8000: //Contains more data in last nibble
			
			switch(opcode & 0x000F) {
			
			case 0x0000: { //8XY0: Sets VX to the value of VY
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				System.out.println("Setting V[" + x + "] to " + (int)V[y]);
				V[x] = V[y];
				pc += 2;
				break;
			}
				
			case 0x0002: { //8XY2: Sets VX to VX AND VY
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				System.out.println("Set V[" + x + "] to V[" + x + "] = " + (int)V[x] + " & V[" + y + "] = " + (int)V[y] + " = " + (int)(V[x] & V[y]));
				V[x] = (char)(V[x] & V[y]);
				pc += 2;
				break;
			}
				
			case 0x0004: { //Adds VY to VX. VF is set to 1 when carry applies else to 0
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				System.out.print("Adding V[" + x + "] (" + (int)V[x]  + ") to V[" + y + "] (" + (int)V[y]  + ") = " + ((V[x] + V[y]) & 0xFF) + ", ");
				if(V[y] > 0xFF - V[x]) {
					V[0xF] = 1;
					System.out.println("Carry!");
				} else {
					V[0xF] = 0;
					System.out.println("No Carry");
				}
				V[x] = (char)((V[x] + V[y]) & 0xFF);
				pc += 2;
				break;
			}
			
			case 0x0005: { //VY is subtracted from VX. VF is set to 0 when there is a borrow else 1
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				System.out.print("V[" + x + "] = " + (int)V[x] + " V[" + y + "] = " + (int)V[y] + ", ");
				if(V[x] > V[y]) {
					V[0xF] = 1;
					System.out.println("No Borrow");
				} else {
					V[0xF] = 0;
					System.out.println("Borrow");
				}
				V[x] = (char)((V[x] - V[y]) & 0xFF);
				pc += 2;
				break;
			}
			
				default:
					System.err.println("Unsupported Opcode!");
					System.exit(0);
					break;
			}
				
			break;
			
		case 0xA000: //ANNN: Set I to NNN
			I = (char)(opcode & 0x0FFF);
			pc += 2;
			System.out.println("Set I to " + Integer.toHexString(I).toUpperCase());
			break;
			
		case 0xC000: { //CXNN: Set VX to a random number and NN
			int x = (opcode & 0x0F00) >> 8;
			int nn = (opcode & 0x00FF);
			int randomNumber = new Random().nextInt(255) & nn;
			System.out.println("V[" + x + "] has been set to (randomised) " + randomNumber);
			V[x] = (char)randomNumber;
			pc += 2;
			break;
		}
			
		case 0xD000: { //DXYN: Draw a sprite (X, Y) size (8, N). Sprite is located at I
			int x = V[(opcode & 0x0F00) >> 8];
			int y = V[(opcode & 0x00F0) >> 4];
			int height = opcode & 0x000F;
			
			V[0xF] = 0;
			
			for(int _y = 0; _y < height; _y++) {
				int line = memory[I + _y];
				for(int _x = 0; _x < 8; _x++) {
					int pixel = line & (0x80 >> _x);
					if(pixel != 0) {
						int totalX = x + _x;
						int totalY = y + _y;
						int index = (totalY * 64) + totalX;
						
						if(display[index] == 1)
							V[0xF] = 1;
						
						display[index] ^= 1;
					}
				}
			}
			pc += 2;
			needsRedraw = true;
			System.out.println("Drawing at V[" + ((opcode & 0x0F00) >> 8) + "] = " + x + ", V[" + ((opcode & 0x00F0) >> 4) + "] = " + y);
			break;
		}
		
		case 0xE000: {
			switch (opcode & 0x00FF) {
			case 0x009E: { //EX9E Skip the next instruction if the Key VX is pressed
				int x = (opcode & 0x0F00) >> 8;
				int key = V[x];
				if(keys[key] == 1) {
					pc += 4;
				} else {
					pc += 2;
				}
				System.out.println("Skipping next instruction if V[" + (int)V[x] + "] is pressed");
				break;
			}
				
			case 0x00A1: { //EXA1 Skip the next instruction if the Key VX is NOT pressed
				int x = (opcode & 0x0F00) >> 8;
				int key = V[x];
				if(keys[key] == 0) {
					pc += 4;
				} else {
					pc += 2;
				}
				System.out.println("Skipping next instruction if V[" + (int)V[x] + "] is NOT pressed");
				break;
			}
				
				default:
					System.err.println("Unexisting opcode");
					System.exit(0);
					return;
			}
			break;
		}
		
		case 0xF000:
			
			switch(opcode & 0x00FF) {
			
			case 0x0007: { //FX07: Set VX to the value of delay_timer
				int x = (opcode & 0x0F00) >> 8;
				V[x] = (char)delay_timer;
				pc += 2;
				System.out.println("V[" + x + "] has been set to " + delay_timer);
				break;
			}
			
			case 0x0015: { //FX15: Set delay timer to V[x]
				int x = (opcode & 0x0F00) >> 8;
				delay_timer = V[x];
				pc += 2;
				System.out.println("Set delay_timer to V[" + x + "] = " + (int)V[x]);
				break;
			}
			
			case 0x0018: { //FX18: Set the sound timer to V[x]
				int x = (opcode & 0x0F00) >> 8;
				sound_timer = V[x];
				pc += 2;
				break;
			}
			
			case 0x0029: { //FX29: Sets I to the location of the sprite for the character VX (Fontset)
				int x = (opcode & 0x0F00) >> 8;
				int character = V[x];
				I = (char)(0x050 + (character * 5));
				System.out.println("Setting I to Character V[" + x + "] = " + (int)V[x] + " Offset to 0x" + Integer.toHexString(I).toUpperCase());
				pc += 2;
				break;
			}
			
			case 0x0033: { //FX33 Store a binary-coded decimal value VX in I, I + 1 and I + 2
				int x = (opcode & 0x0F00) >> 8;
				int value = V[x];
				int hundreds = (value - (value % 100)) / 100;
				value -= hundreds * 100;
				int tens = (value - (value % 10))/ 10;
				value -= tens * 10;
				memory[I] = (char)hundreds;
				memory[I + 1] = (char)tens;
				memory[I + 2] = (char)value;
				System.out.println("Storing Binary-Coded Decimal V[" + x + "] = " + (int)(V[(opcode & 0x0F00) >> 8]) + " as { " + hundreds+ ", " + tens + ", " + value + "}");
				pc += 2;
				break;
			}
			
			case 0x0065: { //FX65 Fills V0 to VX with values from I
				int x = (opcode & 0x0F00) >> 8;
				for(int i = 0; i <= x; i++) {
					V[i] = memory[I + i];
				}
				System.out.println("Setting V[0] to V[" + x + "] to the values of memory[0x" + Integer.toHexString(I & 0xFFFF).toUpperCase() + "]");
				I = (char)(I + x + 1);
				pc += 2;
				break;
			}
			
			default:
				System.err.println("Unsupported Opcode!");
				System.exit(0);
			}
			break;
		
			default:
				System.err.println("Unsupported Opcode!");
				System.exit(0);
		}
		
		if(sound_timer > 0)
			sound_timer--;
		if(delay_timer > 0)
			delay_timer--;
}    

    public byte[] getDisplay()
    {
	return display;
    }

    public boolean needsRedraw()
    {
	return needsRedraw;
    }

    public void removeDrawFlag()
    {
	needsRedraw = false;
    }

    public void loadProgram(String file) {
	DataInputStream input = null;
	try {
	    input = new DataInputStream(new FileInputStream(new File(file)));
			
	    int offset = 0;
	    while(input.available() > 0) {
		memory[0x200 + offset] = (char)(input.readByte() & 0xFF);
		offset++;
	    }
			
	} catch (IOException e) {
	    e.printStackTrace();
	    System.exit(0);
	} finally {
	    if(input != null) {
		try { input.close(); } catch (IOException ex) {}
	    }
	}
    }

    public void loadFontSet() {
	for(int i=0;i < ChipData.fontset.length;i++) {
	    memory[0x50 + i] = (char)(ChipData.fontset[i] & 0xFF);
	}
    }

    public void setKeyBuffer(int[] keyBuffer)
    {
	for (int i=0;i<keys.length;i++)
	{
	    keys[i] = (byte)keyBuffer[i];
	}    
    }

}
