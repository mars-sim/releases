/**
 * Mars Simulation Project
 * ExplorationSitesPanel.java
 * @version 3.00 2010-08-10
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.tool.mission.create;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Direction;
import org.mars_sim.msp.core.IntPoint;
import org.mars_sim.msp.core.person.ai.mission.CollectResourcesMission;
import org.mars_sim.msp.core.person.ai.mission.Exploration;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.tool.map.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a wizard panel for selecting exploration sites for the mission.
 */
class ExplorationSitesPanel extends WizardPanel {

	// Wizard panel name.
	private final static String NAME = "Exploration Sites";
	
	// Range modifier.
	private final static double RANGE_MODIFIER = .95D;

	// Data members.
	private MapPanel mapPane;
	private EllipseLayer ellipseLayer;
	private NavpointEditLayer navLayer;
    private MineralMapLayer mineralLayer;
	private int navSelected;
	private IntPoint navOffset;
	private JPanel siteListPane;
	private JButton addButton;
	private double range;
	private double missionTimeLimit;
	private double timePerSite;
	
	/**
	 * Constructor
	 * @param wizard the create mission wizard.
	 */
	ExplorationSitesPanel(CreateMissionWizard wizard) {
		// Use WizardPanel constructor.
		super(wizard);
		
		// Set the layout.
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// Set the border.
		setBorder(new MarsPanelBorder());
		
		// Creates the title label.
		JLabel titleLabel = new JLabel("Choose the exploration sites.");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(titleLabel);
		
		// Add a vertical strut 
		add(Box.createVerticalStrut(10));
		
		// Create the center panel.
		JPanel centerPane = new JPanel(new BorderLayout(0, 0));
		centerPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		centerPane.setMaximumSize(new Dimension(Short.MAX_VALUE, 350));
		add(centerPane);
		
		// Create the map main panel.
		JPanel mapMainPane = new JPanel(new BorderLayout(0, 0));
		centerPane.add(mapMainPane, BorderLayout.WEST);
		
		// Create the map panel.
		mapPane = new MapPanel();
        mineralLayer = new MineralMapLayer(mapPane);
        mapPane.addMapLayer(mineralLayer);
		mapPane.addMapLayer(new UnitIconMapLayer(mapPane));
		mapPane.addMapLayer(new UnitLabelMapLayer());
		mapPane.addMapLayer(ellipseLayer = new EllipseLayer(Color.GREEN));
		mapPane.addMapLayer(navLayer = new NavpointEditLayer(mapPane, true));
		mapPane.setBorder(new MarsPanelBorder());
		mapPane.addMouseListener(new NavpointMouseListener());
		mapPane.addMouseMotionListener(new NavpointMouseMotionListener());
		mapMainPane.add(mapPane, BorderLayout.NORTH);
		
        // Create the instruction label panel.
        JPanel instructionLabelPane = new JPanel(new GridLayout(2, 1, 0, 0));
        mapMainPane.add(instructionLabelPane, BorderLayout.SOUTH);
        
		// Create the instruction labels.
		JLabel instructionLabel1 = new JLabel("Drag navpoint flags to the desired exploration", JLabel.LEFT);
		instructionLabel1.setFont(instructionLabel1.getFont().deriveFont(Font.BOLD));
		instructionLabelPane.add(instructionLabel1);
        
        JLabel instructionLabel2 = new JLabel("sites.", JLabel.LEFT);
        instructionLabel2.setFont(instructionLabel2.getFont().deriveFont(Font.BOLD));
        instructionLabelPane.add(instructionLabel2);
		
		// Create the site panel.
		JPanel sitePane = new JPanel(new BorderLayout(0, 0));
		sitePane.setAlignmentX(Component.CENTER_ALIGNMENT);
		sitePane.setMaximumSize(new Dimension(Short.MAX_VALUE, 300));
		centerPane.add(sitePane, BorderLayout.CENTER);
		
        // Create scroll panel for site list.
        JScrollPane siteScrollPane = new JScrollPane();
        sitePane.add(siteScrollPane, BorderLayout.CENTER);
        
        // Create the site list main panel.
        JPanel siteListMainPane = new JPanel(new BorderLayout(0, 0));
        siteScrollPane.setViewportView(siteListMainPane);
        
        // Create the site list panel.
        siteListPane = new JPanel();
        siteListPane.setLayout(new BoxLayout(siteListPane, BoxLayout.Y_AXIS));
        siteListMainPane.add(siteListPane, BorderLayout.NORTH);
        
        // Create the add button panel.
        JPanel addButtonPane = new JPanel(new FlowLayout());
        sitePane.add(addButtonPane, BorderLayout.SOUTH);
        
        // Create the add button.
        addButton = new JButton("Add Site");
        addButton.addActionListener(
        		new ActionListener() {
    				public void actionPerformed(ActionEvent e) {
    					// Add a new exploration site to the mission.
    					SitePanel sitePane = new SitePanel(siteListPane.getComponentCount(), getNewSiteLocation());
    					siteListPane.add(sitePane);
    					navLayer.addNavpointPosition(MapUtils.getRectPosition(sitePane.getSite(), getCenterCoords(), SurfMarsMap.TYPE));
    					mapPane.repaint();
    					addButton.setEnabled(canAddMoreSites());
    					validate();
    				}
    			});
        addButtonPane.add(addButton);
		
        // Create bottom panel.
        JPanel bottomPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(bottomPane);
        
        // Create mineral legend panel.
        JPanel mineralLegendPane = new JPanel(new BorderLayout(0, 0));
        bottomPane.add(mineralLegendPane);
        
        // Create mineral legend label.
        JLabel mineralLegendLabel = new JLabel("Mineral Legend", JLabel.CENTER);
        mineralLegendLabel.setFont(mineralLegendLabel.getFont().deriveFont(Font.BOLD));
        mineralLegendPane.add(mineralLegendLabel, BorderLayout.NORTH);
        
        // Create mineral legend scroll panel.
        JScrollPane mineralLegendScrollPane = new JScrollPane();
        mineralLegendPane.add(mineralLegendScrollPane, BorderLayout.CENTER);
        
        // Create mineral legend table model.
        MineralTableModel mineralTableModel = new MineralTableModel();
        
        // Create mineral legend table.
        JTable mineralLegendTable = new JTable(mineralTableModel);
        mineralLegendTable.setPreferredScrollableViewportSize(new Dimension(300, 50));
        mineralLegendTable.setCellSelectionEnabled(false);
        mineralLegendTable.setDefaultRenderer(Color.class, new ColorTableCellRenderer());
        mineralLegendScrollPane.setViewportView(mineralLegendTable);
        
        // Create a verticle glue to manage layout.
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
		getWizard().getMissionData().setExplorationSites(getSites());
		return true;
	}

