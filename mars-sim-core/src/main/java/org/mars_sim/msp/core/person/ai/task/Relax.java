/**
 * Mars Simulation Project
 * Relax.java
 * @version 3.07 2015-01-06
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.vehicle.Rover;

/** 
 * The Relax class is a simple task that implements resting and doing nothing for a while.
 * The duration of the task is by default chosen randomly, up to 100 millisols.
 */
public class Relax
extends Task
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(Relax.class.getName());

	/** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.relax"); //$NON-NLS-1$
	
    /** Task phases. */
    private static final TaskPhase RELAXING = new TaskPhase(Msg.getString(
            "Task.phase.relaxing")); //$NON-NLS-1$

	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = -.5D;

	/** 
	 * Constructor.
	 * @param person the person to perform the task
	 */
	public Relax(Person person) {
		super(NAME, person, false, false, STRESS_MODIFIER, true, 10D + 
				RandomUtil.getRandomDouble(40D));

		// If person is in a settlement, try to find a place to relax.
		boolean walkSite = false;
		if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {      	
			try {
				Building recBuilding = getAvailableRecreationBuilding(person);
				if (recBuilding != null) {
					// Walk to recreation building.
				    walkToActivitySpotInBuilding(recBuilding, true);
				    walkSite = true;
				}
			}
			catch (Exception e) {
				logger.log(Level.SEVERE,"Relax.constructor(): " + e.getMessage());
				endTask();
			}
		}
		
		if (!walkSite) {
		    if (person.getLocationSituation() == LocationSituation.IN_VEHICLE) {
                // If person is in rover, walk to passenger activity spot.
                if (person.getVehicle() instanceof Rover) {
                    walkToPassengerActivitySpotInRover((Rover) person.getVehicle(), true);
                }
            }
		    else {
                // Walk to random location.
                walkToRandomLocation(true);
            }
		}

		// Initialize phase
		addPhase(RELAXING);
		setPhase(RELAXING);
	}
	
    @Override
    protected BuildingFunction getRelatedBuildingFunction() {
        return BuildingFunction.RECREATION;
    }

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		}
		else if (RELAXING.equals(getPhase())) {
			return relaxingPhase(time);
		}
		else {
			return time;
		}
	}

	/**
	 * Performs the relaxing phase of the task.
	 * @param time the amount of time (millisol) to perform the phase.
	 * @return the amount of time (millisol) left after performing the phase.
	 */
	private double relaxingPhase(double time) {
		// Do nothing
		return 0D; 
	}

	@Override
	protected void addExperience(double time) {
		// This task adds no experience.
	}

	/**
	 * Gets an available recreation building that the person can use.
	 * Returns null if no recreation building is currently available.
	 * @param person the person
	 * @return available recreation building
	 */
	public static Building getAvailableRecreationBuilding(Person person) {

		Building result = null;

		if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
			BuildingManager manager = person.getSettlement().getBuildingManager();
			List<Building> recreationBuildings = manager.getBuildings(BuildingFunction.RECREATION);
			recreationBuildings = BuildingManager.getNonMalfunctioningBuildings(recreationBuildings);
			recreationBuildings = BuildingManager.getLeastCrowdedBuildings(recreationBuildings);

			if (recreationBuildings.size() > 0) {
				Map<Building, Double> recreationBuildingProbs = BuildingManager.getBestRelationshipBuildings(
						person, recreationBuildings);
				result = RandomUtil.getWeightedRandomObject(recreationBuildingProbs);
			}
		}

		return result;
	}

	@Override
	public int getEffectiveSkillLevel() {
		return 0;	
	}

	@Override
	public List<SkillType> getAssociatedSkills() {
		List<SkillType> results = new ArrayList<SkillType>(0);
		return results;
	}
}