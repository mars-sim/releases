/**
 * Mars Simulation Project
 * ObserveAstronomicalObjects.java
 * @version 3.07 2015-01-06
 * @author Sebastien Venot
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
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.science.ScientificStudyManager;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.AstronomicalObservation;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;

/**
 * A task for observing the night sky with an astronomical observatory.
 */
public class ObserveAstronomicalObjects
extends Task
implements ResearchScientificStudy, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default logger. */
    private static Logger logger = Logger.getLogger(ObserveAstronomicalObjects.class.getName());

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.observeAstronomicalObjects"); //$NON-NLS-1$

    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = -.2D; 

    /** Task phases. */
    private static final TaskPhase OBSERVING = new TaskPhase(Msg.getString(
            "Task.phase.observing")); //$NON-NLS-1$

    // Data members.
    /** The scientific study the person is researching for. */
    private ScientificStudy study;
    /** The observatory the person is using. */
    private AstronomicalObservation observatory;
    /** The research assistant. */
    private Person researchAssistant;
    /** True if person is active observer. */
    private boolean isActiveObserver = false;

    /**
     * Constructor.
     * @param person the person performing the task.
     */
    public ObserveAstronomicalObjects(Person person) {
        // Use task constructor.
        super(NAME, person, true, false, STRESS_MODIFIER, true, 
                10D + RandomUtil.getRandomDouble(300D));

        // Determine study.
        study = determineStudy();
        if (study != null) {
            // Determine observatory to use.
            observatory = determineObservatory(person);
            if (observatory != null) {

                // Walk to observatory building.
                walkToActivitySpotInBuilding(observatory.getBuilding(), false);
                observatory.addObserver();
                isActiveObserver = true;
            }
            else {
                logger.info("observatory could not be determined.");
                endTask();
            }
        }

        // Initialize phase
        addPhase(OBSERVING);
        setPhase(OBSERVING);
    }

    @Override
    protected BuildingFunction getRelatedBuildingFunction() {
        return BuildingFunction.ASTRONOMICAL_OBSERVATIONS;
    }

    /**
     * Gets the preferred local astronomical observatory for an observer.
     * @param observer the observer.
     * @return observatory or null if none found.
     */
    public static AstronomicalObservation determineObservatory(Person observer) {
        AstronomicalObservation result = null;

        if (LocationSituation.IN_SETTLEMENT == observer.getLocationSituation()) {

            BuildingManager manager = observer.getSettlement().getBuildingManager();
            List<Building> observatoryBuildings = manager.getBuildings(BuildingFunction.ASTRONOMICAL_OBSERVATIONS);
            observatoryBuildings = BuildingManager.getNonMalfunctioningBuildings(observatoryBuildings);
            observatoryBuildings = getObservatoriesWithAvailableSpace(observatoryBuildings);
            observatoryBuildings = BuildingManager.getLeastCrowdedBuildings(observatoryBuildings);

            if (observatoryBuildings.size() > 0) {
                Map<Building, Double> observatoryBuildingProbs = BuildingManager.getBestRelationshipBuildings(
                        observer, observatoryBuildings);
                Building building = RandomUtil.getWeightedRandomObject(observatoryBuildingProbs);
                result = (AstronomicalObservation) building.getFunction(BuildingFunction.ASTRONOMICAL_OBSERVATIONS);
            }
        }

        return result;
    }

    /**
     * Gets the crowding modifier for an observer to use a given observatory building.
     * @param observer the observer.
     * @param observatory the astronomical observatory.
     * @return crowding modifier.
     */
    public static double getObservatoryCrowdingModifier(Person observer, 
            AstronomicalObservation observatory) {
        double result = 1D;
        if (observer.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            Building observatoryBuilding = observatory.getBuilding();  
            if (observatoryBuilding != null) {
                result *= Task.getCrowdingProbabilityModifier(observer, observatoryBuilding);     
                result *= Task.getRelationshipModifier(observer, observatoryBuilding);
            }
        }
        return result;
    }

    /**
     * Gets a list of observatory buildings with available research space from a list of observatory buildings.
     * @param buildingList list of buildings with astronomical observation function.
     * @return observatory buildings with available observatory space.
     */
    private static List<Building> getObservatoriesWithAvailableSpace(List<Building> buildingList) {
        List<Building> result = new ArrayList<Building>();

        Iterator<Building> i = buildingList.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            AstronomicalObservation observatory = (AstronomicalObservation) building.getFunction(BuildingFunction.ASTRONOMICAL_OBSERVATIONS);
            if (observatory.getObserverNum() < observatory.getObservatoryCapacity()) {
                result.add(building);
            }
        }

        return result;
    }

    /**
     * Determines the scientific study for the observations.
     * @return study or null if none available.
     */
    private ScientificStudy determineStudy() {
        ScientificStudy result = null;

        ScienceType astronomy = ScienceType.ASTRONOMY;
        List<ScientificStudy> possibleStudies = new ArrayList<ScientificStudy>();

        // Add primary study if in research phase.
        ScientificStudyManager manager = Simulation.instance().getScientificStudyManager();
        ScientificStudy primaryStudy = manager.getOngoingPrimaryStudy(person);
        if (primaryStudy != null) {
            if (ScientificStudy.RESEARCH_PHASE.equals(primaryStudy.getPhase()) && 
                    !primaryStudy.isPrimaryResearchCompleted()) {
                if (astronomy == primaryStudy.getScience()) {
                    // Primary study added twice to double chance of random selection.
                    possibleStudies.add(primaryStudy);
                    possibleStudies.add(primaryStudy);
                }
            }
        }

        // Add all collaborative studies in research phase.
        Iterator<ScientificStudy> i = manager.getOngoingCollaborativeStudies(person).iterator();
        while (i.hasNext()) {
            ScientificStudy collabStudy = i.next();
            if (ScientificStudy.RESEARCH_PHASE.equals(collabStudy.getPhase()) && 
                    !collabStudy.isCollaborativeResearchCompleted(person)) {
                if (astronomy == collabStudy.getCollaborativeResearchers().get(person)) {
                    possibleStudies.add(collabStudy);
                }
            }
        }

        // Randomly select study.
        if (possibleStudies.size() > 0) {
            int selected = RandomUtil.getRandomInt(possibleStudies.size() - 1);
            result = possibleStudies.get(selected);
        }

        return result;
    }

    @Override
    protected void addExperience(double time) {
        // Add experience to astronomy skill
        // (1 base experience point per 25 millisols of research time)
        // Experience points adjusted by person's "Academic Aptitude" attribute.
        double newPoints = time / 25D;
        int academicAptitude = person.getNaturalAttributeManager().getAttribute(
                NaturalAttribute.ACADEMIC_APTITUDE);
        newPoints += newPoints * ((double) academicAptitude - 50D) / 100D;
        newPoints *= getTeachingExperienceModifier();
        ScienceType astronomyScience = ScienceType.ASTRONOMY;
        SkillType astronomySkill = astronomyScience.getSkill();
        person.getMind().getSkillManager().addExperience(astronomySkill, newPoints);
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(1);
        results.add(SkillType.ASTRONOMY);
        return results;
    }

    @Override
    public int getEffectiveSkillLevel() {
        SkillManager manager = person.getMind().getSkillManager();
        return manager.getEffectiveSkillLevel(SkillType.ASTRONOMY);
    }

    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (OBSERVING.equals(getPhase())) {
            return observingPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Performs the observing phase.
     * @param time the amount of time (millisols) to perform the phase.
     * @return the amount of time (millisols) left over after performing the phase.
     */
    protected double observingPhase(double time) {

        // If person is incapacitated, end task.
        if (person.getPerformanceRating() == 0D) {
            endTask();
        }

        // Check for observatory malfunction.
        if (observatory.getBuilding().getMalfunctionManager().hasMalfunction()) {
            endTask();
        }

        // Check sunlight and end the task if sunrise
        SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();
        double sunlight = surface.getSurfaceSunlight(person.getCoordinates()); 
        if (sunlight > 0) {
            endTask();
        }

        // Check if research in study is completed.
        boolean isPrimary = study.getPrimaryResearcher().equals(person);
        if (isPrimary) {
            if (study.isPrimaryResearchCompleted()) {
                endTask();
            }
        }
        else {
            if (study.isCollaborativeResearchCompleted(person)) {
                endTask();
            }
        }

        if (isDone()) {
            return time;
        }

        // Add research work time to study.
        double observingTime = getEffectiveObservingTime(time);
        if (isPrimary) {
            study.addPrimaryResearchWorkTime(observingTime);
        }
        else {
            study.addCollaborativeResearchWorkTime(person, observingTime);
        }

        // Add experience
        addExperience(observingTime);

        // Check for lab accident.
        checkForAccident(time);

        return 0D;
    }

    /**
     * Gets the effective observing time based on the person's astronomy skill.
     * @param time the real amount of time (millisol) for observing.
     * @return the effective amount of time (millisol) for observing.
     */
    private double getEffectiveObservingTime(double time) {
        // Determine effective observing time based on the astronomy skill.
        double observingTime = time;
        int astronomySkill = getEffectiveSkillLevel();
        if (astronomySkill == 0) {
            observingTime /= 2D;
        }
        if (astronomySkill > 1) {
            observingTime += observingTime * (.2D * astronomySkill);
        }

        // Modify by tech level of observatory.
        int techLevel = observatory.getTechnologyLevel();
        if (techLevel > 0) {
            observingTime *= techLevel;
        }

        // If research assistant, modify by assistant's effective skill.
        if (hasResearchAssistant()) {
            SkillManager manager = researchAssistant.getMind().getSkillManager();
            int assistantSkill = manager.getEffectiveSkillLevel(ScienceType.ASTRONOMY.getSkill());
            if (astronomySkill > 0) {
                observingTime *= 1D + ((double) assistantSkill / (double) astronomySkill);
            }
        }

        return observingTime;
    }

    /**
     * Check for accident in observatory.
     * @param time the amount of time researching (in millisols)
     */
    private void checkForAccident(double time) {

        double chance = .001D;

        // Astronomy skill modification.
        int skill = person.getMind().getSkillManager().getEffectiveSkillLevel(ScienceType.ASTRONOMY.getSkill());
        if (skill <= 3) {
            chance *= (4 - skill);
        }
        else {
            chance /= (skill - 2);
        }

        Malfunctionable entity = null;
        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            entity = observatory.getBuilding();
        }
        else if (person.getLocationSituation() == LocationSituation.IN_VEHICLE) {
            entity = person.getVehicle();
        }

        if (entity != null) {

            // Modify based on the entity's wear condition.
            chance *= entity.getMalfunctionManager().getWearConditionAccidentModifier();

            if (RandomUtil.lessThanRandPercent(chance * time)) {
                logger.info(person.getName() + " has a observatory accident while observing astronomical objects.");
                entity.getMalfunctionManager().accident();
            }
        }
    }

    @Override
    public void endTask() {
        super.endTask();

        // Remove person from observatory so others can use it.
        try {
            if ((observatory != null) && isActiveObserver) {
                observatory.removeObserver();
                isActiveObserver = false;
            }
        }
        catch(Exception e) {}
    }

    @Override
    public ScienceType getResearchScience() {
        return ScienceType.ASTRONOMY;
    }

    @Override
    public Person getResearcher() {
        return person;
    }

    @Override
    public boolean hasResearchAssistant() {
        return (researchAssistant != null);
    }

    @Override
    public Person getResearchAssistant() {
        return researchAssistant;
    }

    @Override
    public void setResearchAssistant(Person researchAssistant) {
        this.researchAssistant = researchAssistant;
    }

    @Override
    public void destroy() {
        super.destroy();

        study = null;
        observatory = null;
        researchAssistant = null;
    }
}