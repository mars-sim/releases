/**
 * Mars Simulation Project
 * VehicleDisplayInfoBean.java
 * @version 3.07 2014-10-12
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_display_info;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.ui.swing.ImageLoader;

import javax.swing.*;
import java.awt.*;

/**
 * Provides display information about a vehicle.
 */
abstract class VehicleDisplayInfoBean implements UnitDisplayInfo {
    
    // Navigator click range in km.
    private static double VEHICLE_CLICK_RANGE = 40D;
    
    // Data members
    private Icon surfMapIcon;
    private Icon topoMapIcon;
    private Font mapLabelFont;
    
    /**
     * Constructor
     */
    VehicleDisplayInfoBean() {
        surfMapIcon = ImageLoader.getIcon("VehicleSymbol");
        topoMapIcon = ImageLoader.getIcon("VehicleSymbolBlack");
        mapLabelFont = new Font("SansSerif", Font.PLAIN, 9);
    }
    
    /** 
     * Checks if unit is to be displayed on the navigator tool map.
     * @param unit the unit to display
     * @return true if unit is to be displayed on navigator map.
     */
    public boolean isMapDisplayed(Unit unit) {
        boolean result = true;
        
        Unit container = unit.getContainerUnit();
        if (container != null) result = false;
        
        Vehicle vehicle = (Vehicle) unit;
        if (vehicle.isSalvaged()) result = false;
        
        // Do not display towed vehicle on map.
        if (vehicle.getStatus().equals(Vehicle.TOWED)) {
            result = false;
        }
        
        return result;
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
    	return ((Vehicle) unit).isEmergencyBeacon();
    }
    
    /** 
     * Gets the label color for surface navigator map. 
     * @return color
     */
    public Color getSurfMapLabelColor() {
        return Color.white;
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
        return VEHICLE_CLICK_RANGE;
    }
    
    /** 
     * Checks if the unit is to be displayed on the navigator tool globe.
     * @param unit the unit to display.
     * @return true if unit is to be displayed on globe
     */
    public boolean isGlobeDisplayed(Unit unit) {
        boolean result = true;
        
        Unit container = unit.getContainerUnit();
        if (container != null) result = false;
        
        Vehicle vehicle = (Vehicle) unit;
        if (vehicle.isSalvaged()) result = false;
        
        return result;
    }
    
    /** 
     * Gets display color for surface globe. 
     * @return color
     */
    public Color getSurfGlobeColor() {
        return Color.white;
    }
    
    /** 
     * Gets display color for topo globe.
     * @return color
     */
    public Color getTopoGlobeColor() {
        return Color.black;
    }
}