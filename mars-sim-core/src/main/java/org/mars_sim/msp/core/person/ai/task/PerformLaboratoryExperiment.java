/**
 * Mars Simulation Project
 * PerformLaboratoryExperiment.java
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

import org.mars_sim.msp.core.Lab;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.science.ScientificStudyManager;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.Research;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * A task for performing a scientific experiment in a laboratory for a scientific study.
 */
public class PerformLaboratoryExperiment
extends Task
implements ResearchScientificStudy, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default logger. */
    private static Logger logger = Logger.getLogger(PerformLaboratoryExperiment.class.getName());

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.performLaboratoryExperiment"); //$NON-NLS-1$

    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = .2D; 

    /** Task phases. */
    private static final TaskPhase EXPERIMENTING = new TaskPhase(Msg.getString(
            "Task.phase.experimenting")); //$NON-NLS-1$

    // Data members.
    /** The scientific study the person is experimenting for. */
    private ScientificStudy study;
    /** The laboratory the person is working in. */
    private Lab lab;
    /** The science that is being researched with the experiment. */
    private ScienceType science;
    /** The lab's associated malfunction manager. */
    private MalfunctionManager malfunctions;
    /** The research assistant. */
    private Person researchAssistant;

    /**
     * Constructor.
     * @param person the person performing the task.
     */
    public PerformLaboratoryExperiment(Person person) {
        // Use task constructor.
        super(NAME, person, true, false, STRESS_MODIFIER, 
                true, 10D + RandomUtil.getRandomDouble(400D));

        // Determine study.
        study = determineStudy();
        if (study != null) {
            science = getScience(person, study);
            if (science != null) {
                setDescription(Msg.getString("Task.description.performLaboratoryExperiment.detail", 
                        science.getName())); //$NON-NLS-1$
                lab = getLocalLab(person, science);
                if (lab != null) {
                    addPersonToLab();
                }
                else {
                    logger.info("lab could not be determined.");
                    endTask();
                }
            }
            else {
                logger.info("science could not be determined");
                endTask();
            }
        }
        else {
            logger.info("study could not be determined");
            endTask();
        }
        
        // Check if person is in a moving rover.
        if (inMovingRover(person)) {
            endTask();
        }

        // Initialize phase
        addPhase(EXPERIMENTING);
        setPhase(EXPERIMENTING);
    }

    @Override
    protected BuildingFunction getRelatedBuildingFunction() {
        return BuildingFunction.RESEARCH;
    }

    /**
     * Gets all the sciences related to laboratory experimentation.
     * @return list of sciences.
     */
    public static List<ScienceType> getExperimentalSciences() {
        // TODO Create list of possible sciences for laboratory experimentation directly in {@link ScienceType}.
        List<ScienceType> experimentalSciences = new ArrayList<ScienceType>();
        experimentalSciences.add(ScienceType.BOTANY);
        experimentalSciences.add(ScienceType.BIOLOGY);
        experimentalSciences.add(ScienceType.CHEMISTRY);
        experimentalSciences.add(ScienceType.PHYSICS);
        experimentalSciences.add(ScienceType.MEDICINE);
        return experimentalSciences;
    }

    /**
     * Gets the crowding modifier for a researcher to use a given laboratory building.
     * @param researcher the researcher.
     * @param lab the laboratory.
     * @return crowding modifier.
     */
    public static double getLabCrowdingModifier(Person researcher, Lab lab) {
        double result = 1D;
        if (researcher.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            Building labBuilding = ((Research) lab).getBuilding();  
            if (labBuilding != null) {
                result *= Task.getCrowdingProbabilityModifier(researcher, labBuilding);     
                result *= Task.getRelationshipModifier(researcher, labBuilding);
            }
        }
        return result;
    }

    /**
     * Determines the scientific study that will be researched.
     * @return study or null if none available.
     */
    private ScientificStudy determineStudy() {
        ScientificStudy result = null;

        List<ScientificStudy> possibleStudies = new ArrayList<ScientificStudy>();

        // Create list of experimental sciences.
        List<ScienceType> experimentalSciences = getExperimentalSciences();

        // Add primary study if appropriate science and in research phase.
        ScientificStudyManager manager = Simulation.instance().getScientificStudyManager();
        ScientificStudy primaryStudy = manager.getOngoingPrimaryStudy(person);
        if (primaryStudy != null) {
            if (ScientificStudy.RESEARCH_PHASE.equals(primaryStudy.getPhase()) && 
                    !primaryStudy.isPrimaryResearchCompleted()) {
                if (experimentalSciences.contains(primaryStudy.getScience())) {

                    // Check that local lab is available for primary study science.
                    Lab lab = getLocalLab(person, primaryStudy.getScience());
                    if (lab != null) {

                        // Primary study added twice to double chance of random selection.
                        possibleStudies.add(primaryStudy);
                        possibleStudies.add(primaryStudy);
                    }
                }
            }
        }

        // Add all collaborative studies with appropriate sciences and in research phase.
        Iterator<ScientificStudy> i = manager.getOngoingCollaborativeStudies(person).iterator();
        while (i.hasNext()) {
            ScientificStudy collabStudy = i.next();
            if (ScientificStudy.RESEARCH_PHASE.equals(collabStudy.getPhase()) && 
                    !collabStudy.isCollaborativeResearchCompleted(person)) {
                ScienceType collabScience = collabStudy.getCollaborativeResearchers().get(person);
                if (experimentalSciences.contains(collabScience)) {
                    // Check that local lab is available for collaboration study science.
                    Lab lab = getLocalLab(person, collabScience);
                    if (lab != null) {

                        possibleStudies.add(collabStudy);
                    }
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

    /**
     * Gets the field of science that the researcher is involved with in a study.
     * @param researcher the researcher.
     * @param study the scientific study.
     * @return {@link ScienceType} the field of science or null if researcher is not involved with study.
     */
    private static ScienceType getScience(Person researcher, ScientificStudy study) {
        ScienceType result = null;

        if (study.getPrimaryResearcher().equals(researcher)) {
            result = study.getScience();
        }
        else if (study.getCollaborativeResearchers().containsKey(researcher)) {
            result = study.getCollaborativeResearchers().get(researcher);
        }

        return result;
    }

    /**
     * Gets a local lab for experimentation.
     * @param person the person checking for the lab.
     * @param science the science to research.
     * @return laboratory found or null if none.
     * @throws Exception if error getting a lab.
     */
    public static Lab getLocalLab(Person person, ScienceType science) {
        Lab result = null;

        LocationSituation location = person.getLocationSituation();
        if (location == LocationSituation.IN_SETTLEMENT) {
            result = getSettlementLab(person, science);
        }
        else if (location == LocationSituation.IN_VEHICLE) {
            result = getVehicleLab(person.getVehicle(), science);
        }

        return result;
    }

    /**
     * Gets a settlement lab for experimentation.
     * @param person the person looking for a lab.
     * @param science the science to research.
     * @return a valid research lab.
     */
    private static Lab getSettlementLab(Person person, ScienceType science) {
        Lab result = null;

        BuildingManager manager = person.getSettlement().getBuildingManager();
        List<Building> labBuildings = manager.getBuildings(BuildingFunction.RESEARCH);
        labBuildings = getSettlementLabsWithSpecialty(science, labBuildings);
        labBuildings = BuildingManager.getNonMalfunctioningBuildings(labBuildings);
        labBuildings = getSettlementLabsWithAvailableSpace(labBuildings);
        labBuildings = BuildingManager.getLeastCrowdedBuildings(labBuildings);

        if (labBuildings.size() > 0) {
            Map<Building, Double> labBuildingProbs = BuildingManager.getBestRelationshipBuildings(
                    person, labBuildings);
            Building building = RandomUtil.getWeightedRandomObject(labBuildingProbs);
            result = (Research) building.getFunction(BuildingFunction.RESEARCH);
        }

        return result;
    }

    /**
     * Gets a list of research buildings with available research space from a list of buildings 
     * with the research function.
     * @param buildingList list of buildings with research function.
     * @return research buildings with available lab space.
     * @throws BuildingException if building list contains buildings without research function.
     */
    private static List<Building> getSettlementLabsWithAvailableSpace(
            List<Building> buildingList) {
        List<Building> result = new ArrayList<Building>();

        Iterator<Building> i = buildingList.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            Research lab = (Research) building.getFunction(BuildingFunction.RESEARCH);
            if (lab.getResearcherNum() < lab.getLaboratorySize()) result.add(building);
        }

        return result;
    }

    /**
     * Gets a list of research buildings with a given science specialty from a list of 
     * buildings with the research function.
     * @param science the science specialty.
     * @param buildingList list of buildings with research function.
     * @return research buildings with science specialty.
     * @throws BuildingException if building list contains buildings without research function.
     */
    private static List<Building> getSettlementLabsWithSpecialty(ScienceType science, 
            List<Building> buildingList) {
        List<Building> result = new ArrayList<Building>();

        Iterator<Building> i = buildingList.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            Research lab = (Research) building.getFunction(BuildingFunction.RESEARCH);
            if (lab.hasSpecialty(science)) {
                result.add(building);
            }
        }

        return result;
    }

    /**
     * Gets an available lab in a vehicle.
     * Returns null if no lab is currently available.
     * @param vehicle the vehicle
     * @param science the science to research.
     * @return available lab
     */
    private static Lab getVehicleLab(Vehicle vehicle, ScienceType science) {

        Lab result = null;

        if (vehicle instanceof Rover) {
            Rover rover = (Rover) vehicle;
            if (rover.hasLab()) {
                Lab lab = rover.getLab();
                boolean availableSpace = (lab.getResearcherNum() < lab.getLaboratorySize());
                boolean specialty = lab.hasSpecialty(science);
                boolean malfunction = (rover.getMalfunctionManager().hasMalfunction());
                if (availableSpace && specialty && !malfunction) {
                    result = lab;
                }
            }
        }

        return result;
    }

    /**
     * Adds a person to a lab.
     */
    private void addPersonToLab() {

        try {
            LocationSituation location = person.getLocationSituation();
            if (location == LocationSituation.IN_SETTLEMENT) {
                Building labBuilding = ((Research) lab).getBuilding();

                // Walk to lab building.
                walkToActivitySpotInBuilding(labBuilding, false);

                lab.addResearcher();
                malfunctions = labBuilding.getMalfunctionManager();
            }
            else if (location == LocationSituation.IN_VEHICLE) {

                // Walk to lab internal location in rover.
                walkToLabActivitySpotInRover((Rover) person.getVehicle(), false);

                lab.addResearcher();
                malfunctions = person.getVehicle().getMalfunctionManager();
            }
        }
        catch (Exception e) {
            logger.severe("addPersonToLab(): " + e.getMessage());
        }
    }

    @Override
    protected void addExperience(double time) {
        // Add experience to relevant science skill
        // (1 base experience point per 15 millisols of research time)
        // Experience points adjusted by person's "Academic Aptitude" attribute.
        double newPoints = time / 15D;
        int academicAptitude = person.getNaturalAttributeManager().getAttribute(NaturalAttribute.ACADEMIC_APTITUDE);
        newPoints += newPoints * ((double) academicAptitude - 50D) / 100D;
        newPoints *= getTeachingExperienceModifier();
        SkillType scienceSkill = science.getSkill();
        person.getMind().getSkillManager().addExperience(scienceSkill, newPoints);
    }

    /**
     * Gets the effective research time based on the person's science skill.
     * @param time the real amount of time (millisol) for research.
     * @return the effective amount of time (millisol) for research.
     */
    private double getEffectiveResearchTime(double time) {
        // Determine effective research time based on the science skill.
        double researchTime = time;
        int scienceSkill = getEffectiveSkillLevel();
        if (scienceSkill == 0) {
            researchTime /= 2D;
        }
        else if (scienceSkill > 1) {
            researchTime += researchTime * (.2D * scienceSkill);
        }

        // Modify by tech level of laboratory.
        int techLevel = lab.getTechnologyLevel();
        if (techLevel > 0) {
            researchTime *= techLevel;
        }

        // If research assistant, modify by assistant's effective skill.
        if (hasResearchAssistant()) {
            SkillManager manager = researchAssistant.getMind().getSkillManager();
            int assistantSkill = manager.getEffectiveSkillLevel(science.getSkill());
            if (scienceSkill > 0) {
                researchTime *= 1D + ((double) assistantSkill / (double) scienceSkill);
            }
        }

        return researchTime;
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(1);
        SkillType scienceSkill = science.getSkill();
        results.add(scienceSkill);
        return results;
    }

    @Override
    public int getEffectiveSkillLevel() {
        SkillType scienceSkill = science.getSkill();
        SkillManager manager = person.getMind().getSkillManager();
        return manager.getEffectiveSkillLevel(scienceSkill);
    }

    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (EXPERIMENTING.equals(getPhase())) {
            return experimentingPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Performs the experimenting phase.
     * @param time the amount of time (millisols) to perform the phase.
     * @return the amount of time (millisols) left over after performing the phase.
     */
    private double experimentingPhase(double time) {
        // If person is incapacitated, end task.
        if (person.getPerformanceRating() == 0D) {
            endTask();
        }

        // Check for laboratory malfunction.
        if (malfunctions.hasMalfunction()) {
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
        
        // Check if person is in a moving rover.
        if (inMovingRover(person)) {
            endTask();
        }

        if (isDone()) {
            return time;
        }

        // Add research work time to study.
        double researchTime = getEffectiveResearchTime(time);
        if (isPrimary) {
            study.addPrimaryResearchWorkTime(researchTime);
        }
        else {
            study.addCollaborativeResearchWorkTime(person, researchTime);
        }

        // Add experience
        addExperience(researchTime);

        // Check for lab accident.
        checkForAccident(time);

        return 0D;
    }

    /**
     * Check for accident in laboratory.
     * @param time the amount of time researching (in millisols)
     */
    private void checkForAccident(double time) {

        double chance = .01D;

        // Science skill modification.
        SkillType scienceSkill = science.getSkill();
        int skill = person.getMind().getSkillManager().getEffectiveSkillLevel(scienceSkill);
        if (skill <= 3) {
            chance *= (4 - skill);
        }
        else {
            chance /= (skill - 2);
        }

        Malfunctionable entity = null;
        if (lab instanceof Research) {
            entity = ((Research) lab).getBuilding();
        }
        else {
            entity = person.getVehicle();
        }

        if (entity != null) {

            // Modify based on the entity's wear condition.
            chance *= entity.getMalfunctionManager().getWearConditionAccidentModifier();

            if (RandomUtil.lessThanRandPercent(chance * time)) {
                logger.info(person.getName() + " has a lab accident while performing " + 
                        science.getName() + " experiment");
                entity.getMalfunctionManager().accident();
            }
        }
    }
    
    /**
     * Checks if the person is in a moving vehicle.
     * @param person the person.
     * @return true if person is in a moving vehicle.
     */
    public static boolean inMovingRover(Person person) {
        
        boolean result = false;
        
        if (person.getLocationSituation() == LocationSituation.IN_VEHICLE) {
            Vehicle vehicle = person.getVehicle();
            if (vehicle.getStatus().equals(Vehicle.MOVING)) {
                result = true;
            }
            else if (vehicle.getStatus().equals(Vehicle.TOWED)) {
                Vehicle towingVehicle = vehicle.getTowingVehicle();
                if (towingVehicle.getStatus().equals(Vehicle.MOVING) ||
                        towingVehicle.getStatus().equals(Vehicle.TOWED)) {
                    result = false;
                }
            }
        }
        
        return result;
    }

    @Override
    public void endTask() {
        super.endTask();

        // Remove person from lab so others can use it.
        try {
            if (lab != null) {
                lab.removeResearcher();
            }
        }
        catch(Exception e) {}
    }

    @Override
    public ScienceType getResearchScience() {
        return science;
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
        lab = null;
        science = null;
        malfunctions = null;
        researchAssistant = null;
    }
}