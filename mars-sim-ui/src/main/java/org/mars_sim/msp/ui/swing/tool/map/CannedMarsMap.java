/**
 * Mars Simulation Project
 * CannedMarsMap.java
 * @version 3.06 2014-01-29
 * @author Greg Whelan
 */

package org.mars_sim.msp.ui.swing.tool.map;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;

import org.mars_sim.msp.core.*;
import org.mars_sim.msp.mapdata.MapData;

/** 
 * The CannedMarsMap class reads in data from files in the map_data
 * jar file in order to generate a map image.
 */
public abstract class CannedMarsMap implements Map {
    
    private static Logger logger = Logger.getLogger(CannedMarsMap.class.getName());
	
	// Data members
	private MapData mapData;
	private JComponent displayArea = null;
	private Coordinates currentCenter = null;
	private Image mapImage = null;
	private boolean mapImageDone = false;
    
	/**
	 * Constructor
	 * @param displayArea the component display area.
	 * @param mapData the map data.
	 */
	public CannedMarsMap(JComponent displayArea, MapData mapData) {
	    this.mapData = mapData;
	    this.displayArea = displayArea;
	}
	
	/**
	 * Creates a map image for a given center location.
	 * @param center the center location of the map display.
	 * @return the map image.
	 */
	private Image createMapImage(Coordinates center) {
	    return mapData.getMapImage(center.getPhi(), center.getTheta());
	}
	
	/** 
	 * Creates a 2D map at a given center point.
	 * 
	 * @param newCenter the new center location
	 */
	public void drawMap(Coordinates newCenter) {
		
		if ((newCenter != null) && (!newCenter.equals(currentCenter))) {
			mapImage = createMapImage(newCenter);
		
			MediaTracker mt = new MediaTracker(displayArea);
			mt.addImage(mapImage, 0);
			try {
				mt.waitForID(0);
			} 
			catch (InterruptedException e) {
				logger.log(Level.SEVERE,"MediaTracker interrupted " + e);
			}
			mapImageDone = true;
			currentCenter = new Coordinates(newCenter);
		}
	}
	
	/** 
	 * Checks if a requested map is complete.
	 * 
	 * @return true if requested map is complete
	 */
	public boolean isImageDone() {
		return mapImageDone;
	}
	
	/** 
	 * Gets the constructed map image.
	 * 
	 * @return constructed map image
	 */
	public Image getMapImage() {
		return mapImage;
	}
}