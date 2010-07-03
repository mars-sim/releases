/**
 * Mars Simulation Project
 * TendGreenhouse.java
 * @version 2.90 2010-01-21
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.person.*;
import org.mars_sim.msp.core.person.ai.Skill;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.building.*;
import org.mars_sim.msp.core.structure.building.function.*;
import org.mars_sim.msp.core.structure.goods.GoodsManager;
import org.mars_sim.msp.core.structure.goods.GoodsUtil;

/** 
 * The TendGreenhouse class is a task for tending the greenhouse in a settlement.
 * This is an effort driven task.
 */
public class TendGreenhouse extends Task implements Serializable {
    
    private static String CLASS_NAME = "org.mars_sim.msp.simulation.person.ai.task.TendGreenhouse";
	
    private static Logger logger = Logger.getLogger(CLASS_NAME);
	
	// Task phase
	private static final String TENDING = "Tending";

	// Static members
	private static final double STRESS_MODIFIER = -.1D; // The stress modified per millisol.

    // Data members
    private Farming greenhouse; // The greenhouse the person is tending.

    /**
     * Constructor
     * @param person the person performing the task.
     * @throws Exception if error constructing task.
     */
    public TendGreenhouse(Person person) throws Exception {
        // Use Task constructor
        super("Tending Greenhouse", person, false, false, STRESS_MODIFIER, true, RandomUtil.getRandomDouble(100D));
        
        // Initialize data members
        if (person.getSettlement() != null)
        	setDescription("Tending Greenhouse at " + person.getSettlement().getName());
        else endTask();
        
        // Get available greenhouse if any.
        try {
        	Building farmBuilding = getAvailableGreenhouse(person);
        	if (farmBuilding != null) {
        		greenhouse = (Farming) farmBuilding.getFunction(Farming.NAME);
        		BuildingManager.addPersonToBuilding(person, farmBuilding);
        	}
        	else endTask();
        }
        catch (BuildingException e) {
			logger.log(Level.SEVERE,"TendGreenhouse: " + e.getMessage());
			endTask();
        }
        
        // Initialize phase
        addPhase(TENDING);
        setPhase(TENDING);
    }

	/** 
	 * Returns the weighted probability that a person might perform this task.
	 * @param person the person to perform the task
	 * @return the weighted probability that a person might perform this task
	 */
    public static double getProbability(Person person) {
        double result = 0D;
        
        if (person.getLocationSituation().equals(Person.INSETTLEMENT)) {
        	try {
        		// See if there is an available greenhouse.
        		Building farmingBuilding = getAvailableGreenhouse(person);
        		if (farmingBuilding != null) {
        			result = 50D;
        		
        			// Crowding modifier.
        			result *= Task.getCrowdingProbabilityModifier(person, farmingBuilding);
        			result *= Task.getRelationshipModifier(person, farmingBuilding);
        		}
        	
        		// Food value modifier.
        		GoodsManager manager = person.getSettlement().getGoodsManager();
        		AmountResource foodResource = AmountResource.findAmountResource("food");
        		double foodValue = manager.getGoodValuePerItem(GoodsUtil.getResourceGood(foodResource));
        		result *= foodValue;
        	}
        	catch (Exception e) {
        		logger.log(Level.SEVERE,"TendGreenhouse.getProbability(): " + e.getMessage());
        	}
        }
        
        // Effort-driven task modifier.
        result *= person.getPerformanceRating();

		// Job modifier.
        Job job = person.getMind().getJob();
		if (job != null) result *= job.getStartTaskProbabilityModifier(TendGreenhouse.class);

        return result;
    }
    
    /**
     * Performs the method mapped to the task's current phase.
     * @param time the amount of time (millisol) the phase is to be performed.
     * @return the remaining time (millisol) after the phase has been performed.
     * @throws Exception if error in performing phase or if phase cannot be found.
     */
    protected double performMappedPhase(double time) throws Exception {
    	if (getPhase() == null) throw new IllegalArgumentException("Task phase is null");
    	if (TENDING.equals(getPhase())) return tendingPhase(time);
    	else return time;
    }
    
