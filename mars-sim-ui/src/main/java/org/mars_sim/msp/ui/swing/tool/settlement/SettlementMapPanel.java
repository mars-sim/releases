/**
 * Mars Simulation Project
 * SettlementMapPanel.java
 * @version 3.07 2014-12-26
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.settlement;

//import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.time.ClockListener;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.ui.swing.MainDesktopPane;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
//import java.util.logging.Logger;

/**
 * A panel for displaying the settlement map.
 */
public class SettlementMapPanel
extends JPanel
implements ClockListener {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// default logger.
	//private static Logger logger = Logger.getLogger(SettlementMapPanel.class.getName());

	// Static members.
	//public static final double DEFAULT_SCALE = 5D;
	public static final double DEFAULT_SCALE = 10D;	
	public static final double MAX_SCALE = 55D;
	public static final double MIN_SCALE = 5D / 11D;
	private static final Color MAP_BACKGROUND = new Color(181, 95, 0);

	// Data members.		
	private double xPos;
	private double yPos;
	private double rotation;
	private double scale;
	
	/** Last X mouse drag position. */
	private int xLast;
	/** Last Y mouse drag position. */
	private int yLast;
	
	private boolean showBuildingLabels;
	private boolean showConstructionLabels;
	private boolean showPersonLabels;
	private boolean showVehicleLabels;
		
	private List<SettlementMapLayer> mapLayers;
	private Map<Settlement, Person> selectedPerson;
	
	private Building building;
	private SettlementWindow settlementWindow;
	private Settlement settlement;
	private PopUpUnitMenu menu;
	//private MainDesktopPane desktop; // may be needed 
	//private SettlementTransparentPanel transparentPanel; // may be needed

	/** Constructor 1
	 * 	A panel for displaying a settlement map.
	 */
	public SettlementMapPanel(final MainDesktopPane desktop, final SettlementWindow settlementWindow) {
		super();
		this.settlementWindow = settlementWindow;
		//this.desktop = desktop;

		setLayout(new BorderLayout());
		//setLayout(null); //new FlowLayout());
		// Initialize data members.
		xPos = 0D;
		yPos = 0D;
		rotation = 0D;
		scale = DEFAULT_SCALE;
		//settlement = null;
		showBuildingLabels = false;
		showConstructionLabels = false;
		showPersonLabels = false;
		showVehicleLabels = false;
		selectedPerson = new HashMap<Settlement, Person>();
		
		// Create map layers.
		mapLayers = new ArrayList<SettlementMapLayer>(5);
		mapLayers.add(new BackgroundTileMapLayer(this));
		mapLayers.add(new StructureMapLayer(this));
		mapLayers.add(new VehicleMapLayer(this));
		mapLayers.add(new PersonMapLayer(this));
		mapLayers.add(new LabelMapLayer(this));

		// Set foreground and background colors.
		setOpaque(false);
		setBackground(MAP_BACKGROUND);
		setForeground(Color.ORANGE);

		Simulation.instance().getMasterClock().addClockListener(this);
		
		// 2015-01-16 Added detectMouseMovement() after refactoring
		detectMouseMovement();
		
		// TODO: will initializing this in SettlementWindow be faster?
	    //SwingUtilities.invokeLater(new Runnable(){
	    //    public void run()  {
		//transparentPanel = 
		new SettlementTransparentPanel(desktop, this);
	    //    } });
        setVisible(true);
	}
	
	/** Constructor 2
	 *  A panel for initializing the display of a building svg image.
	 */
	// 2014-11-04 Added this constructor for loading an svg image
	// Called by BuildingPanel.java
	public SettlementMapPanel(Settlement settlement, Building building) {
		super();

		// Initialize data members.
		xPos = 0D;
		yPos = 0D;
		rotation = 0D;
		scale = DEFAULT_SCALE;
		this.settlement = settlement;
		this.building = building;
		showBuildingLabels = false;
		showConstructionLabels = false;
		showPersonLabels = false;
		showVehicleLabels = false;
		selectedPerson = new HashMap<Settlement, Person>();

		mapLayers = new ArrayList<SettlementMapLayer>(1);
		StructureMapLayer layer = new StructureMapLayer(this);		 
		mapLayers.add(layer);

		// Set preferred size.
		setPreferredSize(new Dimension(100, 100));

		// Set foreground and background colors.
		setOpaque(true);
		setBackground(Color.WHITE);
		setForeground(Color.WHITE);

		Simulation.instance().getMasterClock().addClockListener(this);
		
		repaint();	
	}

	public void detectMouseMovement() {		
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evt) {
				// Set initial mouse drag position.
				xLast = evt.getX();
				yLast = evt.getY();
			}
	
			@Override
			public void mouseClicked(MouseEvent evt) {
				// Select person if clicked on.
				selectPersonAt(evt.getX(), evt.getY());
			}
	
		});
	
		//2014-11-22 Added PopClickListener() to detect mouse right click
		class PopClickListener extends MouseAdapter {
		    public void mousePressed(MouseEvent evt){
				 if (evt.isPopupTrigger()) {
					 doPop(evt);
				 }
		    }
	
		    public void mouseReleased(MouseEvent evt){
				 if (evt.isPopupTrigger()) {
					 doPop(evt);
				 }
					xLast = evt.getX();
					yLast = evt.getY();
		    }
		    //2015-01-14 Added vehicle detection
		    private void doPop(final MouseEvent evt){
		    	final Building building = selectBuildingAt(evt.getX(), evt.getY());
		    	final Vehicle vehicle = selectVehicleAt(evt.getX(), evt.getY());
		    	final Person person = selectPersonAt(evt.getX(), evt.getY());
	
		    	// if NO building is selected, do NOT call popup menu
		    	if (building != null || vehicle != null || person != null) {
		    		
		    	    SwingUtilities.invokeLater(new Runnable(){
		    	        public void run()  {
	    	        		// 2015-01-16 Deconflict cases by the virtue of the if-else order below 
		    	        	// when one or more are detected
		    	        	if (person != null)
		    	        		menu = new PopUpUnitMenu(settlementWindow, person);
		    	        	else if (vehicle != null)
		    	        		menu = new PopUpUnitMenu(settlementWindow, vehicle);
		    	        	else if (building != null)
		    	        		menu = new PopUpUnitMenu(settlementWindow, building);
	
		    	        	menu.show(evt.getComponent(), evt.getX(), evt.getY());
		    	        } });
		        }	
		    }
		}
		addMouseListener(new PopClickListener());
		
		
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent evt) {
				// Move map center based on mouse drag difference.
				double xDiff = evt.getX() - xLast;
				double yDiff = evt.getY() - yLast;
				moveCenter(xDiff, yDiff);
				xLast = evt.getX();
				yLast = evt.getY();
			}
		});

	}
		
	/**
	 * Gets the settlement currently displayed.
	 * @return settlement or null if none.
	 */
	public Settlement getSettlement() {
		return settlement;
	}

	/**
	 * Gets the SettlementWindow class.
	 * @return settlementWindow or null if none.
	 */
	public SettlementWindow getSettlementWindow() {
		return settlementWindow;
	}	

	/**
	 * Sets the settlement to display.
	 * @param settlement the settlement.
	 */
	public void setSettlement(Settlement newSettlement) {
		if (newSettlement != settlement) {
			// TODO : inform SettlementTransparentPanel to update settlement ?
			this.settlement = newSettlement;
			repaint();
		}
	}

	/**
	 * Gets the map scale.
	 * @return scale (pixels per meter).
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * Sets the map scale.
	 * @param scale (pixels per meter).
	 */
	public void setScale(double scale) {
		this.scale = scale;
		repaint();
	}

	/**
	 * Gets the map rotation.
	 * @return rotation (radians).
	 */
	public double getRotation() {
		return rotation;
	}

	/**
	 * Sets the map rotation.
	 * @param rotation (radians).
	 */
	public void setRotation(double rotation) {
		this.rotation = rotation;
		repaint();
	}

	/**
	 * Resets the position, scale and rotation of the map.
	 * Separate function that only uses one repaint.
	 */
	public void reCenter() {
		xPos = 0D;
		yPos = 0D;
		setRotation(0D);
		scale = DEFAULT_SCALE;
		repaint();
	}

	/**
	 * Moves the center of the map by a given number of pixels.
	 * @param xDiff the X axis pixels.
	 * @param yDiff the Y axis pixels.
	 */
	public void moveCenter(double xDiff, double yDiff) {

		xDiff /= scale;
		yDiff /= scale;

		// Correct due to rotation of map.
		double realXDiff = (Math.cos(rotation) * xDiff) + (Math.sin(rotation) * yDiff);
		double realYDiff = (Math.cos(rotation) * yDiff) - (Math.sin(rotation) * xDiff);

		xPos += realXDiff;
		yPos += realYDiff;
		repaint();
	}

	/**
	 * Selects a person if any person is at the given x and y pixel position.
	 * @param xPixel the x pixel position on the displayed map.
	 * @param yPixel the y pixel position on the displayed map.
	 * @return selectedPerson;
	 */
	public Person selectPersonAt(int xPixel, int yPixel) {

		Point.Double settlementPosition = convertToSettlementLocation(xPixel, yPixel);
		double range = 6D / scale;
		Person selectedPerson = null;

		Iterator<Person> i = PersonMapLayer.getPeopleToDisplay(settlement).iterator();
		while (i.hasNext()) {
			Person person = i.next();
			double distanceX = person.getXLocation() - settlementPosition.getX();
			double distanceY = person.getYLocation() - settlementPosition.getY();
			double distance = Math.hypot(distanceX, distanceY);
			if (distance <= range) {
				selectedPerson = person;
			}
		}

		if (selectedPerson != null) {
			selectPerson(selectedPerson);
			repaint();
		}
		return selectedPerson;
	}
	
	/**
	 * Selects a building 
	 * @param xPixel the x pixel position on the displayed map.
	 * @param yPixel the y pixel position on the displayed map.
	 * @return selectedBuilding
	 */
	// 2014-11-22 Added building selection
	public Building selectBuildingAt(int xPixel, int yPixel) {
		Point.Double settlementPosition = convertToSettlementLocation(xPixel, yPixel);
		// Note: 20D is an arbitrary number (by experiment) that 
		// gives an reasonable click detection area
		//double range2 = 3D / scale;
		//logger.info("scale is "+ scale);
		//logger.info("range2 is "+ range2);

		Building selectedBuilding = null;

		//int i = 0;
		Iterator<Building> j = returnBuildingList(settlement).iterator();
		while (j.hasNext()) {
			Building building = j.next();
			// NOTE: Since without knowledge of the rotation orientation
			// of the building, we take the smaller value of width and length
			double width =building.getWidth(); // width is on y-axis ?
			double length = building.getLength(); // length is on x-axis ?
			double newRange;
			if (width < length)
				newRange =  width/2.0;
			else newRange = length/2.0;
				
			double x = building.getXLocation();
			double y = building.getYLocation();
			
			double distanceX = x - settlementPosition.getX();
			double distanceY = y - settlementPosition.getY();
			double distance = Math.hypot(distanceX, distanceY);
			if (distance <= newRange) {	
				//logger.info(i +", width is "+ width );
				//logger.info(i +", length is "+ length);
				//logger.info(i +", distanceX is "+ distanceX);
				//logger.info(i +", distanceY is "+ distanceY);
				//logger.info(i +", x is "+ x);
				//logger.info(i +", y is "+ y);				
				//logger.info(i +", distance is "+ distance);
				//logger.info(i +", newRange is "+ newRange);				
				selectedBuilding = building;
			}
			//i++;
		}
		return selectedBuilding;
	}
	
	// 2014-11-22 Added returnBuildingList
	public static List<Building> returnBuildingList(Settlement settlement) {

		List<Building> result = new ArrayList<Building>();
		if (settlement != null) {
			//Iterator<Building> i = Simulation.instance().getUnitManager().getPeople().iterator();
		    Iterator<Building> i = settlement.getBuildingManager().getBuildings().iterator();
			while (i.hasNext()) {
				Building building = i.next();
						result.add(building);
			}
		}
		return result;
	}


	/**
	 * Selects a vehicle 
	 * @param xPixel the x pixel position on the displayed map.
	 * @param yPixel the y pixel position on the displayed map.
	 * @return selectedVehicle
	 */
	// 2015-01-14 Added selectVehicleAt()
	public Vehicle selectVehicleAt(int xPixel, int yPixel) {
		Point.Double settlementPosition = convertToSettlementLocation(xPixel, yPixel);

		Vehicle selectedVehicle = null;

		Iterator<Vehicle> j = returnVehicleList(settlement).iterator();
		while (j.hasNext()) {
			Vehicle vehicle = j.next();
			double width =vehicle.getWidth(); // width is on y-axis ?
			double length = vehicle.getLength(); // length is on x-axis ?
			double newRange;
			if (width < length)
				newRange =  width/2.0;
			else newRange = length/2.0;
				
			double x = vehicle.getXLocation();
			double y = vehicle.getYLocation();
			
			double distanceX = x - settlementPosition.getX();
			double distanceY = y - settlementPosition.getY();
			double distance = Math.hypot(distanceX, distanceY);
			if (distance <= newRange) {	
				//logger.info(i +", width is "+ width );
				//logger.info(i +", length is "+ length);
				//logger.info(i +", distanceX is "+ distanceX);
				//logger.info(i +", distanceY is "+ distanceY);
				//logger.info(i +", x is "+ x);
				//logger.info(i +", y is "+ y);				
				//logger.info(i +", distance is "+ distance);
				//logger.info(i +", newRange is "+ newRange);				
				selectedVehicle = vehicle;
			}
			//i++;
		}
		return selectedVehicle;
	}
	
	// // 2015-01-14 Added returnVehicleList()
	public static List<Vehicle> returnVehicleList(Settlement settlement) {

		List<Vehicle> result = new ArrayList<Vehicle>();
		if (settlement != null) {
		    Iterator<Vehicle> i = settlement.getParkedVehicles().iterator();
			while (i.hasNext()) {
				Vehicle vehicle = i.next();
						result.add(vehicle);
			}
		}
		return result;
	}
	/**
	 * Selects a person on the map.
	 * @param person the selected person.
	 */
	public void selectPerson(Person person) {
		if ((settlement != null) && (person != null)) {
			Person currentlySelected = selectedPerson.get(settlement);
			if (person.equals(currentlySelected)) {
				selectedPerson.put(settlement, null);
			} else {
				selectedPerson.put(settlement, person);
			}
		}
	}

	/**
	 * Get the selected person for the current settlement.
	 * @return the selected person.
	 */
	public Person getSelectedPerson() {
		Person result = null;
		if (settlement != null) {
			result = selectedPerson.get(settlement);
		}
		return result;
	}


	/**
	 * Selects a building on the map.
	 * @param building the selected building.

	public void selectBuilding(Building building) {
		logger.info("selectBuilding() : building is " + building.getNickName());
		if ((settlement != null) && (building != null)) {
			Building currentlySelected = selectedBuilding.get(settlement);
			// Toggle on/off the selected building
			//if (building.equals(currentlySelected)) {
			//	selectedBuilding.put(settlement, null);
			//} else {
				selectedBuilding.put(settlement, building);
			//}
		}
	}
	 */
	/**
	 * Get the selected building for the current settlement.
	 * @return the selected building.
	
	public Building getSelectedBuilding() {
		Building result = null;
		if (settlement != null) {
			result = selectedBuilding.get(settlement);
		}
		logger.info("getSelectedBuilding() : Selected Building is " + building.getNickName());
		return result;
	}
	 */
	/**
	 * Convert a pixel X,Y position to a X,Y (meter) position local to the settlement in view.
	 * @param xPixel the pixel X position.
	 * @param yPixel the pixel Y position.
	 * @return the X,Y settlement position.
	 */
	public Point.Double convertToSettlementLocation(int xPixel, int yPixel) {

		Point.Double result = new Point.Double(0D, 0D);

		double xDiff1 = (getWidth() / 2) - xPixel;
		double yDiff1 = (getHeight() / 2) - yPixel;

		double xDiff2 = xDiff1 / scale;
		double yDiff2 = yDiff1 / scale;

		// Correct due to rotation of map.
		double xDiff3 = (Math.cos(rotation) * xDiff2) + (Math.sin(rotation) * yDiff2);
		double yDiff3 = (Math.cos(rotation) * yDiff2) - (Math.sin(rotation) * xDiff2);

		double newXPos = xPos + xDiff3;
		double newYPos = yPos + yDiff3;

		result.setLocation(newXPos, newYPos);

		return result;
	}

	/**
	 * Checks if building labels should be displayed.
	 * @return true if building labels should be displayed.
	 */
	public boolean isShowBuildingLabels() {
		return showBuildingLabels;
	}

	/**
	 * Sets if building labels should be displayed.
	 * @param showLabels true if building labels should be displayed.
	 */
	public void setShowBuildingLabels(boolean showLabels) {
		this.showBuildingLabels = showLabels;
		repaint();
	}

	/**
	 * Checks if construction site labels should be displayed.
	 * @return true if construction site labels should be displayed.
	 */
	public boolean isShowConstructionLabels() {
		return showConstructionLabels;
	}

	/**
	 * Sets if construction site labels should be displayed.
	 * @param showLabels true if construction site labels should be displayed.
	 */
	public void setShowConstructionLabels(boolean showLabels) {
		this.showConstructionLabels = showLabels;
		repaint();
	}

	/**
	 * Checks if person labels should be displayed.
	 * @return true if person labels should be displayed.
	 */
	public boolean isShowPersonLabels() {
		return showPersonLabels;
	}

	/**
	 * Sets if person labels should be displayed.
	 * @param showLabels true if person labels should be displayed.
	 */
	public void setShowPersonLabels(boolean showLabels) {
		this.showPersonLabels = showLabels;
		repaint();
	}

	/**
	 * Checks if vehicle labels should be displayed.
	 * @return true if vehicle labels should be displayed.
	 */
	public boolean isShowVehicleLabels() {
		return showVehicleLabels;
	}

	/**
	 * Sets if vehicle labels should be displayed.
	 * @param showLabels true if vehicle labels should be displayed.
	 */
	public void setShowVehicleLabels(boolean showLabels) {
		this.showVehicleLabels = showLabels;
		repaint();
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

//		long startTime = System.nanoTime();

		Graphics2D g2d = (Graphics2D) g;

		// Set graphics rendering hints.
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		// Display all map layers.
		Iterator<SettlementMapLayer> i = mapLayers.iterator();
		while (i.hasNext()) {
			// 2014-11-04 Added building parameter
			i.next().displayLayer(g2d, settlement, building, xPos, yPos, getWidth(), getHeight(), rotation, scale);
				//System.out.println("xPos is " + xPos);
				//System.out.println("yPos is " + yPos);
				//System.out.println("rotation is " +rotation);
				//System.out.println("scale is " + rotation);
		}

//		long endTime = System.nanoTime();
//		double timeDiff = (endTime - startTime) / 1000000D;
//		System.out.println("SMT paint time: " + (int) timeDiff + " ms");
	}
	
	/**
	 * Cleans up the map panel for disposal.
	 */
	public void destroy() {
		// Remove clock listener.
		Simulation.instance().getMasterClock().removeClockListener(this);

		menu = null;
		settlement = null;
		selectedPerson = null;
		//selectedBuilding = null;
		building = null;
		//desktop = null;
		settlementWindow = null;
		//transparentPanel = null;
		// Destroy all map layers.
		Iterator<SettlementMapLayer> i = mapLayers.iterator();
		while (i.hasNext()) {
			i.next().destroy();
		}
	}

	@Override
	public void clockPulse(double time) {
		// Repaint map panel with each clock pulse.
		repaint();
	}

	@Override
	public void pauseChange(boolean isPaused) {
		// Do nothing
	}
}