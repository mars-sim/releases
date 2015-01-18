/**
 * Mars Simulation Project
 * ResourceProcessTabTabPanel.java
 * @version 3.07 2014-12-03
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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

import org.apache.commons.lang3.text.WordUtils;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.ResourceProcess;
import org.mars_sim.msp.core.structure.building.function.ResourceProcessing;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;

/**
 * A tab panel for displaying all of the resource processes in a settlement.
 */
public class TabPanelResourceProcesses
extends TabPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	private List<Building> processingBuildings;
	private JScrollPane processesScrollPane;
	private JPanel processListPanel;
	private JCheckBox overrideCheckbox;

	/**
	 * Constructor.
	 * @param unit the unit to display.
	 * @param desktop the main desktop.
	 */
	public TabPanelResourceProcesses(Unit unit, MainDesktopPane desktop) { 

		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelResourceProcesses.title"), //$NON-NLS-1$
			null,
			Msg.getString("TabPanelResourceProcesses.tooltip"), //$NON-NLS-1$
			unit, desktop
		);

		Settlement settlement = (Settlement) unit;
		processingBuildings = settlement.getBuildingManager().getBuildings(BuildingFunction.RESOURCE_PROCESSING);

		// Prepare resource processes label panel.
		JPanel resourceProcessesLabelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContentPanel.add(resourceProcessesLabelPanel);

		// Prepare esource processes label.
		JLabel resourceProcessesLabel = new JLabel(Msg.getString("TabPanelResourceProcesses.label"), JLabel.CENTER); //$NON-NLS-1$
		resourceProcessesLabel.setFont(new Font("Serif", Font.BOLD, 16));
		resourceProcessesLabel.setForeground(new Color(102, 51, 0)); // dark brown
		resourceProcessesLabelPanel.add(resourceProcessesLabel);

		// Create scroll panel for the outer table panel.
		processesScrollPane = new JScrollPane();
		processesScrollPane.setPreferredSize(new Dimension(220, 280));
		// increase vertical mousewheel scrolling speed for this one
		processesScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		centerContentPanel.add(processesScrollPane,BorderLayout.CENTER);         

		// Prepare process list panel.
		processListPanel = new JPanel(new GridLayout(0, 1, 5, 2));
		processListPanel.setBorder(new MarsPanelBorder());
		processesScrollPane.setViewportView(processListPanel);
		populateProcessList();

		// Create override check box panel.
		JPanel overrideCheckboxPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContentPanel.add(overrideCheckboxPane,BorderLayout.SOUTH);

		// Create override check box.
		overrideCheckbox = new JCheckBox(Msg.getString("TabPanelResourceProcesses.checkbox.overrideResourceProcessToggling")); //$NON-NLS-1$
		overrideCheckbox.setToolTipText(Msg.getString("TabPanelResourceProcesses.tooltip.overrideResourceProcessToggling")); //$NON-NLS-1$
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

		//    	try {
		// Add a label for each process in each processing building.
		Iterator<Building> i = processingBuildings.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			ResourceProcessing processing = (ResourceProcessing) building.getFunction(BuildingFunction.RESOURCE_PROCESSING);
			Iterator<ResourceProcess> j = processing.getProcesses().iterator();
			while (j.hasNext()) {
				ResourceProcess process = j.next();
				processListPanel.add(new ResourceProcessPanel(process, building));
			}
		}
		//    	}
		//    	catch (BuildingException e) {
		//    		e.printStackTrace(System.err);
		//    	}
	}

	@Override
	public void update() {
		// Check if building list has changed.
		Settlement settlement = (Settlement) unit;
		List<Building> tempBuildings = settlement.getBuildingManager().getBuildings(BuildingFunction.RESOURCE_PROCESSING);
		if (!tempBuildings.equals(processingBuildings)) {
			// Populate process list.
			processingBuildings = tempBuildings;
			populateProcessList();
			processesScrollPane.validate();
		}
		else {
			// Update process list.
			Component[] components = processListPanel.getComponents();
			for (Component component : components) {
				ResourceProcessPanel panel = (ResourceProcessPanel) component;
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
	private static class ResourceProcessPanel
	extends JPanel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		// Data members.
		private ResourceProcess process;
		private JLabel label;
		private JButton toggleButton;
		private ImageIcon dotGreen;
		private ImageIcon dotRed;
		private DecimalFormat decFormatter = new DecimalFormat(Msg.getString("TabPanelResourceProcesses.decimalFormat")); //$NON-NLS-1$

		/**
		 * Constructor.
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
			toggleButton.setToolTipText(Msg.getString("TabPanelResourceProcesses.tooltip.toggleButton")); //$NON-NLS-1$
			add(toggleButton);
			// 2014-11-17 Changed building.getName() to building.getNickName()
			label = new JLabel(Msg.getString("TabPanelResourceProcesses.processLabel", building.getNickName(), process.getProcessName())); //$NON-NLS-1$
			add(label);

			// Load green and red dots.
			dotGreen = ImageLoader.getIcon(Msg.getString("img.dotGreen")); //$NON-NLS-1$
			dotRed = ImageLoader.getIcon(Msg.getString("img.dotRed")); //$NON-NLS-1$

			if (process.isProcessRunning()) toggleButton.setIcon(dotGreen);
			else toggleButton.setIcon(dotRed);

			setToolTipText(getToolTipString(building));
		}

		// TODO internationalize the resource processes' dynamic tooltip
		// 2014-11-20 Aligned text to improved tooltip readability (for English Locale only)
		private String getToolTipString(Building building) {
			StringBuilder result = new StringBuilder("<html>");
			result.append("&emsp;&nbsp;Process:&emsp;").append(process.getProcessName()).append("<br>");
			// 2014-11-17 Changed building.getName() to building.getNickName()
			result.append("&emsp;&nbsp;Building:&emsp;").append(building.getNickName()).append("<br>");
			result.append("Power Req:&emsp;").append(decFormatter.format(process.getPowerRequired())).append(" kW<br>");
			result.append("&emsp;&emsp;&nbsp;Inputs:&emsp;");
			Iterator<AmountResource> i = process.getInputResources().iterator();
			// 2014-11-20 Added ambientStr and ii and ii++
			String ambientStr = "";
			int ii = 0;
			while (i.hasNext()) {
				if (ii!=0)	result.append("&nbsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;");
				AmountResource resource = i.next();
				double rate = process.getMaxInputResourceRate(resource) * 1000D;
				String rateString = decFormatter.format(rate);
				//result.append("&nbsp;&nbsp;&emsp;");
				if (process.isAmbientInputResource(resource)) ambientStr = "*";
				// 2014-11-20 Capitalized resource.getName()
				result.append(WordUtils.capitalize(resource.getName())).append(ambientStr).append(" @ ").append(rateString).append(" kg/sol<br>");
				ii++;
			}
			result.append("&emsp;&nbsp;&nbsp;Outputs:&emsp;");
			Iterator<AmountResource> j = process.getOutputResources().iterator();
			// 2014-11-20 Added jj and jj++
			int jj = 0;
			while (j.hasNext()) {
				if (jj!=0) result.append("&nbsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;");
				AmountResource resource = j.next();
				double rate = process.getMaxOutputResourceRate(resource) * 1000D;
				String rateString = decFormatter.format(rate);
				// 2014-11-20 Capitalized resource.getName()
				result.append(WordUtils.capitalize(resource.getName())).append(" @ ").append(rateString).append(" kg/sol<br>");
				jj++;
			}
			// 2014-11-20 Moved * from front to back of the text 
			// Added a note to denote an ambient input resource
			if (ambientStr == "*")
				result.append("&emsp;<i>Note:  * denotes an ambient resource</i>");
			result.append("</html>");
			return result.toString();
		}

		/**
		 * Update the label.
		 */
		void update() {
			if (process.isProcessRunning()) toggleButton.setIcon(dotGreen);
			else toggleButton.setIcon(dotRed);
		}

		private ResourceProcess getProcess() {
			return process;
		}
	}
}