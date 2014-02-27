/**
 * Mars Simulation Project
 * ProspectingSitePanel.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.tool.mission.create;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.IntPoint;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.tool.map.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * A wizard panel for the ice or regolith prospecting site.
 */
class ProspectingSitePanel extends WizardPanel {

	// Wizard panel name.
	private final static String NAME = "Prospecting Site";
	
	// Range modifier.
	private final static double RANGE_MODIFIER = .95D;
	
	// Data members.
	private MapPanel mapPane;
	private EllipseLayer ellipseLayer;
	private NavpointEditLayer navLayer;
	private boolean navSelected;
	private IntPoint navOffset;
	private JLabel locationLabel;
	private int pixelRange;
	
	/**
	 * Constructor
	 * @param wizard the create mission wizard.
	 */
	ProspectingSitePanel(CreateMissionWizard wizard) {
		// Use WizardPanel constructor.
		super(wizard);
		
		// Set the layout.
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// Set the border.
		setBorder(new MarsPanelBorder());
		
		// Create the title label.
		String resource = "";
		String type = getWizard().getMissionData().getType();
		if (type.equals(MissionDataBean.ICE_MISSION)) resource = "ice";
		else if (type.equals(MissionDataBean.REGOLITH_MISSION)) resource = "regolith";
		JLabel titleLabel = new JLabel("Choose " + resource + " collection site.");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(titleLabel);
		
		// Create the map panel.
		mapPane = new MapPanel();
		mapPane.addMapLayer(new UnitIconMapLayer(mapPane));
		mapPane.addMapLayer(new UnitLabelMapLayer());
		mapPane.addMapLayer(ellipseLayer = new EllipseLayer(Color.GREEN));
		mapPane.addMapLayer(navLayer = new NavpointEditLayer(mapPane, false));
		mapPane.addMouseListener(new NavpointMouseListener());
		mapPane.addMouseMotionListener(new NavpointMouseMotionListener());
		mapPane.setMaximumSize(mapPane.getPreferredSize());
		mapPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(mapPane);
		
		// Create the location label.
		locationLabel = new JLabel("Location: ", JLabel.CENTER);
		locationLabel.setFont(locationLabel.getFont().deriveFont(Font.BOLD));
		locationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(locationLabel);
		
		// Create a vertical strut to add some UI space.
		add(Box.createVerticalStrut(10));
		
		// Create the instruction label.
		JLabel instructionLabel = new JLabel("Drag navpoint flag to desired " + resource + 
				" collection site.");
		instructionLabel.setFont(instructionLabel.getFont().deriveFont(Font.BOLD));
		instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(instructionLabel);
		
		// Create verticle glue.
		add(Box.createVerticalGlue());
	}
	
	/**
	 * Gets the wizard panel name.
	 * @return panel name.
	 */
	String getPanelName() {
		return NAME;
	}

	/**
	 * Commits changes from this wizard panel.
	 * @retun true if changes can be committed.
	 */
	boolean commitChanges() {
		IntPoint navpointPixel = navLayer.getNavpointPosition(0);
		Coordinates navpoint = getCenterCoords().convertRectToSpherical(navpointPixel.getiX() - 150, 
				navpointPixel.getiY() - 150, CannedMarsMap.PIXEL_RHO);
		String type = getWizard().getMissionData().getType();
		if (type.equals(MissionDataBean.ICE_MISSION)) 
			getWizard().getMissionData().setIceCollectionSite(navpoint);
		else if (type.equals(MissionDataBean.REGOLITH_MISSION)) 
			getWizard().getMissionData().setRegolithCollectionSite(navpoint);
		return true;
	}

	/**
	 * Clear information on the wizard panel.
	 */
	void clearInfo() {
		getWizard().setButtons(false);
		navLayer.clearNavpointPositions();
	}

