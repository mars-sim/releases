/**
 * Mars Simulation Project
 * MaintenanceEVA.java
 * @version 3.07 2014-09-22
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.LocalBoundedObject;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.malfunction.MalfunctionFactory;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.Structure;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;

/** 
 * The Maintenance class is a task for performing
 * preventive maintenance on malfunctionable entities outdoors.
 */
public class MaintenanceEVA
extends EVAOperation
implements Serializable {
	
	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(MaintenanceEVA.class.getName());

	/** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.maintenanceEVA"); //$NON-NLS-1$
	
    /** Task phases. */
    private static final TaskPhase MAINTAIN = new TaskPhase(Msg.getString(
            "Task.phase.maintain")); //$NON-NLS-1$

	// Data members
	/** Entity to be maintained. */
	private Malfunctionable entity;
	private Settlement settlement;

	/** 
	 * Constructor.
	 * @param person the person to perform the task
	 */
	public MaintenanceEVA(Person person) {
		super(NAME, person, true, RandomUtil.getRandomDouble(50D) + 10D);
		
		settlement = person.getSettlement();
		
		try {
			entity = getMaintenanceMalfunctionable();
			if (entity == null) {
			    endTask();
			    return;
			}
		}
		catch (Exception e) {
		    logger.log(Level.SEVERE,"MaintenanceEVA.constructor()",e);
			endTask();
		}
		
        // Determine location for maintenance.
        Point2D maintenanceLoc = determineMaintenanceLocation();
        setOutsideSiteLocation(maintenanceLoc.getX(), maintenanceLoc.getY());
		
		// Initialize phase
		addPhase(MAINTAIN);
		
		logger.finest(person.getName() + " is starting " + getDescription());
	}
	
    /**
     * Determine location to perform maintenance.
     * @return location.
     */
    private Point2D determineMaintenanceLocation() {
        
        Point2D.Double newLocation = new Point2D.Double(0D, 0D);
        
        if (entity instanceof LocalBoundedObject) {
            LocalBoundedObject bounds = (LocalBoundedObject) entity;
            boolean goodLocation = false;
            for (int x = 0; (x < 50) && !goodLocation; x++) {
                Point2D.Double boundedLocalPoint = LocalAreaUtil.getRandomExteriorLocation(bounds, 1D);
                newLocation = LocalAreaUtil.getLocalRelativeLocation(boundedLocalPoint.getX(), 
                        boundedLocalPoint.getY(), bounds);
                goodLocation = LocalAreaUtil.checkLocationCollision(newLocation.getX(), newLocation.getY(), 
                        person.getCoordinates());
            }
        }
        
        return newLocation;
    }
    
    @Override
    protected TaskPhase getOutsideSitePhase() {
        return MAINTAIN;
    }
	
    @Override
    protected double performMappedPhase(double time) {
        
        time = super.performMappedPhase(time);
        
    	if (getPhase() == null) {
    	    throw new IllegalArgumentException("Task phase is null");
    	}
    	else if (MAINTAIN.equals(getPhase())) {
    	    return maintenancePhase(time);
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
		
		// If phase is maintenance, add experience to mechanics skill.
		if (MAINTAIN.equals(getPhase())) {
			// 1 base experience point per 100 millisols of time spent.
			// Experience points adjusted by person's "Experience Aptitude" attribute.
			double mechanicsExperience = time / 100D;
			mechanicsExperience += mechanicsExperience * experienceAptitudeModifier;
			person.getMind().getSkillManager().addExperience(SkillType.MECHANICS, mechanicsExperience);
		}
	}
	
	/**
	 * Perform the maintenance phase of the task.
	 * @param time the time to perform this phase (in millisols)
	 * @return the time remaining after performing this phase (in millisols)
	 * @throws Exception if error during maintenance.
	 */
	private double maintenancePhase(double time) {
        
		MalfunctionManager manager = entity.getMalfunctionManager();
		boolean malfunction = manager.hasMalfunction();
		boolean finishedMaintenance = (manager.getEffectiveTimeSinceLastMaintenance() < 1000D);
        
		if (finishedMaintenance || malfunction || shouldEndEVAOperation() || addTimeOnSite(time)) {
			setPhase(WALK_BACK_INSIDE);
			return time;
		}
        
		// Determine effective work time based on "Mechanic" skill.
		double workTime = time;
		int mechanicSkill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.MECHANICS);
		if (mechanicSkill == 0) {
		    workTime /= 2;
		}
		if (mechanicSkill > 1) {
		    workTime += workTime * (.2D * mechanicSkill);
		}

        // Add repair parts if necessary.
		Inventory inv = settlement.getInventory();
		if (Maintenance.hasMaintenanceParts(inv, entity)) {
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
		if (skill <= 3) chance *= (4 - skill);
		else chance /= (skill - 2);

		// Modify based on the entity's wear condition.
        chance *= entity.getMalfunctionManager().getWearConditionAccidentModifier();
		
		if (RandomUtil.lessThanRandPercent(chance * time)) {
			logger.info(person.getName() + " has accident while performing maintenance on " 
						     + entity.getName() + ".");
			entity.getMalfunctionManager().accident();
		}
	}
	
	/**
	 * Gets a random malfunctionable to perform maintenance on.
	 * @return malfunctionable or null.
	 * @throws Exception if error finding malfunctionable.
	 */
	private Malfunctionable getMaintenanceMalfunctionable() {
		Malfunctionable result = null;
    	
		// Determine all malfunctionables local to the person.
		Map<Malfunctionable, Double> malfunctionables = new HashMap<Malfunctionable, Double>();
        Iterator<Malfunctionable> i = MalfunctionFactory.getMalfunctionables(person).iterator();
        while (i.hasNext()) {
            Malfunctionable entity = i.next();
            double probability = getProbabilityWeight(entity);
            if (probability > 0D) {
                malfunctionables.put(entity, probability);
            }
        }
        
        if (!malfunctionables.isEmpty()) {
            result = RandomUtil.getWeightedRandomObject(malfunctionables);
        }
		
		if (result != null) {
		    setDescription(Msg.getString("Task.description.maintenanceEVA.detail", 
                    result.getName())); //$NON-NLS-1$;
		}
    	
		return result;
	}
	
	/**
	 * Gets the probability weight for a malfunctionable.
	 * @param malfunctionable the malfunctionable.
	 * @return the probability weight.
	 */
	private double getProbabilityWeight(Malfunctionable malfunctionable) {
		double result = 0D;
		boolean isStructure = (malfunctionable instanceof Structure);
		boolean uninhabitableBuilding = false;
		if (malfunctionable instanceof Building) 
			uninhabitableBuilding = !((Building) malfunctionable).hasFunction(BuildingFunction.LIFE_SUPPORT);
		MalfunctionManager manager = malfunctionable.getMalfunctionManager();
		boolean hasMalfunction = manager.hasMalfunction();
		boolean hasParts = Maintenance.hasMaintenanceParts(person, malfunctionable);
		double effectiveTime = manager.getEffectiveTimeSinceLastMaintenance();
		boolean minTime = (effectiveTime >= 1000D); 
		if ((isStructure || uninhabitableBuilding) && !hasMalfunction && minTime && hasParts) result = effectiveTime;
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
	    
	    entity = null;
	}
}