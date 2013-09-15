/**
 * Mars Simulation Project
 * DigLocalRegolith.java
 * @version 3.04 2013-02-10
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.task;

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
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.Skill;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.goods.GoodsManager;
import org.mars_sim.msp.core.structure.goods.GoodsUtil;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 
 * The DigLocalRegolith class is a task for performing
 * collecting regolith outside a settlement.
 */
public class DigLocalRegolith extends EVAOperation implements Serializable {

    private static Logger logger = Logger.getLogger(DigLocalRegolith.class.getName());
    
    // Phase name
    private static final String COLLECT_REGOLITH = "Collecting Regolith";
    
    //  Collection rate of regolith during EVA (kg/millisol).
    private static final double COLLECTION_RATE = 20D;
    
    // Domain members
    private Airlock airlock; // Airlock to be used for EVA.
    private Bag bag; // Bag for collecting regolith.
    private Settlement settlement;
    
    /**
     * Constructor
     * @param person the person performing the task.
     * @throws Exception if error constructing the task.
     */
    public DigLocalRegolith(Person person) {
        // Use EVAOperation constructor.
        super("Digging local regolith", person);
        
        settlement = person.getSettlement();
        
        // Get an available airlock.
        airlock = getAvailableAirlock(person);
        if (airlock == null) endTask();
        
        // Initialize phase.
        addPhase(COLLECT_REGOLITH);
        
        logger.finest(person.getName() + " starting DigLocalRegolith task.");
    }
    
    /** 
     * Returns the weighted probability that a person might perform this task.
     * It should return a 0 if there is no chance to perform this task given the person and his/her situation.
     * @param person the person to perform the task
     * @return the weighted probability that a person might perform this task
     */
    public static double getProbability(Person person) {
        double result = 0D;

        if (person.getLocationSituation().equals(Person.INSETTLEMENT)) {
            Settlement settlement = person.getSettlement();
            Inventory inv = settlement.getInventory();
            
            try {
                // Factor the value of regolith at the settlement.
                GoodsManager manager = settlement.getGoodsManager();
                AmountResource regolithResource = AmountResource.findAmountResource("regolith");
                double value = manager.getGoodValuePerItem(GoodsUtil.getResourceGood(regolithResource));
                result = value * Bag.CAPACITY;
                if (result > 100D) result = 100D;
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, "Error checking good value of regolith.");
            }
            
            // Check at least one EVA suit at settlement.
            int numSuits = inv.findNumUnitsOfClass(EVASuit.class);
            if (numSuits == 0) result = 0D;
            
            // Check if at least one empty bag at settlement.
            int numEmptyBags = inv.findNumEmptyUnitsOfClass(Bag.class, false);
            if (numEmptyBags == 0) result = 0D;

            // Check if an airlock is available
            if (getAvailableAirlock(person) == null) result = 0D;

            // Check if it is night time.
            SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();
            if (surface.getSurfaceSunlight(person.getCoordinates()) == 0) {
                if (!surface.inDarkPolarRegion(person.getCoordinates()))
                    result = 0D;
            } 
            
            // Crowded settlement modifier
            if (settlement.getCurrentPopulationNum() > settlement.getPopulationCapacity()) result *= 2D;

            // Effort-driven task modifier.
            result *= person.getPerformanceRating();
            
            // Job modifier.
            Job job = person.getMind().getJob();
            if (job != null) result *= job.getStartTaskProbabilityModifier(DigLocalRegolith.class);   
        }
    
