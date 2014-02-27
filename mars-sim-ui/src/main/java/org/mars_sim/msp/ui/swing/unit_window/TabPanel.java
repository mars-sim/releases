/**
 * Mars Simulation Project
 * TabPanel.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_window;

import java.awt.BorderLayout;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.ui.swing.MainDesktopPane;

public abstract class TabPanel extends JScrollPane {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	protected String tabTitle;
	protected Icon tabIcon;
	protected String tabToolTip;
	protected JPanel topContentPanel;
	protected JPanel centerContentPanel;
	protected Unit unit;
	protected MainDesktopPane desktop;

    /**
     * Constructor
     *
     * @param tabTitle the title to be displayed in the tab (may be null).
     * @param tabIcon the icon to be displayed in the tab (may be null).
     * @param tabToolTip the tool tip to be displayed in the icon (may be null).
     * @param unit the unit to display.
     * @param desktop the main desktop.
     */
    public TabPanel(String tabTitle, Icon tabIcon, String tabToolTip, 
            Unit unit, MainDesktopPane desktop) {
                
        // Use JScrollPane constructor
        super();
        
        // Initialize data members
        this.tabTitle = tabTitle;
        this.tabIcon = tabIcon;
        this.tabToolTip = tabToolTip;
        this.unit = unit;
        this.desktop = desktop;

        // Create the view panel
        JPanel viewPanel = new JPanel(new BorderLayout(0, 0));
        setViewportView(viewPanel);
        
        // Create top content panel
        topContentPanel = new JPanel();
        topContentPanel.setLayout(new BoxLayout(topContentPanel, BoxLayout.Y_AXIS));
        topContentPanel.setBorder(MainDesktopPane.newEmptyBorder());
        viewPanel.add(topContentPanel, BorderLayout.NORTH);
        
        // Create center content panel
        centerContentPanel = new JPanel(new BorderLayout(0, 0));
        centerContentPanel.setBorder(MainDesktopPane.newEmptyBorder());
        viewPanel.add(centerContentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Gets the tab title.
     *
     * @return tab title or null.
     */
    public String getTabTitle() {
        return tabTitle;
    }
    
    /**
     * Gets the tab icon.
     *
     * @return tab icon or null.
     */
    public Icon getTabIcon() {
        return tabIcon;
    }
    
    /**
     * Gets the tab tool tip.
     *
     * @return tab tool tip.
     */
    public String getTabToolTip() {
        return tabToolTip;
    }
    
    /**
     * Updates the info on this panel.
     */
    public abstract void update();
    
    /**
     * Gets the main desktop.
     * @return desktop.
     */
    public MainDesktopPane getDesktop() {
    	return desktop;
    }
 
    /**
     * Gets the unit.
     * @return unit.
     */
    public Unit getUnit() {
    	return unit;
    }
}