	/**
	 * Clear information on the wizard panel.
	 */
	void clearInfo() {
		siteListPane.removeAll();
		navLayer.clearNavpointPositions();
		getWizard().setButtons(false);
	}

	/**
	 * Updates the wizard panel information.
	 */
	void updatePanel() {
		range = getRange();
		missionTimeLimit = getMissionTimeLimit();
		timePerSite = getTimePerSite();
		
		Coordinates startingSite = getCenterCoords().getNewLocation(new Direction(0D), range / 4D);
		SitePanel startingSitePane = new SitePanel(0, startingSite);
		siteListPane.add(startingSitePane);
		navLayer.addNavpointPosition(MapUtils.getRectPosition(startingSitePane.getSite(), getCenterCoords(), SurfMarsMap.TYPE));
		mapPane.showMap(getCenterCoords());
		addButton.setEnabled(canAddMoreSites());
		getWizard().setButtons(true);
	}
	
	/**
	 * Checks if mission can add more exporation sites.
	 * @return true if can add more sites.
	 */
	private boolean canAddMoreSites() {
		return (missionTimeLimit > (getTotalMissionTime() + getTimePerSite()));
	}
	
	/**
	 * Gets a new exploration site default location.
	 * @return site location.
	 */
	private Coordinates getNewSiteLocation() {
		Coordinates result = null;
		Coordinates[] sites = getSites();
		Coordinates lastSite = sites[sites.length - 1];
		result = determineNewSiteLocation(lastSite, getCenterCoords(), getRemainingRange(true));
		return result;
	}
	
	/**
	 * Gets the remaining mission range.
	 * @param newSite true if needing to add additional exploration site.
	 * @return range (km).
	 */
	private double getRemainingRange(boolean newSite) {
		double travelTime = missionTimeLimit - getTotalSiteTime();
		if (newSite) travelTime -= timePerSite;
		Rover rover = getWizard().getMissionData().getRover();
		double timeRange = (travelTime / 1000D) * rover.getEstimatedTravelDistancePerSol();
		double realRange = range;
		if (timeRange < range) realRange = timeRange;
		return realRange - getDistance();
	}
	
