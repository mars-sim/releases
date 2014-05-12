/**
 * Mars Simulation Project
 * Towing.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */

package org.mars_sim.msp.core.vehicle;

public interface Towing {

	/**
	 * Sets the vehicle this rover is currently towing.
	 * @param towedVehicle the vehicle being towed.
	 */
	public void setTowedVehicle(Vehicle towedVehicle);

	/**
	 * Gets the vehicle this rover is currently towing.
	 * @return towed vehicle.
	 */
	public Vehicle getTowedVehicle();
}