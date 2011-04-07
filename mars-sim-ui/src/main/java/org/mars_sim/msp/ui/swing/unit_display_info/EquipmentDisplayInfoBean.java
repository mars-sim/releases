/**
 * Mars Simulation Project
 * EquipmentDisplayInfo.java
 * @version 3.00 2010-08-10
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_display_info;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.sound.SoundConstants;

import javax.swing.*;
import java.awt.*;

/**
 * Provides display information about a piece of equipment.
 */
class EquipmentDisplayInfoBean implements UnitDisplayInfo {
    
    // Data members
    private Icon buttonIcon;
    
    /**
     * Constructor
     */
    EquipmentDisplayInfoBean() {
        buttonIcon = ImageLoader.getIcon("EquipmentIcon");
    }
    
    /** 
     * Checks if unit is to be displayed on the navigator tool map.
     * @param unit the unit to display
     * @return true if unit is to be displayed on navigator map.
     */
    public boolean isMapDisplayed(Unit unit) {
        return false;
    }
    
    /** 
     * Gets display icon for the surface navigator map. 
     * @param unit the unit to display 
     * @return icon
     */
    public Icon getSurfMapIcon(Unit unit) {
        return null;
    }
    
    /** 
     * Gets display icon for topo navigator map. 
     * @param unit the unit to display 
     * @return icon
     */
    public Icon getTopoMapIcon(Unit unit) {
        return null;
    }
    
    /**
     * Checks if the map icon should blink on and off.
     * @param unit the unit to display
     * @return true if blink
     */
    public boolean isMapBlink(Unit unit) {
    	return false;
    }
    
    /** 
     * Gets the label color for surface navigator map. 
     * @return color
     */
    public Color getSurfMapLabelColor() {
        return null;
    }
    
    /** 
     * Gets the label color for topo navigator map. 
     * @return color
     */
    public Color getTopoMapLabelColor() {
        return null;
    }
    
    /** 
     * Gets the label font for navigator map. 
     * @return font
     */
    public Font getMapLabelFont() {
        return null;
    }

    /** 
     * Gets the range (km) for clicking on unit on navigator map. 
     * @return clicking range
     */
    public double getMapClickRange() {
        return 0;
    }
    
    /** 
     * Checks if the unit is to be displayed on the navigator tool globe.
     * @param unit the unit to display.
     * @return true if unit is to be displayed on globe
     */
    public boolean isGlobeDisplayed(Unit unit) {
        return false;
    }
    
    /** 
     * Gets display color for surface globe. 
     * @return color
     */
    public Color getSurfGlobeColor() {
        return null;
    }
    
    /** 
     * Gets display color for topo globe.
     * @return color
     */
    public Color getTopoGlobeColor() {
        return null;
    }
    
    /** 
     * Gets icon for unit button.
     * @return icon
     */
    public Icon getButtonIcon() {
        return buttonIcon;
    }
    
    /**
     * Gets a sound appropriate for this unit.
     * @param unit the unit to display.
     * @returns sound filepath for unit or empty string if none.
     */
    public String getSound(Unit unit) {
    	return SoundConstants.SND_EQUIPMENT;
    }
}