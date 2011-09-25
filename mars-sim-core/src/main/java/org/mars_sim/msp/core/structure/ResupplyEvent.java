/**
 * Mars Simulation Project
 * ResupplyEvent.java
 * @version 3.00 2010-08-10
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure;

import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.events.HistoricalEventManager;

/**
 * An historical event for the arrival of a settlement 
 * resupply mission from Earth.
 */
public class ResupplyEvent extends HistoricalEvent {

	/**
	 * Constructor
	 * @param settlement the name of the settlement getting the supplies.
	 * @param resupplyName the name of the resupply mission.
	 */
	public ResupplyEvent(Settlement settlement, String resupplyName) {
		super(HistoricalEventManager.SUPPLY, "Supplies delivered", settlement, 
			resupplyName + " arrive at " + settlement.getName());
	}
}