/**
 * Mars Simulation Project
 * WalkSettlementInterior.java
 * @version 3.06 2014-02-09
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.connection.BuildingConnector;
import org.mars_sim.msp.core.structure.building.connection.BuildingLocation;
import org.mars_sim.msp.core.structure.building.connection.Hatch;
import org.mars_sim.msp.core.structure.building.connection.InsideBuildingPath;
import org.mars_sim.msp.core.structure.building.connection.InsidePathLocation;
import org.mars_sim.msp.core.time.MarsClock;

/**
 * A subtask for walking between two interior locations in a settlement.
 * (Ex: Between two connected inhabitable buildings or two locations in 
 * a single inhabitable building.)
 */
public class WalkSettlementInterior extends Task implements Serializable {

    private static Logger logger = Logger.getLogger(WalkSettlementInterior.class.getName());
    
    // Task phase
    private static final String WALKING = "Walking";
    
    // Static members
    private static final double WALKING_SPEED = 5D; // km / hr.
    private static final double VERY_SMALL_DISTANCE = .00001D;
    
    // Data members
    private Settlement settlement;
    private Building startBuilding;
    private Building destBuilding;
    private double destXLoc;
    private double destYLoc;
    private InsideBuildingPath walkingPath;
    
    /**
     * Constructor
     * @param person the person performing the task.
     * @param destinationBuilding the building that is walked to. (Can be same as current building).
     * @param destinationXLocation the destination X location at the settlement.
     * @param destinationYLocation the destination Y location at the settlement.
     */
    public WalkSettlementInterior(Person person, Building destinationBuilding, 
            double destinationXLocation, double destinationYLocation) {
        super("Walking Settlement Interior", person, false, false, 0D, false, 0D);
        
        // Check that the person is currently inside the settlement.
        String location = person.getLocationSituation();
        if (!location.equals(Person.INSETTLEMENT)) {
            throw new IllegalStateException(
                    "WalkSettlementInterior task started when person is not in settlement.");
        }
        
        // Initialize data members.
        this.settlement = person.getSettlement();
        this.destBuilding = destinationBuilding;
        this.destXLoc = destinationXLocation;
        this.destYLoc = destinationYLocation;
        
        // Check that destination location is within destination building.
        if (!LocalAreaUtil.checkLocationWithinLocalBoundedObject(destXLoc, destYLoc, destBuilding)) {
            throw new IllegalStateException(
                    "Given destination walking location not within destination building.");
        }
        
        // Check that the person is currently inside a building.
        startBuilding = BuildingManager.getBuilding(person);
        if (startBuilding == null) {
            throw new IllegalStateException(person.getName() + " is not currently in a building.");
        }
        
        // Determine the walking path to the destination.
        walkingPath = settlement.getBuildingConnectorManager().determineShortestPath(startBuilding, 
                person.getXLocation(), person.getYLocation(), destinationBuilding, destinationXLocation, 
                destinationYLocation);
        
        // If no valid walking path is found, end task.
        if (walkingPath == null) {
            logger.severe(person.getName() + " unable to walk from " + startBuilding.getName() + " to " + 
                    destinationBuilding.getName() + ".  Unable to find valid interior path.");
            endTask();
        }
        
        // Initialize task phase.
        addPhase(WALKING);
        setPhase(WALKING);
    }
    
    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) throw new IllegalArgumentException("Task phase is null");
        if (WALKING.equals(getPhase())) return walkingPhase(time);
        else return time;
    }
    
    /**
     * Performs the walking phase of the task.
     * @param time the amount of time (millisol) to perform the walking phase.
     * @return the amount of time (millisol) left after performing the walking phase.
     */
    double walkingPhase(double time) {
        
        // Check that remaining path locations are valid.
        if (!checkRemainingPathLocations()) {
            logger.severe(person.getName() + " unable to continue walking due to missing path objects.");
        }
        
        // Determine walking distance.
        double timeHours = MarsClock.convertMillisolsToSeconds(time) / 60D / 60D;
        double distanceKm = WALKING_SPEED * timeHours;
        double distanceMeters = distanceKm * 1000D;
        double remainingPathDistance = getRemainingPathDistance();
        
        // Determine time left after walking.
        double timeLeft = 0D;
        if (distanceMeters > remainingPathDistance) {
            double overDistance = distanceMeters - remainingPathDistance;
            timeLeft = MarsClock.convertSecondsToMillisols(overDistance / 1000D / WALKING_SPEED * 60D * 60D);
            distanceMeters = remainingPathDistance;
        }
        
        while (distanceMeters > VERY_SMALL_DISTANCE) {
            
            // Walk to next path location.
            InsidePathLocation location = walkingPath.getNextPathLocation();
            double distanceToLocation = Point2D.distance(person.getXLocation(), person.getYLocation(), 
                    location.getXLocation(), location.getYLocation());
            
            if (distanceMeters >= distanceToLocation) {
                // Set person at next path location, changing buildings if necessary.
                person.setXLocation(location.getXLocation());
                person.setYLocation(location.getYLocation());
                distanceMeters -= distanceToLocation;
                changeBuildings(location);
                if (!walkingPath.isEndOfPath()) {
                    walkingPath.iteratePathLocation();
                }
            }
            else {
                // Walk in direction of next path location.
                
                // Determine direction
                double direction = determineDirection(location.getXLocation(), location.getYLocation());
                
                // Determine person's new location at distance and direction.
                walkInDirection(direction, distanceMeters);
                
                distanceMeters = 0D;
            }
        }
        
        // If path destination is reached, end task.
        if (getRemainingPathDistance() <= VERY_SMALL_DISTANCE) {
            logger.fine(person.getName() + " walked from " + startBuilding.getName() + " to " + 
                    destBuilding.getName());
            endTask();
        }
        
        return timeLeft; 
    }
    
    /**
     * Determine the direction of travel to a location.
     * @param destinationXLocation the destination X location.
     * @param destinationYLocation the destination Y location.
     * @return direction (radians).
     */
    double determineDirection(double destinationXLocation, double destinationYLocation) {
        
        double result = Math.atan2(person.getXLocation() - destinationXLocation, 
                destinationYLocation - person.getYLocation());
        
        while (result > (Math.PI * 2D)) {
            result -= (Math.PI * 2D);
        }
        
        while (result < 0D) {
            result += (Math.PI * 2D);
        }
        
        return result;
    }
    
    /**
     * Walk in a given direction for a given distance.
     * @param direction the direction (radians) of travel.
     * @param distance the distance (meters) to travel.
     */
    void walkInDirection(double direction, double distance) {
        
        double newXLoc = (-1D * Math.sin(direction) * distance) + person.getXLocation();
        double newYLoc = (Math.cos(direction) * distance) + person.getYLocation();
        
        person.setXLocation(newXLoc);
        person.setYLocation(newYLoc);
    }
    
    /**
     * Check that the remaining path locations are valid.
     * @return true if remaining path locations are valid.
     */
    private boolean checkRemainingPathLocations() {
        
        boolean result = true;
        
        // Check all remaining path locations.
        Iterator<InsidePathLocation> i = walkingPath.getRemainingPathLocations().iterator();
        while (i.hasNext()) {
            InsidePathLocation loc = i.next();
            if (loc instanceof Building) {
                // Check that building still exists.
                Building building = (Building) loc;
                if (!settlement.getBuildingManager().containsBuilding(building)) {
                    result = false;
                }
            }
            else if (loc instanceof BuildingLocation) {
                // Check that building still exists.
                BuildingLocation buildingLoc = (BuildingLocation) loc;
                Building building = buildingLoc.getBuilding();
                if (!settlement.getBuildingManager().containsBuilding(building)) {
                    result = false;
                }
            }
            else if (loc instanceof BuildingConnector) {
                // Check that building connector still exists.
                BuildingConnector connector = (BuildingConnector) loc;
                if (!settlement.getBuildingConnectorManager().containsBuildingConnector(connector)) {
                    result = false;
                }
            }
            else if (loc instanceof Hatch) {
                // Check that building connector for hatch still exists.
                Hatch hatch = (Hatch) loc;
                BuildingConnector connector = hatch.getBuildingConnector();
                if (!settlement.getBuildingConnectorManager().containsBuildingConnector(connector)) {
                    result = false;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Gets the remaining path distance.
     * @return distance (meters).
     */
    private double getRemainingPathDistance() {
        
        double result = 0D;
        
        double prevXLoc = person.getXLocation();
        double prevYLoc = person.getYLocation();
        
        Iterator<InsidePathLocation> i = walkingPath.getRemainingPathLocations().iterator();
        while (i.hasNext()) {
            InsidePathLocation nextLoc = i.next();
            result += Point2D.Double.distance(prevXLoc, prevYLoc, nextLoc.getXLocation(), 
                    nextLoc.getYLocation());
            prevXLoc = nextLoc.getXLocation();
            prevYLoc = nextLoc.getYLocation();
        }
        
        return result;
    }
    
    /**
     * Change the person's current building to a new one if necessary.
     * @param location the path location the person has reached.
     */
    private void changeBuildings(InsidePathLocation location) {
        
        if (location instanceof Hatch) {
            // If hatch leads to new building, place person in the new building.
            Hatch hatch = (Hatch) location;
            Building currentBuilding = BuildingManager.getBuilding(person);
            if (!hatch.getBuilding().equals(currentBuilding)) {
                BuildingManager.addPersonToBuildingSameLocation(person, hatch.getBuilding());
            }
        }
        else if (location instanceof BuildingConnector) {
            // If non-split building connector, place person in the new building.
            BuildingConnector connector = (BuildingConnector) location;
            if (!connector.isSplitConnection()) {
                Building currentBuilding = BuildingManager.getBuilding(person);
                Building newBuilding = null;
                if (connector.getBuilding1().equals(currentBuilding)) {
                    newBuilding = connector.getBuilding2();
                }
                else if (connector.getBuilding2().equals(currentBuilding)) {
                    newBuilding = connector.getBuilding1();
                }
                else {
                    throw new IllegalStateException("Connector not connected to " + currentBuilding);
                }
                BuildingManager.addPersonToBuildingSameLocation(person, newBuilding);
            }
        }
    }

    @Override
    public int getEffectiveSkillLevel() {
        return 0;
    }

    @Override
    public List<String> getAssociatedSkills() {
        List<String> results = new ArrayList<String>(0);
        return results;
    }

    @Override
    protected void addExperience(double time) {
        // This task adds no experience.
    }
    
    @Override
    public void destroy() {
        super.destroy();
        
        destBuilding = null;
        walkingPath.destroy();
        walkingPath = null;
    }
}