/**
 * Mars Simulation Project
 * UnitDisplayInfo.java
 * @version 3.00 2010-08-10
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_display_info;

import org.mars_sim.msp.core.Unit;

import javax.swing.*;
import java.awt.*;

/**
 * Provides display information about a unit.
 */
public interface UnitDisplayInfo {
    
    /** 
     * Checks if unit is to be displayed on the navigator tool map.
     * @param unit the unit to display
     * @return true if unit is to be displayed on navigator map.
     */
    public boolean isMapDisplayed(Unit unit);
    
    /** 
     * Gets display icon for the surface navigator map. 
     * @param unit the unit to display 
     * @return icon
     */
    public Icon getSurfMapIcon(Unit unit);
    
    /** 
     * Gets display icon for topo navigator map.
     * @param unit the unit to display  
     * @return icon
     */
    public Icon getTopoMapIcon(Unit unit);
    
    /**
     * Checks if the map icon should blink on and off.
     * @param unit the unit to display
     * @return true if blink
     */
    public boolean isMapBlink(Unit unit);
    
    /** 
     * Gets the label color for surface navigator map. 
     * @return color
     */
    public Color getSurfMapLabelColor();
    
    /** 
     * Gets the label color for topo navigator map. 
     * @return color
     */
    public Color getTopoMapLabelColor();
    
    /** 
     * Gets the label font for navigator map. 
     * @return font
     */
    public Font getMapLabelFont();

    /** 
     * Gets the range (km) for clicking on unit on navigator map. 
     * @return clicking range
     */
    public double getMapClickRange();
    
    /** 
     * Checks if the unit is to be displayed on the navigator tool globe.
     * @param unit the unit to display.
     * @return true if unit is to be displayed on globe
     */
    public boolean isGlobeDisplayed(Unit unit);
    
    /** 
     * Gets display color for surface globe. 
     * @return color
     */
    public Color getSurfGlobeColor();
    
    /** 
     * Gets display color for topo globe.
     * @return color
     */
    public Color getTopoGlobeColor();
    
    /** 
     * Gets icon for unit button.
     * @return icon
     */
    public Icon getButtonIcon();
    
    /**
     * Gets a sound appropriate for this unit.
     * @param unit the unit to display.
     * @return sound filepath for unit or empty string if none.
     */
    public String getSound(Unit unit);
}