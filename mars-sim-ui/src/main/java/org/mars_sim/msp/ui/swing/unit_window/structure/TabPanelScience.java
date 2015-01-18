/**
 * Mars Simulation Project
 * ScienceTabPanel.java
 * @version 3.07 2014-12-01
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang3.text.WordUtils;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.science.ScientificStudyManager;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.NumberCellRenderer;
import org.mars_sim.msp.ui.swing.tool.science.ScienceWindow;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;

/**
 * A tab panel displaying a settlement's scientific studies and achievements.
 */
public class TabPanelScience
extends TabPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	private JButton scienceToolButton;
	private StudyTableModel studyTableModel;
	private JTable studyTable;
	private AchievementTableModel achievementTableModel;
	private JTable achievementTable;
	private JLabel totalAchievementLabel;

	/**
	 * Constructor.
	 * @param settlement the settlement.
	 * @param desktop the main desktop.
	 */
	public TabPanelScience(Settlement settlement, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelScience.title"), //$NON-NLS-1$
			null,
			Msg.getString("TabPanelScience.tooltip"), //$NON-NLS-1$
			settlement, desktop
		);

		// Create the title panel.
		JPanel titlePane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContentPanel.add(titlePane);

		// Create the title label.
		JLabel titleLabel = new JLabel(Msg.getString("TabPanelScience.label"), JLabel.CENTER); //$NON-NLS-1$
		titleLabel.setFont(new Font("Serif", Font.BOLD, 16));
		titleLabel.setForeground(new Color(102, 51, 0)); // dark brown
		titlePane.add(titleLabel);

		// Create the main panel.
		JPanel mainPane = new JPanel(new GridLayout(2, 1, 0, 0));
		centerContentPanel.add(mainPane);

		// Create the studies panel.
		JPanel studiesPane = new JPanel(new BorderLayout());
		studiesPane.setBorder(new MarsPanelBorder());
		mainPane.add(studiesPane);

		// Create the studies label.
		JLabel studiesLabel = new JLabel(Msg.getString("TabPanelScience.scientificStudies"), JLabel.CENTER); //$NON-NLS-1$
		studiesPane.add(studiesLabel, BorderLayout.NORTH);

		// Create the study scroll panel.
		JScrollPane studyScrollPane = new JScrollPane();
		studyScrollPane.setBorder(new MarsPanelBorder());
		studyScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		studiesPane.add(studyScrollPane, BorderLayout.CENTER);

		// Create the study table.
		studyTableModel = new StudyTableModel(settlement);
		studyTable = new JTable(studyTableModel);
		studyTable.setPreferredScrollableViewportSize(new Dimension(225, -1));
		studyTable.setCellSelectionEnabled(false);
		studyTable.setRowSelectionAllowed(true);
		studyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		studyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) {
					if (studyTable.getSelectedRow() >= 0) setEnabledScienceToolButton(true);
				}
			}
		});
		studyScrollPane.setViewportView(studyTable);     

		// Create the button panel.
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		studiesPane.add(buttonPane, BorderLayout.SOUTH);

		// Create the science tool button.
		scienceToolButton = new JButton(ImageLoader.getIcon(Msg.getString("img.science"))); //$NON-NLS-1$
		scienceToolButton.setEnabled(false);
		scienceToolButton.setMargin(new Insets(1, 1, 1, 1));
		scienceToolButton.setToolTipText(Msg.getString("TabPanelScience.tooltip.science")); //$NON-NLS-1$
		scienceToolButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				displayStudyInScienceTool();
			}
		});
		buttonPane.add(scienceToolButton);

		// Create the achievement panel.
		JPanel achievementPane = new JPanel(new BorderLayout());
		achievementPane.setBorder(new MarsPanelBorder());
		mainPane.add(achievementPane);

		// Create achievement label panel.
		JPanel achievementLabelPane = new JPanel(new GridLayout(2, 1, 0, 0));
		achievementPane.add(achievementLabelPane, BorderLayout.NORTH);

		// Create the achievement label.
		JLabel achievementLabel = new JLabel(Msg.getString("TabPanelScience.scientificAchievement"), JLabel.CENTER); //$NON-NLS-1$
		achievementLabelPane.add(achievementLabel);

		DecimalFormat formatter = new DecimalFormat(Msg.getString("TabPanelScience.decimalFormat")); //$NON-NLS-1$
		String totalAchievementString = formatter.format(settlement.getTotalScientificAchievement());
		totalAchievementLabel = new JLabel(
			Msg.getString(
				"TabPanelScience.totalAchievementCredit", //$NON-NLS-1$
				totalAchievementString
			),JLabel.CENTER
		);
		achievementLabelPane.add(totalAchievementLabel);

		// Create the achievement scroll panel.
		JScrollPane achievementScrollPane = new JScrollPane();
		achievementScrollPane.setBorder(new MarsPanelBorder());
		achievementScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		achievementPane.add(achievementScrollPane, BorderLayout.CENTER);

		// Create the achievement table.
		achievementTableModel = new AchievementTableModel(settlement);
		achievementTable = new JTable(achievementTableModel);
		achievementTable.setPreferredScrollableViewportSize(new Dimension(225, -1));
		achievementTable.setCellSelectionEnabled(false);
		achievementTable.setDefaultRenderer(Double.class, new NumberCellRenderer(1));
		achievementScrollPane.setViewportView(achievementTable);     
	}

	@Override
	public void update() {
		// Get selected study in table if any.
		int selectedStudyIndex = studyTable.getSelectedRow();
		ScientificStudy selectedStudy = null;
		if (selectedStudyIndex >= 0) selectedStudy = studyTableModel.getStudy(selectedStudyIndex);

		// Update study table model.
		studyTableModel.update();

		// Reselect study in table.
		if (selectedStudy != null) {
			int newStudyIndex = studyTableModel.getStudyIndex(selectedStudy);
			if (newStudyIndex >= 0) 
				studyTable.getSelectionModel().setSelectionInterval(newStudyIndex, newStudyIndex);
		}

		// Update achievement table model.
		achievementTableModel.update();

		// Update total achievement label.
		Settlement settlement = (Settlement) getUnit();
		DecimalFormat formatter = new DecimalFormat(Msg.getString("TabPanelScience.decimalFormat")); //$NON-NLS-1$
		String totalAchievementString = formatter.format(settlement.getTotalScientificAchievement());
		//totalAchievementLabel.setText(Msg.getString("TabPanelScience.totalAchievementCredit") + totalAchievementString); //$NON-NLS-1$
		totalAchievementLabel.setText(Msg.getString("TabPanelScience.totalAchievementCredit", totalAchievementString)); //$NON-NLS-1$
	}

	/**
	 * Sets if the science tool button is enabled or not.
	 * @param enabled true if button enabled.
	 */
	private void setEnabledScienceToolButton(boolean enabled) {
		scienceToolButton.setEnabled(enabled);
	}

	/**
	 * Displays the scientific study selected in the table in the science tool.
	 */
	private void displayStudyInScienceTool() {
		int selectedStudyIndex = studyTable.getSelectedRow();
		if (selectedStudyIndex >= 0) {
			ScientificStudy selectedStudy = studyTableModel.getStudy(selectedStudyIndex);
			((ScienceWindow) desktop.getToolWindow(ScienceWindow.NAME)).setScientificStudy(selectedStudy);
			getDesktop().openToolWindow(ScienceWindow.NAME);
		}
	}

	/**
	 * Inner class for study table model.
	 */
	private class StudyTableModel extends AbstractTableModel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		// Data members.
		private Settlement settlement;
		private List<ScientificStudy> studies;

		/**
		 * Constructor
		 * @param settlement the settlement.
		 */
		private StudyTableModel(Settlement settlement) {
			// Use AbstractTableModel constructor.
			super();

			this.settlement = settlement;

			// Get all studies the settlement is primary for.
			ScientificStudyManager manager = Simulation.instance().getScientificStudyManager();
			studies = manager.getAllStudies(settlement);
		}

		/**
		 * Returns the number of columns in the model.
		 * @return the number of columns in the model.
		 */
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) return Msg.getString("TabPanelScience.column.study"); //$NON-NLS-1$
			else if (columnIndex == 1) return Msg.getString("TabPanelScience.column.status"); //$NON-NLS-1$
			else return null;
		}

		/**
		 * Returns the number of rows in the model.
		 * @return the number of rows in the model.
		 */
		public int getRowCount() {
			return studies.size();
		}

		/**
		 * Returns the value for the cell at columnIndex and rowIndex.
		 * @param rowIndex the row whose value is to be queried.
		 * @param columnIndex the column whose value is to be queried.
		 * @return the value Object at the specified cell.
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {
			String result = null;
			if ((rowIndex >= 0) && (rowIndex < studies.size())) {
				ScientificStudy study = studies.get(rowIndex);
				// 2014-12-01 Added WordUtils.capitalize()
				if (columnIndex == 0) result = WordUtils.capitalize(study.toString());
				else if (columnIndex == 1) {
					if (study.isCompleted()) result = study.getCompletionState();
					else result = study.getPhase();
				}
			}
			return result;
		}

		/**
		 * Updates the table model.
		 */
		private void update() {
			ScientificStudyManager manager = Simulation.instance().getScientificStudyManager();
			List<ScientificStudy> newStudies = manager.getAllStudies(settlement);
			if (!newStudies.equals(studies)) studies = newStudies;
			fireTableDataChanged();
		}

		/**
		 * Gets the scientific study in the table at a given row index.
		 * @param rowIndex the row index in the table.
		 * @return scientific study or null if invalid index.
		 */
		private ScientificStudy getStudy(int rowIndex) {
			ScientificStudy result = null;
			if ((rowIndex >= 0) && (rowIndex < studies.size())) 
				result = studies.get(rowIndex);
			return result;
		}

		/**
		 * Gets the row index of a given scientific study.
		 * @param study the scientific study.
		 * @return the table row index or -1 if not in table.
		 */
		private int getStudyIndex(ScientificStudy study) {
			int result = -1;
			if ((study != null) && studies.contains(study)) result = studies.indexOf(study);
			return result;
		}
	}

	/**
	 * Inner class for achievement table model.
	 */
	private class AchievementTableModel
	extends AbstractTableModel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		// Data members.
		private Settlement settlement;
		private List<ScienceType> sciences;

		/** hidden constructor. */
		private AchievementTableModel(Settlement settlement) {
			// Use AbstractTableModel constructor.
			super();
			this.settlement = settlement;
			sciences = ScienceType.valuesList();
		}

		/**
		 * Returns the number of columns in the model.
		 * @return the number of columns in the model.
		 */
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) return Msg.getString("TabPanelScience.column.science"); //$NON-NLS-1$
			else if (columnIndex == 1) return Msg.getString("TabPanelScience.column.achievementCredit"); //$NON-NLS-1$
			else return null;
		}

		/**
		 * Returns the most specific superclass for all the cell values in the column.
		 * @param columnIndex the index of the column.
		 * @return the common ancestor class of the object values in the model.
		 */
		public Class<?> getColumnClass(int columnIndex) {
			Class<?> dataType = super.getColumnClass(columnIndex);
			if (columnIndex == 0) dataType = String.class;
			else if (columnIndex == 1) dataType = Double.class;
			return dataType;
		}

		/**
		 * Returns the number of rows in the model.
		 * @return the number of rows in the model.
		 */
		public int getRowCount() {
			return sciences.size();
		}

		/**
		 * Returns the value for the cell at columnIndex and rowIndex.
		 * @param rowIndex the row whose value is to be queried.
		 * @param columnIndex the column whose value is to be queried.
		 * @return the value Object at the specified cell.
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {
			Object result = null;
			if ((rowIndex >= 0) && (rowIndex < sciences.size())) {
				ScienceType science = sciences.get(rowIndex);
				if (columnIndex == 0) result = science.getName();
				else if (columnIndex == 1) {
					result = settlement.getScientificAchievement(science);
				}
			}
			return result;
		}

		/**
		 * Updates the table model.
		 */
		private void update() {
			fireTableDataChanged();
		}
	}
}