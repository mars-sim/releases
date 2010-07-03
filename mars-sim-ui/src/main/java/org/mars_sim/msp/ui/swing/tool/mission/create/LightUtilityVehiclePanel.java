/**
 * Mars Simulation Project
 * LightUtilityVehiclePanel.java
 * @version 2.84 2008-06-11
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.tool.mission.create;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.mars_sim.msp.core.CollectionUtils;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;

/**
 * A wizard panel for selecting the mission light utility vehicle.
 */
class LightUtilityVehiclePanel extends WizardPanel {

	// The wizard panel name.
	private final static String NAME = "Light Utility Vehicle";
	
	// Data members.
	private VehicleTableModel vehicleTableModel;
	private JTable vehicleTable;
	private JLabel errorMessageLabel;
	
	/**
	 * Constructor
	 * @param wizard the create mission wizard.
	 */
	LightUtilityVehiclePanel(CreateMissionWizard wizard) {
		// User WizardPanel constructor.
		super(wizard);
		
		// Set the layout.
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// Set the border.
		setBorder(new MarsPanelBorder());
		
		// Create the select vehicle label.
		JLabel selectVehicleLabel = new JLabel("Select a light utility vehicle for the mission.", 
				JLabel.CENTER);
		selectVehicleLabel.setFont(selectVehicleLabel.getFont().deriveFont(Font.BOLD));
		selectVehicleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(selectVehicleLabel);
		
		// Create the vehicle panel.
		JPanel vehiclePane = new JPanel(new BorderLayout(0, 0));
		vehiclePane.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));
		vehiclePane.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(vehiclePane);
		
        // Create scroll panel for vehicle list.
        JScrollPane vehicleScrollPane = new JScrollPane();
        vehiclePane.add(vehicleScrollPane, BorderLayout.CENTER);
        
        // Create the vehicle table model.
        vehicleTableModel = new VehicleTableModel();
        
        // Create the vehicle table.
        vehicleTable = new JTable(vehicleTableModel);
        vehicleTable.setDefaultRenderer(Object.class, new UnitTableCellRenderer(vehicleTableModel));
        vehicleTable.setRowSelectionAllowed(true);
        vehicleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vehicleTable.getSelectionModel().addListSelectionListener(
        	new ListSelectionListener() {
        		public void valueChanged(ListSelectionEvent e) {
        			if (e.getValueIsAdjusting()) {
        				// Get the selected vehicle index.
        				int index = vehicleTable.getSelectedRow();
        				if (index > -1) {
        					// Check if selected row has a failure cell.
        					if (vehicleTableModel.isFailureRow(index)) {
        						// Set the error message and disable the next button.
        						errorMessageLabel.setText("Light utility vehicle cannot be " +
        								"used on the mission (see red cells).");
        						getWizard().setButtons(false);
        					}
        					else {
        						// Clear the error message and enable the next button.
        						errorMessageLabel.setText(" ");
        						getWizard().setButtons(true);
        					}
        				}
        			}
        		}
        	});
        vehicleTable.setPreferredScrollableViewportSize(vehicleTable.getPreferredSize());
        vehicleScrollPane.setViewportView(vehicleTable);
		
        // Create the error message label.
		errorMessageLabel = new JLabel(" ", JLabel.CENTER);
		errorMessageLabel.setFont(errorMessageLabel.getFont().deriveFont(Font.BOLD));
		errorMessageLabel.setForeground(Color.RED);
		errorMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(errorMessageLabel);
		
		// Add a vertical glue.
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
		int selectedIndex = vehicleTable.getSelectedRow();
		LightUtilityVehicle selectedVehicle = 
			(LightUtilityVehicle) vehicleTableModel.getUnit(selectedIndex);
		getWizard().getMissionData().setLUV(selectedVehicle);
		return true;
	}

	/**
	 * Clear information on the wizard panel.
	 */
	void clearInfo() {
		vehicleTable.clearSelection();
		errorMessageLabel.setText(" ");
	}

	/**
	 * Updates the wizard panel information.
	 */
	void updatePanel() {
		vehicleTableModel.updateTable();
		vehicleTable.setPreferredScrollableViewportSize(vehicleTable.getPreferredSize());
	}
	
	/**
	 * A table model for vehicles.
	 */
    private class VehicleTableModel extends UnitTableModel {
    	
    	/**
    	 * Constructor
    	 */
    	private VehicleTableModel() {
    		// Use UnitTableModel constructor.
    		super();
    		
    		// Add columns.
    		columns.add("Name");
    		columns.add("Status");
    		columns.add("Mission");
    	}
    	
    	/**
    	 * Returns the value for the cell at columnIndex and rowIndex.
    	 * @param row the row whose value is to be queried
    	 * @param column the column whose value is to be queried
    	 * @return the value Object at the specified cell
    	 */
    	public Object getValueAt(int row, int column) {
    		Object result = "unknown";
    		
            if (row < units.size()) {
            	LightUtilityVehicle vehicle = (LightUtilityVehicle) getUnit(row);
            	
            	try {
            		if (column == 0) 
            			result = vehicle.getName();
            		else if (column == 1) 
            			result = vehicle.getStatus();
            		else if (column == 2) {
            			Mission mission = Simulation.instance().getMissionManager().
    							getMissionForVehicle(vehicle);
            			if (mission != null) result = mission.getDescription();
            			else result = "None";
            		}
            	}
            	catch (Exception e) {}
            }
            
            return result;
        }
    	
    	/**
    	 * Updates the table data.
    	 */
    	void updateTable() {
    		units.clear();
    		Settlement startingSettlement = getWizard().getMissionData().getStartingSettlement();
    		Collection<Vehicle> vehicles = CollectionUtils.sortByName(
    				startingSettlement.getParkedVehicles());
    		Iterator<Vehicle> i = vehicles.iterator();
    		while (i.hasNext()) {
    			Vehicle vehicle = i.next();
    			if (vehicle instanceof LightUtilityVehicle) units.add(vehicle);
    		}
    		fireTableDataChanged();
    	}
    	
    	/**
    	 * Checks if a table cell is a failure cell.
    	 * @param row the table row.
    	 * @param column the table column.
    	 * @return true if cell is a failure cell.
    	 */
    	boolean isFailureCell(int row, int column) {
    		boolean result = false;
    		LightUtilityVehicle vehicle = (LightUtilityVehicle) getUnit(row);
    		
    		if (column == 1) {
    			if (!vehicle.getStatus().equals(Vehicle.PARKED)) result = true;
    		}
    		else if (column == 2) {
    			Mission mission = Simulation.instance().getMissionManager().
    					getMissionForVehicle(vehicle);
    			if (mission != null) result = true;
    		}
    		
    		return result;
    	}
    }
}