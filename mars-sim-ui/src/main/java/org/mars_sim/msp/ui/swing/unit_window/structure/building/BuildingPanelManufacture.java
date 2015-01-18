/**
 * Mars Simulation Project
 * ManufactureBuildingPanel.java
 * @version 3.07 2015-01-01
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_window.structure.building;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.commons.lang3.text.WordUtils;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.manufacture.ManufactureProcess;
import org.mars_sim.msp.core.manufacture.ManufactureProcessInfo;
import org.mars_sim.msp.core.manufacture.ManufactureUtil;
import org.mars_sim.msp.core.manufacture.SalvageProcess;
import org.mars_sim.msp.core.manufacture.SalvageProcessInfo;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.function.Manufacture;
import org.mars_sim.msp.ui.swing.JComboBoxMW;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.unit_window.structure.ManufacturePanel;
import org.mars_sim.msp.ui.swing.unit_window.structure.SalvagePanel;

/**
 * A building panel displaying the manufacture building function.
 */
public class BuildingPanelManufacture
extends BuildingFunctionPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(BuildingPanelManufacture.class.getName());

	/** The manufacture building. */
	private Manufacture workshop;
	/** Panel for displaying process panels. */
	private JPanel processListPane;
	private JScrollPane scrollPanel;
	/** List of manufacture processes in building. */
	private List<ManufactureProcess> processCache;
	/** List of salvage processes in building. */
	private List<SalvageProcess> salvageCache;
	/** Process selector. */
	private JComboBoxMW processComboBox;
	/** List of available processes. */
	private Vector<ManufactureProcessInfo> processComboBoxCache;
	/** List of available salvage processes. */
	private Vector<SalvageProcessInfo> salvageSelectionCache;
	/** Process selection button. */
	private JButton newProcessButton;

	/**
	 * Constructor.
	 * @param workshop the manufacturing building function.
	 * @param desktop the main desktop.
	 */
	public BuildingPanelManufacture(Manufacture workshop, MainDesktopPane desktop) {
		// Use BuildingFunctionPanel constructor.
		super(workshop.getBuilding(), desktop);

		// Initialize data model.
		this.workshop = workshop;
		
        // Set panel layout
        setLayout(new BorderLayout());
        
        // Prepare label panel
        //JPanel labelPane = new JPanel(new GridLayout(3, 1, 0, 0));
        JPanel labelPane = new JPanel();
        labelPane.setLayout(new GridLayout(3, 1, 0, 0));
        labelPane.setOpaque(false);
        labelPane.setBackground(new Color(0,0,0,128));
		
        add(labelPane, BorderLayout.NORTH);
        
        // Prepare manufacturing label
        JLabel manufactureLabel = new JLabel("Manufacturing", JLabel.CENTER);
        manufactureLabel.setFont(new Font("Serif", Font.BOLD, 16));
        manufactureLabel.setForeground(new Color(102, 51, 0)); // dark brown
        labelPane.add(manufactureLabel);
        
        // Prepare tech level label
        JLabel techLabel = new JLabel("Tech Level: " + workshop.getTechLevel(), JLabel.CENTER);
        labelPane.add(techLabel);
        
        // Prepare processCapacity label
        JLabel processCapacityLabel = new JLabel("Process Capacity: " + workshop.getConcurrentProcesses(), JLabel.CENTER);
        labelPane.add(processCapacityLabel);
        
        // Create scroll pane for manufacturing processes
        scrollPanel = new JScrollPane();
        scrollPanel.setPreferredSize(new Dimension(170, 90));
        add(scrollPanel, BorderLayout.CENTER);
        scrollPanel.setOpaque(false);
        scrollPanel.setBackground(new Color(0,0,0,128));
        scrollPanel.getViewport().setOpaque(false);
        scrollPanel.getViewport().setBackground(new Color(0,0,0,128));
        scrollPanel.setBorder( BorderFactory.createLineBorder(Color.LIGHT_GRAY) );
        
        // Create process list main panel
        JPanel processListMainPane = new JPanel(new BorderLayout(0, 0));
        scrollPanel.setViewportView(processListMainPane);
        processListMainPane.setOpaque(false);
        processListMainPane.setBackground(new Color(0,0,0,128));
        
        // Create process list panel
        processListPane = new JPanel();
        processListPane.setLayout(new BoxLayout(processListPane, BoxLayout.Y_AXIS));
        processListMainPane.add(processListPane, BorderLayout.NORTH);
        processListPane.setOpaque(false);
        processListPane.setBackground(new Color(0,0,0,128));
        
        // Create process panels
        processCache = new ArrayList<ManufactureProcess>(workshop.getProcesses());
        Iterator<ManufactureProcess> i = processCache.iterator();
        while (i.hasNext()) processListPane.add(new ManufacturePanel(i.next(), false, 23));
        
        // Create salvage panels.
        salvageCache = new ArrayList<SalvageProcess>(workshop.getSalvageProcesses());
        Iterator<SalvageProcess> j = salvageCache.iterator();
        while (j.hasNext()) processListPane.add(new SalvagePanel(j.next(), false, 23));
        
        // Create interaction panel.
        JPanel interactionPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        add(interactionPanel, BorderLayout.SOUTH);
        interactionPanel.setOpaque(false);
        interactionPanel.setBackground(new Color(0,0,0,128));
        
        // Create new manufacture process selection.
        processComboBoxCache = getAvailableProcesses();
        processComboBox = new JComboBoxMW(processComboBoxCache);
        processComboBox.setOpaque(false);
        processComboBox.setBackground(new Color(51,25,0,128));
        //processComboBox.setBackground(Color.LIGHT_GRAY);
        processComboBox.setForeground(Color.orange);
        processComboBox.setRenderer(new ManufactureSelectionListCellRenderer());
        processComboBox.setToolTipText("Select an Available Manufacturing Process");
        interactionPanel.add(processComboBox);
        
        // Add available salvage processes.
        salvageSelectionCache = getAvailableSalvageProcesses();
        Iterator<SalvageProcessInfo> k = salvageSelectionCache.iterator();
        while (k.hasNext()) processComboBox.addItem(k.next());
        
        // Create new process button.
        newProcessButton = new JButton("Create New Process");
        newProcessButton.setOpaque(false);
        newProcessButton.setBackground(new Color(51,25,0,128));
        newProcessButton.setForeground(Color.ORANGE);
        newProcessButton.setEnabled(processComboBox.getItemCount() > 0);
        newProcessButton.setToolTipText("Create a New Manufacturing Process or Salvage a Process");
        newProcessButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent event) {
        		try {
        		    Object selectedItem = processComboBox.getSelectedItem();
        		    if (selectedItem != null) {
        		        if (selectedItem instanceof ManufactureProcessInfo) {
        		            ManufactureProcessInfo selectedProcess = (ManufactureProcessInfo) selectedItem;
        		            if (ManufactureUtil.canProcessBeStarted(selectedProcess, getWorkshop())) {
                                getWorkshop().addProcess(new ManufactureProcess(selectedProcess, getWorkshop()));
                                update();
                            }
        		        }
        		        else if (selectedItem instanceof SalvageProcessInfo) {
        		            SalvageProcessInfo selectedSalvage = (SalvageProcessInfo) selectedItem;
        		            if (ManufactureUtil.canSalvageProcessBeStarted(selectedSalvage, getWorkshop())) {
        		                Unit salvagedUnit = ManufactureUtil.findUnitForSalvage(selectedSalvage, 
        		                        getWorkshop().getBuilding().getBuildingManager().getSettlement());
                                getWorkshop().addSalvageProcess(new SalvageProcess(selectedSalvage, 
                                        getWorkshop(), salvagedUnit));
                                update();
                            }
        		        }
        		    }
        		}
        		catch (Exception e) {
        			logger.log(Level.SEVERE, "new process button", e);
        		}
        	}
        });
        interactionPanel.add(newProcessButton);
	}
	
	
	@Override
	public void update() {
		
		// Update processes and salvage processes if necessary.
		List<ManufactureProcess> processes = workshop.getProcesses();
		List<SalvageProcess> salvages = workshop.getSalvageProcesses();
		if (!processCache.equals(processes) || !salvageCache.equals(salvages)) {
			
			// Add process panels for new processes.
			Iterator<ManufactureProcess> i = processes.iterator();
			while (i.hasNext()) {
				ManufactureProcess process = i.next();
				if (!processCache.contains(process)) 
					processListPane.add(new ManufacturePanel(process, false, 23));
			}
			
			// Add salvage panels for new salvage processes.
			Iterator<SalvageProcess> k = salvages.iterator();
			while (k.hasNext()) {
			    SalvageProcess salvage = k.next();
			    if (!salvageCache.contains(salvage))
			        processListPane.add(new SalvagePanel(salvage, false, 23));
			}
			
			// Remove process panels for old processes.
			Iterator<ManufactureProcess> j = processCache.iterator();
			while (j.hasNext()) {
				ManufactureProcess process = j.next();
				if (!processes.contains(process)) {
					ManufacturePanel panel = getManufacturePanel(process);
					if (panel != null) processListPane.remove(panel);
				}
			}
			
			// Remove salvage panels for old salvages.
			Iterator<SalvageProcess> l = salvageCache.iterator();
            while (l.hasNext()) {
                SalvageProcess salvage = l.next();
                if (!salvages.contains(salvage)) {
                    SalvagePanel panel = getSalvagePanel(salvage);
                    if (panel != null) processListPane.remove(panel);
                }
            }
			
			// Update processCache
			processCache.clear();
			processCache.addAll(processes);
			
			// Update salvageCache
			salvageCache.clear();
			salvageCache.addAll(salvages);
			
			scrollPanel.validate();
		}
		
		// Update all process panels.
		Iterator<ManufactureProcess> i = processes.iterator();
		while (i.hasNext()) {
			ManufacturePanel panel = getManufacturePanel(i.next());
			if (panel != null) panel.update();
		}
		
		// Update all salvage panels.
		Iterator<SalvageProcess> j = salvages.iterator();
		while (j.hasNext()) {
		    SalvagePanel panel = getSalvagePanel(j.next());
		    if (panel != null) panel.update();
		}
		
		// Update process selection list.
		Vector<ManufactureProcessInfo> newProcesses = getAvailableProcesses();
		Vector<SalvageProcessInfo> newSalvages = getAvailableSalvageProcesses();
		if (!newProcesses.equals(processComboBoxCache) || 
		        !newSalvages.equals(salvageSelectionCache)) {
			processComboBoxCache = newProcesses;
			salvageSelectionCache = newSalvages;
			Object currentSelection = processComboBox.getSelectedItem();
			processComboBox.removeAllItems();
			
			Iterator<ManufactureProcessInfo> k = processComboBoxCache.iterator();
			while (k.hasNext()) processComboBox.addItem(k.next());
			
			Iterator<SalvageProcessInfo> l = salvageSelectionCache.iterator();
            while (l.hasNext()) processComboBox.addItem(l.next());
			
			if (currentSelection != null) {
				if (processComboBoxCache.contains(currentSelection)) 
					processComboBox.setSelectedItem(currentSelection);
			}
		}
		
		// Update new process button.
		newProcessButton.setEnabled(processComboBox.getItemCount() > 0);
	}
	
	/**
	 * Gets the panel for a manufacture process.
	 * @param process the manufacture process.
	 * @return manufacture panel or null if none.
	 */
	private ManufacturePanel getManufacturePanel(ManufactureProcess process) {
		ManufacturePanel result = null;
		
		for (int x = 0; x < processListPane.getComponentCount(); x++) {
			Component component = processListPane.getComponent(x);
			if (component instanceof ManufacturePanel) {
				ManufacturePanel panel = (ManufacturePanel) component;
				if (panel.getManufactureProcess().equals(process)) result = panel;
			}
		}
		
		return result;
	}
	
	/**
	 * Gets the panel for a salvage process.
	 * @param process the salvage process.
	 * @return the salvage panel or null if none.
	 */
	private SalvagePanel getSalvagePanel(SalvageProcess process) {
	    SalvagePanel result = null;
        
        for (int x = 0; x < processListPane.getComponentCount(); x++) {
            Component component = processListPane.getComponent(x);
            if (component instanceof SalvagePanel) {
                SalvagePanel panel = (SalvagePanel) component;
                if (panel.getSalvageProcess().equals(process)) result = panel;
            }
        }
        
        return result;
	}
	
	/**
	 * Gets all manufacturing processes available at the workshop.
	 * @return vector of processes.
	 */
	private Vector<ManufactureProcessInfo> getAvailableProcesses() {
		Vector<ManufactureProcessInfo> result = new Vector<ManufactureProcessInfo>();
		
		if (workshop.getProcesses().size() < workshop.getConcurrentProcesses()) {
		    
		    // Determine highest materials science skill level at settlement.
		    Settlement settlement = workshop.getBuilding().getBuildingManager().getSettlement();
		    int highestSkillLevel = 0;
            Iterator<Person> i = settlement.getAllAssociatedPeople().iterator();
            while (i.hasNext()) {
                Person tempPerson = i.next();
                SkillManager skillManager = tempPerson.getMind().getSkillManager();
                int skill = skillManager.getSkillLevel(SkillType.MATERIALS_SCIENCE);
                if (skill > highestSkillLevel) {
                    highestSkillLevel = skill;
                }
            }
		    
			try {
				Iterator<ManufactureProcessInfo> j = Collections.unmodifiableList(
				        ManufactureUtil.getManufactureProcessesForTechSkillLevel(
				        workshop.getTechLevel(), highestSkillLevel)).iterator();
				while (j.hasNext()) {
					ManufactureProcessInfo process = j.next();
					if (ManufactureUtil.canProcessBeStarted(process, workshop)) 
						result.add(process);
				}
			}
			catch (Exception e) {}
		}
		
		return result;
	}
	
	/**
	 * Gets all salvage processes available at the workshop.
	 * @return vector of salvage processes.
	 */
	private Vector<SalvageProcessInfo> getAvailableSalvageProcesses() {
	    Vector<SalvageProcessInfo> result = new Vector<SalvageProcessInfo>();
	    
	    if (workshop.getProcesses().size() < workshop.getConcurrentProcesses()) {
            try {
                Iterator<SalvageProcessInfo> i = Collections.unmodifiableList(
                        ManufactureUtil.getSalvageProcessesForTechLevel(
                        workshop.getTechLevel())).iterator();
                while (i.hasNext()) {
                    SalvageProcessInfo process = i.next();
                    if (ManufactureUtil.canSalvageProcessBeStarted(process, workshop))
                        result.add(process);
                }
            }
            catch (Exception e) {}
	    }
	    
	    return result;
	}
	
	/**
	 * Gets the workshop for this panel.
	 * @return workshop
	 */
	private Manufacture getWorkshop() {
		return workshop;
	}
	
	/**
	 * Inner class for the manufacture selection list cell renderer.
	 */
	private static class ManufactureSelectionListCellRenderer extends DefaultListCellRenderer {
		
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
				boolean isSelected, boolean cellHasFocus) {
			Component result = super.getListCellRendererComponent(list, value, index, isSelected, 
					cellHasFocus);
			if (value instanceof ManufactureProcessInfo) {
			    ManufactureProcessInfo info = (ManufactureProcessInfo) value;
			    if (info != null) {
			    	// 2014-11-21 Capitalized processName
			        String processName = WordUtils.capitalize(info.getName());
			        if (processName.length() > 30) processName = processName.substring(0, 30) + "...";
			        ((JLabel) result).setText(processName);
			        ((JComponent) result).setToolTipText(ManufacturePanel.getToolTipString(info, null));
			    }
			}
			else if (value instanceof SalvageProcessInfo) {
			    SalvageProcessInfo info = (SalvageProcessInfo) value;
			    if (info != null) {
			    	// 2014-11-21 Capitalized processName
			        String processName = WordUtils.capitalize(info.toString());
			        if (processName.length() > 30) processName = processName.substring(0, 30) + "...";
                    ((JLabel) result).setText(processName);
                    ((JComponent) result).setToolTipText(SalvagePanel.getToolTipString(null, info, null));
			    }
			}
			return result;
		}
	}
}