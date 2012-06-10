/**
 * Mars Simulation Project
 * CollectResourcesMissionCustomInfoPanel.java
 * @version 3.01 2011-07-19
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.mission;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.UnitEvent;
import org.mars_sim.msp.core.UnitListener;
import org.mars_sim.msp.core.person.ai.mission.CollectResourcesMission;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionEvent;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * A panel for displaying collect resources mission information.
 */
public class CollectResourcesMissionCustomInfoPanel extends
        MissionCustomInfoPanel implements UnitListener {

    // Data members.
    private CollectResourcesMission mission;
    private AmountResource resource;
    private Rover missionRover;
    private JLabel collectionValueLabel;
    private double resourceAmountCache;
    
    /**
     * Constructor.
     */
    CollectResourcesMissionCustomInfoPanel(AmountResource resource) {
        // Use MissionCustomInfoPanel constructor.
        super();
        
        // Initialize data members.
        this.resource = resource;
        
        // Set layout.
        setLayout(new BorderLayout());
        
        // Create content panel.
        JPanel contentPanel = new JPanel(new GridLayout(1, 2));
        add(contentPanel, BorderLayout.NORTH);
        
        // Create collection title label.
        String resourceString = resource.getName().substring(0, 1).toUpperCase() + 
                resource.getName().substring(1);
        JLabel collectionTitleLabel = new JLabel("Total " + resourceString + " Collected: ");
        contentPanel.add(collectionTitleLabel);
        
        // Create collection value label.
        collectionValueLabel = new JLabel("0 kg", JLabel.LEFT);
        contentPanel.add(collectionValueLabel);
    }
    
    @Override
    public void updateMission(Mission mission) {
        if (mission instanceof CollectResourcesMission) {
            // Remove as unit listener to any existing rovers.
            if (missionRover != null) {
                missionRover.removeUnitListener(this);
            }
            
            // Set the mission and mission rover.
            this.mission = (CollectResourcesMission) mission;
            if (this.mission.getRover() != null) {
                missionRover = this.mission.getRover();
                // Register as unit listener for mission rover.
                missionRover.addUnitListener(this);
            }
            
            resourceAmountCache = this.mission.getTotalCollectedResources();
            
            // Update the collection value label.
            updateCollectionValueLabel();
        }
    }

    @Override
    public void updateMissionEvent(MissionEvent e) {
        // Do nothing.
    }

    @Override
    public void unitUpdate(UnitEvent event) {
        if (Inventory.INVENTORY_RESOURCE_EVENT.equals(event.getType())) {
            if (resource.equals(event.getTarget())) {
                updateCollectionValueLabel();   
            }
        }
    }
    
    /**
     * Updates the collection value label.
     */
    private void updateCollectionValueLabel() {
        double resourceAmount = 0D;
        if (missionRover != null) {
            resourceAmount = missionRover.getInventory().getAmountResourceStored(resource);
            if (resourceAmount > resourceAmountCache) {
                resourceAmountCache = resourceAmount;
            }
            else {
                resourceAmount = resourceAmountCache;
            }
        }
        
        // Update collection value label.
        collectionValueLabel.setText((int) resourceAmount + " kg");
    }
}