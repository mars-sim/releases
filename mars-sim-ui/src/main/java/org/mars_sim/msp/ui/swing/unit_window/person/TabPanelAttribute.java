/**
 * Mars Simulation Project
 * TabPanelAttribute.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.person;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;


/** 
 * The TabPanelAttribute is a tab panel for the natural attributes of a person.
 */
public class TabPanelAttribute
extends TabPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	private AttributeTableModel attributeTableModel;

	/**
	 * Constructor.
	 * @param person {@link Person} the person.
	 * @param desktop {@link MainDesktopPane} the main desktop.
	 */
	public TabPanelAttribute(Person person, MainDesktopPane desktop) { 
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelAttribute.title"), //$NON-NLS-1$
			null,
			Msg.getString("TabPanelAttribute.tooltip"), //$NON-NLS-1$
			person,
			desktop
		);

		// Create attribute label panel.
		JPanel attributeLabelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContentPanel.add(attributeLabelPanel);

		// Create attribute label
		JLabel attributeLabel = new JLabel(Msg.getString("TabPanelAttribute.label"), JLabel.CENTER); //$NON-NLS-1$
		attributeLabelPanel.add(attributeLabel);

		// Create attribute scroll panel
		JScrollPane attributeScrollPanel = new JScrollPane();
		attributeScrollPanel.setBorder(new MarsPanelBorder());
		centerContentPanel.add(attributeScrollPanel);

		// Create attribute table model
		attributeTableModel = new AttributeTableModel(person);

		// Create attribute table
		JTable attributeTable = new JTable(attributeTableModel);
		attributeTable.setPreferredScrollableViewportSize(new Dimension(225, 100));
		attributeTable.getColumnModel().getColumn(0).setPreferredWidth(100);
		attributeTable.getColumnModel().getColumn(1).setPreferredWidth(70);
		attributeTable.setCellSelectionEnabled(false);
		// attributeTable.setDefaultRenderer(Integer.class, new NumberCellRenderer());
		attributeScrollPanel.setViewportView(attributeTable);
	}

	/**
	 * Updates the info on this panel.
	 */
	@Override
	public void update() {}

	/** 
	 * Internal class used as model for the attribute table.
	 */
	private static class AttributeTableModel
	extends AbstractTableModel {

		private List<Map<String,NaturalAttribute>> attributes;

		/** default serial id. */
		private static final long serialVersionUID = 1L;
		private NaturalAttributeManager manager;

		/**
		 * hidden constructor.
		 * @param person {@link Person}
		 */
		private AttributeTableModel(Person person) {
			manager = person.getNaturalAttributeManager();
			attributes = new ArrayList<Map<String,NaturalAttribute>>();
			for (NaturalAttribute value : NaturalAttribute.values()) {
				Map<String,NaturalAttribute> map = new TreeMap<String,NaturalAttribute>();
				map.put(value.getName(),value);
				attributes.add(map);
			}
			Collections.sort(
				attributes,
				new Comparator<Map<String,NaturalAttribute>>() {
					@Override
					public int compare(Map<String,NaturalAttribute> o1,Map<String,NaturalAttribute> o2) {
						return o1.keySet().iterator().next().compareTo(o2.keySet().iterator().next());
					}
				}
			);
		}

		@Override
		public int getRowCount() {
			return manager.getAttributeNum();
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			Class<?> dataType = super.getColumnClass(columnIndex);
			if (columnIndex == 0) dataType = String.class;
			if (columnIndex == 1) dataType = String.class;
			return dataType;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) return Msg.getString("TabPanelAttribute.column.attribute"); //$NON-NLS-1$
			else if (columnIndex == 1) return Msg.getString("TabPanelAttribute.column.level"); //$NON-NLS-1$
			else return null;
		}

		@Override
		public Object getValueAt(int row, int column) {
			if (column == 0) return attributes.get(row).keySet().iterator().next();
			else if (column == 1) return getLevelString(manager.getAttribute(attributes.get(row).values().iterator().next()));
			else return null;
		}
		/*
		public void update() {}
		 */
		public String getLevelString(int level) {
			String result = null;
			if (level < 5) result = Msg.getString("TabPanelAttribute.level.0"); //$NON-NLS-1$
			else if (level < 20) result = Msg.getString("TabPanelAttribute.level.1"); //$NON-NLS-1$
			else if (level < 35) result = Msg.getString("TabPanelAttribute.level.2"); //$NON-NLS-1$
			else if (level < 45) result = Msg.getString("TabPanelAttribute.level.3"); //$NON-NLS-1$
			else if (level < 55) result = Msg.getString("TabPanelAttribute.level.4"); //$NON-NLS-1$
			else if (level < 65) result = Msg.getString("TabPanelAttribute.level.5"); //$NON-NLS-1$
			else if (level < 80) result = Msg.getString("TabPanelAttribute.level.6"); //$NON-NLS-1$
			else if (level < 95) result = Msg.getString("TabPanelAttribute.level.7"); //$NON-NLS-1$
			else result = Msg.getString("TabPanelAttribute.level.8"); //$NON-NLS-1$
			return result;
		}
	}
}
