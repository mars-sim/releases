/**
 * Mars Simulation Project
 * WalkingSteps.java
 * @version 3.07 2014-10-10
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Airlock;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.LocalBoundedObject;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * A helper class for determining the walking steps from one location to another.
 */
public class WalkingSteps
implements Serializable {
    
	/** default serial id. */
	private static final long serialVersionUID = 1L;
	
	private static Logger logger = Logger.getLogger(WalkingSteps.class.getName());

	// Data members.
	private List<WalkStep> walkingSteps;
	private boolean canWalkAllSteps;

	/**
	 * constructor.
	 */
	public WalkingSteps(Person person, double xLoc, double yLoc, LocalBoundedObject interiorObject) {
        
        // Initialize data members.
        canWalkAllSteps = true;
        walkingSteps = new ArrayList<WalkStep>();
        
        // Determine initial walk state.
        WalkState initialWalkState = determineInitialWalkState(person);
        
        // Determine destination walk state.
        WalkState destinationWalkState = determineDestinationWalkState(xLoc, yLoc, interiorObject);
        
        // Determine walking steps to destination.
        determineWalkingSteps(initialWalkState, destinationWalkState);
    }
    
    /**
     * Gets a list of walking steps to the destination.
     * @return list of walk steps.  Returns empty list if a valid path isn't found.
     */
    public List<WalkStep> getWalkingStepsList() {
        return walkingSteps;
    }
    
    /**
     * Gets the number of walking steps to the destination.
     * @return number of walking steps.  Returns 0 if a valid path isn't found.
     */
    public int getWalkingStepsNumber() {
        int result = 0;
        
        if (walkingSteps != null) {
            result = walkingSteps.size();
        }
        
        return result;
    }
    
    /**
     * Checks if a valid path has been found to the destination.
     * @return true if valid path to destination found.
     */
    public boolean canWalkAllSteps() {
        return canWalkAllSteps;
    }
    
    /**
     * Determines the person's initial walk state.
     * @param person the person walking.
     * @return the initial location state.
     */
    private WalkState determineInitialWalkState(Person person) {
        
        WalkState result = null;
        
        LocationSituation locationSituation = person.getLocationSituation();

        // Determine initial walk state based on person's location situation.
        if (LocationSituation.IN_SETTLEMENT == locationSituation) {
            
            Building building = BuildingManager.getBuilding(person);
            if (building == null) {
                return null;
            }
            
            result = new WalkState(WalkState.BUILDING_LOC);
            result.building = building;
            
            if (!LocalAreaUtil.checkLocationWithinLocalBoundedObject(person.getXLocation(), 
                    person.getYLocation(), building)) {
                throw new IllegalStateException(person.getName() + " has invalid walk start location. (" + 
                    person.getXLocation() + ", " + person.getYLocation() + ") is not within building " + building);
            }
        }
        else if (LocationSituation.IN_VEHICLE == locationSituation) {
            
            Vehicle vehicle = person.getVehicle();
            
            if (vehicle instanceof Rover) {
                result = new WalkState(WalkState.ROVER_LOC);
                result.rover = (Rover) vehicle;
                
                if (!LocalAreaUtil.checkLocationWithinLocalBoundedObject(person.getXLocation(), 
                        person.getYLocation(), vehicle)) {
                    throw new IllegalStateException(person.getName() + " has invalid walk start location. (" + 
                        person.getXLocation() + ", " + person.getYLocation() + ") is not within vehicle " + vehicle);
                }
            }
            else {
                result = new WalkState(WalkState.OUTSIDE_LOC); 
            }
        }
        else if (LocationSituation.OUTSIDE == locationSituation) {
            
            result = new WalkState(WalkState.OUTSIDE_LOC); 
        }
        else {
            
            throw new IllegalStateException(person.getName() + 
                    " is in an invalid location situation for walking task: " + 
                    locationSituation);
        }
        
        // Set person X and Y location.
        if (result != null) {
            result.xLoc = person.getXLocation();
            result.yLoc = person.getYLocation();
        }
        
        return result;
    }
    
    /**
     * Determines the destination walk state.
     * @param xLoc the destination X location.
     * @param yLoc the destination Y location.
     * @param interiorObject the destination interior object (inhabitable building or rover).
     * @return destination walk state.
     */
    private WalkState determineDestinationWalkState(double xLoc, double yLoc, 
            LocalBoundedObject interiorObject) {
        
        WalkState result = null;
        
        if (interiorObject instanceof Building) {
            Building building = (Building) interiorObject;
            result = new WalkState(WalkState.BUILDING_LOC);
            result.building = building;
            
            if (!LocalAreaUtil.checkLocationWithinLocalBoundedObject(xLoc, yLoc, building)) {
                throw new IllegalStateException("Invalid walk destination location. (" + 
                    xLoc + ", " + yLoc + ") is not within building " + building);
            }
        }
        else if (interiorObject instanceof Rover) {
            Rover rover = (Rover) interiorObject;
            result = new WalkState(WalkState.ROVER_LOC);
            result.rover = rover;
            
            if (!LocalAreaUtil.checkLocationWithinLocalBoundedObject(xLoc, yLoc, rover)) {
                throw new IllegalStateException("Invalid walk destination location. (" + 
                    xLoc + ", " + yLoc + ") is not within rover " + rover);
            }
        }
        else {
            result = new WalkState(WalkState.OUTSIDE_LOC);
        }
        
        result.xLoc = xLoc;
        result.yLoc = yLoc;
        
        return result;
    }
    
    /**
     * Determine the walk steps from an initial walk state to a destination walk state.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineWalkingSteps(WalkState initialWalkState, WalkState destinationWalkState) {
        
        // If cannot walk steps, return.
        if (!canWalkAllSteps) {
            return;
        }
        
        // Determine walking steps based on initial walk state.
        switch(initialWalkState.stateType) {
        
            case WalkState.BUILDING_LOC:    determineBuildingInteriorWalkingSteps(initialWalkState, 
                    destinationWalkState);
                                            break;
            case WalkState.ROVER_LOC:       determineRoverInteriorWalkingSteps(initialWalkState, 
                    destinationWalkState);
                                            break;
            case WalkState.INTERIOR_AIRLOCK:determineAirlockInteriorWalkingSteps(initialWalkState, 
                    destinationWalkState);
                                            break;
            case WalkState.EXTERIOR_AIRLOCK:determineAirlockExteriorWalkingSteps(initialWalkState, 
                    destinationWalkState);
                                            break;
            case WalkState.OUTSIDE_LOC:     determineOutsideWalkingSteps(initialWalkState, 
                    destinationWalkState);
                                            break;
            default:                        throw new IllegalArgumentException("Invalid walk state type: " + 
                    initialWalkState.stateType);
        }
        
        return;
    }
    
    /**
     * Determine the walking steps in a building interior.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineBuildingInteriorWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        // Determine walking steps based on the destination walk state.
        switch(destinationWalkState.stateType) {
        
            case WalkState.BUILDING_LOC:    determineBuildingInteriorToBuildingInteriorWalkingSteps(
                    initialWalkState, destinationWalkState); 
                                            break;
            case WalkState.ROVER_LOC:       determineBuildingInteriorToRoverWalkingSteps(
                    initialWalkState, destinationWalkState);
                                            break;
            case WalkState.OUTSIDE_LOC:     determineBuildingInteriorToOutsideWalkingSteps(
                    initialWalkState, destinationWalkState);
                                            break;
            default:                        throw new IllegalArgumentException("Invalid walk state type: " + 
                initialWalkState.stateType);
        }
    }
    
    /**
     * Determine the walking steps from a building interior to another building interior.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineBuildingInteriorToBuildingInteriorWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        Building initialBuilding = initialWalkState.building;
        Building destinationBuilding = destinationWalkState.building;
        Settlement settlement = initialBuilding.getBuildingManager().getSettlement();
        
        // Check if two buildings have walkable path.
        if (settlement.getBuildingConnectorManager().hasValidPath(initialBuilding, destinationBuilding)) {
            
            // Add settlement interior walk step.
            createWalkSettlementInteriorStep(destinationWalkState.xLoc, destinationWalkState.yLoc, 
                    destinationBuilding);
        }
        else {
            
            // Find closest walkable airlock to destination.
            Airlock airlock = settlement.getClosestWalkableAvailableAirlock(initialBuilding, 
                    destinationWalkState.xLoc, destinationWalkState.yLoc);
            if (airlock == null) {
                canWalkAllSteps = false;
                logger.severe("Cannot find walkable airlock from building interior to building interior.");
                return;
            }
            
            Building airlockBuilding = (Building) airlock.getEntity();
            Point2D interiorAirlockPosition = airlock.getAvailableInteriorPosition();
            
            // Add settlement interior walk step to starting airlock.
            createWalkSettlementInteriorStep(interiorAirlockPosition.getX(), 
                    interiorAirlockPosition.getY(), airlockBuilding);
            
            // Create interior airlock walk state.
            WalkState interiorAirlockState = new WalkState(WalkState.INTERIOR_AIRLOCK);
            interiorAirlockState.airlock = airlock;
            interiorAirlockState.building = airlockBuilding;
            interiorAirlockState.xLoc = interiorAirlockPosition.getX();
            interiorAirlockState.yLoc = interiorAirlockPosition.getY();
            
            determineWalkingSteps(interiorAirlockState, destinationWalkState);
        }
    }
    
    /**
     * Determine the walking steps from a building interior to a rover interior.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineBuildingInteriorToRoverWalkingSteps(WalkState initialWalkState,
            WalkState destinationWalkState) {
        
        Building initialBuilding = initialWalkState.building;
        Rover destinationRover = destinationWalkState.rover;
        Settlement settlement = initialBuilding.getBuildingManager().getSettlement();
        
        // Check if rover is parked in garage or outside.
        Building garageBuilding = BuildingManager.getBuilding(destinationRover);
        if (garageBuilding != null) {
            
            // Create walking steps to garage building.
            WalkState garageWalkState = new WalkState(WalkState.BUILDING_LOC);
            garageWalkState.building = garageBuilding;
            garageWalkState.xLoc = destinationWalkState.xLoc;
            garageWalkState.yLoc = destinationWalkState.yLoc;
            determineBuildingInteriorToBuildingInteriorWalkingSteps(initialWalkState, garageWalkState);
            
            // Add enter rover walk step.
            WalkStep enterRoverInGarageStep = new WalkStep(WalkStep.ENTER_GARAGE_ROVER);
            enterRoverInGarageStep.rover = destinationRover;
            enterRoverInGarageStep.building = garageBuilding;
            enterRoverInGarageStep.xLoc = destinationWalkState.xLoc;
            enterRoverInGarageStep.yLoc = destinationWalkState.yLoc;
            walkingSteps.add(enterRoverInGarageStep);
        }
        else {
            
            // Find closest walkable airlock to destination.
            Airlock airlock = settlement.getClosestWalkableAvailableAirlock(initialBuilding, 
                    destinationWalkState.xLoc, destinationWalkState.yLoc);
            if (airlock == null) {
                canWalkAllSteps = false;
                logger.severe("Cannot find walkable airlock from building interior to rover interior.");
                return;
            }
            
            Building airlockBuilding = (Building) airlock.getEntity();
            Point2D interiorAirlockPosition = airlock.getAvailableInteriorPosition();
            
            // Add settlement interior walk step to starting airlock.
            createWalkSettlementInteriorStep(interiorAirlockPosition.getX(), 
                    interiorAirlockPosition.getY(), airlockBuilding);
            
            // Create interior airlock walk state.
            WalkState interiorAirlockState = new WalkState(WalkState.INTERIOR_AIRLOCK);
            interiorAirlockState.airlock = airlock;
            interiorAirlockState.building = airlockBuilding;
            interiorAirlockState.xLoc = interiorAirlockPosition.getX();
            interiorAirlockState.yLoc = interiorAirlockPosition.getY();
            
            determineWalkingSteps(interiorAirlockState, destinationWalkState);
        }
    }
    
    /**
     * Determine the walking steps between a building interior and outside.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineBuildingInteriorToOutsideWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        Building initialBuilding = initialWalkState.building;
        Settlement settlement = initialBuilding.getBuildingManager().getSettlement();
        
        // Find closest walkable airlock to destination.
        Airlock airlock = settlement.getClosestWalkableAvailableAirlock(initialBuilding, 
                destinationWalkState.xLoc, destinationWalkState.yLoc);
        if (airlock == null) {
            canWalkAllSteps = false;
            logger.severe("Cannot find walkable airlock from building interior to outside.");
            return;
        }
        
        Building airlockBuilding = (Building) airlock.getEntity();
        Point2D interiorAirlockPosition = airlock.getAvailableInteriorPosition();
        
        // Add settlement interior walk step to starting airlock.
        createWalkSettlementInteriorStep(interiorAirlockPosition.getX(), 
                interiorAirlockPosition.getY(), airlockBuilding);
        
        // Create interior airlock walk state.
        WalkState interiorAirlockState = new WalkState(WalkState.INTERIOR_AIRLOCK);
        interiorAirlockState.airlock = airlock;
        interiorAirlockState.building = airlockBuilding;
        interiorAirlockState.xLoc = interiorAirlockPosition.getX();
        interiorAirlockState.yLoc = interiorAirlockPosition.getY();
        
        determineWalkingSteps(interiorAirlockState, destinationWalkState);
    }
    
    /**
     * Determine the walking steps between two rover interior locations.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineRoverInteriorWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        switch(destinationWalkState.stateType) {
        
            case WalkState.BUILDING_LOC:    determineRoverToBuildingInteriorWalkingSteps(
                    initialWalkState, destinationWalkState); 
                                            break;
            case WalkState.ROVER_LOC:       determineRoverToRoverWalkingSteps(
                    initialWalkState, destinationWalkState);
                                            break;
            case WalkState.OUTSIDE_LOC:     determineRoverToOutsideWalkingSteps(
                    initialWalkState, destinationWalkState);
                                            break;
            default:                        throw new IllegalArgumentException("Invalid walk state type: " + 
                    initialWalkState.stateType);
        }
    }
    
    /**
     * Determine the walking steps between a rover interior and a building interior.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineRoverToBuildingInteriorWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        Rover initialRover = initialWalkState.rover;
        
        // Check if rover is parked in garage or outside.
        Building garageBuilding = BuildingManager.getBuilding(initialRover);
        if (garageBuilding != null) {
            
            // Add exit rover walk step.
            WalkStep exitRoverInGarageStep = new WalkStep(WalkStep.EXIT_GARAGE_ROVER);
            exitRoverInGarageStep.rover = initialRover;
            exitRoverInGarageStep.building = garageBuilding;
            exitRoverInGarageStep.xLoc = initialWalkState.xLoc;
            exitRoverInGarageStep.yLoc = initialWalkState.yLoc;
            walkingSteps.add(exitRoverInGarageStep);
            
            // Create walking steps to destination building.
            WalkState buildingWalkState = new WalkState(WalkState.BUILDING_LOC);
            buildingWalkState.building = garageBuilding;
            buildingWalkState.xLoc = initialWalkState.xLoc;
            buildingWalkState.yLoc = initialWalkState.yLoc;
            determineBuildingInteriorToBuildingInteriorWalkingSteps(buildingWalkState, 
                    destinationWalkState);
        }
        else {
            
            // Walk to rover airlock.
            Airlock airlock = initialRover.getAirlock();
            Point2D interiorAirlockPosition = airlock.getAvailableInteriorPosition();
            
            // Add rover interior walk step to starting airlock.
            createWalkRoverInteriorStep(interiorAirlockPosition.getX(), 
                    interiorAirlockPosition.getY(), initialRover);
            
            // Create interior airlock walk state.
            WalkState interiorAirlockState = new WalkState(WalkState.INTERIOR_AIRLOCK);
            interiorAirlockState.airlock = airlock;
            interiorAirlockState.rover = initialRover;
            interiorAirlockState.xLoc = interiorAirlockPosition.getX();
            interiorAirlockState.yLoc = interiorAirlockPosition.getY();
            
            determineWalkingSteps(interiorAirlockState, destinationWalkState);
        }
    }
    
    /**
     * Determine the walking steps between a rover interior and a rover interior.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineRoverToRoverWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        Rover initialRover = initialWalkState.rover;
        Rover destinationRover = destinationWalkState.rover;
        
        if (initialRover.equals(destinationRover)) {
            
            // Walk to rover interior location.
            createWalkRoverInteriorStep(destinationWalkState.xLoc, 
                    destinationWalkState.yLoc, destinationRover);
        }
        else {
            
            // Check if initial rover is in a garage.
            Building garageBuilding = BuildingManager.getBuilding(initialRover);
            if (garageBuilding != null) {
                
                // Add exit rover walk step.
                WalkStep exitRoverInGarageStep = new WalkStep(WalkStep.EXIT_GARAGE_ROVER);
                exitRoverInGarageStep.rover = initialRover;
                exitRoverInGarageStep.building = garageBuilding;
                exitRoverInGarageStep.xLoc = initialWalkState.xLoc;
                exitRoverInGarageStep.yLoc = initialWalkState.yLoc;
                walkingSteps.add(exitRoverInGarageStep);
                
                // Create walking steps to destination rover.
                WalkState buildingWalkState = new WalkState(WalkState.BUILDING_LOC);
                buildingWalkState.building = garageBuilding;
                buildingWalkState.xLoc = initialWalkState.xLoc;
                buildingWalkState.yLoc = initialWalkState.yLoc;
                
                determineBuildingInteriorToRoverWalkingSteps(buildingWalkState, 
                        destinationWalkState);
            }
            else {
                
                // Walk to rover airlock.
                Airlock airlock = initialRover.getAirlock();
                Point2D interiorAirlockPosition = airlock.getAvailableInteriorPosition();
                
                // Add rover interior walk step to starting airlock.
                createWalkRoverInteriorStep(interiorAirlockPosition.getX(), 
                        interiorAirlockPosition.getY(), initialRover);
                
                // Create interior airlock walk state.
                WalkState interiorAirlockState = new WalkState(WalkState.INTERIOR_AIRLOCK);
                interiorAirlockState.airlock = airlock;
                interiorAirlockState.rover = initialRover;
                interiorAirlockState.xLoc = interiorAirlockPosition.getX();
                interiorAirlockState.yLoc = interiorAirlockPosition.getY();
                
                determineWalkingSteps(interiorAirlockState, destinationWalkState);
            }
        }
    }
    
    /**
     * Determine the walking steps between a rover interior and outside location.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineRoverToOutsideWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        Rover initialRover = initialWalkState.rover;
        
        // Check if rover is parked in garage or outside.
        Building garageBuilding = BuildingManager.getBuilding(initialRover);
        if (garageBuilding != null) {
            
            // Add exit rover walk step.
            WalkStep exitRoverInGarageStep = new WalkStep(WalkStep.EXIT_GARAGE_ROVER);
            exitRoverInGarageStep.rover = initialRover;
            exitRoverInGarageStep.building = garageBuilding;
            exitRoverInGarageStep.xLoc = initialWalkState.xLoc;
            exitRoverInGarageStep.yLoc = initialWalkState.yLoc;
            walkingSteps.add(exitRoverInGarageStep);
            
            // Create walking steps to destination building.
            WalkState buildingWalkState = new WalkState(WalkState.BUILDING_LOC);
            buildingWalkState.building = garageBuilding;
            buildingWalkState.xLoc = initialWalkState.xLoc;
            buildingWalkState.yLoc = initialWalkState.yLoc;
            
            determineBuildingInteriorToOutsideWalkingSteps(buildingWalkState, 
                    destinationWalkState);
        }
        else {
            
            // Walk to rover airlock.
            Airlock airlock = initialRover.getAirlock();
            Point2D interiorAirlockPosition = airlock.getAvailableInteriorPosition();
            
            // Add rover interior walk step to starting airlock.
            createWalkRoverInteriorStep(interiorAirlockPosition.getX(), 
                    interiorAirlockPosition.getY(), initialRover);
            
            // Create interior airlock walk state.
            WalkState interiorAirlockState = new WalkState(WalkState.INTERIOR_AIRLOCK);
            interiorAirlockState.airlock = airlock;
            interiorAirlockState.rover = initialRover;
            interiorAirlockState.xLoc = interiorAirlockPosition.getX();
            interiorAirlockState.yLoc = interiorAirlockPosition.getY();
            
            determineWalkingSteps(interiorAirlockState, destinationWalkState);
        }
    }
    
    /**
     * Determine the walking steps from an airlock interior location.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineAirlockInteriorWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        switch(destinationWalkState.stateType) {
        
            case WalkState.BUILDING_LOC:    determineAirlockInteriorToBuildingInteriorWalkingSteps(
                    initialWalkState, destinationWalkState); 
                                            break;
            case WalkState.ROVER_LOC:       determineAirlockInteriorToRoverWalkingSteps(
                    initialWalkState, destinationWalkState);
                                            break;
            case WalkState.OUTSIDE_LOC:     determineAirlockInteriorToOutsideWalkingSteps(
                    initialWalkState, destinationWalkState);
                                            break;
            default:                        throw new IllegalArgumentException("Invalid walk state type: " + 
                    initialWalkState.stateType);
        }
    }
    
    /**
     * Determine the walking steps between an airlock interior and a building interior location. 
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineAirlockInteriorToBuildingInteriorWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        Airlock airlock = initialWalkState.airlock;
        Building destinationBuilding = destinationWalkState.building;
        Settlement settlement = destinationBuilding.getBuildingManager().getSettlement();
        
        // Check if airlock is for a building or a rover.
        if (airlock.getEntity() instanceof Building) {
            
            Building airlockBuilding = (Building) airlock.getEntity();
            
            // Check if walkable interior path between airlock building and destination building.
            if (settlement.getBuildingConnectorManager().hasValidPath(airlockBuilding, destinationBuilding)) {
                
                // Add settlement interior walk step.
                createWalkSettlementInteriorStep(destinationWalkState.xLoc, destinationWalkState.yLoc, 
                        destinationBuilding);
            }
            else {
                
                // Add exit airlock walk step.
                createExitAirlockStep(airlock);
                
                // Create exterior airlock walk state.
                WalkState exteriorAirlockState = new WalkState(WalkState.EXTERIOR_AIRLOCK);
                exteriorAirlockState.airlock = airlock;
                Point2D exteriorAirlockPosition = airlock.getAvailableExteriorPosition();
                exteriorAirlockState.xLoc = exteriorAirlockPosition.getX();
                exteriorAirlockState.yLoc = exteriorAirlockPosition.getY();
                
                determineWalkingSteps(exteriorAirlockState, destinationWalkState);
            }
        }
        else if (airlock.getEntity() instanceof Rover) {
            
            // Add exit airlock walk step.
            createExitAirlockStep(airlock);
            
            // Create exterior airlock walk state.
            WalkState exteriorAirlockState = new WalkState(WalkState.EXTERIOR_AIRLOCK);
            exteriorAirlockState.airlock = airlock;
            Point2D exteriorAirlockPosition = airlock.getAvailableExteriorPosition();
            exteriorAirlockState.xLoc = exteriorAirlockPosition.getX();
            exteriorAirlockState.yLoc = exteriorAirlockPosition.getY();
            
            determineWalkingSteps(exteriorAirlockState, destinationWalkState);
        }
        else {
            throw new IllegalArgumentException("Invalid airlock entity for walking: " + airlock.getEntity());
        }
    }
    
    /**
     * Determine the walking steps between an airlock interior and rover interior location.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineAirlockInteriorToRoverWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        Airlock airlock = initialWalkState.airlock;
        Rover destinationRover = destinationWalkState.rover;
        
        // Check if airlock is for a building or a rover.
        if (airlock.getEntity() instanceof Building) {
            
            Building airlockBuilding = (Building) airlock.getEntity();
            
            // Check if rover is in a garage or outside.
            Building garageBuilding = BuildingManager.getBuilding(destinationRover);
            if (garageBuilding != null) {
                
                // Check if garage building has a walkable interior path from airlock building.
                Settlement settlement = airlockBuilding.getBuildingManager().getSettlement();
                if (settlement.getBuildingConnectorManager().hasValidPath(airlockBuilding, garageBuilding)) {
                    
                    // Add settlement interior walk step.
                    createWalkSettlementInteriorStep(destinationWalkState.xLoc, destinationWalkState.yLoc, 
                            garageBuilding);
                    
                    // Add enter rover walk step.
                    WalkStep enterRoverInGarageStep = new WalkStep(WalkStep.ENTER_GARAGE_ROVER);
                    enterRoverInGarageStep.rover = destinationRover;
                    enterRoverInGarageStep.building = garageBuilding;
                    enterRoverInGarageStep.xLoc = destinationWalkState.xLoc;
                    enterRoverInGarageStep.yLoc = destinationWalkState.yLoc;
                    walkingSteps.add(enterRoverInGarageStep);
                }
                else {
                    
                    // Add exit airlock walk step.
                    createExitAirlockStep(airlock);
                    
                    // Create exterior airlock walk state.
                    WalkState exteriorAirlockState = new WalkState(WalkState.EXTERIOR_AIRLOCK);
                    exteriorAirlockState.airlock = airlock;
                    Point2D exteriorAirlockPosition = airlock.getAvailableExteriorPosition();
                    exteriorAirlockState.xLoc = exteriorAirlockPosition.getX();
                    exteriorAirlockState.yLoc = exteriorAirlockPosition.getY();
                    
                    determineWalkingSteps(exteriorAirlockState, destinationWalkState);
                }
            }
            else {
                
                // Add exit airlock walk step.
                createExitAirlockStep(airlock);
                
                // Create exterior airlock walk state.
                WalkState exteriorAirlockState = new WalkState(WalkState.EXTERIOR_AIRLOCK);
                exteriorAirlockState.airlock = airlock;
                Point2D exteriorAirlockPosition = airlock.getAvailableExteriorPosition();
                exteriorAirlockState.xLoc = exteriorAirlockPosition.getX();
                exteriorAirlockState.yLoc = exteriorAirlockPosition.getY();
                
                determineWalkingSteps(exteriorAirlockState, destinationWalkState);
            }
            
        }
        else if (airlock.getEntity() instanceof Rover) {
            
            Rover airlockRover = (Rover) airlock.getEntity();
            
            // Check if airlockRover is the same as destinationRover.
            if (airlockRover.equals(destinationRover)) {
                
                // Create walking step internal to rover.
                createWalkRoverInteriorStep(destinationWalkState.xLoc, destinationWalkState.yLoc, 
                        destinationRover);
            }
            else {
                
                // Add exit airlock walk step.
                createExitAirlockStep(airlock);
                
                // Create exterior airlock walk state.
                WalkState exteriorAirlockState = new WalkState(WalkState.EXTERIOR_AIRLOCK);
                exteriorAirlockState.airlock = airlock;
                Point2D exteriorAirlockPosition = airlock.getAvailableExteriorPosition();
                exteriorAirlockState.xLoc = exteriorAirlockPosition.getX();
                exteriorAirlockState.yLoc = exteriorAirlockPosition.getY();
                
                determineWalkingSteps(exteriorAirlockState, destinationWalkState);
            }
        }
        else {
            throw new IllegalArgumentException("Invalid airlock entity for walking: " + airlock.getEntity());
        }
    }
    
    /**
     * Determine the walking steps between an airlock interior and an outside location.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineAirlockInteriorToOutsideWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        Airlock airlock = initialWalkState.airlock;
        
        // Add exit airlock walk step.
        createExitAirlockStep(airlock);
        
        // Create exterior airlock walk state.
        WalkState exteriorAirlockState = new WalkState(WalkState.EXTERIOR_AIRLOCK);
        exteriorAirlockState.airlock = airlock;
        Point2D exteriorAirlockPosition = airlock.getAvailableExteriorPosition();
        exteriorAirlockState.xLoc = exteriorAirlockPosition.getX();
        exteriorAirlockState.yLoc = exteriorAirlockPosition.getY();
        
        determineWalkingSteps(exteriorAirlockState, destinationWalkState);
    }
    
    /**
     * Determine the walking steps from an airlock exterior location.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineAirlockExteriorWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        switch(destinationWalkState.stateType) {
        
            case WalkState.BUILDING_LOC:    determineAirlockExteriorToBuildingInteriorWalkingSteps(
                    initialWalkState, destinationWalkState); 
                                            break;
            case WalkState.ROVER_LOC:       determineAirlockExteriorToRoverWalkingSteps(
                    initialWalkState, destinationWalkState);
                                            break;
            case WalkState.OUTSIDE_LOC:     determineAirlockExteriorToOutsideWalkingSteps(
                    initialWalkState, destinationWalkState);
                                            break;
            default:                        throw new IllegalArgumentException("Invalid walk state type: " + 
                    initialWalkState.stateType);
        }
    }
    
    /**
     * Determine the walking steps between an airlock exterior and building interior location.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineAirlockExteriorToBuildingInteriorWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        Airlock airlock = initialWalkState.airlock;
        Building destinationBuilding = destinationWalkState.building;
        
        if (airlock.getEntity() instanceof Building) {
            
            Building airlockBuilding = (Building) airlock.getEntity();
            
            // Check if valid interior walking path between airlock building and destination building.
            Settlement settlement = airlockBuilding.getBuildingManager().getSettlement();
            if (settlement.getBuildingConnectorManager().hasValidPath(airlockBuilding, destinationBuilding)) {
                
                // Create enter airlock walk step.
                createEnterAirlockStep(airlock);
                
                // Create airlock interior state.
                WalkState interiorAirlockState = new WalkState(WalkState.INTERIOR_AIRLOCK);
                interiorAirlockState.airlock = airlock;
                interiorAirlockState.building = airlockBuilding;
                Point2D interiorAirlockPosition = airlock.getAvailableInteriorPosition();
                interiorAirlockState.xLoc = interiorAirlockPosition.getX();
                interiorAirlockState.yLoc = interiorAirlockPosition.getY();
                
                determineWalkingSteps(interiorAirlockState, destinationWalkState);
            }
            else {
                
                // Determine closest airlock to destination building.
                Airlock destinationAirlock = settlement.getClosestWalkableAvailableAirlock(destinationBuilding, 
                        initialWalkState.xLoc, initialWalkState.yLoc);
                if (destinationAirlock != null) {
                    
                    // Create walk step to exterior airlock position.
                    Point2D destinationAirlockExteriorPosition = destinationAirlock.getAvailableExteriorPosition();
                    createWalkExteriorStep(destinationAirlockExteriorPosition.getX(), 
                            destinationAirlockExteriorPosition.getY());
                    
                    // Create exterior airlock walk state.
                    WalkState exteriorAirlockState = new WalkState(WalkState.EXTERIOR_AIRLOCK);
                    exteriorAirlockState.airlock = destinationAirlock;
                    exteriorAirlockState.xLoc = destinationAirlockExteriorPosition.getX();
                    exteriorAirlockState.yLoc = destinationAirlockExteriorPosition.getY();
                    
                    determineWalkingSteps(exteriorAirlockState, destinationWalkState);
                }
                else {
                    
                    // Cannot walk to destination building.
                    canWalkAllSteps = false;
                    logger.severe("Cannot find walkable airlock from building airlock exterior to building interior.");
                }
            }
        }
        else if (airlock.getEntity() instanceof Rover) {
            
            Settlement settlement = destinationBuilding.getBuildingManager().getSettlement();
            
            // Determine closest airlock to destination building.
            Airlock destinationAirlock = settlement.getClosestWalkableAvailableAirlock(destinationBuilding, 
                    initialWalkState.xLoc, initialWalkState.yLoc);
            if (destinationAirlock != null) {
                
                // Create walk step to exterior airlock position.
                Point2D destinationAirlockExteriorPosition = destinationAirlock.getAvailableExteriorPosition();
                createWalkExteriorStep(destinationAirlockExteriorPosition.getX(), 
                        destinationAirlockExteriorPosition.getY());
                
                // Create exterior airlock walk state.
                WalkState exteriorAirlockState = new WalkState(WalkState.EXTERIOR_AIRLOCK);
                exteriorAirlockState.airlock = destinationAirlock;
                exteriorAirlockState.xLoc = destinationAirlockExteriorPosition.getX();
                exteriorAirlockState.yLoc = destinationAirlockExteriorPosition.getY();
                
                determineWalkingSteps(exteriorAirlockState, destinationWalkState);
            }
            else {
                
                // Cannot walk to destination building.
                canWalkAllSteps = false;
                logger.severe("Cannot find walkable airlock from rover airlock exterior to building interior.");
            }
        }
        else {
            throw new IllegalArgumentException("Invalid airlock entity for walking: " + airlock.getEntity());
        }
    }
    
    /**
     * Determine the walking steps between an airlock exterior and a rover interior location.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineAirlockExteriorToRoverWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        Airlock initialAirlock = initialWalkState.airlock;
        Rover destinationRover = destinationWalkState.rover;
        
        // Check if rover is in a garage or outside.
        Building garageBuilding = BuildingManager.getBuilding(destinationRover);
        if (garageBuilding != null) {
            
            Settlement settlement = garageBuilding.getBuildingManager().getSettlement();
            Airlock destinationAirlock = settlement.getClosestWalkableAvailableAirlock(garageBuilding, 
                    initialWalkState.xLoc, initialWalkState.yLoc);
            if (destinationAirlock != null) {
                
                if (initialAirlock.equals(destinationAirlock)) {
                    
                    // Create enter airlock walk step.
                    createEnterAirlockStep(initialAirlock);
                    
                    // Create airlock interior state.
                    WalkState interiorAirlockState = new WalkState(WalkState.INTERIOR_AIRLOCK);
                    interiorAirlockState.airlock = initialAirlock;
                    interiorAirlockState.building = (Building) initialAirlock.getEntity();
                    Point2D interiorAirlockPosition = initialAirlock.getAvailableInteriorPosition();
                    interiorAirlockState.xLoc = interiorAirlockPosition.getX();
                    interiorAirlockState.yLoc = interiorAirlockPosition.getY();
                    
                    determineWalkingSteps(interiorAirlockState, destinationWalkState);
                }
                else {
                    
                    // Create walk step to exterior airlock position.
                    Point2D destinationAirlockExteriorPosition = destinationAirlock.getAvailableExteriorPosition();
                    createWalkExteriorStep(destinationAirlockExteriorPosition.getX(), 
                            destinationAirlockExteriorPosition.getY());
                    
                    // Create exterior airlock walk state.
                    WalkState exteriorAirlockState = new WalkState(WalkState.EXTERIOR_AIRLOCK);
                    exteriorAirlockState.airlock = destinationAirlock;
                    exteriorAirlockState.xLoc = destinationAirlockExteriorPosition.getX();
                    exteriorAirlockState.yLoc = destinationAirlockExteriorPosition.getY();
                    
                    determineWalkingSteps(exteriorAirlockState, destinationWalkState);
                }
            }
            else {
                
                // Cannot walk to destination building.
                canWalkAllSteps = false;
                logger.severe("Cannot find walkable airlock from airlock exterior to rover in garage.");
            }
        }
        else {
            
            Object airlockEntity = initialAirlock.getEntity();
            
            if (airlockEntity.equals(destinationRover)) {
                
                // Create enter airlock walk step.
                createEnterAirlockStep(initialAirlock);
                
                // Create airlock interior state.
                WalkState interiorAirlockState = new WalkState(WalkState.INTERIOR_AIRLOCK);
                interiorAirlockState.airlock = initialAirlock;
                interiorAirlockState.rover = destinationRover;
                Point2D interiorAirlockPosition = initialAirlock.getAvailableInteriorPosition();
                interiorAirlockState.xLoc = interiorAirlockPosition.getX();
                interiorAirlockState.yLoc = interiorAirlockPosition.getY();
                
                determineWalkingSteps(interiorAirlockState, destinationWalkState);
            }
            else {
                
                Airlock destinationAirlock = destinationRover.getAirlock(); 
                
                // Create walk step to exterior airlock position.
                Point2D destinationAirlockExteriorPosition = destinationAirlock.getAvailableExteriorPosition();
                createWalkExteriorStep(destinationAirlockExteriorPosition.getX(), 
                        destinationAirlockExteriorPosition.getY());
                
                // Create exterior airlock walk state.
                WalkState exteriorAirlockState = new WalkState(WalkState.EXTERIOR_AIRLOCK);
                exteriorAirlockState.airlock = destinationAirlock;
                exteriorAirlockState.xLoc = destinationAirlockExteriorPosition.getX();
                exteriorAirlockState.yLoc = destinationAirlockExteriorPosition.getY();
                
                determineWalkingSteps(exteriorAirlockState, destinationWalkState);
            }
        }
    }
    
    /**
     * Determine the walking steps between an airlock exterior and an outside location.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineAirlockExteriorToOutsideWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        // Create walk step to exterior location.
        createWalkExteriorStep(destinationWalkState.xLoc, destinationWalkState.yLoc); 
    }
    
    /**
     * Determine the walking steps from an outside location.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineOutsideWalkingSteps(WalkState initialWalkState, WalkState destinationWalkState) {
        
        switch(destinationWalkState.stateType) {
        
            case WalkState.BUILDING_LOC:    determineOutsideToBuildingInteriorWalkingSteps(
                    initialWalkState, destinationWalkState);
                                            break;
            case WalkState.ROVER_LOC:       determineOutsideToRoverWalkingSteps(
                    initialWalkState, destinationWalkState);
                                            break;
            case WalkState.OUTSIDE_LOC:     determineOutsideToOutsideWalkingSteps(
                    initialWalkState, destinationWalkState);
                                            break;
            default:                        throw new IllegalArgumentException("Invalid walk state type: " + 
                    initialWalkState.stateType);
        }
    }
    
    /**
     * Determine the walking steps between an outside and a building interior location.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineOutsideToBuildingInteriorWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        Building destinationBuilding = destinationWalkState.building;
        Settlement settlement = destinationBuilding.getBuildingManager().getSettlement();
        
        // Determine closest airlock to destination building.
        Airlock destinationAirlock = settlement.getClosestWalkableAvailableAirlock(destinationBuilding, 
                initialWalkState.xLoc, initialWalkState.yLoc);
        if (destinationAirlock != null) {
            
            // Create walk step to exterior airlock position.
            Point2D destinationAirlockExteriorPosition = destinationAirlock.getAvailableExteriorPosition();
            createWalkExteriorStep(destinationAirlockExteriorPosition.getX(), 
                    destinationAirlockExteriorPosition.getY());
            
            // Create exterior airlock walk state.
            WalkState exteriorAirlockState = new WalkState(WalkState.EXTERIOR_AIRLOCK);
            exteriorAirlockState.airlock = destinationAirlock;
            exteriorAirlockState.xLoc = destinationAirlockExteriorPosition.getX();
            exteriorAirlockState.yLoc = destinationAirlockExteriorPosition.getY();
            
            determineWalkingSteps(exteriorAirlockState, destinationWalkState);
        }
        else {
            
            // Cannot walk to destination building.
            canWalkAllSteps = false;
            logger.severe("Cannot find walkable airlock from outside to building interior.");
        }
    }
    
    /**
     * Determine the walking steps between an outside and rover interior location.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destination walk state.
     */
    private void determineOutsideToRoverWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        Rover destinationRover = destinationWalkState.rover;
        
        // Check if rover is in a garage or outside.
        Building garageBuilding = BuildingManager.getBuilding(destinationRover);
        if (garageBuilding != null) {
            
            Settlement settlement = garageBuilding.getBuildingManager().getSettlement();
            Airlock destinationAirlock = settlement.getClosestWalkableAvailableAirlock(garageBuilding, 
                    initialWalkState.xLoc, initialWalkState.yLoc);
            if (destinationAirlock != null) {
                  
                // Create walk step to exterior airlock position.
                Point2D destinationAirlockExteriorPosition = destinationAirlock.getAvailableExteriorPosition();
                createWalkExteriorStep(destinationAirlockExteriorPosition.getX(), 
                        destinationAirlockExteriorPosition.getY());

                // Create exterior airlock walk state.
                WalkState exteriorAirlockState = new WalkState(WalkState.EXTERIOR_AIRLOCK);
                exteriorAirlockState.airlock = destinationAirlock;
                exteriorAirlockState.xLoc = destinationAirlockExteriorPosition.getX();
                exteriorAirlockState.yLoc = destinationAirlockExteriorPosition.getY();

                determineWalkingSteps(exteriorAirlockState, destinationWalkState);
            }
            else {
                
                // Cannot walk to destination building.
                canWalkAllSteps = false;
                logger.severe("Cannot find walkable airlock from outside to rover in garage.");
            }
        }
        else {
                
            Airlock destinationAirlock = destinationRover.getAirlock(); 

            // Create walk step to exterior airlock position.
            Point2D destinationAirlockExteriorPosition = destinationAirlock.getAvailableExteriorPosition();
            createWalkExteriorStep(destinationAirlockExteriorPosition.getX(), 
                    destinationAirlockExteriorPosition.getY());

            // Create exterior airlock walk state.
            WalkState exteriorAirlockState = new WalkState(WalkState.EXTERIOR_AIRLOCK);
            exteriorAirlockState.airlock = destinationAirlock;
            exteriorAirlockState.xLoc = destinationAirlockExteriorPosition.getX();
            exteriorAirlockState.yLoc = destinationAirlockExteriorPosition.getY();

            determineWalkingSteps(exteriorAirlockState, destinationWalkState);
        }
    }
    
    /**
     * Determine the walking steps between an outside and outside location.
     * @param initialWalkState the initial walk state.
     * @param destinationWalkState the destinatino walk state.
     */
    private void determineOutsideToOutsideWalkingSteps(WalkState initialWalkState, 
            WalkState destinationWalkState) {
        
        // Create walk step to exterior location.
        createWalkExteriorStep(destinationWalkState.xLoc, destinationWalkState.yLoc); 
    }
    
    /**
     * Create a rover interior walking step.
     * @param destXLoc the destination X location.
     * @param destYLoc the destination Y location.
     * @param destinationRover the destination rover.
     */
    private void createWalkRoverInteriorStep(double destXLoc, double destYLoc, 
            Rover destinationRover) {
        
        WalkStep walkStep = new WalkStep(WalkStep.ROVER_INTERIOR_WALK);
        walkStep.xLoc = destXLoc;
        walkStep.yLoc = destYLoc;
        walkStep.rover = destinationRover;
        walkingSteps.add(walkStep);
    }
    
    /**
     * Create a settlement interior walking step.
     * @param destXLoc the destination X location.
     * @param destYLoc the destination Y location.
     * @param destinationBuilding the destination building.
     */
    private void createWalkSettlementInteriorStep(double destXLoc, double destYLoc, 
            Building destinationBuilding) {
        
        WalkStep walkStep = new WalkStep(WalkStep.SETTLEMENT_INTERIOR_WALK);
        walkStep.xLoc = destXLoc;
        walkStep.yLoc = destYLoc;
        walkStep.building = destinationBuilding;
        walkingSteps.add(walkStep);
    }
    
    /**
     * Create an exterior walking step.
     * @param destXLoc the destination X location.
     * @param destYLoc the destination Y location.
     */
    private void createWalkExteriorStep(double destXLoc, double destYLoc) {
        
        WalkStep walkExterior = new WalkStep(WalkStep.EXTERIOR_WALK);
        walkExterior.xLoc = destXLoc;
        walkExterior.yLoc = destYLoc;
        walkingSteps.add(walkExterior);
    }
    
    /**
     * Create an exit airlock walking step.
     * @param airlock the airlock.
     */
    private void createExitAirlockStep(Airlock airlock) {
        
        WalkStep exitAirlockStep = new WalkStep(WalkStep.EXIT_AIRLOCK);
        exitAirlockStep.airlock = airlock;
        walkingSteps.add(exitAirlockStep);
    }
    
    /**
     * Create an enter airlock walking step.
     * @param airlock the airlock.
     */
    private void createEnterAirlockStep(Airlock airlock) {
        
        WalkStep enterAirlockStep = new WalkStep(WalkStep.ENTER_AIRLOCK);
        enterAirlockStep.airlock = airlock;
        walkingSteps.add(enterAirlockStep);
    }
    
    /**
     * Inner class for representing a walking state.
     */
    private class WalkState {
        
        // State types.
        private static final int BUILDING_LOC = 0;
        private static final int INTERIOR_AIRLOCK = 1;
        private static final int EXTERIOR_AIRLOCK = 2;
        private static final int ROVER_LOC = 3;
        private static final int OUTSIDE_LOC = 4;
        
        // Data members
        private int stateType;
        private double xLoc;
        private double yLoc;
        private Building building;
        private Rover rover;
        private Airlock airlock;
        
        private WalkState(int stateType) {
            this.stateType = stateType;
        }
    }
    
    /**
     * Inner class for representing a walking step.
     */
    class WalkStep implements Serializable {
        
        /** default serial id. */
        private static final long serialVersionUID = 1L;
        
        // Step types.
        static final int SETTLEMENT_INTERIOR_WALK = 0;
        static final int ROVER_INTERIOR_WALK = 1;
        static final int EXTERIOR_WALK = 2;
        static final int EXIT_AIRLOCK = 3;
        static final int ENTER_AIRLOCK = 4;
        static final int ENTER_GARAGE_ROVER = 5;
        static final int EXIT_GARAGE_ROVER = 6;
        
        // Data members
        int stepType;
        double xLoc;
        double yLoc;
        Building building;
        Rover rover;
        Airlock airlock;
        
        private WalkStep(int stepType) {
            this.stepType = stepType;
        }
    }
}
