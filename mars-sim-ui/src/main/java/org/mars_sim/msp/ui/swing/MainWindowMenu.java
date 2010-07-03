/**
 * Mars Simulation Project
 * MainWindowMenu.java
 * @version 2.87 2009-10-01
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing;

import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import org.mars_sim.msp.ui.swing.tool.about.AboutWindow;
import org.mars_sim.msp.ui.swing.tool.guide.GuideWindow;
import org.mars_sim.msp.ui.swing.tool.mission.MissionWindow;
import org.mars_sim.msp.ui.swing.tool.monitor.MonitorWindow;
import org.mars_sim.msp.ui.swing.tool.navigator.NavigatorWindow;
import org.mars_sim.msp.ui.swing.tool.preferences.PreferencesWindow;
import org.mars_sim.msp.ui.swing.tool.science.ScienceWindow;
import org.mars_sim.msp.ui.swing.tool.search.SearchWindow;
import org.mars_sim.msp.ui.swing.tool.time.TimeWindow;

/** The MainWindowMenu class is the menu for the main window.
 */
public class MainWindowMenu extends JMenuBar implements ActionListener, MenuListener {

    // Data members
    private MainWindow mainWindow;                // The main window frame
    private JMenuItem newItem;                    // New menu item
    private JMenuItem loadItem;                   // Load menu item
    private JMenuItem saveItem;                   // Save menu item
    private JMenuItem saveAsItem;                 // Save As menu item
    private JMenuItem exitItem;                   // Exit menu item
    private JCheckBoxMenuItem marsNavigatorItem;  // Mars navigator menu item
    private JCheckBoxMenuItem searchToolItem;     // Search tool menu item
    private JCheckBoxMenuItem timeToolItem;       // Time tool menu item
    private JCheckBoxMenuItem monitorToolItem;    // Monitor tool menu item
    private JCheckBoxMenuItem prefsToolItem;      // Prefs tool menu item
    private JCheckBoxMenuItem missionToolItem;    // Mission tool menu item
    private JCheckBoxMenuItem scienceToolItem;    // Science tool menu item
    private JCheckBoxMenuItem aboutMspItem;       // About Mars Simulation Project menu item
    private JCheckBoxMenuItem guideItem;          // User Guide menu item

