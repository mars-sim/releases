/**
 * Mars Simulation Project
 * AssociatedPeopleTabPanel.java
 * @version 3.07 2014-12-03
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
 * The AssociatedPeopleTabPanel is a tab panel for information on all people 
 * associated with a settlement.
 */
public class TabPanelAssociatedPeople
extends TabPanel
implements MouseListener, ActionListener {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	private AssociatedPopulationListModel populationListModel;
	private JList<Person> populationList;
	private JScrollPane populationScrollPanel;

	/**
	 * Constructor.
	 * @param unit the unit to display.
	 * @param desktop the main desktop.
	 */
	public TabPanelAssociatedPeople(Unit unit, MainDesktopPane desktop) { 
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelAssociatedPeople.title"), //$NON-NLS-1$
			null,
			Msg.getString("TabPanelAssociatedPeople.tooltip"), //$NON-NLS-1$
			unit, desktop
		);

		Settlement settlement = (Settlement) unit;

		// Create label
		JPanel associatedLabelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContentPanel.add(associatedLabelPanel);

		// Create associated people label
		JLabel label = new JLabel(Msg.getString("TabPanelAssociatedPeople.label"), JLabel.CENTER); //$NON-NLS-1$
		label.setFont(new Font("Serif", Font.BOLD, 16));
		label.setForeground(new Color(102, 51, 0)); // dark brown
		associatedLabelPanel.add(label);

		// Create population display panel
		JPanel populationDisplayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		populationDisplayPanel.setBorder(new MarsPanelBorder());
		topContentPanel.add(populationDisplayPanel);

		// Create scroll panel for population list.
		populationScrollPanel = new JScrollPane();
		populationScrollPanel.setPreferredSize(new Dimension(175, 250));
		populationDisplayPanel.add(populationScrollPanel);

		// Create population list model
		populationListModel = new AssociatedPopulationListModel(settlement);

		// Create population list
		populationList = new JList<Person>(populationListModel);
		populationList.addMouseListener(this);
		populationScrollPanel.setViewportView(populationList);

		// Create population monitor button
		JButton monitorButton = new JButton(ImageLoader.getIcon(Msg.getString("img.monitor"))); //$NON-NLS-1$
		monitorButton.setMargin(new Insets(1, 1, 1, 1));
		monitorButton.addActionListener(this);
		monitorButton.setToolTipText(Msg.getString("TabPanelAssociatedPeople.tooltip.monitor")); //$NON-NLS-1$
		populationDisplayPanel.add(monitorButton);		
	}

	/**
	 * Updates the info on this panel.
	 */
	public void update() {

		// Update population list
		populationListModel.update();
		populationScrollPanel.validate();
	}
	
	/**
     * List model for settlement population.
     */
    private class AssociatedPopulationListModel extends AbstractListModel<Person> {

        /** default serial id. */
        private static final long serialVersionUID = 1L;
        
        private Settlement settlement;
        private List<Person> populationList;
        
        private AssociatedPopulationListModel(Settlement settlement) {
            this.settlement = settlement;
            
            populationList = new ArrayList<Person>(settlement.getAllAssociatedPeople());
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
            
            if (!populationList.containsAll(settlement.getAllAssociatedPeople()) || 
                    !settlement.getAllAssociatedPeople().containsAll(populationList)) {
                
                List<Person> oldPopulationList = populationList;
                
                List<Person> tempPopulationList = new ArrayList<Person>(settlement.getAllAssociatedPeople());
                Collections.sort(tempPopulationList);
                
                populationList = tempPopulationList;
                fireContentsChanged(this, 0, getSize());
                
                oldPopulationList.clear();
            }
        }
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

	public void mousePressed(MouseEvent arg0) {}
	public void mouseReleased(MouseEvent arg0) {}
	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}

	/** 
	 * Action event occurs.
	 * @param event the action event
	 */
	public void actionPerformed(ActionEvent event) {
		// If the population monitor button was pressed, create tab in monitor tool.
		desktop.addModel(new PersonTableModel((Settlement) unit, true));
	}
}