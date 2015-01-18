/**
 * Mars Simulation Project
 * NewTransportItemDialog.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.resupply;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.mars_sim.msp.ui.swing.JComboBoxMW;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;

/**
 * A dialog for creating a new transport item.
 * TODO externalize strings
 */
public class NewTransportItemDialog
extends JDialog {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Transport item types.
	private static final String DEFAULT_MESSAGE = "Select Transport Item Type";
	private static final String RESUPPLY_MISSION = "New Resupply Mission";
	private static final String ARRIVING_SETTLEMENT = "New Arriving Settlement";

	// Data members.
	private TransportItemEditingPanel editingPanel;
	private JPanel mainEditingPanel;
	private CardLayout mainEditingLayout;
	private JPanel emptyPanel;
	private TransportItemEditingPanel resupplyMissionPanel;
	private TransportItemEditingPanel arrivingSettlementPanel;
	private JButton createButton;

	/**
	 * Constructor.
	 * @param owner the owner of this dialog.
	 * @param transportItem the transport item to modify.
	 */
	public NewTransportItemDialog(JFrame owner) {
		// Use JDialog constructor.
		super(owner, "New Transport Item", true);

		// Set the layout.
		setLayout(new BorderLayout(0, 0));

		// Set the border.
		((JComponent) getContentPane()).setBorder(new MarsPanelBorder());

		// Create transport type panel.
		JPanel transportTypePanel = new JPanel(new FlowLayout(10, 10, FlowLayout.CENTER));
		getContentPane().add(transportTypePanel, BorderLayout.NORTH);

		// Create combo box for determining transport item type.
		JComboBox<String> typeBox = new JComboBoxMW<String>();
		typeBox.addItem(DEFAULT_MESSAGE);
		typeBox.addItem(RESUPPLY_MISSION);
		typeBox.addItem(ARRIVING_SETTLEMENT);
		typeBox.setSelectedItem(DEFAULT_MESSAGE);
		typeBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				JComboBox<?> cb = (JComboBox<?>) evt.getSource();
				setEditingPanel((String) cb.getSelectedItem());
			}
		});
		transportTypePanel.add(typeBox);

		// Create main editing panel.
		mainEditingLayout = new CardLayout();
		mainEditingPanel = new JPanel(mainEditingLayout);
		getContentPane().add(mainEditingPanel, BorderLayout.CENTER);

		// Create empty default panel.
		emptyPanel = new JPanel();
		emptyPanel.setBorder(new MarsPanelBorder());
		mainEditingPanel.add(emptyPanel, DEFAULT_MESSAGE);

		// Create resupply mission editing panel.
		resupplyMissionPanel = new ResupplyMissionEditingPanel(null);
		mainEditingPanel.add(resupplyMissionPanel, RESUPPLY_MISSION);

		// Create arriving settlement editing panel.
		arrivingSettlementPanel = new ArrivingSettlementEditingPanel(null);
		mainEditingPanel.add(arrivingSettlementPanel, ARRIVING_SETTLEMENT);

		// Create the button pane.
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		// Create create button.
		createButton = new JButton("Create");
		createButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				// Create transport item and close dialog.
				createTransportItem();
			}
		});
		createButton.setEnabled(false);
		buttonPane.add(createButton);

		// Create cancel button.
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// Close dialog.
				dispose();
			}

		});
		buttonPane.add(cancelButton);

		// Finish and display dialog.
		pack();
		setLocationRelativeTo(owner);
		setResizable(false);
		setVisible(true);
	}

	/**
	 * Set the editing panel.
	 * @param panelKey the panel key string.
	 */
	private void setEditingPanel(String panelKey) {

		if (panelKey != null) {
			mainEditingLayout.show(mainEditingPanel, panelKey);

			if (panelKey.equals(DEFAULT_MESSAGE)) {
				editingPanel = null;
				createButton.setEnabled(false);
			}
			else if (panelKey.equals(RESUPPLY_MISSION)) {
				editingPanel = resupplyMissionPanel;
				createButton.setEnabled(true);
			}
			else if (panelKey.equals(ARRIVING_SETTLEMENT)) {
				editingPanel = arrivingSettlementPanel;
				createButton.setEnabled(true);
			}
		}
	}

	/**
	 * Create the new transport item and close the dialog.
	 */
	private void createTransportItem() {
		if ((editingPanel != null) && editingPanel.createTransportItem()) {
			dispose();
		}
	}
}