/**
 * Mars Simulation Project
 * MonitorWindow.java
 * @version 2.73 2002-02-04
 * @author Barry Evans
 */

package org.mars_sim.msp.ui.standard.monitor;

import org.mars_sim.msp.simulation.*;
import org.mars_sim.msp.ui.standard.ToolWindow;
import org.mars_sim.msp.ui.standard.UIProxyManager;
import org.mars_sim.msp.ui.standard.MainDesktopPane;
import org.mars_sim.msp.ui.standard.ViewFrameListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


/** The MonitorWindow is a tool window that displays a selection of tables
 *  each of which monitor a set of Units.
 */
public class MonitorWindow extends ToolWindow
implements Runnable {

    final private static String ROWSUFFIX = " items";
    final private static int STATUSHEIGHT = 17;
    final private static int REFRESH_PERIOD = 3000;


    // Data members
    private MainDesktopPane desktop; // Desktop pane
    private JTabbedPane tabsSection;
    private JLabel rowCount;
    private ArrayList tabs = new ArrayList();
    private Thread updateThread;     // Model update thread


    /** Constructs a TableWindow object
     *  @param desktop the desktop pane
     */
    public MonitorWindow(MainDesktopPane desktop) {

        // Use TableWindow constructor
        super("Monitor Tool");

        // Set internal frame listener
        addInternalFrameListener(new ViewFrameListener());

        // Initialize data members
        this.desktop = desktop;

        // Get content pane
        JPanel mainPane = new JPanel(new BorderLayout());
        mainPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(mainPane);

        // Create toolbar
        JToolBar toolbar = new JToolBar();
        mainPane.add(toolbar, "North");


        // Create graph button
        JButton pieButton = new JButton(PieChartTab.PIEICON);
        pieButton.setToolTipText("Create a Pie chart of a single column");
        pieButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            createPieChart();
                        }
                    });

        toolbar.add(pieButton);

        JButton barButton = new JButton(BarChartTab.BARICON);
        barButton.setToolTipText("Create a Bar chart of multiple columns");
        barButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            createBarChart();
                        }
                    });
        toolbar.add(barButton);

        JButton tabRemove = new JButton(new ImageIcon("images/TabRemove.gif"));
        tabRemove.setToolTipText("Remove selected tab");
        tabRemove.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            MonitorTab selected = getSelected();
                            if (!selected.getMandatory()) {
                                removeTab(getSelected());
                            }
                        }
                    });
        toolbar.add(tabRemove);
        toolbar.addSeparator();

        // Create buttons based on selection
        JButton mapButton = new JButton(new ImageIcon("images/CenterMap.gif"));
        mapButton.setMargin(new Insets(1, 1, 1, 1));
        mapButton.setToolTipText("Center map on selected unit");
        mapButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            centerMap();
                        }
                });
        toolbar.add(mapButton);

        JButton detailsButton = new JButton(new ImageIcon("images/ShowDetails.gif"));
        detailsButton.setToolTipText("Show details dialog");
        detailsButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            displayDetails();
                        }
                    });
        toolbar.add(detailsButton);
        toolbar.addSeparator();

        JButton propsButton = new JButton(new ImageIcon("images/Preferences.gif"));
        propsButton.setToolTipText("Change the displayed columns");
        propsButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            displayProps();
                        }
                    });
        toolbar.add(propsButton);

        // Create tabbed pane for the table
        tabsSection = new JTabbedPane();
        mainPane.add(tabsSection, "Center");

        // Create a status panel
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
        mainPane.add(statusPanel, "South");

        // Status item for row
        rowCount = new JLabel(ROWSUFFIX);
        rowCount.setHorizontalAlignment(SwingConstants.LEFT);
        rowCount.setBorder(BorderFactory.createLoweredBevelBorder());
        statusPanel.add(rowCount);
        Dimension dims = new Dimension(100, STATUSHEIGHT);
        rowCount.setPreferredSize(dims);

        // Add the default table tabs
        UnitManager unitManager =
                desktop.getMainWindow().getVirtualMars().getUnitManager();
        addTab(new TableTab(new PersonTableModel(unitManager), true));
        addTab(new TableTab(new VehicleTableModel(unitManager), true));
        addTab(new TableTab(new SettlementTableModel(unitManager), true));

        tabsSection.setSelectedIndex(0);
        tabChanged();

        // Add a listener for the tab changes
        tabsSection.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        tabChanged();
                    }
                }
                );

        // Have to define a starting size
        setSize(new Dimension(600, 300));

        // Start a thread to keep the displayed model updated
        start();
    }

    /**
     * This method add the specified Unit table as a new tab in the Monitor. The
     * model is displayed as a table by default.
     * The name of the tab is that of the Model.
     *
     * @param model The new model to display.
     */
    public void displayModel(UnitTableModel model) {
        addTab(new TableTab(model, false));
    }

    /**
     * This method creates a new chart window based on the model of the
     * currently selected window. The chart is added as a seperate tab to the
     * window.
     */
    private void createBarChart() {
        MonitorModel model = getSelected().getModel();

        // Show modal column selector
        int columns[] = ColumnSelector.createBarSelector(desktop.getMainWindow(),
                                                        model);
        if (columns.length > 0) {
            addTab(new BarChartTab(model, columns));
        }
    }

    private void createPieChart() {
        MonitorModel model = getSelected().getModel();

        // Show modal column selector
        int column = ColumnSelector.createPieSelector(desktop.getMainWindow(),
                                                        model);
        if (column >= 0) {
            addTab(new PieChartTab(model, column));
        }
    }
    /**
     * Return the currently selected tab.
     *
     * @return Monitor tab being displayed.
     */
    private MonitorTab getSelected() {
        MonitorTab selected = null;
        int selectedIdx = tabsSection.getSelectedIndex();
        if (selectedIdx != -1) {
            selected = (MonitorTab)tabs.get(selectedIdx);
        }
        return selected;
    }

    private void tabChanged() {
        MonitorTab selected = getSelected();
        if (selected != null) {
            selected.update();
            rowCount.setText("  " + selected.getModel().getRowCount() +
                             ROWSUFFIX);
        }
    }

    private void addTab(MonitorTab newTab) {
        tabs.add(newTab);
        tabsSection.addTab(newTab.getName(), newTab.getIcon(), newTab);
        tabsSection.setSelectedIndex(tabs.size()-1);
    }

    private void removeTab(MonitorTab oldTab) {
        tabs.remove(oldTab);
        tabsSection.remove(oldTab);

        oldTab.removeTab();
        if (getSelected() == oldTab) {
            tabsSection.setSelectedIndex(0);
        }
    }

    private void centerMap() {
        MonitorTab selected = getSelected();
        if (selected != null) {
            selected.centerMap(desktop);
        }
    }

    private void displayDetails() {
        MonitorTab selected = getSelected();
        if (selected != null) {
            selected.displayDetails(desktop);
        }
    }

    private void displayProps() {
        MonitorTab selected = getSelected();
        if (selected != null) {
            selected.displayProps(desktop);
        }
    }
    /** Starts display update thread, and creates a new one if necessary */
    public void start() {
        if ((updateThread == null) || (!updateThread.isAlive())) {
            updateThread = new Thread(this, "monitor window");
            updateThread.start();
        }
    }

    /** Update thread runner */
    public void run() {

        // Endless refresh loop
        while (true) {

            // Pause for 1 second between display refreshes
            try {
                updateThread.sleep(REFRESH_PERIOD);
            }
            catch (InterruptedException e) {
            }

            // Update window
            MonitorTab selected = getSelected();
            if (selected != null) {
                selected.update();
            }
        }
    }
}
