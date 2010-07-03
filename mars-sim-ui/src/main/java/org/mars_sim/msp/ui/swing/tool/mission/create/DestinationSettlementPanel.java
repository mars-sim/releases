/**
 * Mars Simulation Project
 * DestinationSettlementPanel.java
 * @version 2.84 2008-05-12
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

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;

/**
 * This is a wizard panel for selecting the mission destination settlement.
 */
class DestinationSettlementPanel extends WizardPanel {

	// Wizard panel name.
	private final static String NAME = "Destination Settlement";
	
	// Data members.
	private SettlementTableModel settlementTableModel;
	private JTable settlementTable;
	private JLabel errorMessageLabel;
	
	/**
	 * Constructor
	 * @param wizard the create mission wizard.
	 */
	DestinationSettlementPanel(CreateMissionWizard wizard) {
		// Use WizardPanel constructor.
		super(wizard);
		
		// Set the layout.
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// Set the border.
		setBorder(new MarsPanelBorder());
		
		// Create the select settlement label.
		JLabel selectSettlementLabel = new JLabel("Select a destination settlement.", JLabel.CENTER);
		selectSettlementLabel.setFont(selectSettlementLabel.getFont().deriveFont(Font.BOLD));
		selectSettlementLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(selectSettlementLabel);
		
		// Create the settlement panel.
		JPanel settlementPane = new JPanel(new BorderLayout(0, 0));
		settlementPane.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));
		settlementPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(settlementPane);
		
        // Create scroll panel for settlement list.
        JScrollPane settlementScrollPane = new JScrollPane();
        settlementPane.add(settlementScrollPane, BorderLayout.CENTER);
        
        // Create the settlement table model.
        settlementTableModel = new SettlementTableModel();
        
        // Create the settlement table.
        settlementTable = new JTable(settlementTableModel);
        settlementTable.setDefaultRenderer(Object.class, new UnitTableCellRenderer(settlementTableModel));
        settlementTable.setRowSelectionAllowed(true);
        settlementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        settlementTable.getSelectionModel().addListSelectionListener(
        	new ListSelectionListener() {
        		public void valueChanged(ListSelectionEvent e) {
        			if (e.getValueIsAdjusting()) {
        				int index = settlementTable.getSelectedRow();
        				if (index > -1) {
        					if (settlementTableModel.isFailureRow(index)) {
        						// If selected row is a failure row, display warning and disable final button.
        						errorMessageLabel.setText("Selected destination settlement is not within rover range.");
        						getWizard().setButtons(false);
        					}
        					else {
        						// If selected row is valid, clear warning and enable final button.
        						errorMessageLabel.setText(" ");
        						getWizard().setButtons(true);
        					}
        				}
        			}
        		}
        	});
        settlementTable.setPreferredScrollableViewportSize(settlementTable.getPreferredSize());
        settlementScrollPane.setViewportView(settlementTable);
		
        // Create error message label.
		errorMessageLabel = new JLabel(" ", JLabel.CENTER);
		errorMessageLabel.setFont(errorMessageLabel.getFont().deriveFont(Font.BOLD));
		errorMessageLabel.setForeground(Color.RED);
		errorMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(errorMessageLabel);
		
		// Create a verticle glue for the remainder of the panel.
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
		int selectedIndex = settlementTable.getSelectedRow();
		Settlement selectedSettlement = (Settlement) settlementTableModel.getUnit(selectedIndex);
		getWizard().getMissionData().setDestinationSettlement(selectedSettlement);
		return true;
	}

	/**
	 * Clear information on the wizard panel.
	 */
	void clearInfo() {
		settlementTable.clearSelection();
		errorMessageLabel.setText(" ");
	}

	/**
	 * Updates the wizard panel information.
	 */
	void updatePanel() {
		settlementTableModel.updateTable();
		settlementTable.setPreferredScrollableViewportSize(settlementTable.getPreferredSize());
	}
	
	/**
	 * A table model for settlements.
	 */
    private class SettlementTableModel extends UnitTableModel {
    	
    	/**
    	 * Constructor
    	 */
    	private SettlementTableModel() {
    		// Use UnitTableModel constructor.
    		super();
    		
    		columns.add("Name");
    		columns.add("Distance");
    		columns.add("Inhabitants");
    		columns.add("Pop. Capacity");
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
            	Settlement settlement = (Settlement) getUnit(row);
            
            	if (column == 0) 
            		result = settlement.getName();
            	else if (column == 1) {
            		Settlement startingSettlement = getWizard().getMissionData().getStartingSettlement();
            		double distance = startingSettlement.getCoordinates().getDistance(settlement.getCoordinates());
            		return new Integer((int) distance);
            	}
            	else if (column == 2) 
            		result = new Integer(settlement.getCurrentPopulationNum());
            	else if (column == 3) {
            		result = new Integer(settlement.getPopulationCapacity());
            	}
            }
            
            return result;
        }
    	
    	/**
    	 * Updates the table data.
    	 */
    	void updateTable() {
    		units.clear();
    		Settlement startingSettlement = getWizard().getMissionData().getStartingSettlement();    		
    		Collection<Settlement> settlements = Simulation.instance().getUnitManager().getSettlements();
    		settlements.remove(startingSettlement);
    		
    		// Add all settlements sorted by distance from mission starting point.
    		while (settlements.size() > 0) {
    			double smallestDistance = Double.MAX_VALUE;
    			Settlement smallestDistanceSettlement = null;
    			Iterator<Settlement> i = settlements.iterator();
    			while (i.hasNext()) {
    				Settlement settlement = i.next();
    				double distance = startingSettlement.getCoordinates().getDistance(settlement.getCoordinates());
    				if (distance < smallestDistance) {
    					smallestDistance = distance;
    					smallestDistanceSettlement = settlement;
    				}
    			}
    			settlements.remove(smallestDistanceSettlement);
    			units.add(smallestDistanceSettlement);
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
    		Settlement settlement = (Settlement) getUnit(row);
    		
    		if (column == 1) {
    			try {
    				Settlement startingSettlement = getWizard().getMissionData().getStartingSettlement();
    				double distance = startingSettlement.getCoordinates().getDistance(settlement.getCoordinates());
    				double roverRange = getWizard().getMissionData().getRover().getRange();
    				if (roverRange < distance) result = true;
    			}
    			catch (Exception e) {}
    		}
    		
    		return result;
    	}
    }
}