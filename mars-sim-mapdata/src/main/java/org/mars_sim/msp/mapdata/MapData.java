/**
 * Mars Simulation Project
 * MapData.java
 * @version 3.02 2011-11-09
 * @author Scott Davis
 */

package org.mars_sim.msp.mapdata;

import java.awt.Color;
import java.awt.Image;

/**
 * An interface for map data.
 */
public interface MapData {

    /**
     * Generates and returns a map image with the given parameters.
     * @param centerPhi the phi center location of the map.
     * @param centerTheta the theta center location of the map.
     * @return The map image.
     */
    public Image getMapImage(double centerPhi, double centerTheta);
    
    /**
     * Gets the RGB map color at a given location.
     * @param phi the phi location.
     * @param theta the theta location.
     * @return the RGB map color.
     */
    public Color getRGBColor(double phi, double theta);
}