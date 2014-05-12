/**
 * Mars Simulation Project
 * DigLocalIce.java
 * @version 3.06 2014-02-24
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Airlock;
import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.LocalBoundedObject;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.equipment.Bag;
import org.mars_sim.msp.core.equipment.EVASuit;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.goods.GoodsManager;
import org.mars_sim.msp.core.structure.goods.GoodsUtil;

/** 
 * The DigLocalIce class is a task for performing
 * collecting ice outside of a settlement.
 */
public class DigLocalIce 
extends EVAOperation 
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(DigLocalIce.class.getName());

	// TODO Phase name should be an enum
	private static final String COLLECT_ICE = "Collecting Ice";

	/**  Collection rate of ice during EVA (kg/millisol). */
	private static final double COLLECTION_RATE = 2D;

	// Domain members
	/** Airlock to be used for EVA. */
	private Airlock airlock;
	/** Bag for collecting ice. */
	private Bag bag;
	private Settlement settlement;

	/**
	 * Constructor.
	 * @param person the person performing the task.
	 * @throws Exception if error constructing the task.
	 */
	public DigLocalIce(Person person) {
        // Use EVAOperation constructor.
        super("Digging local ice", person, false, 0D);
        
        settlement = person.getSettlement();
        
        // Get an available airlock.
        airlock = getWalkableAvailableAirlock(person);
        if (airlock == null) {
            endTask();
        }
        
        // Determine digging location.
        Point2D.Double diggingLoc = determineDiggingLocation();
        setOutsideSiteLocation(diggingLoc.getX(), diggingLoc.getY());
        
        // Take bags for collecting ice.
        if (!hasBags()) {
            takeBag();
            
            // If bags are not available, end task.
            if (!hasBags()) {
                logger.fine(person.getName() + " not able to find bag to collect ice.");
                endTask();
            }
        }
        
        // Add task phases
        addPhase(COLLECT_ICE);
        
        logger.finest(person.getName() + " starting DigLocalIce task.");
    }
    
    /** 
     * Returns the weighted probability that a person might perform this task.
     * It should return a 0 if there is no chance to perform this task given the person and his/her situation.
     * @param person the person to perform the task
     * @return the weighted probability that a person might perform this task
     */
    public static double getProbability(Person person) {
        double result = 0D;

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            Settlement settlement = person.getSettlement();
            Inventory inv = settlement.getInventory();
            
            try {
                // Factor the value of ice at the settlement.
                GoodsManager manager = settlement.getGoodsManager();
                AmountResource iceResource = AmountResource.findAmountResource("ice");
                double value = manager.getGoodValuePerItem(GoodsUtil.getResourceGood(iceResource));
                result = value;
                if (result > 100D) {
                    result = 100D;
                }
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, "Error checking good value of ice.");
            }
            
            // Check at least one EVA suit at settlement.
            int numSuits = inv.findNumUnitsOfClass(EVASuit.class);
            if (numSuits == 0) {
                result = 0D;
            }
            
            // Check if at least one empty bag at settlement.
            int numEmptyBags = inv.findNumEmptyUnitsOfClass(Bag.class, false);
            if (numEmptyBags == 0) {
                result = 0D;
            }

            // Check if an airlock is available
            if (getWalkableAvailableAirlock(person) == null) {
                result = 0D;
            }

            // Check if it is night time.
            SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();
            if (surface.getSurfaceSunlight(person.getCoordinates()) == 0) {
                if (!surface.inDarkPolarRegion(person.getCoordinates())) {
                    result = 0D;
                }
            } 
            
            // Crowded settlement modifier
            if (settlement.getCurrentPopulationNum() > settlement.getPopulationCapacity()) {
                result *= 2D;
            }

            // Effort-driven task modifier.
            result *= person.getPerformanceRating();
            
            // Job modifier.
            Job job = person.getMind().getJob();
            if (job != null) {
                result *= job.getStartTaskProbabilityModifier(DigLocalIce.class);   
            }
        }
    
        return result;
    }
    
    /**
     * Checks if the person is carrying any bags.
     * @return true if carrying bags.
     */
    private boolean hasBags() {
        return person.getInventory().containsUnitClass(Bag.class);
    }

    /**
     * Takes the most full bag from the rover.
     */
    private void takeBag() {
        Bag emptyBag = null;
        Iterator<Unit> i = settlement.getInventory().findAllUnitsOfClass(Bag.class).iterator();
        while (i.hasNext() && (emptyBag == null)) {
            Bag foundBag = (Bag) i.next();
            if (foundBag.getInventory().isEmpty(false)) {
                emptyBag = foundBag;
            }
        }
        
        if (emptyBag != null) {
            if (person.getInventory().canStoreUnit(emptyBag, false)) {
                settlement.getInventory().retrieveUnit(emptyBag);
                person.getInventory().storeUnit(emptyBag);
                bag = emptyBag;
            }
            else {
                logger.severe(person.getName() + " unable to carry empty bag");
            }
        }
        else {
            logger.severe("Unable to find empty bag in settlement inventory");
        }
    }

    @Override
    protected String getOutsideSitePhase() {
        return COLLECT_ICE;
    }
    
    /**
     * Performs the method mapped to the task's current phase.
     * @param time the amount of time the phase is to be performed.
     * @return the remaining time after the phase has been performed.
     * @throws Exception if error in performing phase or if phase cannot be found.
     */
    protected double performMappedPhase(double time) {
        
        time = super.performMappedPhase(time);
        
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (COLLECT_ICE.equals(getPhase())) {
            return collectIce(time);
        }
        else {
            return time;
        }
    }
    
    @Override
    protected void addExperience(double time) {
        // Add experience to "EVA Operations" skill.
        // (1 base experience point per 100 millisols of time spent)
        double evaExperience = time / 100D;
        
        // Experience points adjusted by person's "Experience Aptitude" attribute.
        NaturalAttributeManager nManager = person.getNaturalAttributeManager();
        int experienceAptitude = nManager.getAttribute(NaturalAttribute.EXPERIENCE_APTITUDE);
        double experienceAptitudeModifier = (((double) experienceAptitude) - 50D) / 100D;
        evaExperience += evaExperience * experienceAptitudeModifier;
        evaExperience *= getTeachingExperienceModifier();
        person.getMind().getSkillManager().addExperience(SkillType.EVA_OPERATIONS, evaExperience);
        
        // If phase is collect ice, add experience to areology skill.
        if (COLLECT_ICE.equals(getPhase())) {
            // 1 base experience point per 10 millisols of collection time spent.
            // Experience points adjusted by person's "Experience Aptitude" attribute.
            double areologyExperience = time / 10D;
            areologyExperience += areologyExperience * experienceAptitudeModifier;
            person.getMind().getSkillManager().addExperience(SkillType.AREOLOGY, areologyExperience);
        }
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(2);
        results.add(SkillType.EVA_OPERATIONS);
        results.add(SkillType.AREOLOGY);
        return results; 
    }

    @Override
    public int getEffectiveSkillLevel() {
        SkillManager manager = person.getMind().getSkillManager();
        int EVAOperationsSkill = manager.getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
        int areologySkill = manager.getEffectiveSkillLevel(SkillType.AREOLOGY);
        return (int) Math.round((double)(EVAOperationsSkill + areologySkill) / 2D); 
    }
    
    /**
     * Determine location for digging ice.
     * @return digging X and Y location outside settlement.
     */
    private Point2D.Double determineDiggingLocation() {
        
        Point2D.Double newLocation = null;
        boolean goodLocation = false;
        for (int x = 0; (x < 5) && !goodLocation; x++) {
            for (int y = 0; (y < 10) && !goodLocation; y++) {
                if (airlock.getEntity() instanceof LocalBoundedObject) {
                    LocalBoundedObject boundedObject = (LocalBoundedObject) airlock.getEntity();
                    
                    double distance = RandomUtil.getRandomDouble(100D) + (x * 100D) + 50D;
                    double radianDirection = RandomUtil.getRandomDouble(Math.PI * 2D);
                    double newXLoc = boundedObject.getXLocation() - (distance * Math.sin(radianDirection));
                    double newYLoc = boundedObject.getYLocation() + (distance * Math.cos(radianDirection));
                    Point2D.Double boundedLocalPoint = new Point2D.Double(newXLoc, newYLoc);
                    
                    newLocation = LocalAreaUtil.getLocalRelativeLocation(boundedLocalPoint.getX(), 
                            boundedLocalPoint.getY(), boundedObject);
                    goodLocation = LocalAreaUtil.checkLocationCollision(newLocation.getX(), newLocation.getY(), 
                            person.getCoordinates());
                }
            }
        }
        
        return newLocation;
    }
    
    @Override
    public void endTask() {
        
        // Unload bag to rover's inventory.
        if (bag != null) {
            AmountResource iceResource = AmountResource.findAmountResource("ice");
            double collectedAmount = bag.getInventory().getAmountResourceStored(iceResource, false);
            double settlementCap = settlement.getInventory().getAmountResourceRemainingCapacity(
                    iceResource, false, false);
            
            // Try to store ice in settlement.
            if (collectedAmount < settlementCap) {
                bag.getInventory().retrieveAmountResource(iceResource, collectedAmount);
                settlement.getInventory().storeAmountResource(iceResource, collectedAmount, false);
            }

            // Store bag.
            person.getInventory().retrieveUnit(bag);
            settlement.getInventory().storeUnit(bag);
            
            // Recalculate settlement good value for output item.
            GoodsManager goodsManager = settlement.getGoodsManager();
            goodsManager.updateGoodValue(GoodsUtil.getResourceGood(iceResource), false);
        }
        
        super.endTask();
    }
    
    /**
     * Perform collect ice phase.
     * @param time time (millisol) to perform phase.
     * @return time (millisol) remaining after performing phase.
     */
    private double collectIce(double time) {
        
        // Check for an accident during the EVA operation.
        checkForAccident(time);
        
        // Check if there is reason to cut the collect 
        // ice phase short and return.
        if (shouldEndEVAOperation()) {
            setPhase(WALK_BACK_INSIDE);
            return time;
        }
        
        AmountResource ice = AmountResource.findAmountResource("ice");
        double remainingPersonCapacity = person.getInventory().getAmountResourceRemainingCapacity(
                ice, true, false);
        
        double iceCollected = time * COLLECTION_RATE;
        boolean finishedCollecting = false;
        if (iceCollected >= remainingPersonCapacity) {
            iceCollected = remainingPersonCapacity;
            finishedCollecting = true;
        }
        
        person.getInventory().storeAmountResource(ice, iceCollected, true);
        
        if (finishedCollecting) {
            setPhase(WALK_BACK_INSIDE);
        }
        
        // Add experience points
        addExperience(time);
        
        return 0D;
    }
    
    @Override
    public void destroy() {
        super.destroy();
        
        airlock = null;
        bag = null;
        settlement = null;
    }
}