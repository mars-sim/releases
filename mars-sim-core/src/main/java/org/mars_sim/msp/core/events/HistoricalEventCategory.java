package org.mars_sim.msp.core.events;

import org.mars_sim.msp.core.Msg;

/**
 * @author stpa
 * 2014-03-06
 */
public enum HistoricalEventCategory {

	MEDICAL (Msg.getString("HistoricalEventType.medical")), //$NON-NLS-1$
	MALFUNCTION (Msg.getString("HistoricalEventType.malfunction")), //$NON-NLS-1$
	MISSION (Msg.getString("HistoricalEventType.mission")), //$NON-NLS-1$
	TASK (Msg.getString("HistoricalEventType.task")), //$NON-NLS-1$
	TRANSPORT (Msg.getString("HistoricalEventType.transport")); //$NON-NLS-1$

	private String name;

	/** hidden constructor. */
	private HistoricalEventCategory(String name) {
		this.name = name;
	}

	/** gets the internationalized name for display in user interface. */
	public String getName() {
		return this.name;
	}
}
