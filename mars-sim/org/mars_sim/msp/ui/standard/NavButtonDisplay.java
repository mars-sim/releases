/**
 * Mars Simulation Project
 * NavButtonDisplay.java
 * @version 2.71 2000-10-22
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.standard;

import org.mars_sim.msp.simulation.Coordinates;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** The NavButtonDisplay class is a component that displays and
 *  implements the behavior of the navigation buttons which control
 *  the globe and map.
 */
public class NavButtonDisplay extends JComponent implements MouseListener {

    // Constant data members
    static final double degree = Math.PI / 180D; // Circular degree unit

    // Data members
    private NavigatorWindow parentNavigator; // Parent NavigatorWindow
    private int buttonLight; // Button currently lit, -1 otherwise
    private Image navMain; // Unlit buttons image
    private Coordinates centerCoords; // Current coordinates
    private final Image[] lightUpButtons; // Lit button images
    private final Rectangle[] hotSpots; // Regions encompassing images

    /** Constructs a NavButtonDisplay object
     *  @param parentNavigator the navigator window pane
     */
    public NavButtonDisplay(NavigatorWindow parentNavigator) {

        // Set component size
        setPreferredSize(new Dimension(150, 150));
        setMaximumSize(getPreferredSize());
        setMinimumSize(getPreferredSize());

        // Set mouse listener
        addMouseListener(this);

        // Initialize globals
        centerCoords = new Coordinates(Math.PI / 2D, 0D);
        buttonLight = -1;
        lightUpButtons = new Image[9];
        this.parentNavigator = parentNavigator;

        // Load Button Images
        navMain = ImageLoader.getImage("NavMain.gif");
        lightUpButtons[0] = ImageLoader.getImage("NavMainPlus.gif");
        lightUpButtons[1] = ImageLoader.getImage("NavNorth.gif");
        lightUpButtons[2] = ImageLoader.getImage("NavSouth.gif");
        lightUpButtons[3] = ImageLoader.getImage("NavEast.gif");
        lightUpButtons[4] = ImageLoader.getImage("NavWest.gif");
        lightUpButtons[5] = ImageLoader.getImage("NavNorthPlus.gif");
        lightUpButtons[6] = ImageLoader.getImage("NavSouthPlus.gif");
        lightUpButtons[7] = ImageLoader.getImage("NavEastPlus.gif");
        lightUpButtons[8] = ImageLoader.getImage("NavWestPlus.gif");

        MediaTracker mtrack = new MediaTracker(this);

        mtrack.addImage(navMain, 0);
        for (int x = 0; x < 9; x++) {
            mtrack.addImage(lightUpButtons[x], x + 1);
        }

        try { mtrack.waitForAll(); }
        catch (InterruptedException e) {
            System.out.println("NavButtonDisplay Media Tracker Error " + e);
        }

        // Set hot spots for mouse clicks
        hotSpots = new Rectangle[9];
        hotSpots[0] = new Rectangle(45, 45, 60, 60);
        hotSpots[1] = new Rectangle(38, 16, 74, 21);
        hotSpots[2] = new Rectangle(38, 112, 74, 21);
        hotSpots[3] = new Rectangle(113, 38, 21, 74);
        hotSpots[4] = new Rectangle(17, 38, 21, 74);
        hotSpots[5] = new Rectangle(60, 0, 29, 14);
        hotSpots[6] = new Rectangle(60, 134, 29, 14);
        hotSpots[7] = new Rectangle(135, 61, 15, 28);
        hotSpots[8] = new Rectangle(0, 61, 15, 28);
    }

    /** Update coordinates
     *  @param newCenter the new center position
     */
    public void updateCoords(Coordinates newCenter) {
        centerCoords.setCoords(newCenter);
    }

