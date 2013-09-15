/**
 * Mars Simulation Project
 * PersonMapLayer.java
 * @version 3.04 2013-03-10
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.settlement;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;

/**
 * A settlement map layer for displaying people.
 */
public class PersonMapLayer implements SettlementMapLayer {

    // Static members
    private static final Color PERSON_COLOR = new Color(0, 255, 255);
    private static final Color PERSON_OUTLINE_COLOR = new Color(0, 0, 0, 190);
    private static final Color SELECTED_COLOR = new Color(255, 255, 255);
    private static final Color SELECTED_OUTLINE_COLOR = new Color(0, 0, 0, 190);
    
    // Data members
    private SettlementMapPanel mapPanel;
    
    /**
     * Gets a list of people to display on a settlement map.
     * @param settlement the settlement
     * @return list of people to display.
     */
    public static List<Person> getPeopleToDisplay(Settlement settlement) {
    	
    	List<Person> result = new ArrayList<Person>();
    	
    	if (settlement != null) {
            Iterator<Person> i = Simulation.instance().getUnitManager().getPeople().iterator();
            while (i.hasNext()) {
                Person person = i.next();
                
                // Only select living people.
                if (!person.getPhysicalCondition().isDead()) {

                    // Select a person that is at the settlement location.
                    Coordinates settlementLoc = settlement.getCoordinates();
                    Coordinates personLoc = person.getCoordinates();
                    if (personLoc.equals(settlementLoc)) {
                        result.add(person);
                    }
                }
            }
    	}
    	
    	return result;
    }
    
    /**
     * Constructor
     * @param mapPanel the settlement map panel.
     */
    public PersonMapLayer(SettlementMapPanel mapPanel) {
        
        // Initialize data members.
        this.mapPanel = mapPanel;
    }
    
    @Override
    public void displayLayer(Graphics2D g2d, Settlement settlement,
            double xPos, double yPos, int mapWidth, int mapHeight,
            double rotation, double scale) {
            
        // Save original graphics transforms.
        AffineTransform saveTransform = g2d.getTransform();

        // Get the map center point.
        double mapCenterX = mapWidth / 2D;
        double mapCenterY = mapHeight / 2D;

        // Translate map from settlement center point.
        g2d.translate(mapCenterX + (xPos * scale), mapCenterY + (yPos * scale));

        // Rotate map from North.
        g2d.rotate(rotation, 0D - (xPos * scale), 0D - (yPos * scale));

        // Draw all people.
        drawPeople(g2d, settlement);
        
        // Restore original graphic transforms.
        g2d.setTransform(saveTransform);
    }
    
    /**
     * Draw people at a settlement.
     * @param g2d the graphics context.
     * @param settlement the settlement to draw people at.
     */
    private void drawPeople(Graphics2D g2d, Settlement settlement) {
        
    	List<Person> people = getPeopleToDisplay(settlement);
    	Person selectedPerson = mapPanel.getSelectedPerson();
    	
    	// Draw all people except selected person.
    	Iterator<Person> i = people.iterator();
    	while (i.hasNext()) {
    		Person person = i.next();
    		if (!person.equals(selectedPerson)) {
    			drawPerson(g2d, person, PERSON_COLOR, PERSON_OUTLINE_COLOR);
    		}
    	}
    	
    	// Draw selected person.
    	if (people.contains(selectedPerson)) {
    		drawPerson(g2d, selectedPerson, SELECTED_COLOR, SELECTED_OUTLINE_COLOR);
    	}
    }
    
    /**
     * Draw a person at a settlement.
     * @param g2d the graphics context.
     * @param person the person to draw.
     */
    private void drawPerson(Graphics2D g2d, Person person, Color iconColor, Color outlineColor) {
        
        if (person != null) {
            
            // Save original graphics transforms.
            AffineTransform saveTransform = g2d.getTransform();
            
            double circleDiameter = 10D;
            double centerX = circleDiameter / 2D;
            double centerY = circleDiameter / 2D;
            
            double translationX = (-1D * person.getXLocation() * mapPanel.getScale() - centerX);
            double translationY = (-1D * person.getYLocation() * mapPanel.getScale() - centerY);
            
            // Apply graphic transforms for label.
            AffineTransform newTransform = new AffineTransform(saveTransform);
            newTransform.translate(translationX, translationY);
            newTransform.rotate(mapPanel.getRotation() * -1D, centerX, centerY);
            g2d.setTransform(newTransform);
            
            // Set color outline color.
            g2d.setColor(outlineColor);
            
            // Draw outline circle.
            g2d.fillOval(0,  0, 11, 11);
            
            // Set circle color.
            g2d.setColor(iconColor);
            
            // Draw circle.
            g2d.fillOval(0, 0, 10, 10);
            
            // Restore original graphic transforms.
            g2d.setTransform(saveTransform);
        }
    }
    
    @Override
    public void destroy() {
        // Do nothing
    }
}