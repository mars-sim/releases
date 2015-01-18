/**
 * Mars Simulation Project
 * Sleep.java
 * @version 3.07 2015-01-06
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.LivingAccommodations;
import org.mars_sim.msp.core.vehicle.Rover;

/** 
 * The Sleep class is a task for sleeping.
 * The duration of the task is by default chosen randomly, between 250 - 330 millisols.
 * Note: Sleeping reduces fatigue and stress.
 */
public class Sleep extends Task implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default logger. */
    private static Logger logger = Logger.getLogger(Sleep.class.getName());

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.sleep"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase SLEEPING = new TaskPhase(Msg.getString(
            "Task.phase.sleeping")); //$NON-NLS-1$

    // Static members
    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = -.3D;
    /** The base alarm time (millisols) at 0 degrees longitude. */
    private static final double BASE_ALARM_TIME = 300D;

    // Data members
    /** The living accommodations if any. */
    private LivingAccommodations accommodations;
    /** The previous time (millisols). */
    private double previousTime;

    /** 
     * Constructor.
     * @param person the person to perform the task
     */
    public Sleep(Person person) {
        super(NAME, person, false, false, STRESS_MODIFIER, true, 
                (250D + RandomUtil.getRandomDouble(80D)));

        boolean walkSite = false;

        // If person is in a settlement, try to find a living accommodations building.
        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {

            Building quarters = getAvailableLivingQuartersBuilding(person);
            if (quarters != null) {
                // Walk to quarters.
                walkToActivitySpotInBuilding(quarters, true);
                accommodations = (LivingAccommodations) quarters.getFunction(
                        BuildingFunction.LIVING_ACCOMODATIONS);
                accommodations.addSleeper();
                walkSite = true;
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


        previousTime = Simulation.instance().getMasterClock().getMarsClock().getMillisol();

        // Initialize phase
        addPhase(SLEEPING);
        setPhase(SLEEPING);
    }

    @Override
    protected BuildingFunction getRelatedBuildingFunction() {
        return BuildingFunction.LIVING_ACCOMODATIONS;
    }

    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (SLEEPING.equals(getPhase())) {
            return sleepingPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Performs the sleeping phase.
     * @param time the amount of time (millisols) to perform the phase.
     * @return the amount of time (millisols) left over after performing the phase.
     */
    private double sleepingPhase(double time) {

        // Reduce person's fatigue
        double newFatigue = person.getPhysicalCondition().getFatigue() - (5D * time);
        if (newFatigue < 0D) {
            newFatigue = 0D;
        }
        person.getPhysicalCondition().setFatigue(newFatigue);

        // Check if alarm went off.
        double newTime = Simulation.instance().getMasterClock().getMarsClock().getMillisol();
        double alarmTime = getAlarmTime();
        if ((previousTime <= alarmTime) && (newTime >= alarmTime)) {
            endTask();
            logger.finest(person.getName() + " woke up from alarm.");
        }
        else {
            previousTime = newTime;
        }

        return 0D;
    }

    @Override
    protected void addExperience(double time) {
        // This task adds no experience.
    }

    @Override
    public void endTask() {
        super.endTask();

        // Remove person from living accommodations bed so others can use it.
        if (accommodations != null && accommodations.getSleepers() > 0) {
            accommodations.removeSleeper();
        }

    }

    /**
     * Gets an available living accommodations building that the person can use.
     * Returns null if no living accommodations building is currently available.
     * @param person the person
     * @return available living accommodations building
     */
    public static Building getAvailableLivingQuartersBuilding(Person person) {

        Building result = null;

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            BuildingManager manager = person.getSettlement().getBuildingManager();
            List<Building> quartersBuildings = manager.getBuildings(BuildingFunction.LIVING_ACCOMODATIONS);
            quartersBuildings = BuildingManager.getNonMalfunctioningBuildings(quartersBuildings);
            quartersBuildings = getQuartersWithEmptyBeds(quartersBuildings);
            quartersBuildings = BuildingManager.getLeastCrowdedBuildings(quartersBuildings);

            if (quartersBuildings.size() > 0) {
                Map<Building, Double> quartersBuildingProbs = BuildingManager.getBestRelationshipBuildings(
                        person, quartersBuildings);
                result = RandomUtil.getWeightedRandomObject(quartersBuildingProbs);
            }
        }

        return result;
    }

    /**
     * Gets living accommodations with empty beds from a list of buildings with the living accommodations function.
     * @param buildingList list of buildings with the living accommodations function.
     * @return list of buildings with empty beds.
     */
    private static List<Building> getQuartersWithEmptyBeds(List<Building> buildingList) {
        List<Building> result = new ArrayList<Building>();

        Iterator<Building> i = buildingList.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            LivingAccommodations quarters = (LivingAccommodations) building.getFunction(BuildingFunction.LIVING_ACCOMODATIONS);
            if (quarters.getSleepers() < quarters.getBeds()) {
                result.add(building);
            }
        }

        return result;
    }

    /**
     * Gets the wakeup alarm time for the person's longitude.
     * @return alarm time in millisols.
     */
    private double getAlarmTime() {
        double timeDiff = 1000D * (person.getCoordinates().getTheta() / (2D * Math.PI));
        double modifiedAlarmTime = BASE_ALARM_TIME - timeDiff;
        if (modifiedAlarmTime < 0D) {
            modifiedAlarmTime += 1000D;
        }
        return modifiedAlarmTime;
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

        accommodations = null;
    }
}