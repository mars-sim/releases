/**
 * Mars Simulation Project
 * SettlementDisplayInfo.java
 * @version 2.78 2005-08-23
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_display_info;  
 
import java.awt.*;
import javax.swing.*;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.sound.SoundConstants;

/**
 * Provides display information about a settlement.
 */
class SettlementDisplayInfoBean implements UnitDisplayInfo {
    
    // Navigator click range in km.
    private static double SETTLEMENT_CLICK_RANGE = 90D;
    
    // Data members
    private Icon surfMapIcon;
    private Icon topoMapIcon;
    private Icon buttonIcon;
    private Font mapLabelFont;
    
    /**
     * Constructor
     */
    SettlementDisplayInfoBean() {
        surfMapIcon = ImageLoader.getIcon("SettlementSymbol");
        topoMapIcon = ImageLoader.getIcon("SettlementSymbolBlack");
        buttonIcon = ImageLoader.getIcon("SettlementIcon");
        mapLabelFont = new Font("SansSerif", Font.PLAIN, 12);
    }
    
    /** 
     * Checks if unit is to be displayed on the navigator tool map.
     * @param unit the unit to display
     * @return true if unit is to be displayed on navigator map.
     */
    public boolean isMapDisplayed(Unit unit) {
        return true;
    }
    
    /** 
     * Gets display icon for the surface navigator map. 
     * @param unit the unit to display 
     * @return icon
     */
    public Icon getSurfMapIcon(Unit unit) {
        return surfMapIcon;
    }
    
    /** 
     * Gets display icon for topo navigator map. 
     * @param unit the unit to display 
     * @return icon
     */
    public Icon getTopoMapIcon(Unit unit) {
        return topoMapIcon;
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
        return Color.green;
    }
    
    /** 
     * Gets the label color for topo navigator map. 
     * @return color
     */
    public Color getTopoMapLabelColor() {
        return Color.black;
    }
    
    /** 
     * Gets the label font for navigator map. 
     * @return font
     */
    public Font getMapLabelFont() {
        return mapLabelFont;
    }

    /** 
     * Gets the range (km) for clicking on unit on navigator map. 
     * @return clicking range
     */
    public double getMapClickRange() {
        return SETTLEMENT_CLICK_RANGE;
    }
    
    /** 
     * Checks if the unit is to be displayed on the navigator tool globe.
     * @param unit the unit to display.
     * @return true if unit is to be displayed on globe
     */
    public boolean isGlobeDisplayed(Unit unit) {
        return true;
    }
    
    /** 
     * Gets display color for surface globe. 
     * @return color
     */
    public Color getSurfGlobeColor() {
        return Color.green;
    }
    
    /** 
     * Gets display color for topo globe.
     * @return color
     */
    public Color getTopoGlobeColor() {
        return Color.black;
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
    	return SoundConstants.SND_SETTLEMENT;
    }
}