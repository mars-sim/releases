/**
 * Mars Simulation Project
 * FoodProductionProcess.java
 * @version 3.07 2014-11-23
 * @author Manny Kung
 */

package org.mars_sim.msp.core.foodProduction;

import org.mars_sim.msp.core.structure.building.function.FoodProduction;

import java.io.Serializable;

/**
 * A manufacturing process.
 */
public class FoodProductionProcess implements Serializable {

	// Data members.
	private FoodProduction kitchen;
	private FoodProductionProcessInfo info;
	private double workTimeRemaining;
	private double processTimeRemaining;
	
	/**
	 * Constructor
	 * @param info information about the process.
	 * @param kitchen the manufacturing kitchen where the process is taking place.
	 */
	public FoodProductionProcess(FoodProductionProcessInfo info, FoodProduction kitchen) {
		this.info = info;
		this.kitchen = kitchen;
		workTimeRemaining = info.getWorkTimeRequired();
		processTimeRemaining = info.getProcessTimeRequired();
	}
	
	/**
	 * Gets the information about the process.
	 * @return process information
	 */
	public FoodProductionProcessInfo getInfo() {
		return info;
	}
	
	/**
	 * Gets the remaining work time.
	 * @return work time (millisols)
	 */
	public double getWorkTimeRemaining() {
		return workTimeRemaining;
	}
	
	/**
	 * Adds work time to the process.
	 * @param workTime work time (millisols)
	 */
	public void addWorkTime(double workTime) {
		workTimeRemaining -= workTime;
		if (workTimeRemaining < 0D) workTimeRemaining = 0D;
	}
	
	/**
	 * Gets the remaining process time.
	 * @return process time (millisols)
	 */
	public double getProcessTimeRemaining() {
		return processTimeRemaining;
	}
	
	/**
	 * Adds process time to the process.
	 * @param processTime process time (millisols)
	 */
	public void addProcessTime(double processTime) {
		processTimeRemaining -= processTime;
		if (processTimeRemaining < 0D) processTimeRemaining = 0D;
	}
	
	@Override
	public String toString() {
		return info.getName();
	}

	/**
	 * Gets the food production building function.
	 * @return food production building function.
	 */
	public FoodProduction getKitchen() {
		return kitchen;
	}

	/**
	 * Prepare object for garbage collection.
	 */
    public void destroy() {
        kitchen = null;
        info.destroy();
        info = null;
    }
}