/**
 * Mars Simulation Project 
 * MarsProject.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */
package org.mars_sim.msp;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.helpGenerator.HelpGenerator;
import org.mars_sim.msp.ui.swing.MainWindow;
import org.mars_sim.msp.ui.swing.SplashWindow;
import org.mars_sim.msp.ui.swing.configeditor.TempSimulationConfigEditor;

import javax.swing.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * MarsProject is the main class for the application. It creates both the simulation and
 * the user interface.
 */
public class MarsProject {

    /** initialized logger for this class. */
    private static Logger logger = Logger.getLogger(MarsProject.class.getName());
    
    /** true if displaying graphic user interface. */
    private boolean useGUI = true;

    /** true if help documents should be generated from config xml files. */
    private boolean generateHelp = false;

    /**
     * Constructor.
     * @param args command line arguments.
     */
    public MarsProject(String args[]) {

        logger.info("Starting Mars Simulation");

        List<String> argList = Arrays.asList(args);
        useGUI = !argList.contains("-headless");
        generateHelp = argList.contains("-generateHelp");
        
        if (useGUI) {
            // Create a splash window
            SplashWindow splashWindow = new SplashWindow();
            splashWindow.show();

            boolean newSim = initializeSimulation(args);

            // Create the main desktop window.
            MainWindow w = new MainWindow(newSim);
            w.getFrame().setVisible(true);
     
            // Start simulation
            startSimulation();
        
            // Dispose the splash window.
            splashWindow.hide();
        }
        else {
            // Initialize the simulation.
            initializeSimulation(args);
            
            // Start the simulation.
            startSimulation();
        }
        
        // this will generate html files for in-game help based on config xml files
        if (generateHelp) {
        	HelpGenerator.generateHtmlHelpFiles();
        }
    }

    /**
     * Initialize the simulation.
     * @param args the command arguments.
     * @return true if new simulation (not loaded)
     */
    private boolean initializeSimulation(String[] args) {
        boolean result = false;
        
        // Create a simulation
        List<String> argList = Arrays.asList(args);

        if (argList.contains("-new")) {
            // If new argument, create new simulation.
            handleNewSimulation(); // if this fails we always exit, continuing is useless
            result = true;

        } else if (argList.contains("-load")) {
            // If load argument, load simulation from file.
            try {
                handleLoadSimulation(argList);
            } catch (Exception e) {
                showError("Could not load the desired simulation, trying to create a new Simulation...", e);
                handleNewSimulation();
                result = true;
            }
        } else {
            try {
                handleLoadDefaultSimulation();
            } catch (Exception e) {
//                showError("Could not load the default simulation, trying to create a new Simulation...", e);
                handleNewSimulation();
                result = true;
            }
        }
        
        return result;
    }

    /**
     * Exit the simulation with an error message.
     * @param message the error message.
     * @param e the thrown exception or null if none.
     */
    private void exitWithError(String message, Exception e) {
        showError(message, e);
        System.exit(1);
    }

    /**
     * Show a modal error message dialog.
     * @param message the error message.
     * @param e the thrown exception or null if none.
     */
    private void showError(String message, Exception e) {
        if (e != null) {
            logger.log(Level.SEVERE, message, e);
        }
        else {
            logger.log(Level.SEVERE, message);
        }
        
        if (useGUI) {
            JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads the simulation from the default save file.
     * @throws Exception if error loading the default saved simulation.
     */
    private void handleLoadDefaultSimulation() throws Exception {
        try {
            // Load a the default simulation
            Simulation.instance().loadSimulation(null);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not load default simulation", e);
            throw e;
        }
    }

    /**
     * Loads the simulation from a save file.
     * @param argList the command argument list.
     * @throws Exception if error loading the saved simulation.
     */
    private void handleLoadSimulation(List<String> argList) throws Exception {
        try {
            int index = argList.indexOf("-load");
            // Get the next argument as the filename.
            File loadFile = new File(argList.get(index + 1));
            if (loadFile.exists() && loadFile.canRead()) {
                Simulation.instance().loadSimulation(loadFile);
            } else {
                exitWithError("Problem loading simulation. " + argList.get(index + 1) + 
                        " not found.", null); 
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem loading existing simulation", e);
            throw e;
        }
    }

    /**
     * Create a new simulation instance.
     */
    private void handleNewSimulation() {
        try {
            SimulationConfig.loadConfig();
            if (useGUI) {
                TempSimulationConfigEditor editor = new TempSimulationConfigEditor(null, 
                        SimulationConfig.instance());
                editor.setVisible(true);
            }
            Simulation.createNewSimulation();
        } catch (Exception e) {
            e.printStackTrace();
            exitWithError("Could not create a new simulation, startup cannot continue", e);
        }
    }

    /**
     * Start the simulation instance.
     */
    public void startSimulation() {
        // Start the simulation.
        Simulation.instance().start();
    }

    /**
     * The starting method for the application
     *
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* [landrus, 27.11.09]: Read the logging configuration from the classloader, so that this gets
           * webstart compatible. Also create the logs dir in user.home */
        new File(System.getProperty("user.home"), ".mars-sim" + File.separator + "logs").mkdirs();

        try {
            LogManager.getLogManager().readConfiguration(MarsProject.class.getResourceAsStream("/logging.properties"));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not load logging properties", e);
            try {
                LogManager.getLogManager().readConfiguration();
            } catch (IOException e1) {
                logger.log(Level.WARNING, "Could read logging default config", e);
            }
        }
        
        // starting the simulation
        System.setProperty("swing.aatext", "true"); // general text antialiasing

        new MarsProject(args);
    }
}