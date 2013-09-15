/**
 * Mars Simulation Project
 * StudyPanel.java
 * @version 3.00 2010-08-10
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.mission.create;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.science.Science;
import org.mars_sim.msp.core.science.ScienceUtil;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.science.ScientificStudyManager;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A wizard panel to select a scientific study for the mission.
 */
public class StudyPanel extends WizardPanel {

    // The wizard panel name.
    private final static String NAME = "Scientific Study";
    
    // Data members.
    private StudyTableModel studyTableModel;
    private JTable studyTable;
    private JLabel errorMessageLabel;
    
    StudyPanel(CreateMissionWizard wizard) {
        // Use WizardPanel constructor.
        super(wizard);
        
        // Set the layout.
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        // Set the border.
        setBorder(new MarsPanelBorder());
        
        // Create the select study label.
        JLabel selectStudyLabel = new JLabel("Select a scientific study.", JLabel.CENTER);
        selectStudyLabel.setFont(selectStudyLabel.getFont().deriveFont(Font.BOLD));
        selectStudyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(selectStudyLabel);
        
        // Create the study panel.
        JPanel studyPane = new JPanel(new BorderLayout(0, 0));
        studyPane.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));
        studyPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(studyPane);
        
        // Create scroll panel for study list.
        JScrollPane studyScrollPane = new JScrollPane();
        studyPane.add(studyScrollPane, BorderLayout.CENTER);
        
        // Create the study table model.
        Science studyScience = null;
        String missionType = wizard.getMissionData().getType();
        if (MissionDataBean.AREOLOGY_FIELD_MISSION.equals(missionType)) 
            studyScience = ScienceUtil.getScience(Science.AREOLOGY);
        else if (MissionDataBean.BIOLOGY_FIELD_MISSION.equals(missionType))
            studyScience = ScienceUtil.getScience(Science.BIOLOGY);
        studyTableModel = new StudyTableModel(studyScience);
        
        // Create the study table.
        studyTable = new JTable(studyTableModel);
        studyTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                
                Component result = super.getTableCellRendererComponent(table, value, isSelected, 
                        hasFocus, row, column);
                
                // If failure cell, mark background red.
                if (studyTableModel.isFailureCell(row, column)) setBackground(Color.RED);
                else if (!isSelected) setBackground(Color.WHITE);
                
                return result;
            }
        });
        studyTable.setRowSelectionAllowed(true);
        studyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studyTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) {
                        int index = studyTable.getSelectedRow();
                        if (index > -1) {
                            if (studyTableModel.isFailureRow(index)) {
                                errorMessageLabel.setText("mission cannot use study (see red cells).");
                                getWizard().setButtons(false);
                            }
                            else {
                                errorMessageLabel.setText(" ");
                                getWizard().setButtons(true);
                            }
                        }
                    }
                }
            });
        studyTable.setPreferredScrollableViewportSize(studyTable.getPreferredSize());
        studyScrollPane.setViewportView(studyTable);
        
        // Create the error message label.
        errorMessageLabel = new JLabel(" ", JLabel.CENTER);
        errorMessageLabel.setForeground(Color.RED);
        errorMessageLabel.setFont(errorMessageLabel.getFont().deriveFont(Font.BOLD));
        errorMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(errorMessageLabel);
        
        // Add a vertical glue.
        add(Box.createVerticalGlue());
    }
    
    @Override
    void clearInfo() {
        studyTable.clearSelection();
        errorMessageLabel.setText(" ");
    }

    @Override
    boolean commitChanges() {
        int selectedIndex = studyTable.getSelectedRow();
        ScientificStudy selectedStudy = studyTableModel.getStudy(selectedIndex);
        getWizard().getMissionData().setScientificStudy(selectedStudy);
        return true;
    }

    @Override
    String getPanelName() {
        return NAME;
    }

    @Override
    void updatePanel() {
        studyTableModel.updateTable();
        studyTable.setPreferredScrollableViewportSize(studyTable.getPreferredSize());
    }
    
    /**
     * A table model for scientific studies.
     */
    private static class StudyTableModel extends AbstractTableModel {
        
        // Data members
        private Science studyScience;
        private String scienceName;
        private List<ScientificStudy> studies;
        
        /**
         * Constructor
         */
        private StudyTableModel(Science studyScience) {
            // Use AbstractTableModel constructor.
            super();
            
            this.studyScience = studyScience;
            
            // Add all ongoing scientific studies to table sorted by name.
            ScientificStudyManager manager = Simulation.instance().getScientificStudyManager();
            studies = manager.getOngoingStudies();
            Collections.sort(studies);
            
            scienceName = studyScience.getName().substring(0, 1).toUpperCase() + 
                    studyScience.getName().substring(1);
        }
        
        /**
         * Gets the scientific study in the table at a given index.
         * @param index the index of the study.
         * @return study.
         */
        private ScientificStudy getStudy(int index) {
            ScientificStudy result = null;
            if ((index >= 0) && (index < studies.size()))
                result = studies.get(index);
            return result;
        }
        
        /**
         * Returns the number of columns in the model.
         * @return number of columns.
         */
        public int getColumnCount() {
            return 3;
        }
        
        /**
         * Returns the number of rows in the model.
         * @return number of rows.
         */
        public int getRowCount() {
            return studies.size();
        }
        
        /**
         * Returns the name of the column at columnIndex.
         * @param columnIndex the column index.
         * @return column name.
         */
        public String getColumnName(int columnIndex) {
            String result = "unknown";
            if (columnIndex == 0) result = "Study";
            else if (columnIndex == 1) result = "Phase";
            else if (columnIndex == 2) result = scienceName + " Researchers";
            return result;
        }
        
        /**
         * Returns the value for the cell at columnIndex and rowIndex.
         * @param row the row whose value is to be queried
         * @param column the column whose value is to be queried
         * @return the value Object at the specified cell
         */
        public Object getValueAt(int row, int column) {
            Object result = "unknown";
            
            if (row < studies.size()) {
                try {
                    ScientificStudy study = studies.get(row);
                    
                    if (column == 0) 
                        result = study.toString();
                    else if (column == 1) 
                        result = study.getPhase();
                    else if (column == 2) 
                        result = getScienceResearcherNum(study);
                }
                catch (Exception e) {}
            }
            
            return result;
        }
        
        /**
         * Gets the number of researchers for a particular science in a study.
         * @param study the scientific study.
         * @return number of researchers.
         */
        private int getScienceResearcherNum(ScientificStudy study) {
            int result = 0;
            
            if (study.getScience().equals(studyScience)) result++;
            
            Iterator<Science> i = study.getCollaborativeResearchers().values().iterator();
            while (i.hasNext()) {
                if (i.next().equals(studyScience)) result++;
            }
            
            return result;
        }
        
        /**
         * Updates the table data.
         */
        void updateTable() {
            // Add all ongoing scientific studies to table sorted by name.
            ScientificStudyManager manager = Simulation.instance().getScientificStudyManager();
            studies = manager.getOngoingStudies();
            Collections.sort(studies);
            
            fireTableStructureChanged();
        }
        
        /**
         * Checks if a table cell is a failure cell.
         * @param row the table row.
         * @param column the table column.
         * @return true if cell is a failure cell.
         */
        boolean isFailureCell(int row, int column) {
            boolean result = false;
            ScientificStudy study = studies.get(row);
            
            try {
                if (column == 1) {
                    if (!ScientificStudy.RESEARCH_PHASE.equals(study.getPhase())) result = true;
                }
                else if (column == 2) {
                    if (getScienceResearcherNum(study) == 0) result = true;
                }
            }
            catch (Exception e) {}
            
            return result;
        }
        
        /**
         * Checks if row contains a failure cell.
         * @param row the row index.
         * @return true if row has failure cell.
         */
        boolean isFailureRow(int row) {
            boolean result = false;
            for (int x = 0; x < getColumnCount(); x++) {
                if (isFailureCell(row, x)) result = true;
            }
            return result;
        }
    }
}