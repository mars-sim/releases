/**
 * Mars Simulation Project
 * MapLayer.java
 * @version 2.80 2006-10-28
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.tool.map;

import java.awt.Graphics;

import org.mars_sim.msp.core.Coordinates;

/**
 * The MapLayer interface is a graphics layer painted on the map display.
 */
public interface MapLayer {
	
	/**
     * Displays the layer on the map image.
     * @param mapCenter the location of the center of the map.
     * @param mapType the type of map.
     * @param g graphics context of the map display.
     */
    public void displayLayer(Coordinates mapCenter, String mapType, Graphics g);
}