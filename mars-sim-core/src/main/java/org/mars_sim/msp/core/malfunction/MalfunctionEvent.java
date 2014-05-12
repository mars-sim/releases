package org.mars_sim.msp.core.malfunction;

import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.events.HistoricalEventCategory;
import org.mars_sim.msp.core.person.EventType;

/**
 * This class represents the historical action of a Malfunction occuring or
 * being resolved.
 */
public class MalfunctionEvent
extends HistoricalEvent {

	/**
	 * Create an event associated to a Malfunction.
	 * @param entity Malfunctionable entity with problem.
	 * @param malfunction Problem that has occurred.
	 * @param fixed Is the malfunction resolved.
	 */
	public MalfunctionEvent(Malfunctionable entity, Malfunction malfunction, boolean fixed) {
		super(
			HistoricalEventCategory.MALFUNCTION,
			(fixed ? EventType.MALFUNCTION_FIXED : EventType.MALFUNCTION_UNFIXED), 
			entity,
			malfunction.getName() + (fixed? " fixed" : " occurred")
		);
	}
}