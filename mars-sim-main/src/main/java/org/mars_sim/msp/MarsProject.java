/**
 * Mars Simulation Project 
 * MarsProject.java
 * @version 2.88 2009-12-21
 * @author Scott Davis
 */
package org.mars_sim.msp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.ui.swing.MainWindow;
import org.mars_sim.msp.ui.swing.SplashWindow;

/**
 * MarsProject is the main class for the application. It creates both Mars and
 * the user interface.
 */
public class MarsProject {

    private static String CLASS_NAME = "org.mars_sim.msp.MarsProject";

    private static Logger logger = Logger.getLogger(CLASS_NAME);

    /**
     * Constructor
     * 
     * @param args command line arguments.
     */
    public MarsProject(String args[]) {

        logger.info("Starting Mars Simulation");

        // Create a splash window
        SplashWindow splashWindow = new SplashWindow();

        // Create a simulation
        List<String> argList = Arrays.asList(args);

        if (argList.contains("-new")) {
            // If new argument, create new simulation.
            try {
                Simulation.createNewSimulation();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Problem creating new simulation", e);
                System.exit(0);
            }
        }
        else if (argList.contains("-load")) {
            // If load argument, load simulation from file.
            try {
                int index = argList.indexOf("-load");
                // Get the next argument as the filename.
                File loadFile = new File((String) argList.get(index + 1));
                if (loadFile.exists()) Simulation.instance().loadSimulation(loadFile);
                else {
                    logger.log(Level.SEVERE, "Problem loading simulation");
                    logger.log(Level.SEVERE, argList.get(index + 1) + " not found.");
                    System.exit(0);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Problem loading existing simulation", e);
                System.exit(0);
            }
        }
        else {
            try {
                // Load a the default simulation
                Simulation.instance().loadSimulation(null);
            } catch (Exception e) {
                if (!(e instanceof java.io.InvalidClassException)) {
                    logger.log(Level.SEVERE, "Problem loading default simulation", e);
                }

                try {
                    // If error reading default saved file, create new
                    // simulation.
                    logger.warning("Creating new simulation");
                    Simulation.createNewSimulation();
                } catch (Exception e2) {
                    logger.log(Level.SEVERE, "Problem creating new simulation", e2);
                    System.exit(0);
                }
            }
        }

        // Start the simulation.
        Simulation.instance().start();

        // Create the main desktop window.
        new MainWindow();

        // Dispose the splash window.
        splashWindow.dispose();
    }

    /**
     * The starting method for the application
     * 
     * @param args the command line arguments
     */
    public static void main(String args[]) {
    	/* [landrus, 27.11.09]: Read the logging configuration from the classloader, so that this gets
    	 * webstart compatible. Also create the logs dir in user.home */
    	new File(System.getProperty("user.home"), "mars-sim" + File.separator + "logs").mkdirs();
    	
    	try {
			LogManager.getLogManager().readConfiguration(MarsProject.class.getResourceAsStream("/logging.properties"));
		} catch (IOException e) {
			try {
				LogManager.getLogManager().readConfiguration();
			} catch (IOException e1) {
			}
		}
        // starting the simulation
        System.setProperty("swing.aatext", "true"); // general text antialiasing
        new MarsProject(args);
    }
}