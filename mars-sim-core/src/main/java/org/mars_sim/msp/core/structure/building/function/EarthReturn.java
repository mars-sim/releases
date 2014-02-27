/**
 * Mars Simulation Project
 * EarthReturn.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */

package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;
import java.util.Iterator;

import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingConfig;

/**
 * A building function for launching an Earth return mission.
 */
public class EarthReturn extends Function implements Serializable {

    public static final String NAME = "Earth Return";
    
    // Data members
    private int crewCapacity;
    private boolean hasLaunched;
    
    /**
     * Constructor
     * @param building the building this function is for.
     */
    public EarthReturn(Building building) {
        // Use Function constructor.
        super(NAME, building);
        
        // Populate data members.
        BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();
        crewCapacity = config.getEarthReturnCrewCapacity(building.getName());
        
        // Initialize hasLaunched to false.
        hasLaunched = false;
    }
    
    /**
     * Gets the value of the function for a named building.
     * @param buildingName the building name.
     * @param newBuilding true if adding a new building.
     * @param settlement the settlement.
     * @return value (VP) of building function.
     */
    public static double getFunctionValue(String buildingName, boolean newBuilding,
            Settlement settlement) {
        
        // Settlements need enough Earth return facilities to support population.
        double demand = settlement.getAllAssociatedPeople().size();
        
        // Supply based on wear condition of buildings.
        double supply = 0D;
        Iterator<Building> i = settlement.getBuildingManager().getBuildings(NAME).iterator();
        while (i.hasNext()) {
            Building earthReturnBuilding = i.next();
            EarthReturn earthReturn = (EarthReturn) earthReturnBuilding.getFunction(NAME);
            double crewCapacity = earthReturn.getCrewCapacity();
            double wearFactor = ((earthReturnBuilding.getMalfunctionManager().getWearCondition() / 100D) * .75D) + .25D;
            supply += crewCapacity * wearFactor;
        }
        
        if (!newBuilding) {
            BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();
            supply -= config.getEarthReturnCrewCapacity(buildingName);
            if (supply < 0D) supply = 0D;
        }
        
        return demand / (supply + 1D);
    }
    
    /**
     * Get the crew capacity for an Earth return mission.
     * @return crew capacity.
     */
    public int getCrewCapacity() {
        return crewCapacity;
    }
    
    /**
     * Checks if the Earth return mission for this building has launched.
     * @return true if mission has launched.
     */
    public boolean hasLaunched() {
        return hasLaunched;
    }
    
    @Override
    public double getFullPowerRequired() {
        return 0D;
    }

    @Override
    public double getPowerDownPowerRequired() {
        return 0D;
    }

    @Override
    public void timePassing(double time) {
        // Do nothing
    }
}