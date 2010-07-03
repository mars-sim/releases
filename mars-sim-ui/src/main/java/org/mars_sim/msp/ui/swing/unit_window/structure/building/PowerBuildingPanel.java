/**
 * Mars Simulation Project
 * PowerBuildingPanel.java
 * @version 2.75 2004-04-05
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_window.structure.building;

import java.awt.GridLayout;
import java.text.DecimalFormat;

import javax.swing.JLabel;

import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.function.PowerGeneration;
import org.mars_sim.msp.ui.swing.MainDesktopPane;


/**
 * The PowerBuildingPanel class is a building function panel representing 
 * the power production and use of a settlement building.
 */
public class PowerBuildingPanel extends BuildingFunctionPanel {
    
    private boolean isProducer; // Is the building a power producer?
    private JLabel powerStatusLabel; // The power status label.
    private JLabel productionLabel; // The power production label.
    private JLabel usedLabel; // The power used label.
    private DecimalFormat formatter = new DecimalFormat("0.0");  // Decimal formatter.
    
    // Caches
    private String powerStatusCache; // The power status cache.
    private double productionCache; // The power production cache.
    private double usedCache; // The power used cache.
    
    /**
     * Constructor
     *
     * @param building the building the panel is for.
     * @param desktop The main desktop.
     */
    public PowerBuildingPanel(Building building, MainDesktopPane desktop) {
        
        // Use BuildingFunctionPanel constructor
        super(building, desktop);
        
        // Check if the building is a power producer.
        if (building.hasFunction(PowerGeneration.NAME)) isProducer = true;
        else isProducer = false;
        
        // Set the layout
        if (isProducer) setLayout(new GridLayout(3, 1, 0, 0));
        else setLayout(new GridLayout(2, 1, 0, 0));
        
        // Prepare power status label.
        powerStatusCache = building.getPowerMode();
        powerStatusLabel = new JLabel("Power Status: " + powerStatusCache, JLabel.CENTER);
        add(powerStatusLabel);
        
        // If power producer, prepare power producer label.
        if (isProducer) {
        	try {
        		PowerGeneration generator = (PowerGeneration) building.getFunction(PowerGeneration.NAME);
            	productionCache = generator.getGeneratedPower();
            	productionLabel = new JLabel("Power Produced: " + formatter.format(productionCache) + " kW.",
                	JLabel.CENTER);
            	add(productionLabel);
        	}
        	catch (BuildingException e) {}
        }
        
        // Prepare power used label.
        if (powerStatusCache.equals(Building.FULL_POWER)) 
            usedCache = building.getFullPowerRequired();
        else if (powerStatusCache.equals(Building.POWER_DOWN)) 
            usedCache = building.getPoweredDownPowerRequired();
        else usedCache = 0D;
        usedLabel = new JLabel("Power Used: " + formatter.format(usedCache) + " kW.", JLabel.CENTER);
        add(usedLabel);
    }
    
    /**
     * Update this panel
     */
    public void update() {
    
        // Update power status if necessary.
        if (!powerStatusCache.equals(building.getPowerMode())) {
            powerStatusCache = building.getPowerMode();
            powerStatusLabel.setText("Power Status: " + powerStatusCache);
        }
        
        // Update power production if necessary.
        if (isProducer) {
        	try {
        		PowerGeneration generator = (PowerGeneration) building.getFunction(PowerGeneration.NAME);
            	if (productionCache != generator.getGeneratedPower()) {
                	productionCache = generator.getGeneratedPower();
                	productionLabel.setText("Power Produced: " + formatter.format(productionCache) + " kW.");
            	}
            }
            catch (BuildingException e) {}
        }
        
        // Update power used if necessary.
        double usedPower = 0D;
        if (powerStatusCache.equals(Building.FULL_POWER)) 
            usedPower = building.getFullPowerRequired();
        else if (powerStatusCache.equals(Building.POWER_DOWN)) 
            usedPower = building.getPoweredDownPowerRequired();
        if (usedCache != usedPower) {
            usedCache = usedPower;
            usedLabel.setText("Power Used: " + formatter.format(usedCache) + " kW.");
        }
    }
}