/**
 * Mars Simulation Project
 * SettlementMapPanel.java
 * @version 3.04 2012-03-10
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.tool.settlement;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.ClockListener;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A panel for displaying the settlement map.
 */
public class SettlementMapPanel extends JPanel implements ClockListener {

    // Static members.
    public static final double DEFAULT_SCALE = 5D;
    public static final double MAX_SCALE = 55D;
    public static final double MIN_SCALE = 5D / 11D;
    private static final Color MAP_BACKGROUND = new Color(181, 95, 0);
    
    // Data members.
    private Settlement settlement;
    private double xPos;
    private double yPos;
    private double rotation;
    private double scale;
    private boolean showBuildingLabels;
    private boolean showConstructionLabels;
    private boolean showPersonLabels;
    private boolean showVehicleLabels;
    private List<SettlementMapLayer> mapLayers;
    private Map<Settlement, Person> selectedPerson;
    
    /**
     * A panel for displaying a settlement map.
     */
    public SettlementMapPanel() {
        // Use JPanel constructor.
        super();
        
        // Initialize data members.
        xPos = 0D;
        yPos = 0D;
        rotation = 0D;
        scale = DEFAULT_SCALE;
        settlement = null;
        showBuildingLabels = true;
        showConstructionLabels = true;
        showPersonLabels = true;
        showVehicleLabels = true;
        selectedPerson = new HashMap<Settlement, Person>();
        
        // Create map layers.
        mapLayers = new ArrayList<SettlementMapLayer>(5);
        mapLayers.add(new BackgroundTileMapLayer(this));
        mapLayers.add(new StructureMapLayer(this));
        mapLayers.add(new VehicleMapLayer(this));
        mapLayers.add(new PersonMapLayer(this));
        mapLayers.add(new LabelMapLayer(this));
        
        // Set preferred size.
        setPreferredSize(new Dimension(400, 400));
        
        // Set foreground and background colors.
        setOpaque(true);
        setBackground(MAP_BACKGROUND);
        setForeground(Color.WHITE);
        
        Simulation.instance().getMasterClock().addClockListener(this);
    }
    
    /**
     * Gets the settlement currently displayed.
     * @return settlement or null if none.
     */
    public Settlement getSettlement() {
        return settlement;
    }
    
    /**
     * Sets the settlement to display.
     * @param settlement the settlement.
     */
    public void setSettlement(Settlement settlement) {
        this.settlement = settlement;
        repaint();
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
     */
    public void selectPersonAt(int xPixel, int yPixel) {
    	
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
    		}
    		else {
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
        
        //long startTime = System.nanoTime();
        
        Graphics2D g2d = (Graphics2D) g;
        
        // Set graphics rendering hints.
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Display all map layers.
        Iterator<SettlementMapLayer> i = mapLayers.iterator();
        while (i.hasNext()) {
            i.next().displayLayer(g2d, settlement, xPos, yPos, getWidth(), getHeight(), rotation, scale);
        }
        
        //long endTime = System.nanoTime();
        //double timeDiff = (endTime - startTime) / 1000000D;
        //System.out.println("SMT paint time: " + (int) timeDiff + " ms");
    }
    
    /**
     * Cleans up the map panel for disposal.
     */
    public void destroy() {
        // Remove clock listener.
        Simulation.instance().getMasterClock().removeClockListener(this);

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