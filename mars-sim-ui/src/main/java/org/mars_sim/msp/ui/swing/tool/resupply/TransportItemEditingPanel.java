/**
 * Mars Simulation Project
 * TransportEditingPanel.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.resupply;

import javax.swing.JPanel;

import org.mars_sim.msp.core.interplanetary.transport.Transportable;

/**
 * An abstract panel for editing a transport item.
 */
public abstract class TransportItemEditingPanel
extends JPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	private Transportable transportItem;

	/**
	 * Constructor.
	 * @param transportItem the transport item to edit.
	 */
	public TransportItemEditingPanel(Transportable transportItem) {
		// Use JPanel constructor
		super();

		// Initialize data members.
		this.transportItem = transportItem;
	}

	/**
	 * Gets the transport item.
	 * @return transport item.
	 */
	public Transportable getTransportItem() {
		return transportItem;
	}

	/**
	 * Modifies the transport item with the editing panel information.
	 */
	public abstract boolean modifyTransportItem();

	/**
	 * Creates the transport item with the editing panel information.
	 */
	public abstract boolean createTransportItem();
}