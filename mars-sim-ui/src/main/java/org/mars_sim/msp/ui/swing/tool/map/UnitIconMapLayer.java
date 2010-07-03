/**
 * Mars Simulation Project
 * UnitIconMapLayer.java
 * @version 2.87 2009-09-23
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.tool.map;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.IntPoint;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.ui.swing.unit_display_info.UnitDisplayInfo;
import org.mars_sim.msp.ui.swing.unit_display_info.UnitDisplayInfoFactory;

/**
 * The UnitMapLayer is a graphics layer to display unit icons.
 */
public class UnitIconMapLayer extends UnitMapLayer {

	private Component displayComponent;
	
	public UnitIconMapLayer(Component displayComponent) {
		this.displayComponent = displayComponent;
	}
	
	/**
	 * Displays a unit on the map.
	 * @param unit the unit to display.
	 * @param mapCenter the location center of the map.
	 * @param mapType the type of map.
	 * @param g the graphics context.
	 */
	protected void displayUnit(Unit unit, Coordinates mapCenter, String mapType, Graphics g) {
		
		IntPoint location = MapUtils.getRectPosition(unit.getCoordinates(), mapCenter, mapType);
        UnitDisplayInfo displayInfo = UnitDisplayInfoFactory.getUnitDisplayInfo(unit);
        
        IntPoint imageLocation = getUnitDrawLocation(location, displayInfo.getSurfMapIcon(unit));
        int locX = imageLocation.getiX();
        int locY = imageLocation.getiY();
        
        if (!(displayInfo.isMapBlink(unit) && getBlinkFlag())) {
        	Icon displayIcon = null;
        	if (TopoMarsMap.TYPE.equals(mapType)) displayIcon = displayInfo.getTopoMapIcon(unit);
        	else displayIcon = displayInfo.getSurfMapIcon(unit);
        	displayIcon.paintIcon(displayComponent, g, locX, locY);
        }
	}
	
    /** 
     * Gets the unit image draw position on the map image.
     *
     * @param unitPosition absolute unit position
     * @param unitIcon unit's map image icon
     * @return draw position for unit image
     */
    private IntPoint getUnitDrawLocation(IntPoint unitPosition, Icon unitIcon) {
        
        int unitX = unitPosition.getiX();
        int unitY = unitPosition.getiY();
        int iconHeight = unitIcon.getIconHeight();
        int iconWidth = unitIcon.getIconWidth();
        
        return new IntPoint(unitX - (iconWidth / 2), unitY - (iconHeight / 2));
    }
}