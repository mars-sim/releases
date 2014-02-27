/**
 * Mars Simulation Project
 * MaintainGroundVehicleEVA.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.task;

import org.mars_sim.msp.core.Airlock;
import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.LocalBoundedObject;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.Skill;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.function.GroundVehicleMaintenance;
import org.mars_sim.msp.core.vehicle.GroundVehicle;
import org.mars_sim.msp.core.vehicle.Vehicle;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/** 
 * The MaintainGroundVehicleGarage class is a task for performing
 * preventive maintenance on ground vehicles outside a settlement.
 */
public class MaintainGroundVehicleEVA extends EVAOperation implements Serializable {
	
    private static Logger logger = Logger.getLogger(MaintainGroundVehicleEVA.class.getName());
	
    // Phase names
    private static final String WALK_TO_VEHICLE = "Walk to Vehicle";
    private static final String MAINTAIN_VEHICLE = "Maintain Vehicle";
    private static final String WALK_TO_AIRLOCK = "Walk to Airlock";
 
    // Data members.
    private GroundVehicle vehicle; // Vehicle to be maintained.
    private Airlock airlock; // Airlock to be used for EVA.
    private double maintenanceXLoc;
    private double maintenanceYLoc;
    private double enterAirlockXLoc;
    private double enterAirlockYLoc;
    
	/** 
	 * Constructor
	 * @param person the person to perform the task
	 * @throws Exception if error constructing task.
	 */
    public MaintainGroundVehicleEVA(Person person) {
        super("Performing Vehicle Maintenance", person);
   
        // Choose an available needy ground vehicle.
        vehicle = getNeedyGroundVehicle(person);
        if (vehicle != null) {
            vehicle.setReservedForMaintenance(true);
            
            // Determine location for maintenance.
            Point2D maintenanceLoc = determineMaintenanceLocation();
            maintenanceXLoc = maintenanceLoc.getX();
            maintenanceYLoc = maintenanceLoc.getY();
            
            // Get an available airlock.
            airlock = getClosestWalkableAvailableAirlock(person, vehicle.getXLocation(), 
                    vehicle.getYLocation());
            if (airlock == null) {
                endTask();
            }
            else {
                // Determine location for reentering building airlock.
                Point2D enterAirlockLoc = determineAirlockEnteringLocation();
                enterAirlockXLoc = enterAirlockLoc.getX();
                enterAirlockYLoc = enterAirlockLoc.getY();
            }
        }
        else {
            endTask();
        }
        
        // Initialize phase.
        addPhase(WALK_TO_VEHICLE);
        addPhase(MAINTAIN_VEHICLE);
        addPhase(WALK_TO_AIRLOCK);
        
        logger.finest(person.getName() + " starting MaintainGroundVehicleEVA task.");
    }
    
    /** 
     * Returns the weighted probability that a person might perform this task.
     * It should return a 0 if there is no chance to perform this task given the person and his/her situation.
     * @param person the person to perform the task
     * @return the weighted probability that a person might perform this task
     */
    public static double getProbability(Person person) {
        double result = 0D;

		// Get all vehicles needing maintenance.
		if (person.getLocationSituation().equals(Person.INSETTLEMENT)) {
			Iterator<Vehicle> i = getAllVehicleCandidates(person).iterator();
			while (i.hasNext()) {
				MalfunctionManager manager = i.next().getMalfunctionManager();
				double entityProb = (manager.getEffectiveTimeSinceLastMaintenance() / 20D);
				if (entityProb > 100D) entityProb = 100D;
				result += entityProb;
			}
		}

		// Determine if settlement has a garage.
        if (person.getLocationSituation().equals(Person.INSETTLEMENT)) {	
			if (person.getSettlement().getBuildingManager().getBuildings(GroundVehicleMaintenance.NAME).size() > 0) 
				result = 0D;
        }

        // Check if an airlock is available
        if (getWalkableAvailableAirlock(person) == null) {
            result = 0D;
        }

        // Check if it is night time.
        SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();
        if (surface.getSurfaceSunlight(person.getCoordinates()) == 0) {
        	if (!surface.inDarkPolarRegion(person.getCoordinates()))
        		result = 0D;
        } 
        
		// Crowded settlement modifier
		if (person.getLocationSituation().equals(Person.INSETTLEMENT)) {
			Settlement settlement = person.getSettlement();
			if (settlement.getCurrentPopulationNum() > settlement.getPopulationCapacity()) result *= 2D;
		}

        // Effort-driven task modifier.
        result *= person.getPerformanceRating();
        
		// Job modifier.
        Job job = person.getMind().getJob();
		if (job != null) result *= job.getStartTaskProbabilityModifier(MaintainGroundVehicleEVA.class);        
	
        return result;
    }
    
