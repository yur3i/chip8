/**
 * @description A simple emulator of a Chip 8 System
 * Tested with Pong
 * @author Jorde Kang -- jorde@protonmail.ch
 */

package chip8;

public class Main extends Thread {
	
    private Chip chip8;
    private ChipFrame frame;
	
    public Main(String path) {
	chip8 = new Chip();
	chip8.loadProgram(path);
	frame = new ChipFrame(chip8);
    }
	
    public void run() {
	//60 hz, 60 updates per second
	while(true) {
	    chip8.setKeyBuffer(frame.getKeyBuffer());
	    chip8.run();
	    if(chip8.needsRedraw()) {
		frame.repaint();
		chip8.removeDrawFlag();
	    }
	    try {
		Thread.sleep(4);
	    } catch (InterruptedException e) {
		//Unthrown exception
	    }
	}
    }
	
    public static void main(String[] args) {
	Main main = new Main(args[1]);
	main.start();
    }

}
