/**
 * Mars Simulation Project
 * ResearchBuildingPanel.java
 * @version 3.07 2014-11-21
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure.building;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.structure.building.function.Research;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;


/**
 * The ResearchBuildingPanel class is a building function panel representing 
 * the research info of a settlement building.
 */
public class BuildingPanelResearch
extends BuildingFunctionPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	/** The research building. */
	private Research lab;

	// Data cache
	/** The number of researchers cache. */
	private int researchersCache;
	
	private JLabel researchersLabel;

	/**
	 * Constructor.
	 * @param lab the research building this panel is for.
	 * @param desktop The main desktop.
	 */
	public BuildingPanelResearch(Research lab, MainDesktopPane desktop) {

		// Use BuildingFunctionPanel constructor
		super(lab.getBuilding(), desktop);

		// Initialize data members
		this.lab = lab;

		// Set panel layout
		setLayout(new BorderLayout());

		// Prepare label panel
		JPanel labelPanel = new JPanel(new GridLayout(4, 1, 0, 0));
		add(labelPanel, BorderLayout.NORTH);
		labelPanel.setOpaque(false);
		labelPanel.setBackground(new Color(0,0,0,128));
		
		// Prepare research label
		// 2014-11-21 Changed font type, size and color and label text
		// 2014-11-21 Added internationalization for the three labels
		JLabel researchLabel = new JLabel(Msg.getString("BuildingPanelResearch.title"), JLabel.CENTER); //$NON-NLS-1$
		researchLabel.setFont(new Font("Serif", Font.BOLD, 16));
		researchLabel.setForeground(new Color(102, 51, 0)); // dark brown
		labelPanel.add(researchLabel);

		// Prepare researcher number label
		researchersCache = lab.getResearcherNum();
		researchersLabel = new JLabel(Msg.getString("BuildingPanelResearch.numberOfResearchers", researchersCache), JLabel.CENTER);
		labelPanel.add(researchersLabel);

		// Prepare researcher capacityLabel
		JLabel researcherCapacityLabel = new JLabel(Msg.getString("BuildingPanelResearch.researcherCapacity",
				lab.getLaboratorySize()),
				JLabel.CENTER);
		labelPanel.add(researcherCapacityLabel);

		// Prepare specialties label
		JLabel specialtiesLabel = new JLabel(Msg.getString("BuildingPanelResearch.namesOfSpecialties"), JLabel.CENTER);
		labelPanel.add(specialtiesLabel);

		// Get the research specialties of the building.
		ScienceType[] specialties = lab.getTechSpecialties();

		// Prepare specialtiesListPanel
		JPanel specialtiesListPanel = new JPanel(new GridLayout(specialties.length, 1, 10, 3));
		specialtiesListPanel.setBorder(new EmptyBorder(1, 20, 1, 20)); //(int top, int left, int bottom, int right)
		specialtiesListPanel.setOpaque(false);
		specialtiesListPanel.setBackground(new Color(0,0,0,128));
		
		//add(specialtiesListPanel, BorderLayout.CENTER);

		// For each specialty, add specialty name panel.
		for (ScienceType specialty : specialties) {
			JLabel specialtyLabel = new JLabel(specialty.getName(), JLabel.CENTER);
			specialtyLabel.setFont(new Font("SansSerif", Font.ITALIC, 10));
			//specialtyLabel.setForeground(Color.DARK_GRAY);
			//specialtyLabel.setBackground(Color.WHITE);
			specialtiesListPanel.add(specialtyLabel);
		}
		
		JPanel listPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));		
		listPanel.add(specialtiesListPanel);
		specialtiesListPanel.setBorder(new MarsPanelBorder());
		add(listPanel, BorderLayout.CENTER);
		listPanel.setOpaque(false);
		listPanel.setBackground(new Color(0,0,0,128));
		
	}

	/**
	 * Update this panel.
	 */
	@Override
	public void update() {
		// Update researchers label if necessary.
		if (researchersCache != lab.getResearcherNum()) {
			researchersCache = lab.getResearcherNum();
			researchersLabel.setText(
				Msg.getString("BuildingPanelResearch.numberOfResearchers",
						researchersCache));
		}
	}
}