	/**
	 * Gets the rover range.
	 * @return rover range.
	 */
	private double getRange() {
		try {
			// Use range modifier.
			return getWizard().getMissionData().getRover().getRange() * RANGE_MODIFIER;
		}
		catch (Exception e) {
			return 0D;
		}
	}
	
	/**
	 * Gets the mission time limit.
	 * @return time limit (millisols)
	 */
	private double getMissionTimeLimit() {
		Rover rover = getWizard().getMissionData().getRover();
		int memberNum = getWizard().getMissionData().getMembers().size();
		try {
			return CollectResourcesMission.getTotalTripTimeLimit(rover, memberNum, true);
		}
		catch (Exception e) {
			return 0D;
		}
	}
	
	/**
	 * Gets the estimated time required per exploration site.
	 * @return time (millisols)
	 */
	private double getTimePerSite() {
    	return Exploration.EXPLORING_SITE_TIME;
	}
	
	/**
	 * Gets the total estimated time required for all exploration sites in mission.
	 * @return time (millisols)
	 */
	private double getTotalSiteTime() {
		return timePerSite * siteListPane.getComponentCount();
	}
	
	/**
	 * Gets the estimated time spent travelling on the mission.
	 * @return time (millisols)
	 */
	private double getTravelTime() {
		Rover rover = getWizard().getMissionData().getRover();
		return getDistance() / (rover.getEstimatedTravelDistancePerSol() / 1000D);
	}
	
	/**
	 * Gets the total estimated mission time.
	 * @return time (millisols)
	 */
	private double getTotalMissionTime() {
		return getTravelTime() + getTotalSiteTime();
	}
	
	/**
	 * Gets the total distance travelled in the mission.
	 * @return distance (km)
	 */
	private double getDistance() {
		double result = 0D;
		Coordinates[] sites = getSites();
		
		result += getCenterCoords().getDistance(sites[0]);
		
		for (int x = 1; x < sites.length; x++) 
			result += sites[x - 1].getDistance(sites[x]);
		
		result += sites[sites.length - 1].getDistance(getCenterCoords());
		
		return result;
	}
	
	/**
	 * Gets the mission exploration sites.
	 * @return array of sites
	 */
	private Coordinates[] getSites() {
		Coordinates[] result = new Coordinates[siteListPane.getComponentCount()];
		for (int x = 0; x < siteListPane.getComponentCount(); x++) 
			result[x] = ((SitePanel) siteListPane.getComponent(x)).getSite();
		
		return result;
	}
	
	/**
	 * Updates the exploration site numbers.
	 */
	private void updateSiteNumbers() {
		navLayer.clearNavpointPositions();
		for (int x = 0; x < siteListPane.getComponentCount(); x++) {
			SitePanel sitePane = (SitePanel) siteListPane.getComponent(x);
			sitePane.setSiteNum(x);
			navLayer.addNavpointPosition(MapUtils.getRectPosition(sitePane.getSite(), getCenterCoords(), SurfMarsMap.TYPE));
		}
		mapPane.repaint();
	}
	
	/**
	 * Gets the center coordinates for the mission.
	 * @return center coordinates.
	 */
	private Coordinates getCenterCoords() {
		return getWizard().getMissionData().getStartingSettlement().getCoordinates();
	}
	
	/**
	 * Gets a new exploration site location.
	 * @param prevNav the previous site.
	 * @param nextNav the next site.
	 * @param range the remaining range (km) in the mission.
	 * @return new site location.
	 */
	private Coordinates determineNewSiteLocation(Coordinates prevNav, Coordinates nextNav, double range) {
		double fociDistance = prevNav.getDistance(nextNav);
		double distanceFromCenterOfAxis = Math.sqrt(Math.pow(((range + fociDistance) / 2D), 2D) - Math.pow((fociDistance / 2D), 2D));
		double initialDistanceFromAxis = distanceFromCenterOfAxis / 2D;
		double initialDistanceFromFoci = Math.sqrt(Math.pow((fociDistance / 2D), 2D) + Math.pow(initialDistanceFromAxis, 2D));
		Direction initialDirectionFromFoci = new Direction(Math.asin(initialDistanceFromAxis / initialDistanceFromFoci)); 
		Direction fociDirection = prevNav.getDirectionToPoint(nextNav);
		Direction directionToNewSite = new Direction(fociDirection.getDirection() - initialDirectionFromFoci.getDirection());
		return prevNav.getNewLocation(directionToNewSite, initialDistanceFromFoci);
	}
	
