/**
 * Mars Simulation Project
 * Workout.java
 * @version 3.07 2015-01-06
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.Exercise;

/**
 * The Workout class is a task for working out in an exercise facility.
 */
public class Workout
extends Task
implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.workout"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase EXERCISING = new TaskPhase(Msg.getString(
            "Task.phase.exercising")); //$NON-NLS-1$

    // Static members
    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = -1D;

    // Data members
    /** The exercise building the person is using. */
    private Exercise gym;

    /**
     * Constructor. This is an effort-driven task.
     * @param person the person performing the task.
     */
    public Workout(Person person) {
        // Use Task constructor.
        super(NAME, person, true, false, STRESS_MODIFIER, true,
                10D + RandomUtil.getRandomDouble(30D));

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {

            // If person is in a settlement, try to find a gym.
            Building gymBuilding = getAvailableGym(person);
            if (gymBuilding != null) {
                // Walk to gym building.
                walkToActivitySpotInBuilding(gymBuilding, false);

                gym = (Exercise) gymBuilding.getFunction(BuildingFunction.EXERCISE);
            } 
            else {
                endTask();
            }
        } 
        else {
            endTask();
        }

        // Initialize phase
        addPhase(EXERCISING);
        setPhase(EXERCISING);
    }

    @Override
    protected BuildingFunction getRelatedBuildingFunction() {
        return BuildingFunction.EXERCISE;
    }

    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (EXERCISING.equals(getPhase())) {
            return exercisingPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Performs the exercising phase.
     * @param time the amount of time (millisols) to perform the phase.
     * @return the amount of time (millisols) left over after performing the phase.
     */
    private double exercisingPhase(double time) {

        // Do nothing

        return 0D;
    }

    @Override
    protected void addExperience(double time) {
        // This task adds no experience.
    }

    @Override
    public void endTask() {
        super.endTask();

        // Remove person from exercise function so others can use it.
        if (gym != null && gym.getNumExercisers() > 0) {
            gym.removeExerciser();
        }
    }

    /**
     * Gets an available building with the exercise function.
     * @param person the person looking for the gym.
     * @return an available exercise building or null if none found.
     */
    public static Building getAvailableGym(Person person) {
        Building result = null;

        // If person is in a settlement, try to find a building with a gym.
        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            BuildingManager buildingManager = person.getSettlement()
                    .getBuildingManager();
            List<Building> gyms = buildingManager.getBuildings(BuildingFunction.EXERCISE);
            gyms = BuildingManager.getNonMalfunctioningBuildings(gyms);
            gyms = BuildingManager.getLeastCrowdedBuildings(gyms);

            if (gyms.size() > 0) {
                Map<Building, Double> gymProbs = BuildingManager.getBestRelationshipBuildings(
                        person, gyms);
                result = RandomUtil.getWeightedRandomObject(gymProbs);
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

    @Override
    public void destroy() {
        super.destroy();

        gym = null;
    }
}