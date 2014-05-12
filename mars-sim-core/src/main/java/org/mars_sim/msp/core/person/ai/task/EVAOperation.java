/**
 * Mars Simulation Project
 * EVAOperation.java
 * @version 3.06 2014-05-09
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Airlock;
import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.LocalBoundedObject;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.equipment.EVASuit;
import org.mars_sim.msp.core.mars.Mars;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.vehicle.Airlockable;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

/** 
 * The EVAOperation class is an abstract task that involves an extra vehicular activity. 
 */
public abstract class EVAOperation
extends Task
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default serial id. */
	private static Logger logger = Logger.getLogger(EVAOperation.class.getName());

	// TODO Task phase names should be an enum
	protected static final String WALK_TO_OUTSIDE_SITE = "Walk to Outside Site";
	protected static final String WALK_BACK_INSIDE = "Walk Back Inside";

	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = .5D;
	/** The base chance of an accident per millisol. */
	public static final double BASE_ACCIDENT_CHANCE = .001;

	// Data members
	/** Flag for ending EVA operation externally. */
	private boolean endEVA;
	private boolean hasSiteDuration;
	private double siteDuration;
	private double timeOnSite;
	private LocalBoundedObject interiorObject;
	private Point2D returnInsideLoc;
	private double outsideSiteXLoc;
	private double outsideSiteYLoc;

	/** 
	 * Constructor.
	 * @param name the name of the task
	 * @param person the person to perform the task
	 */
    public EVAOperation(String name, Person person, boolean hasSiteDuration, double siteDuration) { 
        super(name, person, true, false, STRESS_MODIFIER, false, 0D);
        
        // Initialize data members
        this.hasSiteDuration = hasSiteDuration;
        this.siteDuration = siteDuration;
        timeOnSite = 0D;
        
        // Check if person is in a settlement or a rover.
        if (LocationSituation.IN_SETTLEMENT == person.getLocationSituation()) {
            interiorObject = BuildingManager.getBuilding(person);
            if (interiorObject == null) {
                throw new IllegalStateException(person.getName() + " not in building.");
            }
        }
        else if (LocationSituation.IN_VEHICLE == person.getLocationSituation()) {
            if (person.getVehicle() instanceof Rover) {
                interiorObject = (Rover) person.getVehicle();
            }
            else {
                throw new IllegalStateException(person.getName() + " not in a rover vehicle: " + 
                        person.getVehicle());
            }
        }
        else {
            throw new IllegalStateException(person.getName() + 
                    " not in a value location situation to start EVA task: " + 
                    person.getLocationSituation());
        }
        
        // Add task phases.
        addPhase(WALK_TO_OUTSIDE_SITE);
        addPhase(WALK_BACK_INSIDE);
        
        // Set initial phase.
        setPhase(WALK_TO_OUTSIDE_SITE);
    }
    
    /**
     * Check if EVA should end.
     */
    public void endEVA() {
    	endEVA = true;
    }
    
    /**
     * Add time at EVA site.
     * @param time the time to add (millisols).
     * @return true if site phase should end.
     */
    protected boolean addTimeOnSite(double time) {
        
        boolean result = false;
        
        timeOnSite += time;
        
        if (hasSiteDuration && (timeOnSite >= siteDuration)) {
            result = true;
        }
        
        return result;
    }
    
    /**
     * Gets the phase string for the outside site phase.
     * @return phase string.
     */
    protected abstract String getOutsideSitePhase();
    
    /**
     * Set the outside side local location.
     * @param xLoc the X location.
     * @param yLoc the Y location.
     */
    protected void setOutsideSiteLocation(double xLoc, double yLoc) {
        outsideSiteXLoc = xLoc;
        outsideSiteYLoc = yLoc;
    }
    
    @Override
    protected double performMappedPhase(double time) {
        
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (WALK_TO_OUTSIDE_SITE.equals(getPhase())) {
            return walkToOutsideSitePhase(time);
        }
        else if (WALK_BACK_INSIDE.equals(getPhase())) {
            return walkBackInsidePhase(time);
        }
        else {
            return time;
        }
    }
    
    /**
     * Perform the walk to outside site phase.
     * @param time the time to perform the phase.
     * @return remaining time after performing the phase.
     */
    private double walkToOutsideSitePhase(double time) {
        
        // If not at field work site location, create walk outside subtask.
        Point2D personLocation = new Point2D.Double(person.getXLocation(), person.getYLocation());
        Point2D outsideLocation = new Point2D.Double(outsideSiteXLoc, outsideSiteYLoc);
        boolean closeToLocation = LocalAreaUtil.areLocationsClose(personLocation, outsideLocation);
        
        if (LocationSituation.OUTSIDE != person.getLocationSituation() || !closeToLocation) {
            if (Walk.canWalkAllSteps(person, outsideSiteXLoc, outsideSiteYLoc, null)) {
                Task walkingTask = new Walk(person, outsideSiteXLoc, outsideSiteYLoc, null);
                addSubTask(walkingTask);
            }
            else {
                logger.severe(person.getName() + " cannot walk to outside site.");
                endTask();
            }
        }
        else {
            setPhase(getOutsideSitePhase());
        }
        
        return time;
    }
    
    /**
     * Perform the walk back inside phase.
     * @param time the time to perform the phase.
     * @return remaining time after performing the phase.
     */
    private double walkBackInsidePhase(double time) {
        
        if ((returnInsideLoc == null) || !LocalAreaUtil.checkLocationWithinLocalBoundedObject(
                returnInsideLoc.getX(), returnInsideLoc.getY(), interiorObject)) {
            // Set return location.        
            Point2D rawReturnInsideLoc = LocalAreaUtil.getRandomInteriorLocation(interiorObject);
            returnInsideLoc = LocalAreaUtil.getLocalRelativeLocation(rawReturnInsideLoc.getX(), 
                    rawReturnInsideLoc.getY(), interiorObject);
        }
        
        // If not at return inside location, create walk inside subtask.
        Point2D personLocation = new Point2D.Double(person.getXLocation(), person.getYLocation());
        boolean closeToLocation = LocalAreaUtil.areLocationsClose(personLocation, returnInsideLoc);
        
        if (LocationSituation.OUTSIDE == person.getLocationSituation() || !closeToLocation) {
            if (Walk.canWalkAllSteps(person, returnInsideLoc.getX(), returnInsideLoc.getY(), interiorObject)) {
                Task walkingTask = new Walk(person, returnInsideLoc.getX(), returnInsideLoc.getY(), interiorObject);
                addSubTask(walkingTask);
            }
            else {
                logger.severe(person.getName() + " cannot walk back to inside location.");
                endTask();
            }
        }
        else {
            endTask();
        }
        
        return time;
    }

    /**
     * Checks if situation requires the EVA operation to end prematurely 
     * and the person should return to the airlock.
     * @return true if EVA operation should end
     */
    protected boolean shouldEndEVAOperation() {

        boolean result = false;
        
        // Check end EVA flag.
        if (endEVA) {
            result = true;
        }
        // Check if any EVA problem.
        else if (checkEVAProblem(person)) {
            result = true;
        }
	
        return result;
    }
    
    /**
     * Checks if there is an EVA problem for a person.
     * @param person the person.
     * @return true if an EVA problem.
     */
    public static boolean checkEVAProblem(Person person) {
        
        boolean result = false;
        
        // Check if it is night time. 
        Mars mars = Simulation.instance().getMars();
        if (mars.getSurfaceFeatures().getSurfaceSunlight(person.getCoordinates()) == 0) {
            logger.fine(person.getName() + " should end EVA: night time.");
            if (!mars.getSurfaceFeatures().inDarkPolarRegion(person.getCoordinates()))
                result = true;
        }

        EVASuit suit = (EVASuit) person.getInventory().findUnitOfClass(EVASuit.class);
        if (suit == null) {
            logger.fine(person.getName() + " should end EVA: No EVA suit found.");
            return true;
        }
        Inventory suitInv = suit.getInventory();
    
        try {
            // Check if EVA suit is at 15% of its oxygen capacity.
            AmountResource oxygenResource = AmountResource.findAmountResource("oxygen");
            double oxygenCap = suitInv.getAmountResourceCapacity(oxygenResource, false);
            double oxygen = suitInv.getAmountResourceStored(oxygenResource, false);
            if (oxygen <= (oxygenCap * .15D)) {
                logger.fine(person.getName() + " should end EVA: EVA suit oxygen level less than 15%"); 
                result = true;
            }

            // Check if EVA suit is at 15% of its water capacity.
            AmountResource waterResource = AmountResource.findAmountResource("water");
            double waterCap = suitInv.getAmountResourceCapacity(waterResource, false);
            double water = suitInv.getAmountResourceStored(waterResource, false);
            if (water <= (waterCap * .15D)) {
                logger.fine(person.getName() + " should end EVA: EVA suit water level less than 15%");  
                result = true;
            }

            // Check if life support system in suit is working properly.
            if (!suit.lifeSupportCheck()) {
                logger.fine(person.getName() + " should end EVA: EVA suit failed life support check."); 
                result = true;
            }
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
        }

        // Check if suit has any malfunctions.
        if (suit.getMalfunctionManager().hasMalfunction()) {
            logger.fine(person.getName() + " should end EVA: EVA suit has malfunction.");   
            result = true;
        }
    
        // Check if person's medical condition is sufficient to continue phase.
        if (person.getPerformanceRating() < .5D) {
            logger.fine(person.getName() + " should end EVA: medical problems.");   
            result = true;
        }
        
        return result;
    }

    /**
     * Check for accident with EVA suit.
     * @param time the amount of time on EVA (in millisols)
     */
    protected void checkForAccident(double time) {

        EVASuit suit = (EVASuit) person.getInventory().findUnitOfClass(EVASuit.class);
        if (suit != null) {
	    
            double chance = BASE_ACCIDENT_CHANCE;

            // EVA operations skill modification.
            int skill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
            if (skill <= 3) chance *= (4 - skill);
            else chance /= (skill - 2);

            // Modify based on the suit's wear condition.
            chance *= suit.getMalfunctionManager().getWearConditionAccidentModifier();
            
            if (RandomUtil.lessThanRandPercent(chance * time)) {
                logger.fine(person.getName() + " has accident during EVA operation.");
                suit.getMalfunctionManager().accident();
            }
        }
    }
    
    /**
     * Gets the closest available airlock to a given location that has a walkable path 
     * from the person's current location.
     * @param person the person.
     * @param double xLocation the destination's X location.
     * @param double yLocation the destination's Y location.
     * @return airlock or null if none available
     */
    public static Airlock getClosestWalkableAvailableAirlock(Person person, double xLocation, 
            double yLocation) {
        Airlock result = null;
        LocationSituation location = person.getLocationSituation();
        
        if (location == LocationSituation.IN_SETTLEMENT) {
            Settlement settlement = person.getSettlement();
            result = settlement.getClosestWalkableAvailableAirlock(person, xLocation, yLocation);
        }
        else if (location == LocationSituation.IN_VEHICLE) {
            Vehicle vehicle = person.getVehicle();
            if (vehicle instanceof Airlockable) {
                result = ((Airlockable) vehicle).getAirlock();
            }
        }
        
        return result;
    }
    
    /**
     * Gets an available airlock to a given location that has a walkable path 
     * from the person's current location.
     * @param person the person.
     * @return airlock or null if none available
     */
    public static Airlock getWalkableAvailableAirlock(Person person) {
        
        return getClosestWalkableAvailableAirlock(person, person.getXLocation(), person.getYLocation());
    }
    
    @Override
    public void destroy() {
        super.destroy();
        
        interiorObject = null;
    }
}