	/**
	 * Updates the wizard panel information.
	 */
	void updatePanel() {
		try {
			double range = (getWizard().getMissionData().getRover().getRange() * RANGE_MODIFIER) / 2D;
			pixelRange = convertRadiusToMapPixels(range);
			ellipseLayer.setEllipseDetails(new IntPoint(150, 150), new IntPoint(150, 150), (pixelRange * 2));
			IntPoint initialNavpointPos = new IntPoint(150, 150 - (pixelRange / 2));
			navLayer.addNavpointPosition(initialNavpointPos);
			Coordinates initialNavpoint = getCenterCoords().convertRectToSpherical(0, (-1 * (pixelRange / 2)), 
			        CannedMarsMap.PIXEL_RHO);
			locationLabel.setText("Location: " + initialNavpoint.getFormattedString());
			mapPane.showMap(getCenterCoords());
		}
		catch (Exception e) {}
		
		getWizard().setButtons(true);
	}
	
	/**
	 * Gets the center coordinates.
	 * @return center coordinates.
	 */
	private Coordinates getCenterCoords() {
		return getWizard().getMissionData().getStartingSettlement().getCoordinates();
	}
	
	/**
	 * Converts radius (km) into pixel range on map.
	 * @param radius the radius (km).
	 * @return pixel radius.
	 */
	private int convertRadiusToMapPixels(double radius) {
		return MapUtils.getPixelDistance(radius, SurfMarsMap.TYPE);
	}
	
	/**
	 * Inner class for listening to mouse events on the navpoint display.
	 */
	private class NavpointMouseListener extends MouseAdapter {
	
		/**
		 * Invoked when a mouse button has been pressed on a component.
		 * @param event the mouse event.
		 */
		public void mousePressed(MouseEvent event) {
			if (navLayer.overNavIcon(event.getX(), event.getY()) == 0) {
				// Select navpoint flag.
				navSelected = true;
				navLayer.selectNavpoint(0);
				navOffset = determineOffset(event.getX(), event.getY());
				ellipseLayer.setDisplayEllipse(true);
				mapPane.repaint();
			}
		}
		
		/**
		 * Gets the pixel offset from the currently selected navpoint.
		 * @param x the x coordinate selected.
		 * @param y the y coordinate selected.
		 * @return the pixel offset.
		 */
		private IntPoint determineOffset(int x, int y) {
			int xOffset = navLayer.getNavpointPosition(0).getiX() - x;
			int yOffset = navLayer.getNavpointPosition(0).getiY() - y;
			return new IntPoint(xOffset, yOffset);
		}
	
		/**
		 * Invoked when a mouse button has been released on a component.
		 * @param event the mouse event.
		 */
		public void mouseReleased(MouseEvent event) {
			navSelected = false;
			navLayer.clearSelectedNavpoint();
			ellipseLayer.setDisplayEllipse(false);
			mapPane.repaint();
		}
	}
	
	/**
	 * Inner class for listening to mouse movement on the navpoint display.
	 */
	private class NavpointMouseMotionListener extends MouseMotionAdapter {
		
		/**
		 * Invoked when a mouse button is pressed on a component and then dragged.
		 * @param event the mouse event.
		 */
		public void mouseDragged(MouseEvent event) {
			if (navSelected) {
				// Drag navpoint flag.
				int displayX = event.getPoint().x + navOffset.getiX();
				int displayY = event.getPoint().y + navOffset.getiY();
				IntPoint displayPos = new IntPoint(displayX, displayY);
				if (withinBounds(displayPos)) {
					navLayer.setNavpointPosition(0, displayPos);
					Coordinates center = getWizard().getMissionData().getStartingSettlement().getCoordinates();
					Coordinates navpoint = center.convertRectToSpherical(displayPos.getiX() - 150, 
					        displayPos.getiY() - 150, CannedMarsMap.PIXEL_RHO);
					locationLabel.setText("Location: " + navpoint.getFormattedString());
				
					mapPane.repaint();
				}
			}
		}
		
		/**
		 * Checks if mouse location is within range boundaries and edge of map display. 
		 * @param position the mouse location.
		 * @return true if within boundaries.
		 */
		private boolean withinBounds(IntPoint position) {
			boolean result = true;
			
			if (!navLayer.withinDisplayEdges(position)) result = false;
			
			int radius = (int) Math.round(Math.sqrt(Math.pow(150D - position.getX(), 2D) + 
			        Math.pow(150D - position.getY(), 2D)));
			if (radius > pixelRange) result = false;
			
			return result;
		}
	}
}