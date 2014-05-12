/**
 * Mars Simulation Project
 * PopulationTabPanel.java
 * @version 3.06 2014-04-30
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.tool.monitor.PersonTableModel;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;

/** 
 * This is a tab panel for population information.
 */
public class TabPanelPopulation
extends TabPanel
implements MouseListener, ActionListener {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private JLabel populationNumLabel;
	private JLabel populationCapLabel;
	private PopulationListModel populationListModel;
	private JList<Person> populationList;
	private JScrollPane populationScrollPanel;
	private int populationNumCache;
	private int populationCapacityCache;

	/**
	 * Constructor.
	 * @param unit the unit to display.
	 * @param desktop the main desktop.
	 */
	public TabPanelPopulation(Unit unit, MainDesktopPane desktop) { 
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelPopulation.title"), //$NON-NLS-1$
			null,
			Msg.getString("TabPanelPopulation.tooltip"), //$NON-NLS-1$
			unit, desktop
		);

		Settlement settlement = (Settlement) unit;

		// Create population count panel
		JPanel populationCountPanel = new JPanel(new GridLayout(2, 1, 0, 0));
		populationCountPanel.setBorder(new MarsPanelBorder());
		topContentPanel.add(populationCountPanel);

		// Create population num label
		populationNumCache = settlement.getCurrentPopulationNum();
		populationNumLabel = new JLabel(Msg.getString("TabPanelPopulation.population", 
		        populationNumCache), JLabel.CENTER); //$NON-NLS-1$
		populationCountPanel.add(populationNumLabel);

		// Create population capacity label
		populationCapacityCache = settlement.getPopulationCapacity();
		populationCapLabel = new JLabel(Msg.getString("TabPanelPopulation.populationCapacity", 
		        populationCapacityCache), JLabel.CENTER); //$NON-NLS-1$
		populationCountPanel.add(populationCapLabel);

		// Create population display panel
		JPanel populationDisplayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		populationDisplayPanel.setBorder(new MarsPanelBorder());
		topContentPanel.add(populationDisplayPanel);

		// Create scroll panel for population list.
		populationScrollPanel = new JScrollPane();
		populationScrollPanel.setPreferredSize(new Dimension(175, 250));
		populationDisplayPanel.add(populationScrollPanel);

		// Create population list model
		populationListModel = new PopulationListModel(settlement);

		// Create population list
		populationList = new JList<Person>(populationListModel);
		populationList.addMouseListener(this);
		populationScrollPanel.setViewportView(populationList);

		// Create population monitor button
		JButton monitorButton = new JButton(ImageLoader.getIcon(Msg.getString("img.monitor"))); //$NON-NLS-1$
		monitorButton.setMargin(new Insets(1, 1, 1, 1));
		monitorButton.addActionListener(this);
		monitorButton.setToolTipText(Msg.getString("TabPanelPopulation.tooltip.monitor")); //$NON-NLS-1$
		populationDisplayPanel.add(monitorButton);
	}

	/**
	 * Updates the info on this panel.
	 */
	public void update() {
		Settlement settlement = (Settlement) unit;

		// Update population num
		if (populationNumCache != settlement.getCurrentPopulationNum()) {
			populationNumCache = settlement.getCurrentPopulationNum();
			populationNumLabel.setText(Msg.getString("TabPanelPopulation.population", 
			        populationNumCache)); //$NON-NLS-1$
		}

		// Update population capacity
		if (populationCapacityCache != settlement.getPopulationCapacity()) {
			populationCapacityCache = settlement.getPopulationCapacity();
			populationCapLabel.setText(Msg.getString("TabPanelPopulation.populationCapacity", 
			        populationCapacityCache)); //$NON-NLS-1$
		}

		// Update population list
		populationListModel.update();
		populationScrollPanel.validate();
	}
	
	/**
	 * List model for settlement population.
	 */
	private class PopulationListModel extends AbstractListModel<Person> {

	    /** default serial id. */
	    private static final long serialVersionUID = 1L;
	    
	    private Settlement settlement;
	    private List<Person> populationList;
	    
	    private PopulationListModel(Settlement settlement) {
	        this.settlement = settlement;
	        
	        populationList = new ArrayList<Person>(settlement.getInhabitants());
	        Collections.sort(populationList);
	    }
	    
        @Override
        public Person getElementAt(int index) {
            
            Person result = null;
            
            if ((index >= 0) && (index < populationList.size())) {
                result = populationList.get(index);
            }
            
            return result;
        }

        @Override
        public int getSize() {
            return populationList.size();
        }
        
        /**
         * Update the population list model.
         */
        public void update() {
            
            if (!populationList.containsAll(settlement.getInhabitants()) || 
                    !settlement.getInhabitants().containsAll(populationList)) {
                
                List<Person> oldPopulationList = populationList;
                
                List<Person> tempPopulationList = new ArrayList<Person>(settlement.getInhabitants());
                Collections.sort(tempPopulationList);
                
                populationList = tempPopulationList;
                fireContentsChanged(this, 0, getSize());
                
                oldPopulationList.clear();
            }
        }
	}

	/** 
	 * Action event occurs.
	 * @param event the action event
	 */
	public void actionPerformed(ActionEvent event) {
		// If the population monitor button was pressed, create tab in monitor tool.
		desktop.addModel(new PersonTableModel((Settlement) unit, false));
	}

	/** 
	 * Mouse clicked event occurs.
	 * @param event the mouse event
	 */
	public void mouseClicked(MouseEvent event) {

		// If double-click, open person window.
		if (event.getClickCount() >= 2) {
			Person person = (Person) populationList.getSelectedValue();
			if (person != null) {
				desktop.openUnitWindow(person, false);
			}
		}
	}

	public void mousePressed(MouseEvent event) {}
	public void mouseReleased(MouseEvent event) {}
	public void mouseEntered(MouseEvent event) {}
	public void mouseExited(MouseEvent event) {}
}
