/**
 * Mars Simulation Project
 * TransportRover.java
 * @version 2.75 2003-05-06
 */

package org.mars_sim.msp.simulation.vehicle;

import org.mars_sim.msp.simulation.*;
import org.mars_sim.msp.simulation.structure.*;
import org.mars_sim.msp.simulation.person.medical.MedicalAid;
import java.io.Serializable;

/**
 * The TransportRover class is a rover designed for transporting people
 * from settlement to settlement.
 */
public class TransportRover extends Rover implements Serializable {

    // Static data members
    private static final double RANGE = 4000D; // Operating range of rover in km.
    private static final int CREW_CAPACITY = 8; // Max number of crewmembers.
    private static final double CARGO_CAPACITY = 12000D; // Cargo capacity of rover in kg.
    private static final double METHANE_CAPACITY = 3750D; // Methane capacity of rover in kg.
    private static final double OXYGEN_CAPACITY = 1000D; // Oxygen capacity of rover in kg.
    private static final double WATER_CAPACITY = 4000D; // Water capacity of rover in kg.
    private static final double FOOD_CAPACITY = 787.5D; // Food capacity of rover in kg.
    private static final int SICKBAY_LEVEL = 3; // Treatment level of sickbay.
    private static final int SICKBAY_BEDS = 2; // Number of beds in sickbay.
    private SickBay sickBay = null;

    /**
     * Constructs an TransportRover object at a given settlement.
     * @param name the name of the rover
     * @param settlement the settlementt he rover is parked at
     * @param mars the mars instance
     */
    public TransportRover(String name, Settlement settlement, Mars mars) {
        // Use the Rover constructor
        super(name, settlement, mars);

        initTransportRoverData();

        // Add EVA suits
        addEVASuits();
    }

    /**
     * Initialize rover data
     */
    private void initTransportRoverData() {

        // Add scope to malfunction manager.
	    malfunctionManager.addScopeString("TransportRover");
	    
        // Set operating range of rover.
        range = RANGE;
        
        // Set crew capacity
	    crewCapacity = CREW_CAPACITY;

        // Set the cargo capacity of rover.
	    inventory.setTotalCapacity(CARGO_CAPACITY);
	
	    // Set resource capacities of rover
	    inventory.setResourceCapacity(Resource.METHANE, METHANE_CAPACITY);
	    inventory.setResourceCapacity(Resource.OXYGEN, OXYGEN_CAPACITY);
	    inventory.setResourceCapacity(Resource.WATER, WATER_CAPACITY);
	    inventory.setResourceCapacity(Resource.FOOD, FOOD_CAPACITY);

        sickBay = new SickBay(this, SICKBAY_LEVEL, SICKBAY_BEDS);
    }

    /**
     * Returns a string describing the vehicle.
     * @return string describing vehicle
     */
    public String getDescription() {
        return "Long Range Transport Rover";
    }

    /**
     * Returns a MedicalAid that is available in this Vehicle. This implementation
     * return a reference to the internal Sickbay.
     * @return Sick Bay
     */
    public MedicalAid getMedicalFacility() {
        return sickBay;
    }
}
