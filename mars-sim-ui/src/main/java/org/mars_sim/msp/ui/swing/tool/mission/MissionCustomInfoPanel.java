/**
 * Mars Simulation Project
 * MissionCustomInfoPanel.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.mission;

import javax.swing.JPanel;

import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionEvent;

/**
 * A panel for displaying custom mission information.
 */
public abstract class MissionCustomInfoPanel
extends JPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/**
	 * Updates the panel based on a mission event.
	 * @param e the mission event.
	 */
	public abstract void updateMissionEvent(MissionEvent e);

	/**
	 * Updates the panel based on a new mission to display.
	 * @param mission the mission to display.
	 */
	public abstract void updateMission(Mission mission);
}