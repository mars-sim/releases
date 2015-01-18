/**
 * Mars Simulation Project
 * EquipmentWindow.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_window.equipment;

import org.mars_sim.msp.core.equipment.Equipment;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.unit_window.InventoryTabPanel;
import org.mars_sim.msp.ui.swing.unit_window.LocationTabPanel;
import org.mars_sim.msp.ui.swing.unit_window.MaintenanceTabPanel;
import org.mars_sim.msp.ui.swing.unit_window.SalvageTabPanel;
import org.mars_sim.msp.ui.swing.unit_window.UnitWindow;


/**
 * The EquipmentWindow is the window for displaying a piece of equipment.
 */
public class EquipmentWindow extends UnitWindow {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	private boolean salvaged;

    /**
     * Constructor
     *
     * @param desktop the main desktop panel.
     * @param equipment the equipment this window is for.
     */
    public EquipmentWindow(MainDesktopPane desktop, Equipment equipment) {
        // Use UnitWindow constructor
        super(desktop, equipment, false);
        
        // Add tab panels
        addTopPanel(new LocationTabPanel(equipment, desktop));
        addTabPanel(new InventoryTabPanel(equipment, desktop));
        if (equipment instanceof Malfunctionable) 
        	addTabPanel(new MaintenanceTabPanel(equipment, desktop));
        
        salvaged = equipment.isSalvaged();
        if (salvaged) addTabPanel(new SalvageTabPanel(equipment, desktop));
    }
    
    /**
     * Updates this window.
     */
    public void update() {
        super.update();
        
        // Check if equipment has been salvaged.
        Equipment equipment = (Equipment) getUnit();
        if (!salvaged && equipment.isSalvaged()) {
            addTabPanel(new SalvageTabPanel(equipment, desktop));
            salvaged = true;
        }
    }
}