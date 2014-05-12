/**
 * Mars Simulation Project
 * Cooking.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import org.mars_sim.msp.core.time.MarsClock;

import java.io.Serializable;

/**
 * This class represents a cooked meal from a kitchen.
 */
public class CookedMeal
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** The time (millisols) between when a meal is cooked and when it expires. */
	private static final double SHELF_LIFE = 100D;

	// Data members
	private int quality;
	private MarsClock expirationTime;

	/**
	 * Constructor.
	 * @param quality the quality of the food
	 * @param creationTime the time the food was cooked.
	 */
	public CookedMeal(int quality, MarsClock creationTime) {
		this.quality = quality;
		expirationTime = (MarsClock) creationTime.clone();
		expirationTime.addTime(SHELF_LIFE);
	}

	/**
	 * Gets the quality of the meal.
	 * @return quality
	 */
	public int getQuality() {
		return quality;
	}

	/**
	 * Gets the expiration time of the meal.
	 * @return expiration time
	 */
	public MarsClock getExpirationTime() {
		return expirationTime;
	}
}