        return result;
    }
    
    /**
     * Performs the method mapped to the task's current phase.
     * @param time the amount of time the phase is to be performed.
     * @return the remaining time after the phase has been performed.
     * @throws Exception if error in performing phase or if phase cannot be found.
     */
    protected double performMappedPhase(double time) {
        if (getPhase() == null) throw new IllegalArgumentException("Task phase is null");
        if (EVAOperation.EXIT_AIRLOCK.equals(getPhase())) return exitEVA(time);
        if (COLLECT_REGOLITH.equals(getPhase())) return collectRegolith(time);
        if (EVAOperation.ENTER_AIRLOCK.equals(getPhase())) return enterEVA(time);
        else return time;
    }
    
    @Override
    protected void addExperience(double time) {
        // Add experience to "EVA Operations" skill.
        // (1 base experience point per 100 millisols of time spent)
        double evaExperience = time / 100D;
        
        // Experience points adjusted by person's "Experience Aptitude" attribute.
        NaturalAttributeManager nManager = person.getNaturalAttributeManager();
        int experienceAptitude = nManager.getAttribute(NaturalAttributeManager.EXPERIENCE_APTITUDE);
        double experienceAptitudeModifier = (((double) experienceAptitude) - 50D) / 100D;
        evaExperience += evaExperience * experienceAptitudeModifier;
        evaExperience *= getTeachingExperienceModifier();
        person.getMind().getSkillManager().addExperience(Skill.EVA_OPERATIONS, evaExperience);
        
        // If phase is collect regolith, add experience to areology skill.
        if (COLLECT_REGOLITH.equals(getPhase())) {
            // 1 base experience point per 10 millisols of collection time spent.
            // Experience points adjusted by person's "Experience Aptitude" attribute.
            double areologyExperience = time / 10D;
            areologyExperience += areologyExperience * experienceAptitudeModifier;
            person.getMind().getSkillManager().addExperience(Skill.AREOLOGY, areologyExperience);
        }
    }

    @Override
    public List<String> getAssociatedSkills() {
        List<String> results = new ArrayList<String>(2);
        results.add(Skill.EVA_OPERATIONS);
        results.add(Skill.AREOLOGY);
        return results; 
    }

    @Override
    public int getEffectiveSkillLevel() {
        SkillManager manager = person.getMind().getSkillManager();
        int EVAOperationsSkill = manager.getEffectiveSkillLevel(Skill.EVA_OPERATIONS);
        int areologySkill = manager.getEffectiveSkillLevel(Skill.AREOLOGY);
        return (int) Math.round((double)(EVAOperationsSkill + areologySkill) / 2D); 
    }
    
    /**
     * Perform the exit airlock phase of the task.
     * @param time the time to perform this phase (in millisols)
     * @return the time remaining after performing this phase (in millisols)
     * @throws Exception if error exiting the airlock.
     */
    private double exitEVA(double time) {
        
        try {
            time = exitAirlock(time, airlock);
        
            // Add experience points
            addExperience(time);
        }
        catch (Exception e) {
            // Person unable to exit airlock.
            endTask();
        }
        
        if (exitedAirlock) {
            // Take bag.
            if (bag == null) {
                Bag emptyBag = null;
                Iterator<Unit> i = settlement.getInventory().findAllUnitsOfClass(Bag.class).iterator();
                while (i.hasNext() && (emptyBag == null)) {
                    Bag foundBag = (Bag) i.next();
                    if (foundBag.getInventory().getTotalInventoryMass(false) == 0D) emptyBag = foundBag;
                }
                
                if (emptyBag != null) {
                    if (person.getInventory().canStoreUnit(emptyBag, false)) {
                        settlement.getInventory().retrieveUnit(emptyBag);
                        person.getInventory().storeUnit(emptyBag);
                        bag = emptyBag;
                    }
                }
            }
            
            if (bag != null) {
                setPhase(COLLECT_REGOLITH);
                
                // Move person to digging location.
                moveToDiggingLocation();
            }
            else {
                logger.log(Level.SEVERE, "Unable to find empty bag in settlement inventory");
                setPhase(ENTER_AIRLOCK);
            }
        }
        return time;
    }
    
    /**
     * Move person to a location for digging regolith.
     */
    private void moveToDiggingLocation() {
        
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
        
        person.setXLocation(newLocation.getX());
        person.setYLocation(newLocation.getY());
    }
    
    /**
     * Perform the enter airlock phase of the task.
     * @param time amount of time to perform the phase
     * @return time remaining after performing the phase
     * @throws Exception if error entering airlock.
     */
    private double enterEVA(double time) {
        time = enterAirlock(time, airlock);
        
        // Add experience points
        addExperience(time);
        
        if (enteredAirlock) {
            if (bag != null) {
                AmountResource regolithResource = AmountResource.findAmountResource("regolith");
                double collectedAmount = bag.getInventory().getAmountResourceStored(regolithResource, false);
                double settlementCap = settlement.getInventory().getAmountResourceRemainingCapacity(
                        regolithResource, false, false);
                
                // Try to store regolith in settlement.
                if (collectedAmount < settlementCap) {
                    bag.getInventory().retrieveAmountResource(regolithResource, collectedAmount);
                    settlement.getInventory().storeAmountResource(regolithResource, collectedAmount, false);
                }

                // Store bag.
                person.getInventory().retrieveUnit(bag);
                settlement.getInventory().storeUnit(bag);
                
                // Recalculate settlement good value for output item.
                GoodsManager goodsManager = settlement.getGoodsManager();
                goodsManager.updateGoodValue(GoodsUtil.getResourceGood(regolithResource), false);
            }
            endTask();
        }
        return time;
    }
    
    /**
     * Perform collect regolith phase.
     * @param time time (millisol) to perform phase.
     * @return time (millisol) remaining after performing phase.
     * @throws Exception
     */
    private double collectRegolith(double time) {
        
        // Check for an accident during the EVA operation.
        checkForAccident(time);
        
        // Check if there is reason to cut the collection phase short and return
        // to the airlock.
        if (shouldEndEVAOperation()) {
            setPhase(EVAOperation.ENTER_AIRLOCK);
            return time;
        }
        
        AmountResource regolith = AmountResource.findAmountResource("regolith");
        double remainingPersonCapacity = person.getInventory().getAmountResourceRemainingCapacity(
                regolith, true, false);
        
        double regolithCollected = time * COLLECTION_RATE;
        boolean finishedCollecting = false;
        if (regolithCollected > remainingPersonCapacity) {
            regolithCollected = remainingPersonCapacity;
            finishedCollecting = true;
        }
        
        person.getInventory().storeAmountResource(regolith, regolithCollected, true);
        
        if (finishedCollecting) setPhase(ENTER_AIRLOCK);
        
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