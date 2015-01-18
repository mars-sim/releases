/**
 * Mars Simulation Project
 * MedicalCareBuildingPanel.java
 * @version 3.07 2014-11-21
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure.building;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.medical.HealthProblem;
import org.mars_sim.msp.core.structure.building.function.MedicalCare;
import org.mars_sim.msp.ui.swing.MainDesktopPane;

/**
 * The MedicalCareBuildingPanel class is a building function panel representing 
 * the medical info of a settlement building.
 */
public class BuildingPanelMedicalCare
extends BuildingFunctionPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	/** The medical care. */
	private MedicalCare medical;
	/** Label of number of physicians. */
	private JLabel physicianLabel;
	/** Table of medical info. */
	private MedicalTableModel medicalTableModel;

	// Data cache
	/** Cache of number of physicians. */
	private int physicianCache;

	/**
	 * Constructor.
	 * @param medical the medical care building this panel is for.
	 * @param desktop The main desktop.
	 */
	public BuildingPanelMedicalCare(MedicalCare medical, MainDesktopPane desktop) {

		// Use BuildingFunctionPanel constructor
		super(medical.getBuilding(), desktop);

		// Initialize data members
		this.medical = medical;

		// Set panel layout
		setLayout(new BorderLayout());

		// Create label panel
		JPanel labelPanel = new JPanel(new GridLayout(3, 1, 0, 0));
		add(labelPanel, BorderLayout.NORTH);
		labelPanel.setOpaque(false);
		labelPanel.setBackground(new Color(0,0,0,128));
        
		// Create medical care label
		// 2014-11-21 Changed font type, size and color and label text
		// 2014-11-21 Added internationalization for labels
		JLabel medicalCareLabel = new JLabel(Msg.getString("BuildingPanelMedicalCare.title"), JLabel.CENTER);
		medicalCareLabel.setFont(new Font("Serif", Font.BOLD, 16));
		medicalCareLabel.setForeground(new Color(102, 51, 0)); // dark brown
		labelPanel.add(medicalCareLabel);

		// Create sick bed label
		JLabel sickBedLabel = new JLabel(Msg.getString("BuildingPanelMedicalCare.numberOfsickBeds",
				medical.getSickBedNum()), JLabel.CENTER);
		labelPanel.add(sickBedLabel);

		// Create physician label
		physicianCache = medical.getPhysicianNum();
		physicianLabel = new JLabel(Msg.getString("BuildingPanelMedicalCare.numberOfPhysicians", 
				physicianCache), JLabel.CENTER);
		labelPanel.add(physicianLabel);

		// Create scroll panel for medical table
		JScrollPane scrollPanel = new JScrollPane();
		scrollPanel.setPreferredSize(new Dimension(160, 80));
		add(scrollPanel, BorderLayout.CENTER);
	    scrollPanel.getViewport().setOpaque(false);
	    scrollPanel.getViewport().setBackground(new Color(0, 0, 0, 0));
	    scrollPanel.setOpaque(false);
	    scrollPanel.setBackground(new Color(0, 0, 0, 0));
        //scrollPanel.setBorder( BorderFactory.createLineBorder(Color.orange) );

		
		// Prepare medical table model
		medicalTableModel = new MedicalTableModel(medical);

		// Prepare medical table
		JTable medicalTable = new JTable(medicalTableModel);
		medicalTable.setCellSelectionEnabled(false);
		scrollPanel.setViewportView(medicalTable);
	}

	/**
	 * Update this panel
	 */
	public void update() {

		// Update physician label
		if (physicianCache != medical.getPhysicianNum()) {
			physicianCache = medical.getPhysicianNum();
			physicianLabel.setText("Physicians: " + physicianCache);
		}

		// Update medical table model.
		medicalTableModel.update();
	}

	/** 
	 * Internal class used as model for the medical table.
	 */
	private static class MedicalTableModel extends AbstractTableModel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		private MedicalCare medical;
		private java.util.List<?> healthProblems;

		private MedicalTableModel(MedicalCare medical) {
			this.medical = medical;
			healthProblems = medical.getProblemsBeingTreated();
		}

		public int getRowCount() {
			return healthProblems.size();
		}

		public int getColumnCount() {
			return 2;
		}

		public Class<?> getColumnClass(int columnIndex) {
			Class<?> dataType = super.getColumnClass(columnIndex);
			if (columnIndex == 0) dataType = String.class;
			else if (columnIndex == 1) dataType = String.class;
			return dataType;
		}

		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) return "Patient";
			else if (columnIndex == 1) return "Condition";
			else return "unknown";
		}

		public Object getValueAt(int row, int column) {

			HealthProblem problem = (HealthProblem) healthProblems.get(row);

			if (column == 0) return problem.getSufferer().getName();
			else if (column == 1) return problem.toString();
			else return "unknown";
		}

		public void update() {
			if (!healthProblems.equals(medical.getProblemsBeingTreated()))
				healthProblems = medical.getProblemsBeingTreated();

			fireTableDataChanged();
		}
	}
}