    /**
     * Determine location to perform vehicle maintenance.
     * @return location.
     */
    private Point2D determineMaintenanceLocation() {
        
        Point2D.Double newLocation = null;
        boolean goodLocation = false;
        for (int x = 0; (x < 50) && !goodLocation; x++) {
            Point2D.Double boundedLocalPoint = LocalAreaUtil.getRandomExteriorLocation(vehicle, 1D);
            newLocation = LocalAreaUtil.getLocalRelativeLocation(boundedLocalPoint.getX(), 
                    boundedLocalPoint.getY(), vehicle);
            goodLocation = LocalAreaUtil.checkLocationCollision(newLocation.getX(), newLocation.getY(), 
                    person.getCoordinates());
        }
        
        return newLocation;
    }
    
    /**
     * Determine location outside building airlock.
     * @return location.
     */
    private Point2D determineAirlockEnteringLocation() {
        
        Point2D result = null;
        
        // Move the person to a random location outside the airlock entity.
        if (airlock.getEntity() instanceof LocalBoundedObject) {
            LocalBoundedObject entityBounds = (LocalBoundedObject) airlock.getEntity();
            Point2D.Double newLocation = null;
            boolean goodLocation = false;
            for (int x = 0; (x < 20) && !goodLocation; x++) {
                Point2D.Double boundedLocalPoint = LocalAreaUtil.getRandomExteriorLocation(entityBounds, 1D);
                newLocation = LocalAreaUtil.getLocalRelativeLocation(boundedLocalPoint.getX(), 
                        boundedLocalPoint.getY(), entityBounds);
                goodLocation = LocalAreaUtil.checkLocationCollision(newLocation.getX(), newLocation.getY(), 
                        person.getCoordinates());
            }
            
            result = newLocation;
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
    	if (getPhase() == null) {
    	    throw new IllegalArgumentException("Task phase is null");
    	}
    	else if (EVAOperation.EXIT_AIRLOCK.equals(getPhase())) {
    	    return exitEVAPhase(time);
    	}
    	else if (WALK_TO_VEHICLE.equals(getPhase())) {
            return walkToVehiclePhase(time);
        }
    	else if (MAINTAIN_VEHICLE.equals(getPhase())) {
    	    return maintainVehiclePhase(time);
    	}
    	else if (WALK_TO_AIRLOCK.equals(getPhase())) {
            return walkToAirlockPhase(time);
        }
    	else if (EVAOperation.ENTER_AIRLOCK.equals(getPhase())) {
    	    return enterEVAPhase(time);
    	}
    	else {
    	    return time;
    	}
    }
    
	/**
	 * Adds experience to the person's skills used in this task.
	 * @param time the amount of time (ms) the person performed this task.
	 */
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
		
		// If phase is maintain vehicle, add experience to mechanics skill.
		if (MAINTAIN_VEHICLE.equals(getPhase())) {
			// 1 base experience point per 100 millisols of collection time spent.
			// Experience points adjusted by person's "Experience Aptitude" attribute.
			double mechanicsExperience = time / 100D;
			mechanicsExperience += mechanicsExperience * experienceAptitudeModifier;
			person.getMind().getSkillManager().addExperience(Skill.MECHANICS, mechanicsExperience);
		}
	}
   
