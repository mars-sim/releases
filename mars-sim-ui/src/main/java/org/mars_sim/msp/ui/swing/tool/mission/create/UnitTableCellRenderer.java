/**
 * Mars Simulation Project
 * UnitTableCellRenderer.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.tool.mission.create;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Cell renderer for unit tables that marks failure cells as red.
 */
class UnitTableCellRenderer extends DefaultTableCellRenderer {

	// Private data members.
	private UnitTableModel model;
	
	/**
	 * Constructor
	 * @param model the unit table model.
	 */
	UnitTableCellRenderer(UnitTableModel model) {
		this.model = model;
	}
	
	/**
	 * Returns the default table cell renderer.
	 * @param table the table the cell is in.
	 * @param value the value in the cell.
	 * @return the rendering component.
	 */
	public Component getTableCellRendererComponent(JTable table, Object value, 
			boolean isSelected, boolean hasFocus, int row, int column) {
		
		Component result = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		
		// If failure cell, mark background red.
		if (model.isFailureCell(row, column)) setBackground(Color.RED);
		else if (!isSelected) setBackground(Color.WHITE);
		
		return result;
	}
}