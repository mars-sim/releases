/**
 * Mars Simulation Project
 * MaintainGroundVehicleEVA.java
 * @version 3.06 2014-02-25
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.vehicle.GroundVehicle;
import org.mars_sim.msp.core.vehicle.Vehicle;

/** 
 * The MaintainGroundVehicleGarage class is a task for performing
 * preventive maintenance on ground vehicles outside a settlement.
 */
public class MaintainGroundVehicleEVA
extends EVAOperation
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(MaintainGroundVehicleEVA.class.getName());

	// TODO Phase names should be an enum.
	private static final String MAINTAIN_VEHICLE = "Maintain Vehicle";

	// Data members.
	/** Vehicle to be maintained. */
	private GroundVehicle vehicle;
	private Settlement settlement;

	/** 
	 * Constructor.
	 * @param person the person to perform the task
	 */
	public MaintainGroundVehicleEVA(Person person) {
        super("Performing Vehicle Maintenance", person, true, RandomUtil.getRandomDouble(50D) + 10D);
   
        settlement = person.getSettlement();
        
        // Choose an available needy ground vehicle.
        vehicle = getNeedyGroundVehicle(person);
        if (vehicle != null) {
            vehicle.setReservedForMaintenance(true);
            
            // Determine location for maintenance.
            Point2D maintenanceLoc = determineMaintenanceLocation();
            setOutsideSiteLocation(maintenanceLoc.getX(), maintenanceLoc.getY());
        }
        else {
            endTask();
        }
        
        // Initialize phase.
        addPhase(MAINTAIN_VEHICLE);
        
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
		if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
			Iterator<Vehicle> i = getAllVehicleCandidates(person).iterator();
			while (i.hasNext()) {
				MalfunctionManager manager = i.next().getMalfunctionManager();
				double entityProb = (manager.getEffectiveTimeSinceLastMaintenance() / 20D);
				if (entityProb > 100D) entityProb = 100D;
				result += entityProb;
			}
		}

		// Determine if settlement has a garage.
        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {	
			if (person.getSettlement().getBuildingManager().getBuildings(
			        BuildingFunction.GROUND_VEHICLE_MAINTENANCE).size() > 0) {
				result = 0D;
			}
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
		if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
			Settlement settlement = person.getSettlement();
			if (settlement.getCurrentPopulationNum() > settlement.getPopulationCapacity()) {
			    result *= 2D;
			}
		}

        // Effort-driven task modifier.
        result *= person.getPerformanceRating();
        
		// Job modifier.
        Job job = person.getMind().getJob();
		if (job != null) {
		    result *= job.getStartTaskProbabilityModifier(MaintainGroundVehicleEVA.class);        
		}
	
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
    
    @Override
    protected String getOutsideSitePhase() {
        return MAINTAIN_VEHICLE;
    }
    
    @Override
    protected double performMappedPhase(double time) {
        
        time = super.performMappedPhase(time);
        
    	if (getPhase() == null) {
    	    throw new IllegalArgumentException("Task phase is null");
    	}
    	else if (MAINTAIN_VEHICLE.equals(getPhase())) {
    	    return maintainVehiclePhase(time);
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
		
		// If phase is maintain vehicle, add experience to mechanics skill.
		if (MAINTAIN_VEHICLE.equals(getPhase())) {
			// 1 base experience point per 100 millisols of collection time spent.
			// Experience points adjusted by person's "Experience Aptitude" attribute.
			double mechanicsExperience = time / 100D;
			mechanicsExperience += mechanicsExperience * experienceAptitudeModifier;
			person.getMind().getSkillManager().addExperience(SkillType.MECHANICS, mechanicsExperience);
		}
	}
    
    /**
     * Perform the maintain vehicle phase of the task.
     * @param time the time to perform this phase (in millisols)
     * @return the time remaining after performing this phase (in millisols)
     */
    private double maintainVehiclePhase(double time) {
        
        MalfunctionManager manager = vehicle.getMalfunctionManager();
        boolean malfunction = manager.hasMalfunction();
        boolean finishedMaintenance = (manager.getEffectiveTimeSinceLastMaintenance() == 0D);
        if (finishedMaintenance) vehicle.setReservedForMaintenance(false);
        
        if (finishedMaintenance || malfunction || shouldEndEVAOperation() || 
                addTimeOnSite(time)) {
            setPhase(WALK_BACK_INSIDE);
            return time;
        }
        
        // Determine effective work time based on "Mechanic" and "EVA Operations" skills.
        double workTime = time;
        int skill = getEffectiveSkillLevel();
        if (skill == 0) workTime /= 2;
        if (skill > 1) workTime += workTime * (.2D * skill);

        // Add repair parts if necessary.
        Inventory inv = settlement.getInventory();
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
			setPhase(WALK_BACK_INSIDE);
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
    
    @Override
    protected void checkForAccident(double time) {

        // Use EVAOperation checkForAccident() method.
        super.checkForAccident(time);
        
        double chance = .001D;

        // Mechanic skill modification.
        int skill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.MECHANICS);
        if (skill <= 3) {
            chance *= (4 - skill);
        }
        else {
            chance /= (skill - 2);
        }

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
        		if ((vehicle instanceof GroundVehicle) && !vehicle.isReservedForMission()) {
        		    result.add(vehicle);
        		}
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
    
	@Override
	public int getEffectiveSkillLevel() {
		SkillManager manager = person.getMind().getSkillManager();
		int EVAOperationsSkill = manager.getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
		int mechanicsSkill = manager.getEffectiveSkillLevel(SkillType.MECHANICS);
		return (int) Math.round((double)(EVAOperationsSkill + mechanicsSkill) / 2D); 
	}
	
	@Override
	public List<SkillType> getAssociatedSkills() {
		List<SkillType> results = new ArrayList<SkillType>(2);
		results.add(SkillType.EVA_OPERATIONS);
		results.add(SkillType.MECHANICS);
		return results;
	}
	
	@Override
	public void destroy() {
	    super.destroy();
	    
	    vehicle = null;
	}
}