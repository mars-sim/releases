/**
 * Mars Simulation Project
 * ConstructedBuildingsPanel.java
 * @version 3.07 2014-12-03
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.mars_sim.msp.core.structure.construction.ConstructedBuildingLogEntry;
import org.mars_sim.msp.core.structure.construction.ConstructionManager;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;

public class ConstructedBuildingsPanel
extends JPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	private ConstructedBuildingTableModel constructedTableModel = null;

	/**
	 * Constructor.
	 * @param manager the settlement construction manager.
	 */
	public ConstructedBuildingsPanel(ConstructionManager manager) {
		// Use JPanel constructor.
		super();

		setLayout(new BorderLayout(0, 0));
		setBorder(new MarsPanelBorder());

		JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		add(titlePanel, BorderLayout.NORTH);

		JLabel titleLabel = new JLabel("Constructed Buildings");
		titlePanel.add(titleLabel);

		// Create scroll panel for the outer table panel.
		JScrollPane scrollPanel = new JScrollPane();
		scrollPanel.setPreferredSize(new Dimension(200, 75));
		scrollPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPanel, BorderLayout.CENTER);         

		// Prepare outer table panel.
		//JPanel outerTablePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		//outerTablePanel.setBorder(new MarsPanelBorder());
		//scrollPanel.setViewportView(outerTablePanel);   

		// Prepare constructed table panel.
		//JPanel constructedTablePanel = new JPanel(new BorderLayout(0, 0));
		//outerTablePanel.add(constructedTablePanel);

		// Prepare constructed table model.
		constructedTableModel = new ConstructedBuildingTableModel(manager);

		// Prepare constructed table.
		JTable constructedTable = new JTable(constructedTableModel);
		scrollPanel.setViewportView(constructedTable);
		constructedTable.setCellSelectionEnabled(false);
		constructedTable.getColumnModel().getColumn(0).setPreferredWidth(105);
		constructedTable.getColumnModel().getColumn(1).setPreferredWidth(105);
		// 2014-12-03 Added the two methods below to make all heatTable columns
		//resizable automatically when its Panel resizes
		constructedTable.setPreferredScrollableViewportSize(new Dimension(225, -1));
		constructedTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		//constructedTablePanel.add(constructedTable.getTableHeader(), BorderLayout.NORTH);
		//constructedTablePanel.add(constructedTable, BorderLayout.CENTER);
	}

	/**
	 * Update the information on this panel.
	 */
	public void update() {
		constructedTableModel.update();
	}

	/** 
	 * Internal class used as model for the constructed table.
	 */
	private static class ConstructedBuildingTableModel
	extends AbstractTableModel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		// Data members
		ConstructionManager manager;

		private ConstructedBuildingTableModel(ConstructionManager manager) {
			this.manager = manager;
		}

		public int getRowCount() {
			return manager.getConstructedBuildingLog().size();
		}

		public int getColumnCount() {
			return 2;
		}

		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) return "Building";
			else if (columnIndex == 1) return "Time Stamp";
			else return null;
		}

		public Object getValueAt(int row, int column) {
			if (row < getRowCount()) {
				ConstructedBuildingLogEntry logEntry = manager.getConstructedBuildingLog().get(row);
				if (column == 0) return logEntry.getBuildingName();
				else if (column == 1) return logEntry.getBuiltTime().toString();
				else return null;  
			}
			else return null;
		}

		public void update() {
			fireTableDataChanged();
		}
	}
}