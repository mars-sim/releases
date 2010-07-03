/**
 * Mars Simulation Project
 * ResourceProcessTabTabPanel.java
 * @version 2.87 2009-10-04
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.function.ResourceProcess;
import org.mars_sim.msp.core.structure.building.function.ResourceProcessing;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;

/**
 * A tab panel for displaying all of the resource processes in a settlement.
 */
public class ResourceProcessesTabPanel extends TabPanel {

	// Data members
	private List<Building> processingBuildings;
	private JScrollPane processesScrollPanel;
	private JPanel processListPanel;
	private JCheckBox overrideCheckbox;
	
    /**
     * Constructor
     * @param unit the unit to display.
     * @param desktop the main desktop.
     */
    public ResourceProcessesTabPanel(Unit unit, MainDesktopPane desktop) { 
        
        // Use the TabPanel constructor
        super("Processes", null, "Resource Processes", unit, desktop);
        
        Settlement settlement = (Settlement) unit;
        processingBuildings = settlement.getBuildingManager().getBuildings(ResourceProcessing.NAME);
        
        // Prepare resource processes label panel.
        JPanel resourceProcessesLabelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topContentPanel.add(resourceProcessesLabelPanel);
        
        // Prepare esource processes label.
        JLabel resourceProcessesLabel = new JLabel("Resource Processes", JLabel.CENTER);
        resourceProcessesLabelPanel.add(resourceProcessesLabel);
        
		// Create scroll panel for the outer table panel.
		processesScrollPanel = new JScrollPane();
		processesScrollPanel.setPreferredSize(new Dimension(220, 280));
		topContentPanel.add(processesScrollPanel);         
        
        // Prepare process list panel.
        processListPanel = new JPanel(new GridLayout(0, 1, 5, 2));
        processListPanel.setBorder(new MarsPanelBorder());
        processesScrollPanel.setViewportView(processListPanel);
        populateProcessList();
        
        // Create override check box panel.
        JPanel overrideCheckboxPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topContentPanel.add(overrideCheckboxPane);
        
        // Create override check box.
        overrideCheckbox = new JCheckBox("Override resource process toggling");
        overrideCheckbox.setToolTipText("Prevents settlement inhabitants from " +
        		"toggling on/off resource processes.");
        overrideCheckbox.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent arg0) {
        		setResourceProcessOverride(overrideCheckbox.isSelected());
        	}
        });
        overrideCheckbox.setSelected(settlement.getManufactureOverride());
        overrideCheckboxPane.add(overrideCheckbox);
    }
    
    /**
     * Populates the process list panel with all building processes.
     */
    private void populateProcessList() {
    	// Clear the list.
    	processListPanel.removeAll();
    	
    	try {
    		// Add a label for each process in each processing building.
    		Iterator<Building> i = processingBuildings.iterator();
    		while (i.hasNext()) {
    			Building building = (Building) i.next();
    			ResourceProcessing processing = (ResourceProcessing) building.getFunction(ResourceProcessing.NAME);
    			Iterator<ResourceProcess> j = processing.getProcesses().iterator();
    			while (j.hasNext()) {
    				ResourceProcess process = (ResourceProcess) j.next();
    				processListPanel.add(new ResourceProcessPanel(process, building));
    			}
    		}
    	}
    	catch (BuildingException e) {
    		e.printStackTrace(System.err);
    	}
    }
	
	@Override
	public void update() {
		
		// Check if building list has changed.
		Settlement settlement = (Settlement) unit;
		List<Building> tempBuildings = settlement.getBuildingManager().getBuildings(ResourceProcessing.NAME);
		if (!tempBuildings.equals(processingBuildings)) {
			// Populate process list.
			processingBuildings = tempBuildings;
			populateProcessList();
			processesScrollPanel.validate();
		}
		else {
			// Update process list.
			Component[] components = processListPanel.getComponents();
			for (int x = 0; x < components.length; x++) {
				ResourceProcessPanel panel = (ResourceProcessPanel) components[x];
				panel.update();
			}
		}
	}
	
	/**
	 * Sets the settlement resource process override flag.
	 * @param override the resource process override flag.
	 */
	private void setResourceProcessOverride(boolean override) {
		Settlement settlement = (Settlement) unit;
		settlement.setResourceProcessOverride(override);
	}
	
	/**
	 * An internal class for a resource process panel.
	 */
	private class ResourceProcessPanel extends JPanel {
		
		// Data members.
		private ResourceProcess process;
		private JLabel label;
		private JButton toggleButton;
		private ImageIcon greenDot;
		private ImageIcon redDot;
		private DecimalFormat decFormatter = new DecimalFormat("0.00");
		
		/**
		 * Constructor
		 * @param process the resource process.
		 * @param building the building the process is in.
		 */
		ResourceProcessPanel(ResourceProcess process, Building building) {
			// Use JPanel constructor.
			super();
			
			setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
			
			this.process = process;
			
	        toggleButton = new JButton();
	        toggleButton.setMargin(new Insets(0, 0, 0, 0));
	        toggleButton.addActionListener(new ActionListener() {
	        	public void actionPerformed(ActionEvent event) {
	        		ResourceProcess process = getProcess();
	        		process.setProcessRunning(!process.isProcessRunning());
	        		update();
	        	}
	        });
	        toggleButton.setToolTipText("Toggle process on/off");
	        add(toggleButton);
	        
	        label = new JLabel(" " + building.getName() + ": " + process.getProcessName());
	        add(label);
			
			// Load green and red dots.
	        greenDot = ImageLoader.getIcon("GreenDot");
	        redDot = ImageLoader.getIcon("RedDot");
	        
			if (process.isProcessRunning()) toggleButton.setIcon(greenDot);
			else toggleButton.setIcon(redDot);
			
			setToolTipText(getToolTipString(building));
		}
		
		private String getToolTipString(Building building) {
			StringBuffer result = new StringBuffer("<html>");
			
			result.append("Resource Process: " + process.getProcessName() + "<br>");
			result.append("Building: " + building.getName() + "<br>");
            
            result.append("Power Required: " + decFormatter.format(process.getPowerRequired()) + " kW<br>");
			
			result.append("Process Inputs:<br>");
			Iterator<AmountResource> i = process.getInputResources().iterator();
			while (i.hasNext()) {
				AmountResource resource = i.next();
				double rate = process.getMaxInputResourceRate(resource) * 1000D;
				String rateString = decFormatter.format(rate);
				result.append("&nbsp;&nbsp;");
				if (process.isAmbientInputResource(resource)) result.append("* ");
				result.append(resource.getName() + ": " + rateString + " kg/sol<br>");
			}
			
			result.append("Process Outputs:<br>");
			Iterator<AmountResource> j = process.getOutputResources().iterator();
			while (j.hasNext()) {
				AmountResource resource = j.next();
				double rate = process.getMaxOutputResourceRate(resource) * 1000D;
				String rateString = decFormatter.format(rate);
				result.append("&nbsp;&nbsp;" + resource.getName() + ": " + rateString + " kg/sol<br>");
			}
			
			result.append("</html>");
			
			return result.toString();
		}
		
		/**
		 * Update the label.
		 */
		void update() {
			if (process.isProcessRunning()) toggleButton.setIcon(greenDot);
			else toggleButton.setIcon(redDot);
		}
		
		private ResourceProcess getProcess() {
			return process;
		}
	}
}