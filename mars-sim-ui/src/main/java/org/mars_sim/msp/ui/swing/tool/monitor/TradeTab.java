/**
 * Mars Simulation Project
 * TradeTab.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.monitor;

import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.UnitManagerEvent;
import org.mars_sim.msp.core.UnitManagerEventType;
import org.mars_sim.msp.core.UnitManagerListener;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.ui.swing.NumberCellRenderer;

/**
 * This class represents a table of trade good values at settlements displayed 
 * within the Monitor Window. 
 */
public class TradeTab
extends TableTab
implements UnitManagerListener {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/**
	 * constructor.
	 * @param window {@link MonitorWindow} the containing window.
	 */
	public TradeTab(final MonitorWindow window) {
		// Use TableTab constructor
		super(window, new TradeTableModel(), true, false);

		// Override default cell renderer for format double values.
		table.setDefaultRenderer(Double.class, new NumberCellRenderer(4));

		// Add as unit manager listener.
		Simulation.instance().getUnitManager().addUnitManagerListener(this);
	}

	@Override
	public void unitManagerUpdate(UnitManagerEvent event) {

		if (event.getUnit() instanceof Settlement) {

			Settlement settlement = (Settlement) event.getUnit();

			if (UnitManagerEventType.ADD_UNIT == event.getEventType()) {
				// If settlement is new, add to settlement columns.
				TableColumn column = new TableColumn(table.getColumnCount());
				column.setHeaderValue(settlement.getName());
				SwingUtilities.invokeLater(new TradeColumnModifier(column, true));
			}
			else if (UnitManagerEventType.REMOVE_UNIT == event.getEventType()) {
				// If settlement is gone, remove from settlement columns.
				TableColumn column = table.getColumn(settlement.getName());
				if (column != null) {
					SwingUtilities.invokeLater(new TradeColumnModifier(column, false));
				}
			}
		}

	}

	/**
	 * An inner class for adding or removing trade table columns.
	 */
	private class TradeColumnModifier
	implements Runnable {

		// Data members.
		private TableColumn column;
		private boolean addColumn;

		/**
		 * Constructor
		 * @param column the column to add or remove.
		 * @param addColumn true for adding column or false for removing column.
		 */
		private TradeColumnModifier(TableColumn column, boolean addColumn) {
			this.column = column;
			this.addColumn = addColumn;
		}

		@Override
		public void run() {
			if (addColumn) {
				table.getColumnModel().addColumn(column);
			}
			else {
				table.getColumnModel().removeColumn(column);
			}
		}
	}
}