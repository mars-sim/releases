/**
 * Mars Simulation Project
 * BuildingsTabPanel.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.ui.swing.JComboBoxMW;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;
import org.mars_sim.msp.ui.swing.unit_window.structure.building.BuildingPanel;

/**
 * The BuildingsTabPanel is a tab panel containing building panels.
 */
public class TabPanelBuildings
extends TabPanel
implements ActionListener {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private DefaultComboBoxModel<Building> buildingComboBoxModel;
	private JComboBoxMW<Building> buildingComboBox;
	private List<Building> buildingsCache;
	private JPanel buildingDisplayPanel;
	private CardLayout buildingLayout;
	private List<BuildingPanel> buildingPanels;
	private int count;

	/**
	 * Constructor.
	 * @param unit the unit to display.
	 * @param desktop the main desktop.
	 */
	public TabPanelBuildings(Unit unit, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelBuildings.title"), //$NON-NLS-1$
			null,
			Msg.getString("TabPanelBuildings.tooltip"), //$NON-NLS-1$
			unit, desktop
		);

		Settlement settlement = (Settlement) unit;
		List<Building> buildings = settlement.getBuildingManager().getBuildings();
		Collections.sort(buildings);

		// Create building select panel.
		JPanel buildingSelectPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buildingSelectPanel.setBorder(new MarsPanelBorder());
		topContentPanel.add(buildingSelectPanel);

		// Create building combo box model.
		buildingComboBoxModel = new DefaultComboBoxModel<Building>();
		buildingsCache = new ArrayList<Building>(buildings);
		Iterator<Building> i = buildingsCache.iterator();
		while (i.hasNext()) buildingComboBoxModel.addElement(i.next());

		// Create building list.
		buildingComboBox = new JComboBoxMW<Building>(buildingComboBoxModel);
		buildingComboBox.addActionListener(this);
		buildingComboBox.setMaximumRowCount(10);
		buildingSelectPanel.add(buildingComboBox);

		// Create building display panel.
		buildingDisplayPanel = new JPanel();
		buildingLayout = new CardLayout();
		buildingDisplayPanel.setLayout(buildingLayout);
		buildingDisplayPanel.setBorder(new MarsPanelBorder());
		centerContentPanel.add(buildingDisplayPanel);

		// Create building panels
		buildingPanels = new ArrayList<BuildingPanel>();
		count = 0;
		Iterator<Building> iter = buildings.iterator();
		while (iter.hasNext()) {
			BuildingPanel panel = new BuildingPanel(String.valueOf(count), iter.next(), desktop);
			buildingPanels.add(panel);
			buildingDisplayPanel.add(panel, panel.getPanelName());
			count++;
		}
	}

	/**
	 * Updates the info on this panel.
	 */
	@Override
	public void update() {
		Settlement settlement = (Settlement) unit;
		List<Building> buildings = settlement.getBuildingManager().getBuildings();

		// Update buildings if necessary.
		if (!buildingsCache.equals(buildings)) {

			// Add building panels for new buildings.
			Iterator<Building> iter1 = buildings.iterator();
			while (iter1.hasNext()) {
				Building building = iter1.next();
				if (!buildingsCache.contains(building)) {
					BuildingPanel panel = new BuildingPanel(String.valueOf(count), building, desktop);
					buildingPanels.add(panel);
					buildingDisplayPanel.add(panel, panel.getPanelName());
					buildingComboBoxModel.addElement(building);
					count++;
				}
			}

			// Remove building panels for destroyed buildings.
			Iterator<Building> iter2 = buildingsCache.iterator();
			while (iter2.hasNext()) {
				Building building = iter2.next();
				if (!buildings.contains(building)) {
					BuildingPanel panel = getBuildingPanel(building);
					if (panel != null) {
						buildingPanels.remove(panel);
						buildingDisplayPanel.remove(panel);
						buildingComboBoxModel.removeElement(building);
					}
				}
			}

			// Update buildings cache.
			buildingsCache = buildings;
		}

		// Have each building panel update.
		Iterator<BuildingPanel> i = buildingPanels.iterator();
		while (i.hasNext()) i.next().update();
	}

	/** 
	 * Action event occurs.
	 * @param event the action event
	 */
	public void actionPerformed(ActionEvent event) {
		Building building = (Building) buildingComboBox.getSelectedItem();
		BuildingPanel panel = getBuildingPanel(building);
		if (panel != null) buildingLayout.show(buildingDisplayPanel, panel.getPanelName());
		else System.err.println(Msg.getString("TabPanelBuildings.err.cantFindPanelForBuilding", building.getName())); //$NON-NLS-1$
	}

	/**
	 * Gets the building panel for a given building.
	 * @param building the given building
	 * @return the building panel or null if none.
	 */
	private BuildingPanel getBuildingPanel(Building building) {
		BuildingPanel result = null;
		Iterator<BuildingPanel> i = buildingPanels.iterator();
		while (i.hasNext()) {
			BuildingPanel panel = i.next();
			if (panel.getBuilding() == building) result = panel;
		}

		return result;
	}
}