    /**
     * Performs the tending phase.
     * @param time the amount of time (millisols) to perform the phase.
     * @return the amount of time (millisols) left over after performing the phase.
     * @throws Exception if error performing the phase.
     */
    private double tendingPhase(double time) throws Exception {
    	
        // Check if greenhouse has malfunction.
        if (greenhouse.getBuilding().getMalfunctionManager().hasMalfunction()) {
            endTask();
            return time;
        }
        
        // Determine amount of effective work time based on "Botany" skill.
        double workTime = time;
        int greenhouseSkill = getEffectiveSkillLevel();
        if (greenhouseSkill == 0) workTime /= 2;
        else workTime += workTime * (.2D * (double) greenhouseSkill);
        
        // Add this work to the greenhouse.
        greenhouse.addWork(workTime);

        // Add experience
        addExperience(time);
        
        // Check for accident in greenhouse.
        checkForAccident(time);
	    
        return 0D;
    }
    
	/**
	 * Adds experience to the person's skills used in this task.
	 * @param time the amount of time (ms) the person performed this task.
	 */
	protected void addExperience(double time) {
		// Add experience to "Botany" skill
		// (1 base experience point per 100 millisols of work)
		// Experience points adjusted by person's "Experience Aptitude" attribute.
        double newPoints = time / 100D;
        int experienceAptitude = person.getNaturalAttributeManager().getAttribute(
        	NaturalAttributeManager.EXPERIENCE_APTITUDE);
        newPoints += newPoints * ((double) experienceAptitude - 50D) / 100D;
		newPoints *= getTeachingExperienceModifier();
        person.getMind().getSkillManager().addExperience(Skill.BOTANY, newPoints);
	}

    /**
     * Check for accident in greenhouse.
     * @param time the amount of time working (in millisols)
     */
    private void checkForAccident(double time) {

        double chance = .001D;

        // Greenhouse farming skill modification.
        int skill = getEffectiveSkillLevel();
        if (skill <= 3) chance *= (4 - skill);
        else chance /= (skill - 2);

        // Modify based on the LUV's wear condition.
        chance *= greenhouse.getBuilding().getMalfunctionManager().getWearConditionAccidentModifier();
        
        if (RandomUtil.lessThanRandPercent(chance * time)) {
            // logger.info(person.getName() + " has accident while tending the greenhouse.");
            greenhouse.getBuilding().getMalfunctionManager().accident();
        }
    }
    
    /** 
     * Gets the greenhouse the person is tending.
     * @return greenhouse
     */
    public Farming getGreenhouse() {
        return greenhouse;
    }
    
    /**
     * Gets an available greenhouse that the person can use.
     * Returns null if no greenhouse is currently available.
     *
     * @param person the person
     * @return available greenhouse
     * @throws BuildingException if error finding farm building.
     */
    private static Building getAvailableGreenhouse(Person person) throws BuildingException {
     
        Building result = null;
     
        String location = person.getLocationSituation();
        if (location.equals(Person.INSETTLEMENT)) {
        	BuildingManager manager = person.getSettlement().getBuildingManager();
            List<Building> farmBuildings = manager.getBuildings(Farming.NAME);
			farmBuildings = BuildingManager.getNonMalfunctioningBuildings(farmBuildings);
			farmBuildings = getFarmsNeedingWork(farmBuildings);
			farmBuildings = BuildingManager.getLeastCrowdedBuildings(farmBuildings); 
			farmBuildings = BuildingManager.getBestRelationshipBuildings(person, farmBuildings);
			
			if (farmBuildings.size() > 0) result = (Building) farmBuildings.get(0);
        }
        
        return result;
    }
    
    /**
     * Gets a list of farm buildings needing work from a list of buildings with the farming function.
     * @param buildingList list of buildings with the farming function.
     * @return list of farming buildings needing work.
     * @throws BuildingException if any buildings in building list don't have the farming function.
     */
    private static List<Building> getFarmsNeedingWork(List<Building> buildingList) throws BuildingException {
    	List<Building> result = new ArrayList<Building>();
    	
    	Iterator<Building> i = buildingList.iterator();
    	while (i.hasNext()) {
    		Building building = i.next();
    		Farming farm = (Farming) building.getFunction(Farming.NAME);
    		if (farm.requiresWork()) result.add(building);
    	}
    	
    	return result;
    }
    
	/**
	 * Gets the effective skill level a person has at this task.
	 * @return effective skill level
	 */
	public int getEffectiveSkillLevel() {
		SkillManager manager = person.getMind().getSkillManager();
		return manager.getEffectiveSkillLevel(Skill.BOTANY);
	}  
	
	/**
	 * Gets a list of the skills associated with this task.
	 * May be empty list if no associated skills.
	 * @return list of skills as strings
	 */
	public List<String> getAssociatedSkills() {
		List<String> results = new ArrayList<String>(1);
		results.add(Skill.BOTANY);
		return results;
	}
}