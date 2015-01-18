/**
 * Mars Simulation Project
 * Towing.java
 * @version 3.07 2014-12-06

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