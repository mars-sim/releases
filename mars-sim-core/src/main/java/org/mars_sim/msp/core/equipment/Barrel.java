/**
 * Mars Simulation Project
 * Barrel.java
 * @version 3.05 2013-07-01
 * @author Scott Davis
 */

package org.mars_sim.msp.core.equipment;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.resource.Phase;

import java.io.Serializable;

/**
 * A barrel container for holding liquid amount resources.
 */
public class Barrel extends Equipment implements Container, Serializable {

	// Static data members
	public static final String TYPE = "Barrel";
    public static final double CAPACITY = 200D;
    public static final double EMPTY_MASS = 10D;
	
	/**
	 * Constructor
	 * @param location the location of the barrel.
	 * @throws Exception if error creating barrel.
	 */
	public Barrel(Coordinates location) {
		// Use Equipment constructor
		super(TYPE, location);
		
		// Sets the base mass of the barrel.
		setBaseMass(EMPTY_MASS);
		
		// Set the liquid capacity.
		getInventory().addAmountResourcePhaseCapacity(Phase.LIQUID, CAPACITY);
	}
	
	/**
	 * Gets the phase of resources this container can hold.
	 * @return resource phase.
	 */
	public Phase getContainingResourcePhase() {
		return Phase.LIQUID;
	}
	
	/**
     * Gets the total capacity of resource that this container can hold.
     * @return total capacity (kg).
     */
    public double getTotalCapacity() {
        return CAPACITY;
    }
}