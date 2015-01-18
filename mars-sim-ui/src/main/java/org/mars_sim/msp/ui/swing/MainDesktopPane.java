/**
 * Mars Simulation Project
 * MainDesktopPane.java
 * @version 3.07 2015-01-17
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitEvent;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.UnitListener;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.UnitManagerEvent;
import org.mars_sim.msp.core.UnitManagerListener;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.ui.swing.sound.AudioPlayer;
import org.mars_sim.msp.ui.swing.sound.SoundConstants;
import org.mars_sim.msp.ui.swing.tool.ToolWindow;
import org.mars_sim.msp.ui.swing.tool.guide.GuideWindow;
import org.mars_sim.msp.ui.swing.tool.mission.MissionWindow;
import org.mars_sim.msp.ui.swing.tool.monitor.MonitorWindow;
import org.mars_sim.msp.ui.swing.tool.monitor.UnitTableModel;
import org.mars_sim.msp.ui.swing.tool.navigator.NavigatorWindow;
import org.mars_sim.msp.ui.swing.tool.resupply.ResupplyWindow;
import org.mars_sim.msp.ui.swing.tool.science.ScienceWindow;
import org.mars_sim.msp.ui.swing.tool.search.SearchWindow;
import org.mars_sim.msp.ui.swing.tool.settlement.SettlementWindow;
import org.mars_sim.msp.ui.swing.tool.time.TimeWindow;
import org.mars_sim.msp.ui.swing.unit_display_info.UnitDisplayInfoFactory;
import org.mars_sim.msp.ui.swing.unit_window.UnitWindow;
import org.mars_sim.msp.ui.swing.unit_window.UnitWindowFactory;
import org.mars_sim.msp.ui.swing.unit_window.UnitWindowListener;

/** 
 * The MainDesktopPane class is the desktop part of the project's UI.
 * It contains all tool and unit windows, and is itself contained,
 * along with the tool bars, by the main window.
 */
