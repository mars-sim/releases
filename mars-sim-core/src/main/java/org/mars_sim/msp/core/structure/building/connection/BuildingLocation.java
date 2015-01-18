/**
 * Mars Simulation Project
 * BuildingLocation.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.connection;

import java.io.Serializable;

import org.mars_sim.msp.core.structure.building.Building;

/**
 * An internal building location.
 */
public class BuildingLocation implements Serializable, InsidePathLocation {

    // Data members
    private Building building;
    private double xLoc;
    private double yLoc;
    
    /**
     * Constructor
     * @param building the building.
     * @param xLoc the X location
     * @param yLoc the Y location
     */
    public BuildingLocation(Building building, double xLoc, double yLoc) {
        this.building = building;
        this.xLoc = xLoc;
        this.yLoc = yLoc;
    }
    
    public Building getBuilding() {
        return building;
    }
    
    @Override
    public double getXLocation() {
        return xLoc;
    }

    @Override
    public double getYLocation() {
        return yLoc;
    }
}