    /** Override paintComponent method. Paints buttons and lit button
     *  @param g graphics context
     */
    public void paintComponent(Graphics g) {

        // paint black background
        g.setColor(Color.black);
        g.fillRect(0, 0, 150, 150);

        // draw main button image
        g.drawImage(navMain, 0, 0, this);

        // draw lit button over top
        if (buttonLight >= 0) {
            g.drawImage(lightUpButtons[buttonLight], 0, 0, this);
        }
    }

    // MouseListener methods overridden

    /** Light navigation button on mouse press */
    public void mousePressed(MouseEvent event) {
        lightButton(event.getX(), event.getY());
    }

    /** Perform appropriate action on mouse release. */
    public void mouseReleased(MouseEvent event) {

        unlightButtons();

        // Use Image Map Technique to Determine Which Button was Selected
        int spot = findHotSpot(event.getX(), event.getY());

        // Results Based on Button Selected
        switch (spot) {
        case 0: // Zoom Button
            parentNavigator.updateCoords(centerCoords);
            break;
        case 1: // Inner Top Arrow
            centerCoords.setPhi(centerCoords.getPhi() - (5D * degree));
            if (centerCoords.getPhi() < 0D)
                centerCoords.setPhi(0D);
            break;
        case 2: // Inner Bottom Arrow
            centerCoords.setPhi(centerCoords.getPhi() + (5D * degree));
            if (centerCoords.getPhi() > Math.PI)
                centerCoords.setPhi(Math.PI);
            break;
        case 3: // Inner Right Arrow
            centerCoords.setTheta(centerCoords.getTheta() + (5D * degree));
            if (centerCoords.getTheta() > (2D * Math.PI))
                centerCoords.setTheta(centerCoords.getTheta() - (2D * Math.PI));
            break;
        case 4: // Inner Left Arrow
            centerCoords.setTheta(centerCoords.getTheta() - (5D * degree));
            if (centerCoords.getTheta() < 0D)
                centerCoords.setTheta(centerCoords.getTheta() + (2D * Math.PI));
            break;
        case 5: // Outer Top Arrow
            centerCoords.setPhi(centerCoords.getPhi() - (30D * degree));
            if (centerCoords.getPhi() < 0D)
                centerCoords.setPhi(0D);
            break;
        case 6: // Outer Bottom Arrow
            centerCoords.setPhi(centerCoords.getPhi() + (30D * degree));
            if (centerCoords.getPhi() > Math.PI)
                centerCoords.setPhi(Math.PI);
            break;
        case 7: // Outer Right Arrow
            centerCoords.setTheta(centerCoords.getTheta() + (30D * degree));
            if (centerCoords.getTheta() >= (2D * Math.PI))
                centerCoords.setTheta(centerCoords.getTheta() - (2D * Math.PI));
            break;
        case 8: // Outer Left Arrow
            centerCoords.setTheta(centerCoords.getTheta() - (30D * degree));
            if (centerCoords.getTheta() < 0D)
                centerCoords.setTheta(centerCoords.getTheta() + (2D * Math.PI));
            break;
        }

        // Reposition Globe If Non-Zoom Button is Selected
        if (spot > 0)
            parentNavigator.updateGlobeOnly(centerCoords);
    }

    public void mouseClicked(MouseEvent event) {}
    public void mouseEntered(MouseEvent event) {}
    public void mouseExited(MouseEvent event) {}

    /** Light button if mouse is on button
     *  @param x x-position
     *  @param y y-position
     */
    private void lightButton(int x, int y) {
        buttonLight = findHotSpot(x, y);
        if (buttonLight >= 0) {
            repaint();
        }
    }

    /** Unlight buttons if any are lighted */
    private void unlightButtons() {
        if (buttonLight >= 0) {
            buttonLight = -1;
            repaint();
        }
    }

    /** Returns button number if mouse is on button
      *  Returns -1 if not on button
      *  Uses rectangular image mapping
      *  @param x x-position
      *  @param y y-position
      */
    private int findHotSpot(int x, int y) {
        for (int i = 0; i < 9; i++) {
            if (hotSpots[i].contains(x, y)) {
                return i;
            }
        }

        return -1;
    }
}
