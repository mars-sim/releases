/**
 * Mars Simulation Project
 * MapDisplay.java
 * @version 2.70 2000-08-31
 * @author Scott Davis
 */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/** The MapDisplay class is a display component for the surface map of
 *  Mars in the project UI. It can show either the surface or
 *  topographical maps at a given point. It uses two SurfaceMap
 *  objects to display the maps.
 *
 *  It will recenter the map on the location of a mouse click, or will
 *  alternatively open a vehicle or settlement window if one of their
 *  icons is clicked.
 */
public class MapDisplay extends JComponent implements MouseListener, Runnable {

    private NavigatorWindow navWindow;        // Navigator Tool Window
    private SurfaceMap marsSurface;           // Surface image object
    private SurfaceMap topoSurface;           // Topographical image object
    private boolean wait;                     // True if map is in pause mode
    private Coordinates centerCoords;         // Spherical coordinates for center point of map
    private Thread showThread;                // Refresh thread
    private boolean topo;                     // True if in topographical mode, false if in real surface mode
    private boolean recreate;                 // True if surface needs to be regenerated
    private boolean labels;                   // True if units should display labels
    private Image mapImage;	              // Main image
    private Image vehicleSymbol;              // Real vehicle symbol
    private Image topoVehicleSymbol;          // Topograhical vehicle symbol
    private Image settlementSymbol;           // Real settlement symbol
    private Image topoSettlementSymbol;       // Topographical settlement symbol

    public MapDisplay(NavigatorWindow navWindow) {

	// Use JComponent constructor
	super();
		
	// Set component size
	setPreferredSize(new Dimension(300, 300));
	setMaximumSize(getPreferredSize());
	setMinimumSize(getPreferredSize());
	
	// Set mouse listener
	addMouseListener(this);

	// Create surface objects for both real and topographical modes
	marsSurface = new SurfaceMap("surface", this);
	topoSurface = new SurfaceMap("topo", this);

	// Initialize global variables
	centerCoords = new Coordinates(Math.PI / 2D, 0D);
	wait = false;
	recreate = true;
	topo = false;
	labels = true;
	this.navWindow = navWindow;
		
	// Load vehicle and settlement images
	vehicleSymbol = (Toolkit.getDefaultToolkit()).getImage("VehicleSymbol.gif"); 
	topoVehicleSymbol = (Toolkit.getDefaultToolkit()).getImage("VehicleSymbolBlack.gif");
	settlementSymbol = (Toolkit.getDefaultToolkit()).getImage("SettlementSymbol.gif");
	topoSettlementSymbol = (Toolkit.getDefaultToolkit()).getImage("SettlementSymbolBlack.gif");
	
	// Initially show real surface map
	showReal();
    }
	
    /** Change label display flag */
    public void setLabels(boolean labels) {
	this.labels = labels;
    }

    /** Displays real surface */
    public void showReal() {
	if (topo) {
	    wait = true;
	    recreate = true;
	}
	topo = false;
	showMap(centerCoords);
    }

    /** Displays topographical surface */
    public void showTopo() {
	if (!topo) {
	    wait = true;
	    recreate = true;
	}
	topo = true;
	showMap(centerCoords);
    }

    /** Displays surface with new coords, regenerating image if necessary */
    public void showMap(Coordinates newCenter) {

	if (!centerCoords.equals(newCenter)) {
	    wait = true;
	    recreate = true;
	    centerCoords.setCoords(newCenter);
	}
	start();
    }

    /** Starts display update thread, and creates a new one if necessary */
    public void start() {
	if ((showThread == null) || (!showThread.isAlive())) {
	    showThread = new Thread(this, "Map");
	    showThread.start();
	}
    }

    /** Display update thread runner */
    public void run() {

	// Endless refresh loop
	while(true) {
	    if (recreate) {
		// Regenerate surface if recreate is true, then display
		if (topo) {
		    topoSurface.drawMap(centerCoords);
		} else {
		    marsSurface.drawMap(centerCoords);
		}
		recreate = false;
		repaint();
	    } else {
	        // Pause for 2 seconds between display refreshs
		try { showThread.sleep(2000); }
		catch (InterruptedException e) {}
		repaint();
	    }
	}
    }

