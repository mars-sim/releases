/**
 * Mars Simulation Project
 * MonitorPropsDialog.java
 * @version 2.72 2001-10-28
 * @author Barry Evans
 */

package org.mars_sim.msp.ui.standard.monitor;

import org.mars_sim.msp.simulation.*;
import org.mars_sim.msp.ui.standard.MainDesktopPane;
import java.util.ArrayList;
import java.util.Enumeration;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * The MonitorPropsDialog class display the columns of a specific table.
 * The columns can be either display or hidden in the target table.
 */
class TableProperties extends JInternalFrame {

    // Data members
    private TableColumnModel model; // Table to change
    private ArrayList columnButtons = new ArrayList(); // Check boxes

    /** Constructs a MonitorPropsDialog class
     *  @param title The name of the specified model
     *  @param table the table to configure
     *  @param desktop the main desktop.
     */
    public TableProperties(String title, JTable table,
                              MainDesktopPane main) {

        // Use JInternalFrame constructor
        super(title + " Properties", false, true);

        // Initialize data members
        this.model = table.getColumnModel();

        // Prepare content pane
        JPanel mainPane = new JPanel();
        mainPane.setLayout(new BorderLayout());
        mainPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(mainPane);

        // Create column pane
        JPanel columnPane = new JPanel(new GridLayout(0, 1));
        columnPane.setBorder(new CompoundBorder(new EtchedBorder(),
                                                new EmptyBorder(5, 5, 5, 5)));

        // Create a checkbox for each column in the model
        TableModel dataModel = table.getModel();
        for(int i = 0; i < dataModel.getColumnCount(); i++) {
            String name = dataModel.getColumnName(i);
            JCheckBox column = new JCheckBox(name);
            column.setSelected(false);

            column.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        columnSelected(event);
                    }
                });
            columnButtons.add(column);
            columnPane.add(column);
        }

        // Selected if column is visible
        Enumeration enum = model.getColumns();
        while(enum.hasMoreElements()) {
            int selected = ((TableColumn)enum.nextElement()).getModelIndex();
            JCheckBox columnButton  = (JCheckBox)columnButtons.get(selected);
            columnButton.setSelected(true);
        }

        mainPane.add(columnPane, "Center");
        pack();
        main.add(this);
    }

    /**
     * The user has selected a column in the dialog.
     *
     * @param event Event driving the action.
     */
    private void columnSelected(ActionEvent event) {
        JCheckBox button = (JCheckBox)event.getSource();
        int index = columnButtons.indexOf(button);

        // Either add or remove column
        if (button.isSelected()) {
            TableColumn col = new TableColumn(index);
            col.setHeaderValue(button.getText());
            model.addColumn(col);
        }
        else {
            TableColumn col = null;
            Enumeration enum = model.getColumns();
            while((col == null) && enum.hasMoreElements()) {
                TableColumn next = (TableColumn)enum.nextElement();
                if (next.getModelIndex() == index) {
                    col = next;
                }
            }
            model.removeColumn(col);
        }
    }

}