	/**
	 * Inner class for an exploration site panel.
	 */
	private class SitePanel extends JPanel {
		
		// Private members.
		private Coordinates site;
		private int siteNum;
		private JLabel siteNumLabel;
		private JLabel siteLocationLabel;
		
		/**
		 * Constructor
		 * @param siteNum the exploration site's number.
		 * @param site the exploration site coordinates.
		 */
		SitePanel(int siteNum, Coordinates site) {
			// Use JPanel constructor.
			super();
		
			// Initialize data members.
			this.siteNum = siteNum;
			this.site = site;
			
			// Set the layout.
			setLayout(new GridLayout(1, 3));
			
			// Set the border.
			setBorder(new MarsPanelBorder());
			
			// Create the site number label.
			siteNumLabel = new JLabel(" Site " + (siteNum + 1));
			add(siteNumLabel);
			
			// Create the site location label.
			siteLocationLabel = new JLabel(site.getFormattedString());
			add(siteLocationLabel);
			
			if (siteNum > 0) {
				// Create the remove button.
				JButton removeButton = new JButton("Remove");
				removeButton.addActionListener(
						new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								// Remove this site panel from the site list.
								setVisible(false);
								siteListPane.remove(getSiteNum());
								updateSiteNumbers();
								siteListPane.validate();
							}
						});
				add(removeButton);
			}
			else add(new JPanel());
		}
		
		/**
		 * Sets the exploration site's number.
		 * @param siteNum site number.
		 */
		void setSiteNum(int siteNum) {
			this.siteNum = siteNum;
			siteNumLabel.setText(" Site " + (siteNum + 1));
		}
		
		/**
		 * Gets the exploration site's number.
		 * @return site number.
		 */
		int getSiteNum() {
			return siteNum;
		}
		
		/**
		 * Sets the exploration site location.
		 * @param site location.
		 */
		void setLocation(Coordinates site) {
			this.site = site;
			siteLocationLabel.setText(site.getFormattedString());
		}
		
		/**
		 * Gets the exploration site's location.
		 * @return location.
		 */
		Coordinates getSite() {
			return site;
		}
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
			// Checks which navpoint flag was selected if any.
			navSelected = navLayer.overNavIcon(event.getX(), event.getY());
			if (navSelected > -1) {
				// Selects the navpoint based on the flag the user selected and updates panel info.
				navLayer.selectNavpoint(navSelected);
				navOffset = determineOffset(event.getX(), event.getY());
				
				IntPoint prevNavpoint = MapUtils.getRectPosition(getPreviousNavpoint(), getCenterCoords(), SurfMarsMap.TYPE);
				IntPoint nextNavpoint = MapUtils.getRectPosition(getNextNavpoint(), getCenterCoords(), SurfMarsMap.TYPE);
				int radiusPixels = convertDistanceToMapPixels(getRadius());
				ellipseLayer.setEllipseDetails(prevNavpoint, nextNavpoint, radiusPixels);
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
			int xOffset = navLayer.getNavpointPosition(navSelected).getiX() - x;
			int yOffset = navLayer.getNavpointPosition(navSelected).getiY() - y;
			return new IntPoint(xOffset, yOffset);
		}
		
		/**
		 * Gets the radius for the currently selected navpoint based on remaining mission range.
		 * @return radius (km)
		 */
		private double getRadius() {
			Coordinates currentNavpoint = getCurrentNavpoint();
			Coordinates prevNavpoint = getPreviousNavpoint();
			Coordinates nextNavpoint = getNextNavpoint();
			double currentDistance = prevNavpoint.getDistance(currentNavpoint) + currentNavpoint.getDistance(nextNavpoint);
			double straightDistance = prevNavpoint.getDistance(nextNavpoint);
			return currentDistance - straightDistance + getRemainingRange(false);
		}
		
		/**
		 * Converts distance (km) into pixel range on map.
		 * @param distance the distance (km).
		 * @return pixel range.
		 */
		private int convertDistanceToMapPixels(double distance) {
			return MapUtils.getPixelDistance(distance, SurfMarsMap.TYPE);
		}
	
		/**
		 * Invoked when a mouse button has been released on a component.
		 * @param event the mouse event.
		 */
		public void mouseReleased(MouseEvent event) {
			navSelected = -1;
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
			if (navSelected > -1) {
				// Drag navpoint flag if selected.
				int displayX = event.getPoint().x + navOffset.getiX();
				int displayY = event.getPoint().y + navOffset.getiY();
				IntPoint displayPos = new IntPoint(displayX, displayY);
				Coordinates center = getWizard().getMissionData().getStartingSettlement().getCoordinates();
				Coordinates navpoint = center.convertRectToSpherical(displayPos.getiX() - 150, displayPos.getiY() - 150);
				
				// Only drag navpoint flag if within ellipse range bounds.
				if (withinBounds(displayPos, navpoint)) {
					navLayer.setNavpointPosition(navSelected, new IntPoint(displayX, displayY));
					SitePanel selectedSitePane = (SitePanel) siteListPane.getComponent(navSelected);
					selectedSitePane.setLocation(navpoint);
					addButton.setEnabled(canAddMoreSites());
					mapPane.repaint();
				}
			}
		}
		
		/**
		 * Checks if mouse location is within range boundries and edge of map display. 
		 * @param position the mouse location.
		 * @param location the navpoint location.
		 * @return true if within boundries.
		 */
		private boolean withinBounds(IntPoint position, Coordinates location) {
			boolean result = true;
			if (!navLayer.withinDisplayEdges(position)) result = false;
			if (getRemainingRange(false) < getDistanceDiff(location)) result = false;
			return result;
		}
		
		/**
		 * Gets the distance difference between a new location and the current selected site location.
		 * @param newSite the new location.
		 * @return distance difference (km).
		 */
		private double getDistanceDiff(Coordinates newSite) {
			Coordinates prevNavpoint = getPreviousNavpoint();
			Coordinates nextNavpoint = getNextNavpoint();
			
			Coordinates currentSite = getCurrentNavpoint();
			double currentSiteDistance = prevNavpoint.getDistance(currentSite) + currentSite.getDistance(nextNavpoint);
			
			double newSiteDistance = prevNavpoint.getDistance(newSite) + newSite.getDistance(nextNavpoint);
			
			return newSiteDistance - currentSiteDistance;
		}
	}
	
	/**
	 * Gets the previous navpoint from the selected navpoint.
	 * @return previous navpoint.
	 */
	private Coordinates getPreviousNavpoint() {
		Coordinates prevNavpoint = null;
		if (navSelected > 0) prevNavpoint = ((SitePanel) siteListPane.getComponent(navSelected - 1)).getSite();
		else prevNavpoint = getCenterCoords();
		return prevNavpoint;
	}
	
	/**
	 * Gets the next navpoint after the selected navpoint.
	 * @return next navpoint.
	 */
	private Coordinates getNextNavpoint() {
		Coordinates nextNavpoint = null;
		if (navSelected < (siteListPane.getComponentCount() - 1)) 
			nextNavpoint = ((SitePanel) siteListPane.getComponent(navSelected + 1)).getSite();
		else nextNavpoint = getCenterCoords();
		return nextNavpoint;
	}
	
	private Coordinates getCurrentNavpoint() {
		return ((SitePanel) siteListPane.getComponent(navSelected)).getSite();
	}
    
    /** 
     * Internal class used as model for the mineral table.
     */
    private class MineralTableModel extends AbstractTableModel {
        
        private java.util.Map<String, Color> mineralColors = null;
        private List<String> mineralNames = null;
        
        private MineralTableModel() {
            mineralColors = mineralLayer.getMineralColors();
            mineralNames = new ArrayList<String>(mineralColors.keySet());
        }
        
        public int getRowCount() {
            return mineralNames.size();
        }
        
        public int getColumnCount() {
            return 2;
        }
        
        public Class<?> getColumnClass(int columnIndex) {
            Class dataType = super.getColumnClass(columnIndex);
            if (columnIndex == 0) dataType = String.class;
            if (columnIndex == 1) dataType = Color.class;
            return dataType;
        }
        
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) return "Mineral";
            else if (columnIndex == 1) return "Color";
            else return "unknown";
        }
        
        public Object getValueAt(int row, int column) {
            if (row < getRowCount()) {
                String mineralName = mineralNames.get(row);
                if (column == 0) {
                    return mineralName;
                }
                else if (column == 1) {
                    return mineralColors.get(mineralName);
                }
                else return "unknown";
            }
            else return "unknown";
        }
    }
    
    /**
     * Internal class used to render color cells in the mineral table.
     */
    private static class ColorTableCellRenderer implements TableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
        
            if ((value != null) && (value instanceof Color)) {
                Color color = (Color) value;
                JPanel colorPanel = new JPanel();
                colorPanel.setOpaque(true);
                colorPanel.setBackground(color);
                return colorPanel;
            }
            else return null;
            
        }
    }
}