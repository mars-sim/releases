/**
 * Mars Simulation Project
 * SettlementWindow.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_window.structure;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.unit_window.InventoryTabPanel;
import org.mars_sim.msp.ui.swing.unit_window.LocationTabPanel;
import org.mars_sim.msp.ui.swing.unit_window.UnitWindow;

/**
 * The SettlementWindow is the window for displaying a settlement.
 */
public class SettlementWindow extends UnitWindow {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/**
     * Constructor
     *
     * @param desktop the main desktop panel.
     * @param unit the unit to display.
     */
    public SettlementWindow(MainDesktopPane desktop, Unit unit) {
        // Use UnitWindow constructor
        super(desktop, unit, false);
        
        Settlement settlement = (Settlement) unit;
        
        // Add tab panels
        addTopPanel(new LocationTabPanel(settlement, desktop));
        addTabPanel(new TabPanelPopulation(settlement, desktop));
        addTabPanel(new TabPanelAssociatedPeople(settlement, desktop));
        addTabPanel(new TabPanelVehicles(settlement, desktop));
        addTabPanel(new InventoryTabPanel(settlement, desktop));
        addTabPanel(new TabPanelPowerGrid(settlement, desktop));
        addTabPanel(new TabPanelBuildings(settlement, desktop));
        addTabPanel(new TabPanelGoods(settlement, desktop));
        addTabPanel(new TabPanelCredit(settlement, desktop));
        addTabPanel(new TabPanelResourceProcesses(settlement, desktop));
        addTabPanel(new TabPanelMaintenance(settlement, desktop));
        addTabPanel(new TabPanelManufacture(settlement, desktop));
        addTabPanel(new TabPanelMissions(settlement, desktop));
        addTabPanel(new TabPanelConstruction(settlement, desktop));
        addTabPanel(new TabPanelScience(settlement, desktop));
    }
}