    /** Overrides paintComponent method.  Displays map image or
     *  "Preparing Map..." message. */
    public void paintComponent(Graphics g) {
	super.paintComponent(g);

	if (wait) {
	    // If in waiting mode, display "Preparing Map..."
	    if (mapImage != null) g.drawImage(mapImage, 0, 0, this);
	    g.setColor(Color.green);
	    String message = new String("Preparing Map...");
	    Font alertFont = new Font("TimesRoman", Font.BOLD, 30);
	    FontMetrics alertMetrics = getFontMetrics(alertFont);
	    int Height = alertMetrics.getHeight();
	    int Width = alertMetrics.stringWidth(message);
	    int x = (300 - Width) / 2;
	    int y = (300 + Height) / 2;
	    g.setFont(alertFont);
	    g.drawString(message, x, y);
	    wait = false;
	} else { 
	    // Paint black background
	    g.setColor(Color.black);
	    g.fillRect(0, 0, 300, 300);
	    
	    // Paint topo or real surface image
	    boolean image_done = false;
	    SurfaceMap tempMap;
	    if (topo) tempMap = topoSurface;
	    else tempMap = marsSurface;
			
	    if (tempMap.imageDone) {
		image_done = true;
		mapImage = tempMap.getMapImage();
		g.drawImage(mapImage, 0, 0, this);
	    }

	    // Set unit label color
	    if (topo) {
		g.setColor(Color.black);
	    } else {
		g.setColor(Color.green);
	    }

	    // Draw a vehicle symbol for each moving vehicle within the viewing map
	    g.setFont(new Font("Helvetica", Font.PLAIN, 9));
	
	    UnitInfo[] vehicleInfo = navWindow.getMovingVehicleInfo();
			
	    int counter = 0;
			
	    for (int x=0; x < vehicleInfo.length; x++) {
		if (centerCoords.getAngle(vehicleInfo[x].getCoords()) < .48587D) {
		    IntPoint rectLocation = getUnitRectPosition(vehicleInfo[x].getCoords());
		    IntPoint imageLocation = getUnitDrawLocation(rectLocation, vehicleSymbol);
		    if (topo) {
			g.drawImage(topoVehicleSymbol, imageLocation.getiX(), imageLocation.getiY(), this);
		    } else {
			g.drawImage(vehicleSymbol, imageLocation.getiX(), imageLocation.getiY(), this);
		    }
		    
		    if (labels) {
			IntPoint labelLocation = getLabelLocation(rectLocation, vehicleSymbol);
			g.drawString(vehicleInfo[x].getName(), labelLocation.getiX(), labelLocation.getiY());	
		    }

		    counter++;
		}
	    }
			
	    // Draw a settlement symbol for each settlement within the viewing map
	    g.setFont(new Font("Helvetica", Font.PLAIN, 12));

	    UnitInfo[] settlementInfo = navWindow.getSettlementInfo();

	    for (int x=0; x < settlementInfo.length; x++) {
		if (centerCoords.getAngle(settlementInfo[x].getCoords()) < .48587D) {
		    IntPoint rectLocation = getUnitRectPosition(settlementInfo[x].getCoords());
		    IntPoint imageLocation = getUnitDrawLocation(rectLocation, settlementSymbol);
		    if (topo) {
			g.drawImage(topoSettlementSymbol, imageLocation.getiX(), imageLocation.getiY(), this);
		    } else {
			g.drawImage(settlementSymbol, imageLocation.getiX(), imageLocation.getiY(), this);
		    }
		    if (labels) {
			IntPoint labelLocation = getLabelLocation(rectLocation, settlementSymbol);
			g.drawString(settlementInfo[x].getName(), labelLocation.getiX(), labelLocation.getiY());
		    }
		}
	    }
	}
    }

    /** MouseListener methods overridden. Perform appropriate action
     *  on mouse release. */
    public void mouseReleased(MouseEvent event) { 

	Coordinates clickedPosition = centerCoords.convertRectToSpherical((double) event.getX() - 149D,
									  (double) event.getY() - 149D);
	boolean unitsClicked = false;
	
	UnitInfo[] movingVehicleInfo = navWindow.getMovingVehicleInfo();
		
	for (int x=0; x < movingVehicleInfo.length; x++) {
	    if (movingVehicleInfo[x].getCoords().getDistance(clickedPosition) < 40D) {
		navWindow.openUnitWindow(movingVehicleInfo[x].getID());
		unitsClicked = true;
	    }
	}
		
	UnitInfo[] settlementInfo = navWindow.getSettlementInfo();
		
	for (int x=0; x < settlementInfo.length; x++) {
	    if (settlementInfo[x].getCoords().getDistance(clickedPosition) < 90D) {
		navWindow.openUnitWindow(settlementInfo[x].getID());
		unitsClicked = true;
	    }
	}
		
	if (!unitsClicked) navWindow.updateCoords(clickedPosition);
    }

    public void mousePressed(MouseEvent event) {}
    public void mouseClicked(MouseEvent event) {}
    public void mouseEntered(MouseEvent event) {}
    public void mouseExited(MouseEvent event) {}
	
    /** Returns unit x, y position on map panel */
    private IntPoint getUnitRectPosition(Coordinates unitCoords) {
	
	double rho = 1440D / Math.PI;
	int half_map = 720;
	int low_edge = half_map - 150;
	
	return Coordinates.findRectPosition(unitCoords, centerCoords, rho, half_map, low_edge);
    }
	
    /** Returns unit image draw position on map panel */
    private IntPoint getUnitDrawLocation(IntPoint unitPosition, Image unitImage) {

	return new IntPoint(unitPosition.getiX() - Math.round(unitImage.getWidth(this) / 2),
			    unitPosition.getiY() - Math.round(unitImage.getHeight(this) / 2));
    }
    
    /** Returns label draw postion on map panel */
    private IntPoint getLabelLocation(IntPoint unitPosition, Image unitImage) {
		
	return new IntPoint(unitPosition.getiX() + Math.round(unitImage.getWidth(this) / 2) + 10,
			    unitPosition.getiY() + Math.round(unitImage.getHeight(this) / 2));
    }
}
