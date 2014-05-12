/**
 * Mars Simulation Project
 * PowerStorageBuildingPanel.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_window.structure.building;

import org.mars_sim.msp.core.structure.building.function.PowerStorage;
import org.mars_sim.msp.ui.swing.MainDesktopPane;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * The PowerStorageBuildingPanel class is a building function panel representing 
 * the power storage of a settlement building.
 */
public class BuildingPanelPowerStorage
extends BuildingFunctionPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	private PowerStorage storage;
	private JLabel capacityLabel;
	private double capacityCache;
	private JLabel storedLabel;
	private double storedCache;

	/**
	 * Constructor.
	 * @param storage The power storage building function.
	 * @param desktop The main desktop.
	 */
	public BuildingPanelPowerStorage(PowerStorage storage, MainDesktopPane desktop) {

		// Use BuildingFunctionPanel constructor
		super(storage.getBuilding(), desktop);

		this.storage = storage;

		// Set the layout
		setLayout(new GridLayout(2, 1, 0, 0));

		DecimalFormat formatter = new DecimalFormat("0.0");

		// Create capacity label.
		capacityCache = storage.getPowerStorageCapacity();
		capacityLabel = new JLabel("Power Capacity: " + formatter.format(capacityCache) + 
				" kW hr", JLabel.CENTER);
		add(capacityLabel);

		// Create stored label.
		storedCache = storage.getPowerStored();
		storedLabel = new JLabel("Power Stored: " + formatter.format(storedCache) + 
				" kW hr", JLabel.CENTER);
		add(storedLabel);
	}

	@Override
	public void update() {

		DecimalFormat formatter = new DecimalFormat("0.0");

		// Update capacity label if necessary.
		double newCapacity = storage.getPowerStorageCapacity();
		if (capacityCache != newCapacity) {
			capacityCache = newCapacity;
			capacityLabel.setText("Power Capacity: " + formatter.format(capacityCache) + 
					" kW hr");
		}

		// Update stored label if necessary.
		double newStored = storage.getPowerStored();
		if (storedCache != newStored) {
			storedCache = newStored;
			storedLabel.setText("Power Stored: " + formatter.format(storedCache) + " kW hr");
		}    
	}
}