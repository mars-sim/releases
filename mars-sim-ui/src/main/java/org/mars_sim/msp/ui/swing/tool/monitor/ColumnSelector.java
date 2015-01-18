/**
 * Mars Simulation Project
 * ColumnSelector.java
 * @version 3.07 2014-12-06

 * @author Barry Evans
 */
package org.mars_sim.msp.ui.swing.tool.monitor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Msg;

/**
 * This window displays a list of columns from the specified model.
 * The columns displayed depends upon the final chart being rendered.
 */
public class ColumnSelector
extends JDialog {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private final static String PIE_MESSAGE = Msg.getString("ColumnSelector.singleColumn"); //$NON-NLS-1$
	private final static String BAR_MESSAGE = Msg.getString("ColumnSelector.multipleColumns"); //$NON-NLS-1$

	// Data members
	/** Check boxes. */
	private JList<?> columnList = null;
	private int columnMappings[] = null;
	private boolean okPressed = false;

	/**
	 * Constructs the dialog with a title and columns extracted from the
	 * specified model.
	 * @param owner the owner of the dialog.
	 * @param model Model driving the columns.
	 * @param bar Display selection for a Bar chart.
	 */
	public ColumnSelector(Frame owner, MonitorModel model, boolean bar) {
		super(owner, model.getName(), true);

		// Add all valid columns into the list
		Vector<String> items = new Vector<String>();
		columnMappings = new int[model.getColumnCount()-1];
		for(int i = 1; i < model.getColumnCount(); i++) {
			// If a valid column then add to model.
			Class<?> columnType = model.getColumnClass(i);

			// If a bar, look for Number classes
			if (bar) {
				if (Number.class.isAssignableFrom(columnType)) {
					columnMappings[items.size()] = i;
					items.add(model.getColumnName(i));
				}
			}
			else if (isCategory(columnType)) {
				columnMappings[items.size()] = i;
				items.add(model.getColumnName(i));
			}
		}

		// Center pane
		JPanel centerPane = new JPanel(new BorderLayout());
		centerPane.setBorder(BorderFactory.createRaisedBevelBorder());
		columnList = new JList<Object>(items);
		if (bar) {
			columnList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			centerPane.add(new JLabel(BAR_MESSAGE), BorderLayout.NORTH);
		}
		else {
			centerPane.add(new JLabel(PIE_MESSAGE), BorderLayout.NORTH);
		}
		centerPane.add(new JScrollPane(columnList), BorderLayout.CENTER);

		// Buttons
		JPanel buttonPanel = new JPanel(new FlowLayout());
		JButton okButton = new JButton(Msg.getString("ColumnSelector.button.ok")); //$NON-NLS-1$
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				okPressed = true;
				setVisible(false);
			}
		});
		buttonPanel.add(okButton);
		JButton cancelButton = new JButton(Msg.getString("ColumnSelector.button.cancel")); //$NON-NLS-1$
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				setVisible(false);
			}
		});
		buttonPanel.add(cancelButton);

		// Package dialog
		JPanel pane = new JPanel(new BorderLayout());
		pane.add(centerPane, BorderLayout.CENTER);
		pane.add(buttonPanel, BorderLayout.SOUTH);
		getContentPane().add(pane);

		pack();
	}

	/**
	 * Create a column selector popup for use with a Bar chart.
	 * @param window Parent frame.
	 * @param model Model containing columns.
	 * @return Array of column indexes to display.
	 */
	public static int[] createBarSelector(Frame window,
			MonitorModel model) {
		ColumnSelector select = new ColumnSelector(window, model, true);
		select.setVisible(true);
		return select.getSelectedColumns();
	}

	/**
	 * Create a column selector popup for a Pie chart.
	 * @param window Parent frame.
	 * @param model Model containign columns.
	 * @return Column index to use as category.
	 */
	public static int createPieSelector(Frame window,
			MonitorModel model) {
		ColumnSelector select = new ColumnSelector(window, model, false);
		select.setVisible(true);
		int [] columns = select.getSelectedColumns();
		if (columns.length > 0) {
			return columns[0];
		}
		else {
			return -1;
		}
	}

	/**
	 * Return the list of selected columns. The return is the index value
	 * as described by the assoicated table model. The return array maybe
	 * zero or more depending on selection and mode.
	 * @return Array of TableColumn indexes of selection.
	 */
	public int[] getSelectedColumns() {
		int index[] = null;
		if (okPressed) {
			int selection[] = columnList.getSelectedIndices();
			index = new int[selection.length];

			for(int i = 0; i < selection.length; i++) {
				index[i] = columnMappings[selection[i]];
			}
		}
		else {
			index = new int[0];
		}

		return index;
	}

	/**
	 * Is the class specified suitable to be displayed as a category dataset.
	 * This is basically any class that is an Class and not a numerical
	 * @param columnClass The Class of the data in the column.
	 * @return Is the class a category
	 */
	private boolean isCategory(Class<?> columnClass) {
		return (!Number.class.isAssignableFrom(columnClass) &&
				!Coordinates.class.equals(columnClass));
	}
}