public class MainDesktopPane
extends JDesktopPane
implements ComponentListener, UnitListener, UnitManagerListener {
	
	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(MainDesktopPane.class.getName());

	// Data members
	/** List of open or buttoned unit windows. */
	private final Collection<UnitWindow> unitWindows;
	/** List of tool windows. */
	private final Collection<ToolWindow> toolWindows;
	/** The main window frame. */
	private final MainWindow mainWindow;
	/** ImageIcon that contains the tiled background. */
	private final ImageIcon backgroundImageIcon;
	/** Label that contains the tiled background. */
	private final JLabel backgroundLabel;
	/** True if this MainDesktopPane hasn't been displayed yet. */
	private boolean firstDisplay;
	/* The desktop update thread. */
	private final UpdateThread updateThread;
	/** The sound player. */
	private final AudioPlayer soundPlayer;
	/** The desktop popup announcement window. */
	private AnnouncementWindow announcementWindow;
	
	// 2014-12-19 Added settlementWindow
	private SettlementWindow settlementWindow;
	private Building building;
	private Settlement settlement;
	// 2014-12-23 Added transportWizard
	private TransportWizard transportWizard;
	private BuildingManager mgr = null; // mgr is very important for FINISH_BUILDING_PLACEMENT_EVENT
	private boolean isTransportingBuilding = false;
	private MarqueeBanner marqueeBanner;
	
	/** 
	 * Constructor.
	 * @param mainWindow the main outer window
	 */
	public MainDesktopPane(MainWindow mainWindow) {

		// Initialize data members
		soundPlayer = new AudioPlayer();
		soundPlayer.play(SoundConstants.SOUNDS_ROOT_PATH + SoundConstants.SND_SPLASH); // play our splash sound

		this.mainWindow = mainWindow;
		unitWindows = new ArrayList<UnitWindow>();
		toolWindows = new ArrayList<ToolWindow>();

		// Set background color to black
		setBackground(Color.black);

		// set desktop manager
		setDesktopManager(new MainDesktopManager());

		// Set component listener
		addComponentListener(this);

		// Create background label and set it to the back layer
		backgroundImageIcon = new ImageIcon();
		backgroundLabel = new JLabel(backgroundImageIcon);
		add(backgroundLabel, Integer.MIN_VALUE);
		backgroundLabel.setLocation(0, 0);
		moveToBack(backgroundLabel);

		// Initialize firstDisplay to true
		firstDisplay = true;

		// Prepare tool windows.
		prepareToolWindows();

		// Create update thread.
		updateThread = new UpdateThread(this);
		updateThread.setRun(true);
		updateThread.start();
		
		// 2014-12-26 Added prepareListeners
		prepareListeners();
				
		// 2014-12-27 Added prepareWindows
		prepareWindows();
		
		//openMarqueeBanner("");
	}
	

	/**
	 * Opens a popup announcement window on the desktop.
	 * @param announcement the announcement text to display.
	 
	// 2014-12-30 Added openMarqueeBanner()
	public void openMarqueeBanner(String announcement) {
		
		final MainDesktopPane d = this;
		final String text = announcement;
		EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
            	marqueeBanner = new MarqueeBanner(d);
            	marqueeBanner.setAnnouncement(text);
        		int Xloc = (getWidth() - announcementWindow.getWidth()) / 2;
          		int Yloc = 0;
        		setLocation(Xloc, Yloc);
        		marqueeBanner.display();
            }
        });
		
  	  	//marqueeBanner.display();
  	  	//marqueeBanner.pack();
		//add(announcementWindow, 0);
		//int Xloc = (getWidth() - announcementWindow.getWidth()) / 2;
		//int Yloc = (getHeight() - announcementWindow.getHeight()) / 2;
		// 2014-12-26 Modified Yloc = 0 to avoid overlapping other images at the center of desktop 
		//int Yloc = 0;
		//marqueeBanner.setLocation(Xloc, Yloc);
		// Note: second window packing seems necessary to get window
		// to display components correctly.
		//marqueeBanner.pack();
		//marqueeBanner.setVisible(true);
	}
	*/
	
	/** 
	 * sets up this class with two listeners 
	 */
	// 2014-12-19 Added prepareListeners()
	public void prepareListeners() {
		
		// Add addUnitManagerListener()
		UnitManager unitManager = Simulation.instance().getUnitManager();
		unitManager.addUnitManagerListener(this);		
				
		// Add addUnitListener()
		Collection<Settlement> settlements = unitManager.getSettlements();
		List<Settlement> settlementList = new ArrayList<Settlement>(settlements);
		settlement = settlementList.get(0);
		List<Building> buildings = settlement.getBuildingManager().getBuildings();
		building = buildings.get(0);
		//building.addUnitListener(this); // not working
		Iterator<Settlement> i = settlementList.iterator();
		while (i.hasNext()) {
			i.next().addUnitListener(this);			
		}
	}
	
	/** Returns the MainWindow instance
	 *  @return MainWindow instance
	 */
	public MainWindow getMainWindow() {
		return mainWindow;
	}

	/**
	 * Create background tile when MainDesktopPane is first
	 * displayed. Recenter logoLabel on MainWindow and set
	 * backgroundLabel to the size of MainDesktopPane.
	 * @param e the component event
	 */
	@Override
	public void componentResized(ComponentEvent e) {

		// If displayed for the first time, create background image tile.
		// The size of the background tile cannot be determined during construction
		// since it requires the MainDesktopPane be displayed first.
		if (firstDisplay) {
			ImageIcon baseImageIcon = ImageLoader.getIcon(Msg.getString("img.background")); //$NON-NLS-1$
			Dimension screen_size =
				Toolkit.getDefaultToolkit().getScreenSize();
			Image backgroundImage =
				createImage((int) screen_size.getWidth(),
						(int) screen_size.getHeight());
			Graphics backgroundGraphics = backgroundImage.getGraphics();

			for (int x = 0; x < backgroundImage.getWidth(this);
			x += baseImageIcon.getIconWidth()) {
				for (int y = 0; y < backgroundImage.getHeight(this);
				y += baseImageIcon.getIconHeight()) {
					backgroundGraphics.drawImage(
							baseImageIcon.getImage(), x, y, this);
				}
			}

			backgroundImageIcon.setImage(backgroundImage);

			backgroundLabel.setSize(getSize());

			firstDisplay = false;
		}

		// Set the backgroundLabel size to the size of the desktop
		backgroundLabel.setSize(getSize());

	}

	// Additional Component Listener methods implemented but not used.
	@Override
	public void componentMoved(ComponentEvent e) {}
	@Override
	public void componentShown(ComponentEvent e) {}
	@Override
	public void componentHidden(ComponentEvent e) {}

	/*
	 * Creates tool windows 
	 */
	private void prepareToolWindows() {

		toolWindows.clear();

		// Prepare navigator window
		NavigatorWindow navWindow = new NavigatorWindow(this);
		try { navWindow.setClosed(true); }
		catch (PropertyVetoException e) { }
		toolWindows.add(navWindow);

		// Prepare search tool window
		SearchWindow searchWindow = new SearchWindow(this);
		try { searchWindow.setClosed(true); }
		catch (PropertyVetoException e) { }
		toolWindows.add(searchWindow);

		// Prepare time tool window
		TimeWindow timeWindow = new TimeWindow(this);
		try { timeWindow.setClosed(true); }
		catch (PropertyVetoException e) { }
		toolWindows.add(timeWindow);

		// Prepare monitor tool window
		MonitorWindow monitorWindow = new MonitorWindow(this);
		try { monitorWindow.setClosed(true); }
		catch (PropertyVetoException e) { }
		toolWindows.add(monitorWindow);

		// Prepare mission tool window
		MissionWindow missionWindow = new MissionWindow(this);
		try { missionWindow.setClosed(true); }
		catch (PropertyVetoException e) { }
		toolWindows.add(missionWindow);

		// Prepare settlement tool window
		SettlementWindow settlementWindow = new SettlementWindow(this);
		try { settlementWindow.setClosed(true); }
		catch (PropertyVetoException e) { }
		toolWindows.add(settlementWindow);

		setSettlementWindow(settlementWindow);

		// Prepare science tool window
		ScienceWindow scienceWindow = new ScienceWindow(this);
		try { scienceWindow.setClosed(true); }
		catch (PropertyVetoException e) { }
		toolWindows.add(scienceWindow);
		
		// Prepare resupply tool window
		ResupplyWindow resupplyWindow = new ResupplyWindow(this);
		try { resupplyWindow.setClosed(true); }
		catch (PropertyVetoException e) { }
		toolWindows.add(resupplyWindow);

		// Prepare guide tool window
		GuideWindow guideWindow = new GuideWindow(this);
		try { guideWindow.setClosed(true); }
		catch (PropertyVetoException e) { }
		toolWindows.add(guideWindow);
		
	}
	
	/*
	 * * Creates announcement windows & transportWizard
	 */
	private void prepareWindows() {
		// Prepare announcementWindow.
		announcementWindow = new AnnouncementWindow(this);
		try { announcementWindow.setClosed(true); }
		catch (java.beans.PropertyVetoException e) { }
		
		// 2014-12-23 Added transportWizard
		transportWizard = new TransportWizard(this);
		try { transportWizard.setClosed(true); }
		catch (java.beans.PropertyVetoException e) { }
		
		// 2014-12-30 Added marqueeBanner
		//marqueeBanner = new MarqueeBanner(this);
      	//try { marqueeBanner.setClosed(true); }
      	//catch (java.beans.PropertyVetoException e) { }
		
	}

	/** Returns a tool window for a given tool name
	 *  @param toolName the name of the tool window
	 *  @return the tool window
	 */
	public ToolWindow getToolWindow(String toolName) {
		ToolWindow result = null;
		Iterator<ToolWindow> i = toolWindows.iterator();
		while (i.hasNext()) {
			ToolWindow window = i.next();
			if (toolName.equals(window.getToolName())) {
				result = window;
			}
		}
		return result;
	}

	/**
	 * Displays a new Unit model in the monitor window.
	 * @param model the new model to display
	 */
	public void addModel(UnitTableModel model) {
		((MonitorWindow) getToolWindow(MonitorWindow.NAME)).displayModel(model);
		openToolWindow(MonitorWindow.NAME);
	}

	/** 
	 * Centers the map and the globe on given coordinates.
	 * Also opens the map tool if it's closed.
	 * @param targetLocation the new center location
	 */
	public void centerMapGlobe(Coordinates targetLocation) {
		((NavigatorWindow) getToolWindow(NavigatorWindow.NAME)).
		updateCoords(targetLocation);
		openToolWindow(NavigatorWindow.NAME);
	}

	/**
	 * Return true if tool window is open.
	 * @param toolName the name of the tool window
	 * @return true true if tool window is open
	 */
	public boolean isToolWindowOpen(String toolName) {
		ToolWindow window = getToolWindow(toolName);
		if (window != null) {
			return !window.isClosed();
		} else {
			return false;
		}
	}

	/**
	 * Opens a tool window if necessary.
	 * @param toolName the name of the tool window
	 */
	public void openToolWindow(String toolName) {
		ToolWindow window = getToolWindow(toolName);
		if (window != null) {
			if (window.isClosed()) {
				if (!window.wasOpened()) {
					UIConfig config = UIConfig.INSTANCE;
					if (config.useUIDefault()) {
						window.setLocation(getCenterLocation(window));
					} else {
						if (config.isInternalWindowConfigured(toolName)) {
							window.setLocation(config.getInternalWindowLocation(toolName));
							if (window.isResizable()) {
								window.setSize(config.getInternalWindowDimension(toolName));
							}
						} else {
							window.setLocation(getRandomLocation(window));
						}
					}
					window.setWasOpened(true);
				}
				add(window, 0);
				try { 
					window.setClosed(false); 
				}
				catch (Exception e) { logger.log(Level.SEVERE,e.toString()); }
			}
			window.show();
			// bring to front if it overlaps with other windows
			try {
				window.setSelected(true);
			} catch (PropertyVetoException e) {
				// ignore if setSelected is vetoed	
			}
		}
	}

	/**
	 * Closes a tool window if it is open
	 * @param toolName the name of the tool window
	 */
	public void closeToolWindow(String toolName) {
		ToolWindow window = getToolWindow(toolName);
		if ((window != null) && !window.isClosed()) {
			try { window.setClosed(true); }
			catch (java.beans.PropertyVetoException e) {}
		}
	}

	/**
	 * Creates and opens a window for a unit if it isn't 
	 * already in existence and open.
	 * @param unit the unit the window is for.
	 * @param initialWindow true if window is opened at UI startup.
	 */
	public void openUnitWindow(Unit unit, boolean initialWindow) {

		UnitWindow tempWindow = null;

		Iterator<UnitWindow> i = unitWindows.iterator();
		while (i.hasNext()) {
			UnitWindow window = i.next();
			if (window.getUnit() == unit) {
				tempWindow = window;
			}
		}

		if (tempWindow != null) {
			if (tempWindow.isClosed()) {
				add(tempWindow, 0);
			}

			try {
				tempWindow.setIcon(false);
			} catch (java.beans.PropertyVetoException e) {
				logger.log(Level.SEVERE,Msg.getString("MainDesktopPane.log.problemReopening") + e); //$NON-NLS-1$
			}
		} else {
			// Create new window for unit.
			tempWindow = UnitWindowFactory.getUnitWindow(unit, this);

			add(tempWindow, 0);
			tempWindow.pack();

			// Set internal frame listener
			tempWindow.addInternalFrameListener(new UnitWindowListener(this));

			if (initialWindow) {
				// Put window in configured position on desktop.
				tempWindow.setLocation(UIConfig.INSTANCE.getInternalWindowLocation(unit.getName()));
			}
			else {
				// Put window in random position on desktop.
				tempWindow.setLocation(getRandomLocation(tempWindow));
			}

			// Add unit window to unit windows
			unitWindows.add(tempWindow);

			// Create new unit button in tool bar if necessary
			mainWindow.createUnitButton(unit);
		}

		tempWindow.setVisible(true);

		// Correct window becomes selected
		try {
			tempWindow.setSelected(true);
			tempWindow.moveToFront();
		} catch (java.beans.PropertyVetoException e) {}

		// Play sound for window.
		String soundFilePath = UnitDisplayInfoFactory.getUnitDisplayInfo(unit).getSound(unit);
		if ((soundFilePath != null) && soundFilePath.length() != 0) {
			soundFilePath = SoundConstants.SOUNDS_ROOT_PATH + soundFilePath;
		}
		soundPlayer.play(soundFilePath);
	}

	/**
	 * Finds an existing unit window for a unit.
	 * @param unit the unit to search for.
	 * @return existing unit window or null if none.
	 */
	public UnitWindow findUnitWindow(Unit unit) {
		UnitWindow result = null;
		Iterator<UnitWindow> i = unitWindows.iterator();
		while (i.hasNext()) {
			UnitWindow window = i.next();
			if (window.getUnit() == unit) {
				result = window;
			}
		}
		return result;
	}

	/** 
	 * Disposes a unit window and button.
	 *
	 * @param unit the unit the window is for.
	 */
	public void disposeUnitWindow(Unit unit) {

		// Dispose unit window
		UnitWindow deadWindow = null;
		Iterator<UnitWindow> i = unitWindows.iterator();
		while (i.hasNext()) {
			UnitWindow window = i.next();
			if (unit == window.getUnit()) {
				deadWindow = window;
			}
		}

		unitWindows.remove(deadWindow);

		if (deadWindow != null) {
			deadWindow.dispose();
		}

		// Have main window dispose of unit button
		mainWindow.disposeUnitButton(unit);
	}

	/** 
	 * Disposes a unit window and button.
	 *
	 * @param window the unit window to dispose.
	 */
	public void disposeUnitWindow(UnitWindow window) {

		if (window != null) {    
			unitWindows.remove(window);
			window.dispose();

			// Have main window dispose of unit button
			mainWindow.disposeUnitButton(window.getUnit());
		}
	}

	/**
	 * Update the desktop and all of its windows.
	 */
	private void update() {

		// Update all unit windows.
		Iterator<UnitWindow> i1 = unitWindows.iterator();
		try {
			while (i1.hasNext()) {
				i1.next().update();
			}
		}
		catch (ConcurrentModificationException e) {
			// Concurrent modifications exceptions may occur as 
			// unit windows are opened.
		}

		// Update all tool windows.
		Iterator<ToolWindow> i2 = toolWindows.iterator();
		try {
			while (i2.hasNext()) {
				i2.next().update();
			}
		}
		catch (ConcurrentModificationException e) {
			// Concurrent modifications exceptions may occur as
			// unit windows are opened.
		}
	}


	void clearDesktop() {
		// Stop update thread.
		updateThread.setRun(false);

		// Give some time for the update thread to finish updating.
		try {
			Thread.sleep(100L);
		} catch (InterruptedException e) {};
		
		// Dispose unit windows
		Iterator<UnitWindow> i1 = unitWindows.iterator();
		while (i1.hasNext()) {
			UnitWindow window = i1.next();
			window.dispose();
			mainWindow.disposeUnitButton(window.getUnit());
			window.destroy();
		}
		unitWindows.clear();

		// Dispose tool windows
		Iterator<ToolWindow> i2 = toolWindows.iterator();
		while (i2.hasNext()) {
			ToolWindow window = i2.next();
			window.dispose();
			window.destroy();
		}
		toolWindows.clear();
	}

	/**
	 * Resets all windows on the desktop.  Disposes of all unit windows
	 * and tool windows, and reconstructs the tool windows.
	 */
	void resetDesktop() {
		// Prepare tool windows
		prepareToolWindows();

		// Restart update thread.
		updateThread.setRun(true);
	}

	private Point getCenterLocation(JInternalFrame tempWindow) {

		Dimension desktop_size = getSize();
		Dimension window_size = tempWindow.getSize();

		int rX = (int) Math.round((desktop_size.width - window_size.width) / 2D);
		int rY = (int) Math.round((desktop_size.height - window_size.height) / 2D);
		
		// 2014-12-25 Added rX checking
		if (rX < 0) {
			rX = 0;
		}
		
		// Make sure y position isn't < 0.
		if (rY < 0) {
			rY = 0;
		}

		return new Point(rX, rY);
	}
	
	/** 
	 * Gets a random location on the desktop for a given {@link JInternalFrame}.
	 * @param tempWindow an internal window
	 * @return random point on the desktop
	 */
	private Point getRandomLocation(JInternalFrame tempWindow) {

		Dimension desktop_size = getSize();
		Dimension window_size = tempWindow.getSize();

		int rX = (int) Math.round(Math.random() *
				(desktop_size.width - window_size.width));
		int rY = (int) Math.round(Math.random() *
				(desktop_size.height - window_size.height));

		// Make sure y position isn't < 0.
		if (rY < 0) {
			rY = 0;
		}

		// 2014-12-25 Added rX checking
		if (rX < 0) {
			rX = 0;
		}
		return new Point(rX, rY);
	}

	/**
	 * Internal class thread for update.
	 */
	private class UpdateThread extends Thread {

		public static final long SLEEP_TIME = 1000; // 1 second.
		MainDesktopPane desktop;
		boolean run = false;

		private UpdateThread(MainDesktopPane desktop) {
			super(Msg.getString("MainDesktopPane.thread.desktopUpdate")); //$NON-NLS-1$
			this.desktop = desktop;
		}

		private void setRun(boolean run) {
			this.run = run;
		}

		@Override
		public void run() {
			while (true) {
				if (run) {
					desktop.update();
				}   
				try {
					Thread.sleep(SLEEP_TIME);
				} catch (InterruptedException e) {}
			}
		}
	}

	/**
	 * Gets the sound player used by the desktop.
	 * @return sound player.
	 */
	public AudioPlayer getSoundPlayer() {
		return soundPlayer;
	}

	/**
	 * Opens a popup announcement window on the desktop.
	 * @param announcement the announcement text to display.
	 */
	public void openAnnouncementWindow(String announcement) {
		announcementWindow.setAnnouncement(announcement);
		announcementWindow.pack();
		add(announcementWindow, 0);
		int Xloc = (getWidth() - announcementWindow.getWidth()) * 3 / 4 ;
		int Yloc = (getHeight() - announcementWindow.getHeight()) * 3 / 4;
		announcementWindow.setLocation(Xloc, Yloc);
		// Note: second window packing seems necessary to get window
		// to display components correctly.
		announcementWindow.pack();
		announcementWindow.setVisible(true);
	}
	/**
	 * Removes the popup announcement window from the desktop.
	 */
	public void disposeAnnouncementWindow() {
		announcementWindow.dispose();
	}

	
	/**
	 * Updates the look & feel of the announcement window.
	 */
	void updateAnnouncementWindowLF() {
	    if (announcementWindow != null) {
	        SwingUtilities.updateComponentTreeUI(announcementWindow);
	    }
	}

	/**
	 * Opens a transport wizard on the desktop.
	 * @param announcement the announcement text to display.
	 */
	// 2014-12-23 Added openTransportWizard()
	public void openTransportWizard(BuildingManager buildingManager) { //, Building building) {
		//transportWizard.setAnnouncement(announcement);
		transportWizard.initialize(buildingManager);//, building);
		transportWizard.deliverBuildings();
		transportWizard.pack();
		//add(transportWizard, 0);
		//int Xloc = (getWidth() - transportWizard.getWidth()) / 2;
		//int Yloc = (getHeight() - transportWizard.getHeight()) / 2;
		//transportWizard.setLocation(Xloc, Yloc);		
		// Note: second window packing seems necessary to get window
		// to display components correctly.
		transportWizard.pack();
		transportWizard.setVisible(true);
	}
	/**
	 * Removes the transport wizard from the desktop.
	 */
	// 2014-12-23 Added disposeTransportWizard()
	public void disposeTransportWizard() {
		//System.out.println("MainDesktopPane : running disposeTransportWizard()");
		//transportWizard.setVisible(false);
		try { transportWizard.setClosed(true); }
		catch (java.beans.PropertyVetoException e) { }
		transportWizard.dispose();
	}
	/**
	 * Removes the marquee banner from the desktop.
	 */
	// 2014-12-30 Added disposeMarqueeBanner()
	public void disposeMarqueeBanner() {
		try { marqueeBanner.setClosed(true); }
		catch (java.beans.PropertyVetoException e) { }
		marqueeBanner.dispose();
	}
	
	/**
	 * Updates the look & feel of the Transport Wizard.
	 */
	// 2014-12-23 Added updateTransportWizardLF()
	void updateTransportWizardLF() {
	    if (transportWizard != null) {
	        SwingUtilities.updateComponentTreeUI(transportWizard);
	    }
	}
	
	/**
	 * Updates the look & feel for all tool windows.
	 */
	void updateToolWindowLF() {
		Iterator<ToolWindow> i = toolWindows.iterator();
		while (i.hasNext()) {
		    ToolWindow toolWindow = i.next();
			SwingUtilities.updateComponentTreeUI(toolWindow);
//			toolWindow.pack();
		}
	}


	/**
	 * Opens all initial windows based on UI configuration.
	 */
	void openInitialWindows() {
		UIConfig config = UIConfig.INSTANCE;
		if (config.useUIDefault()) {

			// Open default windows on desktop.

			// Open user guide tool.
			openToolWindow(GuideWindow.NAME);
			GuideWindow ourGuide = (GuideWindow) getToolWindow(GuideWindow.NAME);
			ourGuide.setURL(Msg.getString("doc.tutorial")); //$NON-NLS-1$

		} else {
			// Open windows in Z-order.
			List<String> windowNames = config.getInternalWindowNames();
			int num = windowNames.size();
			for (int x = 0; x < num; x++) {
				String highestZName = null;
				int highestZ = Integer.MIN_VALUE;
				Iterator<String> i = windowNames.iterator();
				while (i.hasNext()) {
					String name = i.next();
					boolean display = config.isInternalWindowDisplayed(name);
					String type = config.getInternalWindowType(name);
					if (UIConfig.UNIT.equals(type) && !Simulation.instance().isDefaultLoad()) {
						display = false;
					}
					if (display) {
						int zOrder = config.getInternalWindowZOrder(name);
						if (zOrder > highestZ) {
							highestZName = name;
							highestZ = zOrder;
						}
					}
				}
				if (highestZName != null)  {
					String type = config.getInternalWindowType(highestZName);
					if (UIConfig.TOOL.equals(type)) {
						openToolWindow(highestZName);
					} else if (UIConfig.UNIT.equals(type)) {
						Unit unit = Simulation.instance().getUnitManager().findUnit(highestZName);
						if (unit != null) {
							openUnitWindow(unit, true);
						}
					}
					windowNames.remove(highestZName);
				}
			}

			// Create unit bar buttons for closed unit windows.
			if (Simulation.instance().isDefaultLoad()) {
				Iterator<String> i = config.getInternalWindowNames().iterator();
				while (i.hasNext()) {
					String name = i.next();
					if (UIConfig.UNIT.equals(config.getInternalWindowType(name))) {
						if (!config.isInternalWindowDisplayed(name)) {
							Unit unit = Simulation.instance().getUnitManager().findUnit(name);
							if (unit != null) {
								mainWindow.createUnitButton(unit);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * creates a standardized empty border.
	 */
	public static EmptyBorder newEmptyBorder() {
		return new EmptyBorder(1,1,1,1);
	}

	public void setSettlementWindow(SettlementWindow settlementWindow) {
		this.settlementWindow = settlementWindow;
	}

	public TransportWizard getTransportWizard() {
		return transportWizard;
	}
	
	public AnnouncementWindow getAnnouncementWindow() {
		return announcementWindow;
	}
	
	
	
	public Settlement getSettlement() {
		return settlement;
	}
	
	public SettlementWindow getSettlementWindow() {
		return settlementWindow;
	}
	
	public boolean getIsTransportingBuilding() {
		return isTransportingBuilding;
	}
	
	
	
	// 2014-12-19 Added unitUpdate()
	public void unitUpdate(UnitEvent event) {
		UnitEventType eventType = event.getType();
		//System.out.println("MainDesktopPane : unitUpdate() " + eventType);
		Object target = event.getTarget();
		if (eventType == UnitEventType.START_BUILDING_PLACEMENT_EVENT) {
			//	|| eventType == UnitEventType.ADD_BUILDING_EVENT) {
			isTransportingBuilding = true;
			disposeTransportWizard();
			closeToolWindow(SettlementWindow.NAME);
			//System.out.println("MainDesktopPane : unitUpdate() START_BUILDING_PLACEMENT_EVENT is true");
			//isTransportingBuilding = true; // does not get updated for the next building unless ADD_BUILDING_EVENT is used
			building = (Building) target; // overwrite the dummy building object made by the constructor
			mgr = building.getBuildingManager();
			settlement = mgr.getSettlement();
			//System.out.println("MainDesktopPane : mgr is " + mgr);
			//System.out.println("MainDesktopPane : The settlement is " + settlement);
			// Select the relevant settlement
			settlementWindow.getMapPanel().setSettlement(settlement);
			// Open Settlement Map Tool
			openToolWindow(SettlementWindow.NAME);	
			getMainWindow().pauseSimulation();
			openTransportWizard(mgr);//, building); 
			isTransportingBuilding = false;
		}
		else if (eventType == UnitEventType.FINISH_BUILDING_PLACEMENT_EVENT) {
			//System.out.println("MainDesktopPane : FINISH_BUILDING_PLACEMENT_EVENT");
			getMainWindow().unpauseSimulation();
			disposeTransportWizard();
			isTransportingBuilding = false;
            //mgr.getResupply().deliverOthers();
            disposeAnnouncementWindow();
            //getMainWindow().pauseSimulation();
            //getMainWindow().unpauseSimulation();
            
		}	

	}

	
	@Override
	public void unitManagerUpdate(UnitManagerEvent event) {
		if (event.getUnit() instanceof Settlement) {
			
			//removeAllElements();
			UnitManager unitManager = Simulation.instance().getUnitManager();
			List<Settlement> settlements = new ArrayList<Settlement>(unitManager.getSettlements());
			Collections.sort(settlements);

			Iterator<Settlement> i = settlements.iterator();
			while (i.hasNext()) {
				i.next().removeUnitListener(this);			
			}
			Iterator<Settlement> j = settlements.iterator();
			while (j.hasNext()) {
				j.next().addUnitListener(this);
			}
		}
	}

}