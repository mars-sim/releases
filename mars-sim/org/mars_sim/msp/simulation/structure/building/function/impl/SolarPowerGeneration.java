/**
 * Mars Simulation Project
 * SolarPowerGeneration.java
 * @version 2.75 2003-04-16
 * @author Scott Davis
 */
 
package org.mars_sim.msp.simulation.structure.building.function.impl;
 
import java.io.Serializable;
import org.mars_sim.msp.simulation.*;
import org.mars_sim.msp.simulation.structure.*;
import org.mars_sim.msp.simulation.structure.building.*;
import org.mars_sim.msp.simulation.structure.building.function.PowerGeneration;
 
/**
 * Solar power implementation of the PowerGeneration building function.
 */
public class SolarPowerGeneration implements PowerGeneration, Serializable {
    
    private Building building;
    private double powerGeneratable;
    
    /**
     * Constructor
     *
     * @param building the building this is implemented for.
     * @param powerGeneration baseline power generation.
     */
    public SolarPowerGeneration(Building building, double powerGeneratable) {
        this.building = building;
        this.powerGeneratable = powerGeneratable;
    }
    
    /**
     * Gets the amount of electrical power generated.
     * @return power generated in kW
     */
    public double getGeneratedPower() {
        BuildingManager manager = building.getBuildingManager();
        Coordinates location = manager.getSettlement().getCoordinates();
        Mars mars = manager.getSettlement().getMars();
        double sunlight = (double) mars.getSurfaceFeatures().getSurfaceSunlight(location) / 127D;
        return sunlight * powerGeneratable;
    }
}
