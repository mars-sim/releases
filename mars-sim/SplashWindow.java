/**
 * Mars Simulation Project
 * SplashWindow.java
 * @version 2.70 2000-08-31
 * @author Scott Davis
 */

import java.awt.*;
import javax.swing.*;

/** The SplashWindow class is a splash screen shown when the project
 *  is loading. It's started as a new thread. It fails silently if the
 *  splash image file is not found.
 */
public class SplashWindow extends JWindow implements Runnable {

    private static final String splashFile = "SplashImage.jpg";

    public SplashWindow() {
	//start();
	run();
    }

    public void start() {
	Thread kicker = new Thread(this);
	kicker.start();
    }

    public void run() {
		
	// Don't display until window is created.
	setVisible(false);
		
	// Set the background to black.
	setBackground(Color.black);
		
	// Create ImageIcon from SplashImage.jpg.
	ImageIcon splashIcon = new ImageIcon(splashFile);
		
	// Put image on label and add it to the splash window.
	JLabel splashLabel = new JLabel(splashIcon);
	getContentPane().add(splashLabel);
		
	// Pack the splash window to it's minimum size with the image.
	pack();
		
	// Sets root pane to double buffered.
	getRootPane().setDoubleBuffered(true);
		
	// Center the splash window on the screen.
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	Dimension windowSize = getSize();
	setLocation(((screenSize.width - windowSize.width) / 2), ((screenSize.height - windowSize.height) / 2));
		
	// Display the splash window.
	setVisible(true);
    }
    
    /** for component testing */
    public static void main(String argv[]) {
	SplashWindow s = new SplashWindow();
    }
}