    /** 
     * Constructor
     * @param mainWindow the main window pane
     */
    public MainWindowMenu(MainWindow mainWindow) {

        // Use JMenuBar constructor
        super();

        // Initialize data members
        this.mainWindow = mainWindow;

        // Create file menu
        JMenu fileMenu = new JMenu("File");
        add(fileMenu);

        // Create new menu item
        newItem = new JMenuItem("New");
        newItem.addActionListener(this);
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK, false));
        fileMenu.add(newItem);

        // Create load menu item
        loadItem = new JMenuItem("Load");
        loadItem.addActionListener(this);
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK, false));
        fileMenu.add(loadItem);

        fileMenu.add(new JSeparator());

        // Create save menu item
        saveItem = new JMenuItem("Save");
        saveItem.addActionListener(this);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, false));
        fileMenu.add(saveItem);

        // Create save as menu item
        saveAsItem = new JMenuItem("Save As...");
        saveAsItem.addActionListener(this);
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.SHIFT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK, false));
        fileMenu.add(saveAsItem);

        fileMenu.add(new JSeparator());

        // Create exit menu item
        exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(this);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK, false));
        fileMenu.add(exitItem);

        // Create tools menu
        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.addMenuListener(this);
        add(toolsMenu);

        // Create Mars navigator menu item
        marsNavigatorItem = new JCheckBoxMenuItem(NavigatorWindow.NAME);
        marsNavigatorItem.addActionListener(this);
        marsNavigatorItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0, false));
        toolsMenu.add(marsNavigatorItem);

        // Create search tool menu item
        searchToolItem = new JCheckBoxMenuItem(SearchWindow.NAME);
        searchToolItem.addActionListener(this);
        searchToolItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0, false));
        toolsMenu.add(searchToolItem);

        // Create time tool menu item
        timeToolItem = new JCheckBoxMenuItem(TimeWindow.NAME);
        timeToolItem.addActionListener(this);
        timeToolItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0, false));
        toolsMenu.add(timeToolItem);

        // Create monitor tool menu item
        monitorToolItem = new JCheckBoxMenuItem(MonitorWindow.NAME);
        monitorToolItem.addActionListener(this);
        monitorToolItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0, false));
        toolsMenu.add(monitorToolItem);

        // Create prefs tool menu item
        prefsToolItem = new JCheckBoxMenuItem(PreferencesWindow.NAME);
        prefsToolItem.addActionListener(this);
        prefsToolItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, false));
        toolsMenu.add(prefsToolItem);
        
        // Create mission tool menu item
        missionToolItem = new JCheckBoxMenuItem(MissionWindow.NAME);
        missionToolItem.addActionListener(this);
        missionToolItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0, false));
        toolsMenu.add(missionToolItem);
        
        // Create science tool menu item
        scienceToolItem = new JCheckBoxMenuItem(ScienceWindow.NAME);
        scienceToolItem.addActionListener(this);
        scienceToolItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0, false));
        toolsMenu.add(scienceToolItem);
        
        // Create help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.addMenuListener(this);
        add(helpMenu);

        // Create about Mars Simulation Project menu item
        aboutMspItem = new JCheckBoxMenuItem("About The Mars Simulation Project");
        aboutMspItem.addActionListener(this);
        helpMenu.add(aboutMspItem);

        helpMenu.add(new JSeparator());

        // Create User Guide menu item
        guideItem = new JCheckBoxMenuItem("User Guide");
        guideItem.addActionListener(this);
        guideItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK, false));
        helpMenu.add(guideItem);

    }

    // ActionListener method overriding
    public void actionPerformed(ActionEvent event) {

        JMenuItem selectedItem = (JMenuItem) event.getSource();

        if (selectedItem == exitItem) mainWindow.exitSimulation();
        else if (selectedItem == newItem) mainWindow.newSimulation();
        else if (selectedItem == saveItem) mainWindow.saveSimulation(true);
        else if (selectedItem == saveAsItem) mainWindow.saveSimulation(false);
        else if (selectedItem == loadItem) mainWindow.loadSimulation();

        MainDesktopPane desktop = mainWindow.getDesktop();
        
        if (selectedItem == marsNavigatorItem) {
            if (marsNavigatorItem.isSelected()) desktop.openToolWindow(NavigatorWindow.NAME);
            else desktop.closeToolWindow(NavigatorWindow.NAME);
        }

        if (selectedItem == searchToolItem) {
            if (searchToolItem.isSelected()) desktop.openToolWindow(SearchWindow.NAME);
            else desktop.closeToolWindow(SearchWindow.NAME);
        }

        if (selectedItem == timeToolItem) {
            if (timeToolItem.isSelected()) desktop.openToolWindow(TimeWindow.NAME);
            else desktop.closeToolWindow(TimeWindow.NAME);
        }

        if (selectedItem == monitorToolItem) {
            if (monitorToolItem.isSelected()) desktop.openToolWindow(MonitorWindow.NAME);
            else desktop.closeToolWindow(MonitorWindow.NAME);
        }
        
        if (selectedItem == prefsToolItem) {
            if (prefsToolItem.isSelected()) desktop.openToolWindow(PreferencesWindow.NAME);
            else desktop.closeToolWindow(PreferencesWindow.NAME);
        }
        
        if (selectedItem == missionToolItem) {
            if (missionToolItem.isSelected()) desktop.openToolWindow(MissionWindow.NAME);
            else desktop.closeToolWindow(MissionWindow.NAME);
        }
        
        if (selectedItem == scienceToolItem) {
            if (scienceToolItem.isSelected()) desktop.openToolWindow(ScienceWindow.NAME);
            else desktop.closeToolWindow(ScienceWindow.NAME);
        }

        if (selectedItem == aboutMspItem) {
            if (aboutMspItem.isSelected()) desktop.openToolWindow(AboutWindow.NAME);
            else desktop.closeToolWindow(AboutWindow.NAME);
        }

        if (selectedItem == guideItem) {
            if (guideItem.isSelected()) desktop.openToolWindow(GuideWindow.NAME);
            else desktop.closeToolWindow(GuideWindow.NAME);
        }
    }

    // MenuListener method overriding
    public void menuSelected(MenuEvent event) {
        MainDesktopPane desktop = mainWindow.getDesktop();
        marsNavigatorItem.setSelected(desktop.isToolWindowOpen(NavigatorWindow.NAME));
        searchToolItem.setSelected(desktop.isToolWindowOpen(SearchWindow.NAME));
        timeToolItem.setSelected(desktop.isToolWindowOpen(TimeWindow.NAME));
        monitorToolItem.setSelected(desktop.isToolWindowOpen(MonitorWindow.NAME));
        prefsToolItem.setSelected(desktop.isToolWindowOpen(PreferencesWindow.NAME));
        missionToolItem.setSelected(desktop.isToolWindowOpen(MissionWindow.NAME));
        scienceToolItem.setSelected(desktop.isToolWindowOpen(ScienceWindow.NAME));
        aboutMspItem.setSelected(desktop.isToolWindowOpen(AboutWindow.NAME));
        guideItem.setSelected(desktop.isToolWindowOpen(GuideWindow.NAME));
   }

    public void menuCanceled(MenuEvent event) {}
    public void menuDeselected(MenuEvent event) {}
}