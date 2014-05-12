/**
 * Mars Simulation Project
 * SettlementMapLayer.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.settlement;

import java.awt.Graphics2D;

import org.mars_sim.msp.core.structure.Settlement;

/**
 * An interface for a display layer on the settlement map.
 */
public interface SettlementMapLayer {

	/**
	 * Displays the settlement map layer.
	 * @param g2d the graphics context.
	 * @param settlement the settlement to display.
	 * @param xPos the X center position.
	 * @param yPos the Y center position.
	 * @param mapWidth the width of the map.
	 * @param mapHeight the height of the map.
	 * @param rotation the rotation (radians)
	 * @param scale the map scale.
	 */
	public void displayLayer(
		Graphics2D g2d, Settlement settlement, double xPos, 
		double yPos, int mapWidth, int mapHeight, double rotation, double scale
	);

	/**
	 * Destroy the map layer.
	 */
	public void destroy();
}