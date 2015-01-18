/**
 * Mars Simulation Project
 * TabPanelBuildings.java
 * @version 3.07 2014-12-03
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.ui.swing.JComboBoxMW;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;
import org.mars_sim.msp.ui.swing.unit_window.structure.building.BuildingPanel;

/**
 * The BuildingsTabPanel is a tab panel containing building panels.
 */
public class TabPanelBuildings
extends TabPanel
implements ActionListener {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private DefaultComboBoxModel<Building> comboBoxModel;
	private JComboBoxMW<Building> comboBox;
	private List<Building> buildingsCache;
	private JPanel buildingDisplayPanel;
	private CardLayout buildingLayout;
	private List<BuildingPanel> buildingPanels;
	private int count;

	private Building building;
	
	private List<JPanel> panelList = new ArrayList<JPanel>();

	/**
	 * Constructor
	 * @param unit the unit to display.
	 * @param desktop the main desktop.
	 */
	// 2014-11-27 Moved rename building capability to BuildingPanel.java
	public TabPanelBuildings(Unit unit, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelBuildings.title"), //$NON-NLS-1$
			null,
			Msg.getString("TabPanelBuildings.tooltip"), //$NON-NLS-1$
			unit, desktop
		);
        this.setOpaque(false);
        this.setBackground(new Color(0,0,0,128));
        
		Settlement settlement = (Settlement) unit;
		List<Building> buildings = settlement.getBuildingManager().getBuildings();
		Collections.sort(buildings);
		
		// 2014-11-07 Set building to the first element on the list 
		// 2014-11-22 Added if-clause for opening the building panel via the right click popup menu
		if (building == null){	
			building = buildings.get(0);
			setCurrentBuilding(building);
		}
		
		// Create building select panel.
		// 2014-11-21 Added buildingInfoPanel & buildingTitleLabel	
		JPanel buildingInfoPanel = new JPanel(new GridLayout(2,1,0,0));
		//buildingInfoPanel.setBorder(new MarsPanelBorder());
		topContentPanel.add(buildingInfoPanel);
		panelList.add(buildingInfoPanel);
		
        JLabel titleLabel = new JLabel("Buildings", JLabel.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 16));
        titleLabel.setForeground(new Color(102, 51, 0)); // dark brown
        buildingInfoPanel.add(titleLabel);
        titleLabel.setOpaque(false);
        titleLabel.setBackground(new Color(0,0,0,128));
        
		JPanel buildingSelectPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buildingInfoPanel.add(buildingSelectPanel);
		panelList.add(buildingSelectPanel);
		
        //JLabel buildingLabel = new JLabel("Selected Building: ", JLabel.CENTER);
        //buildingLabel.setFont(new Font("Serif", Font.PLAIN, 12));
        //buildingLabel.setForeground(new Color(102, 51, 0)); // dark brown
        //buildingSelectPanel.add(buildingLabel);

		// Create building combo box model.
		comboBoxModel = new DefaultComboBoxModel<Building>();

		buildingsCache = new ArrayList<Building>(buildings);
			//System.out.println("TabPanelBuildings.java : constructor : buildingsCache is "+ buildingsCache);
		Iterator<Building> i = buildingsCache.iterator();		
		while (i.hasNext()) {
			Building b = i.next();
			// 2014-10-29: <<NOT USED>> Modified to load nickName instead of buildingType
			// b.setType(b.getNickName());
	    	comboBoxModel.addElement(b);
		}
		// Create building list.
		comboBox = new JComboBoxMW<Building>(comboBoxModel);
		comboBox.setRenderer(new PromptComboBoxRenderer());
		comboBox.setOpaque(false);
		comboBox.setBackground(new Color(0,0,0,128));
		comboBox.setBackground(new Color(255,229,204));
		//comboBox.setForeground(Color.orange);
		comboBox.addActionListener(this);
		comboBox.setMaximumRowCount(10);
		comboBox.setBorder(null);
		
		buildingSelectPanel.add(comboBox);
		
		// Create building display panel.
		buildingDisplayPanel = new JPanel();
		buildingLayout = new CardLayout();
		buildingDisplayPanel.setLayout(buildingLayout);
		buildingDisplayPanel.setBorder(new MarsPanelBorder());
		centerContentPanel.add(buildingDisplayPanel);
		panelList.add(buildingDisplayPanel);
		
		// Create building panels
		buildingPanels = new ArrayList<BuildingPanel>();
		count = 0;
		Iterator<Building> iter = buildings.iterator();
		while (iter.hasNext()) {
			BuildingPanel panel = new BuildingPanel(String.valueOf(count), iter.next(), desktop);
			buildingPanels.add(panel);
			buildingDisplayPanel.add(panel, panel.getPanelName());
			count++;
		}
	
		setPanelTranslucent();
	}
	
	class PromptComboBoxRenderer extends BasicComboBoxRenderer {	
		
		private static final long serialVersionUID = 1L;
		private String prompt;		
		//public boolean isOptimizedDrawingEnabled();
		//private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
		public PromptComboBoxRenderer(){	
			//defaultRenderer.setHorizontalAlignment(DefaultListCellRenderer.CENTER);
		    //settlementListBox.setRenderer(defaultRenderer);		
		    //setOpaque(false);
		    setHorizontalAlignment(CENTER);
		    setVerticalAlignment(CENTER);
		}
		
		public PromptComboBoxRenderer(String prompt){
				this.prompt = prompt;
			}
			
			@Override
		    public Component getListCellRendererComponent(JList list, Object value,
		            int index, boolean isSelected, boolean cellHasFocus) {
		        JComponent result = (JComponent)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		        //Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		        //component.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);		        
				if (value == null) {
					setText( prompt );
					//this.setForeground(Color.orange);
			        //this.setBackground(new Color(184,134,11));
					return this;
				}
				
				if (isSelected) {
					result.setForeground(new Color(184,134,11));
			        result.setBackground(Color.orange); 
		
		          // unselected, and not the DnD drop location
		        } else {
		        	  result.setForeground(new Color(184,134,11));
		        	  result.setBackground(new Color(255,229,204)); //pale yellow (255,229,204)
				      //Color(184,134,11)) brown
		        }
				
		        //result.setOpaque(false);

		        return result;
		    }
	}

	
	public void setPanelStyle(JPanel p) {
		//if (isTranslucent) {
			p.setOpaque(false);
			p.setBackground(new Color(0,0,0,128)); 
		//}
	}
	
	public void setPanelTranslucent() {	
		//if (isTranslucent) {
		    Iterator<JPanel> i = panelList.iterator();
			 	while (i.hasNext()) {
			 		JPanel pp = i.next();
			 		setPanelStyle(pp);
			 	}
		//}
	}
	
	/** Set the new name of a Building
	 * @return none
	 */
	// 2014-10-29 Added setCurrentBuilding()
	public void setCurrentBuilding(Building building) {
		this.building = building;
	}
		
	/**
	 * Updates the info on this panel.
	 */
	@Override
	public void update() {
		Settlement settlement = (Settlement) unit;
		List<Building> buildings = settlement.getBuildingManager().getBuildings();

		// Update buildings if necessary.
		if (!buildingsCache.equals(buildings)) {

			// Add building panels for new buildings.
			Iterator<Building> iter1 = buildings.iterator();
			while (iter1.hasNext()) {
				Building building = iter1.next();
				if (!buildingsCache.contains(building)) {
					BuildingPanel panel = new BuildingPanel(String.valueOf(count), building, desktop);
					buildingPanels.add(panel);
					panel.setOpaque(false);//setBackground(new Color(139,69,19));
					panel.setForeground(Color.green);
					panel.setBackground(new Color(0,0,0,15));
					buildingDisplayPanel.add(panel, panel.getPanelName());
					// TODO: Modify to load building's nickName instead of buildingType
					comboBoxModel.addElement(building);
					count++;
				}
			}

			// Remove building panels for destroyed buildings.
			Iterator<Building> iter2 = buildingsCache.iterator();
			while (iter2.hasNext()) {
				Building building = iter2.next();
				if (!buildings.contains(building)) {
					BuildingPanel panel = getBuildingPanel(building);
					if (panel != null) {
						buildingPanels.remove(panel);
						buildingDisplayPanel.remove(panel);
						comboBoxModel.removeElement(building);
					}
				}
			}

			// Update buildings cache.
			buildingsCache = buildings;
		}

		// Have each building panel update.
		Iterator<BuildingPanel> i = buildingPanels.iterator();
		while (i.hasNext()) i.next().update();
	}

	/** 
	 * Action event occurs.
	 * @param event the action event
	 */
	public void actionPerformed(ActionEvent event) {
		Building building = (Building) comboBox.getSelectedItem();
		BuildingPanel panel = getBuildingPanel(building);
		
		if (panel != null) buildingLayout.show(buildingDisplayPanel, panel.getPanelName());
		else System.err.println(Msg.getString("TabPanelBuildings.err.cantFindPanelForBuilding", building.getNickName())); //$NON-NLS-1$
	}

	/**
	 * Gets the building panel for a given building.
	 * @param building the given building
	 * @return the building panel or null if none.
	 */
	private BuildingPanel getBuildingPanel(Building building) {
		BuildingPanel result = null;
		Iterator<BuildingPanel> i = buildingPanels.iterator();
		while (i.hasNext()) {
			BuildingPanel panel = i.next();
			if (panel.getBuilding() == building) {				
				// 2014-10-29 Set as current building object
				//setCurrentBuilding(building);
				result = panel;
			}
		}

		return result;
	}
}