    /**
     * Perform the exit airlock phase of the task.
     * @param time the time to perform this phase (in millisols)
     * @return the time remaining after performing this phase (in millisols)
     * @throws Exception if error exiting the airlock.
     */
    private double exitEVAPhase(double time) {
    	
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
            setPhase(WALK_TO_VEHICLE);
        }
        return time;
    }
    
    /**
     * Perform the walk to vehicle maintenance location phase.
     * @param time the time available (millisols).
     * @return remaining time after performing phase (millisols).
     */
    private double walkToVehiclePhase(double time) {
        
        // Check for an accident during the EVA walk.
        checkForAccident(time);
        
        // Check if there is reason to cut the EVA walk phase short and return
        // to the rover.
        if (shouldEndEVAOperation()) {
            setPhase(WALK_TO_AIRLOCK);
            return time;
        }
        
        // If not at vehicle maintenance location, create walk outside subtask.
        if ((person.getXLocation() != maintenanceXLoc) || (person.getYLocation() != maintenanceYLoc)) {
            Task walkingTask = new WalkOutside(person, person.getXLocation(), person.getYLocation(), 
                    maintenanceXLoc, maintenanceYLoc, false);
            addSubTask(walkingTask);
        }
        else {
            setPhase(MAINTAIN_VEHICLE);
        }
        
        return time;
    }
    
    /**
     * Perform the walk to airlock phase.
     * @param time the time available (millisols).
     * @return remaining time after performing phase (millisols).
     */
    private double walkToAirlockPhase(double time) {
        
        // Check for an accident during the EVA walk.
        checkForAccident(time);
        
        // If not at outside airlock location, create walk outside subtask.
        if ((person.getXLocation() != enterAirlockXLoc) || (person.getYLocation() != enterAirlockYLoc)) {
            Task walkingTask = new WalkOutside(person, person.getXLocation(), person.getYLocation(), 
                    enterAirlockXLoc, enterAirlockYLoc, true);
            addSubTask(walkingTask);
        }
        else {
            setPhase(EVAOperation.ENTER_AIRLOCK);
        }
        
        return time;
    }
    
    /**
     * Perform the maintain vehicle phase of the task.
     * @param time the time to perform this phase (in millisols)
     * @return the time remaining after performing this phase (in millisols)
     * @throws Exception if error maintaining vehicle.
     */
    private double maintainVehiclePhase(double time) {
        
        MalfunctionManager manager = vehicle.getMalfunctionManager();
        boolean malfunction = manager.hasMalfunction();
        boolean finishedMaintenance = (manager.getEffectiveTimeSinceLastMaintenance() == 0D);
        if (finishedMaintenance) vehicle.setReservedForMaintenance(false);
        
        if (finishedMaintenance || malfunction || shouldEndEVAOperation()) {
            setPhase(WALK_TO_AIRLOCK);
            return time;
        }
        
        // Determine effective work time based on "Mechanic" and "EVA Operations" skills.
        double workTime = time;
        int skill = getEffectiveSkillLevel();
        if (skill == 0) workTime /= 2;
        if (skill > 1) workTime += workTime * (.2D * skill);

        // Add repair parts if necessary.
        Inventory inv = containerUnit.getInventory();
        if (Maintenance.hasMaintenanceParts(inv, vehicle)) {
        	Map<Part, Integer> parts = new HashMap<Part, Integer>(manager.getMaintenanceParts());
        	Iterator<Part> j = parts.keySet().iterator();
        	while (j.hasNext()) {
        		Part part = j.next();
        		int number = parts.get(part);
        		inv.retrieveItemResources(part, number);
        		manager.maintainWithParts(part, number);
        	}
        }
        else {
			setPhase(WALK_TO_AIRLOCK);
			return time;
		}
        
        // Add work to the maintenance
        manager.addMaintenanceWorkTime(workTime);
        
        // Add experience points
        addExperience(time);
	
        // Check if an accident happens during maintenance.
        checkForAccident(time);

        return 0D;
    }   
    
    /**
     * Perform the enter airlock phase of the task.
     * @param time amount of time to perform the phase
     * @return time remaining after performing the phase
     * @throws Exception if error entering airlock.
     */
    private double enterEVAPhase(double time) {
        time = enterAirlock(time, airlock);
        
        // Add experience points
        addExperience(time);
        
        if (enteredAirlock) {
            endTask();
        }
        
        return time;
    }	
    
    /**
     * Check for accident with entity during maintenance phase.
     * @param time the amount of time (in millisols)
     */
    protected void checkForAccident(double time) {

        // Use EVAOperation checkForAccident() method.
        super.checkForAccident(time);
        
        double chance = .001D;

        // Mechanic skill modification.
        int skill = person.getMind().getSkillManager().getEffectiveSkillLevel(Skill.MECHANICS);
        if (skill <= 3) chance *= (4 - skill);
        else chance /= (skill - 2);

        // Modify based on the vehicle's wear condition.
        chance *= vehicle.getMalfunctionManager().getWearConditionAccidentModifier();
        
        if (RandomUtil.lessThanRandPercent(chance * time)) {
            // logger.info(person.getName() + " has accident while performing maintenance on " + vehicle.getName() + ".");
            vehicle.getMalfunctionManager().accident();
        }
    }
    
    /** 
     * Gets the vehicle  the person is maintaining.
     * Returns null if none.
     * @return entity
     */
    public Malfunctionable getVehicle() {
        return vehicle;
    }
    
    /**
     * Gets all ground vehicles requiring maintenance that are parked outside the settlement.
     *
     * @param person person checking.
     * @return collection of ground vehicles available for maintenance.
     */
    private static Collection<Vehicle> getAllVehicleCandidates(Person person) {
        Collection<Vehicle> result = new ConcurrentLinkedQueue<Vehicle>();
        
        Settlement settlement = person.getSettlement();
        if (settlement != null) {
        	Iterator<Vehicle> vI = settlement.getParkedVehicles().iterator();
        	while (vI.hasNext()) {
        		Vehicle vehicle = vI.next();
        		if ((vehicle instanceof GroundVehicle) && !vehicle.isReservedForMission()) result.add(vehicle);
        	}
        }
        
        return result;
    }
    
    /**
     * Gets a ground vehicle that requires maintenance in a local garage.
     * Returns null if none available.
     * @param person person checking.
     * @return ground vehicle
     * @throws Exception if error finding needy vehicle.
     */
    private GroundVehicle getNeedyGroundVehicle(Person person) {
            
        GroundVehicle result = null;

        // Find all vehicles that can be maintained.
        Collection<Vehicle> availableVehicles = getAllVehicleCandidates(person);
        
        // Populate vehicles and probabilities.
        Map<Vehicle, Double> vehicleProb = new HashMap<Vehicle, Double>(availableVehicles.size());
        Iterator<Vehicle> i = availableVehicles.iterator();
        while (i.hasNext()) {
            Vehicle vehicle = i.next();
            double prob = getProbabilityWeight(vehicle);
            if (prob > 0D) {
                vehicleProb.put(vehicle, prob);
            }
        }
        
        // Randomly determine needy vehicle.
        if (!vehicleProb.isEmpty()) {
            result = (GroundVehicle) RandomUtil.getWeightedRandomObject(vehicleProb);
        }
        
        if (result != null) {
            setDescription("Performing maintenance on " + result.getName());
        }
        
        return result;
    }
    
    /**
     * Gets the probability weight for a vehicle.
     * @param vehicle the vehicle.
     * @return the probability weight.
     * @throws Exception if error determining probability weight.
     */
    private double getProbabilityWeight(Vehicle vehicle) {
    	double result = 0D;
		MalfunctionManager manager = vehicle.getMalfunctionManager();
		boolean hasMalfunction = manager.hasMalfunction();
		double effectiveTime = manager.getEffectiveTimeSinceLastMaintenance();
		boolean minTime = (effectiveTime >= 1000D); 
		boolean enoughParts = Maintenance.hasMaintenanceParts(person, vehicle);
		if (!hasMalfunction && minTime && enoughParts) result = effectiveTime;
		return result;
    }
    
	/**
	 * Gets the effective skill level a person has at this task.
	 * @return effective skill level
	 */
	public int getEffectiveSkillLevel() {
		SkillManager manager = person.getMind().getSkillManager();
		int EVAOperationsSkill = manager.getEffectiveSkillLevel(Skill.EVA_OPERATIONS);
		int mechanicsSkill = manager.getEffectiveSkillLevel(Skill.MECHANICS);
		return (int) Math.round((double)(EVAOperationsSkill + mechanicsSkill) / 2D); 
	}
	
	/**
	 * Gets a list of the skills associated with this task.
	 * May be empty list if no associated skills.
	 * @return list of skills as strings
	 */
	public List<String> getAssociatedSkills() {
		List<String> results = new ArrayList<String>(2);
		results.add(Skill.EVA_OPERATIONS);
		results.add(Skill.MECHANICS);
		return results;
	}
	
	@Override
	public void destroy() {
	    super.destroy();
	    
	    vehicle = null;
	    airlock = null;
	}
}