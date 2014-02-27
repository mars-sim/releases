/**
 * Mars Simulation Project
 * EmergencySupplyMissionCustomInfoPanel.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.mission;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.mars_sim.msp.core.person.ai.mission.EmergencySupplyMission;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionEvent;
import org.mars_sim.msp.core.structure.goods.Good;

/**
 * A panel for displaying emergency supply mission information.
 */
public class EmergencySupplyMissionCustomInfoPanel extends
        MissionCustomInfoPanel {

    // Data members.
    private EmergencySupplyMission mission;
    private EmergencySuppliesTableModel emergencySuppliesTableModel;
    
    /**
     * Constructor
     */
    EmergencySupplyMissionCustomInfoPanel() {
        // Use JPanel constructor
        super();
        
        // Set the layout.
        setLayout(new BorderLayout());
        
        // Create the emergency supplies label.
        JLabel emergencySuppliesLabel = new JLabel("Emergency Supplies:", JLabel.LEFT);
        add(emergencySuppliesLabel, BorderLayout.NORTH);
        
        // Create a scroll pane for the emergency supplies table.
        JScrollPane emergencySuppliesScrollPane = new JScrollPane();
        emergencySuppliesScrollPane.setPreferredSize(new Dimension(-1, -1));
        add(emergencySuppliesScrollPane, BorderLayout.CENTER);
        
        // Create the emergency supplies table and model.
        emergencySuppliesTableModel = new EmergencySuppliesTableModel();
        JTable emergencySuppliesTable = new JTable(emergencySuppliesTableModel);
        emergencySuppliesScrollPane.setViewportView(emergencySuppliesTable);
    }
    
    @Override
    public void updateMissionEvent(MissionEvent e) {
        // Do nothing.
    }

    @Override
    public void updateMission(Mission mission) {
        if (mission instanceof EmergencySupplyMission) {
            this.mission = (EmergencySupplyMission) mission;
            emergencySuppliesTableModel.updateTable();
        }
    }
    
    /**
     * Model for the emergency supplies table.
     */
    private class EmergencySuppliesTableModel extends 
            AbstractTableModel {
        
        // Data members.
        protected Map<Good, Integer> goodsMap;
        protected List<Good> goodsList;
        
        /**
         * Constructor
         */
        private EmergencySuppliesTableModel() {
            // Use AbstractTableModel constructor.
            super();
            
            // Initialize goods map and list.
            goodsList = new ArrayList<Good>();
            goodsMap = new HashMap<Good, Integer>();
        }
        
        /**
         * Returns the number of rows in the model.
         * @return number of rows.
         */
        public int getRowCount() {
            return goodsList.size();
        }

        /**
         * Returns the number of columns in the model.
         * @return number of columns.
         */
        public int getColumnCount() {
            return 2;
        }
        
        /**
         * Returns the name of the column at columnIndex.
         * @param columnIndex the column index.
         * @return column name.
         */
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) return "Good";
            else return "Amount";
        }
        
        /**
         * Returns the value for the cell at columnIndex and rowIndex.
         * @param row the row whose value is to be queried.
         * @param column the column whose value is to be queried.
         * @return the value Object at the specified cell.
         */
        public Object getValueAt(int row, int column) {
            Object result = "unknown";
            
            if (row < goodsList.size()) {
                Good good = goodsList.get(row); 
                if (column == 0) result = good.getName();
                else result = goodsMap.get(good);
            }
            
            return result;
        }
        
        /**
         * Updates the table data.
         */
        protected void updateTable() {
            
            goodsMap = mission.getEmergencySuppliesAsGoods();
            goodsList = new ArrayList<Good>(goodsMap.keySet());
            Collections.sort(goodsList);

            fireTableDataChanged();
        }
    }
}