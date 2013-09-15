/**
 * Mars Simulation Project
 * NavpointPanel.java
 * @version 3.00 2011-02-23
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.tool.mission;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.ai.mission.*;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.tool.map.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Tab panel for displaying a mission's navpoints.
 */
public class NavpointPanel extends JPanel implements ListSelectionListener,
		MissionListener {

	// Private members.
	private Mission currentMission;
	private MapPanel mapPane;
	private VehicleTrailMapLayer trailLayer;
	private NavpointMapLayer navpointLayer;
    private MineralMapLayer mineralLayer;
	private NavpointTableModel navpointTableModel;
	private JTable navpointTable;
	
	/**
	 * Constructor
	 */
	NavpointPanel() {
		
		// Set the layout.
		setLayout(new BorderLayout());
		
		// Create the main panel.
		Box mainPane = Box.createVerticalBox();
		mainPane.setBorder(new MarsPanelBorder());
		add(mainPane, BorderLayout.CENTER);
		
		// Create the map display panel.
		JPanel mapDisplayPane = new JPanel(new BorderLayout(0, 0));
		mainPane.add(mapDisplayPane);
		
		// Create the map panel.
		mapPane = new MapPanel();
		mapPane.addMapLayer(new UnitIconMapLayer(mapPane));
		mapPane.addMapLayer(new UnitLabelMapLayer());
		trailLayer = new VehicleTrailMapLayer();
		mapPane.addMapLayer(trailLayer);
		navpointLayer = new NavpointMapLayer(this);
		mapPane.addMapLayer(navpointLayer);
        mineralLayer = new MineralMapLayer(this);
        // Forcing map panel to be 300x300 size.
        mapPane.setSize(new Dimension(300, 300));
        mapPane.setPreferredSize(new Dimension(300, 300));
		mapDisplayPane.add(mapPane, BorderLayout.CENTER);
		
		// Create the north button.
		JButton northButton = new JButton(ImageLoader.getIcon("NavpointNorth"));
		northButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Recenter the map to the north by a half map.
				Coordinates centerCoords = mapPane.getCenterLocation();
				if (centerCoords != null) {
					double phi = centerCoords.getPhi();
					phi = phi - CannedMarsMap.HALF_MAP_ANGLE;
					if (phi < 0D) phi = 0D;
					mapPane.showMap(new Coordinates(phi, centerCoords.getTheta()));
				}
			}
		});
		mapDisplayPane.add(northButton, BorderLayout.NORTH);
		
		// Create the west button.
		JButton westButton = new JButton(ImageLoader.getIcon("NavpointWest"));
		westButton.setMargin(new Insets(1, 1, 1, 1));
		westButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Recenter the map to the west by a half map.
				Coordinates centerCoords = mapPane.getCenterLocation();
				if (centerCoords != null) {
					double theta = centerCoords.getTheta();
					theta = theta - CannedMarsMap.HALF_MAP_ANGLE;
					if (theta < 0D) theta += (Math.PI * 2D);
					mapPane.showMap(new Coordinates(centerCoords.getPhi(), theta));
				}
			}
		});
		mapDisplayPane.add(westButton, BorderLayout.WEST);
		
		// Create the east button.
		JButton eastButton = new JButton(ImageLoader.getIcon("NavpointEast"));
		eastButton.setMargin(new Insets(1, 1, 1, 1));
		eastButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Recenter the map to the east by a half map.
				Coordinates centerCoords = mapPane.getCenterLocation();
				if (centerCoords != null) {
					double theta = centerCoords.getTheta();
					theta = theta + CannedMarsMap.HALF_MAP_ANGLE;
					if (theta < (Math.PI * 2D)) theta -= (Math.PI * 2D);
					mapPane.showMap(new Coordinates(centerCoords.getPhi(), theta));
				}
			}
		});
		mapDisplayPane.add(eastButton, BorderLayout.EAST);
		
		// Create the south button.
		JButton southButton = new JButton(ImageLoader.getIcon("NavpointSouth"));
		southButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Recenter the map to the south by a half map.
				Coordinates centerCoords = mapPane.getCenterLocation();
				if (centerCoords != null) {
					double phi = centerCoords.getPhi();
					phi = phi + CannedMarsMap.HALF_MAP_ANGLE;
					if (phi > Math.PI) phi = Math.PI;
					mapPane.showMap(new Coordinates(phi, centerCoords.getTheta()));
				}
			}
		});
		mapDisplayPane.add(southButton, BorderLayout.SOUTH);
		
		// Create the navpoint table panel.
		JPanel navpointTablePane = new JPanel(new BorderLayout(0, 0));
		navpointTablePane.setBorder(new MarsPanelBorder());
		navpointTablePane.setPreferredSize(new Dimension(-1, 180));
		mainPane.add(navpointTablePane);
		
		// Create the navpoint scroll panel.
		JScrollPane navpointScrollPane = new JScrollPane();
        navpointTablePane.add(navpointScrollPane, BorderLayout.CENTER);
        
        // Create the navpoint table model.
        navpointTableModel = new NavpointTableModel();
        
        // Create the navpoint table.
        navpointTable = new JTable(navpointTableModel);
        navpointTable.setRowSelectionAllowed(true);
        navpointTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        navpointTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
            	public void valueChanged(ListSelectionEvent e) {
            		if (e.getValueIsAdjusting()) {
            			// Recenter map on selected navpoint.
            			if ((currentMission != null) && (currentMission instanceof TravelMission)) {
            				TravelMission travelMission = (TravelMission) currentMission;
            				int index = navpointTable.getSelectedRow();
            				if (index > -1) {
            					NavPoint navpoint = travelMission.getNavpoint(index); 
            					navpointLayer.setSelectedNavpoint(navpoint);
            					mapPane.showMap(navpoint.getLocation());
            				}
            				else navpointLayer.setSelectedNavpoint(null);
            			}
            		}
            	}
            });
        navpointScrollPane.setViewportView(navpointTable);
	}
	
	/**
	 * Implemented from ListSelectionListener.
	 * Note: this is called when a mission is selected on MissionWindow's mission list.
	 */
	public void valueChanged(ListSelectionEvent e) {
		Mission mission = (Mission) ((JList) e.getSource()).getSelectedValue();
		
		// Remove this as previous mission listener.
		if ((currentMission != null) && (currentMission != mission)) 
			currentMission.removeMissionListener(this);
		
		if (mission != null) {
			if (mission != currentMission) {
				// Add this as listener for new mission.
				mission.addMissionListener(this);
				
				// Update map and info for new mission.
				currentMission = mission;
				if (mission.getPeopleNumber() > 0) {
					if (mission instanceof VehicleMission) {
						trailLayer.setSingleVehicle(((VehicleMission) mission).getVehicle());
                    }
                    
					if (mission instanceof TravelMission) {
						navpointLayer.setSingleMission(mission);
						navpointLayer.setSelectedNavpoint(null);
						navpointTableModel.updateNavpoints();
					}
                    
                    if ((mission instanceof Exploration) || (mission instanceof Mining)) {
                        if (!mapPane.hasMapLayer(mineralLayer)) mapPane.addMapLayer(mineralLayer);
                    }
                    else {
                        if (mapPane.hasMapLayer(mineralLayer)) mapPane.removeMapLayer(mineralLayer);
                    }
                    
					mapPane.showMap(((Unit)currentMission.getPeople().toArray()[0]).getCoordinates());
				}
			}
		}
		else {
			// Clear map and info.
			currentMission = null;
			trailLayer.setSingleVehicle(null);
			navpointLayer.setSingleMission(null);
			navpointLayer.setSelectedNavpoint(null);
			navpointTableModel.updateNavpoints();
            if (mapPane.hasMapLayer(mineralLayer)) mapPane.removeMapLayer(mineralLayer);
			mapPane.showMap(null);
		}
	}

	/**
	 * Catch mission update event.
	 * @param event the mission event.
	 */
	public void missionUpdate(MissionEvent event) {
		String type = event.getType();
		if (TravelMission.NAVPOINTS_EVENT.equals(type)) {
			// Update mission navpoints.
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					navpointTableModel.updateNavpoints();
				}
			});
		}
	}
	
	/**
	 * Prepares navpoint panel for deletion.
	 */
	public void destroy() {
		mapPane.destroy();
	}
	
	/**
	 * Navpoint table model.
	 * Inner class
	 */
	private class NavpointTableModel extends AbstractTableModel {
		
		// Private members.
		private List<NavPoint> navpoints;
		
		/**
		 * Constructor.
		 */
		private NavpointTableModel() {
			navpoints = new ArrayList<NavPoint>();
		}
		
		/**
		 * Returns the number of rows in the model.
		 * @return number of rows.
		 */
		public int getRowCount() {
            return navpoints.size();
        }
		
		/**
		 * Returns the number of columns in the model.
		 * @return number of columns.
		 */
		public int getColumnCount() {
            return 3;
        }
		
		/**
		 * Returns the name of the column at columnIndex.
		 * @param columnIndex the index of the column.
		 * @return the name of the column.
		 */
		public String getColumnName(int columnIndex) {
            if (columnIndex == 0) return "Name";
            else if (columnIndex == 1) return "Location";
            else if (columnIndex == 2) return "Description";
            else return "";
        }
		
		/**
		 * Returns the value for the cell at columnIndex and rowIndex.
		 * @param row the row index.
		 * @param column the column index.
		 * @return the value object.
		 */
		public Object getValueAt(int row, int column) {
            if (row < navpoints.size()) {
            	NavPoint navpoint = navpoints.get(row);
            	if (column == 0) return "Navpoint " + (row + 1);
            	else if (column == 1) return navpoint.getLocation().getFormattedString();
            	else if (column == 2) return navpoint.getDescription();
            	else return "unknown";
            }   
            else return "unknown";
        }
		
		/**
		 * Updates the navpoints for the table.
		 */
		public void updateNavpoints() {
			if ((currentMission != null) && (currentMission instanceof TravelMission)) {
				navpoints.clear();
				TravelMission travelMission = (TravelMission) currentMission;
				for (int x=0; x < travelMission.getNumberOfNavpoints(); x++) 
					navpoints.add(travelMission.getNavpoint(x));
				fireTableDataChanged();
			}
			else {
				if (navpoints.size() > 0) {
					navpoints.clear();
					fireTableDataChanged();
				}
			}
		}
	}
}