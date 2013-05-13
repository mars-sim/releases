/**
 * Mars Simulation Project
 * GroundVehicleMaintenance.java
 * @version 3.04 2012-12-07
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingConfig;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Iterator;
 
/**
 * The GroundVehicleMaintenance class is a building function for a building
 * capable of maintaining ground vehicles.
 */
public class GroundVehicleMaintenance extends VehicleMaintenance implements Serializable {
    
    public static final String NAME = "Ground Vehicle Maintenance";

    /**
     * Constructor
     * @param building the building the function is for.
     * @throws BuildingException if error in construction.
     */
    public GroundVehicleMaintenance(Building building) {
        // Call VehicleMaintenance constructor.
        super(NAME, building);

        BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();

        vehicleCapacity = config.getVehicleCapacity(building.getName());
        
        int parkingLocationNum = config.getParkingLocationNumber(building.getName());
        for (int x = 0; x < parkingLocationNum; x++) {
            Point2D.Double parkingLocationPoint = config.getParkingLocation(building.getName(), x);
            addParkingLocation(parkingLocationPoint.getX(), parkingLocationPoint.getY());
        }
    }
    
    /**
     * Gets the value of the function for a named building.
     * @param buildingName the building name.
     * @param newBuilding true if adding a new building.
     * @param settlement the settlement.
     * @return value (VP) of building function.
     * @throws Exception if error getting function value.
     */
    public static double getFunctionValue(String buildingName, boolean newBuilding,
            Settlement settlement) {
        
        // Demand is one ground vehicle capacity for every ground vehicles.
        double demand = settlement.getAllAssociatedVehicles().size();
        
        double supply = 0D;
        boolean removedBuilding = false;
        Iterator<Building> i = settlement.getBuildingManager().getBuildings(NAME).iterator();
        while (i.hasNext()) {
            Building building = i.next();
            if (!newBuilding && building.getName().equals(buildingName) && !removedBuilding) {
                removedBuilding = true;
            }
            else {
                GroundVehicleMaintenance maintFunction = 
                    (GroundVehicleMaintenance) building.getFunction(NAME);
                double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
                supply += maintFunction.getVehicleCapacity() * wearModifier;
            }
        }
        
        double vehicleCapacityValue = demand / (supply + 1D);
        
        BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();
        double vehicleCapacity = config.getVehicleCapacity(buildingName);
        
        return vehicleCapacity * vehicleCapacityValue;
    }
}