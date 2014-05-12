/**
 * Mars Simulation Project
 * StandardPowerSource.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;

import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;

/**
 * A power source that gives a constant supply of power.
 */
public class StandardPowerSource
extends PowerSource
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	public StandardPowerSource(double maxPower) {
		// Call PowerSource constructor.
		super(PowerSourceType.STANDARD, maxPower);
	}

	/**
	 * Gets the current power produced by the power source.
	 * @param building the building this power source is for.
	 * @return power (kW)
	 */
	public double getCurrentPower(Building building) {
		return getMaxPower();
	}


	public double getAveragePower(Settlement settlement) {
		return getMaxPower();
	}
	
	@Override
	public double getMaintenanceTime() {
	    return getMaxPower() * 2D;
	}
}