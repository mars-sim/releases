/**
 * Mars Simulation Project
 * RespondToStudyInvitation.java
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
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.social.Relationship;
import org.mars_sim.msp.core.person.ai.social.RelationshipManager;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.science.ScientificStudyManager;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * A task for responding to an invitation to collaborate on a scientific study.
 */
public class RespondToStudyInvitation
extends Task
implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default logger. */
    private static Logger logger = Logger.getLogger(RespondToStudyInvitation.class.getName());

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.respondToStudyInvitation"); //$NON-NLS-1$

    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = 0D;

    /** Duration (millisols) of task. */
    private static final double DURATION = 40D;

    /** Task phases. */
    private static final TaskPhase RESPONDING_INVITATION = new TaskPhase(Msg.getString(
            "Task.phase.respondingInvitation")); //$NON-NLS-1$

    /** The scientific study. */
    private ScientificStudy study;

    /**
     * Constructor
     * @param person the person performing the task.
     */
    public RespondToStudyInvitation(Person person) {
        super(NAME, person, false, true, STRESS_MODIFIER, true, DURATION);

        ScientificStudyManager manager = Simulation.instance().getScientificStudyManager();
        List<ScientificStudy> invitedStudies = manager.getOpenInvitationStudies(person);
        if (invitedStudies.size() > 0) {
            study = invitedStudies.get(0);

            // If person is in a settlement, try to find an administration building.
            boolean adminWalk = false;
            if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {         
                Building adminBuilding = getAvailableAdministrationBuilding(person);
                if (adminBuilding != null) {
                    // Walk to administration building.
                    walkToActivitySpotInBuilding(adminBuilding, false);
                    adminWalk = true;
                }
            }

            if (!adminWalk) {

                if (person.getLocationSituation() == LocationSituation.IN_VEHICLE) {
                    // If person is in rover, walk to passenger activity spot.
                    if (person.getVehicle() instanceof Rover) {
                        walkToPassengerActivitySpotInRover((Rover) person.getVehicle(), false);
                    }
                }
                else {
                    // Walk to random location.
                    walkToRandomLocation(true);
                }
            }
        }
        else {
            logger.severe(person.getName() + " does not have any open invited studies.");
            endTask();
        }

        // Initialize phase
        addPhase(RESPONDING_INVITATION);
        setPhase(RESPONDING_INVITATION);
    }

    /**
     * Gets an available administration building that the person can use.
     * @param person the person
     * @return available administration building or null if none.
     */
    public static Building getAvailableAdministrationBuilding(Person person) {

        Building result = null;

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            BuildingManager manager = person.getSettlement().getBuildingManager();
            List<Building> administrationBuildings = manager.getBuildings(BuildingFunction.ADMINISTRATION);
            administrationBuildings = BuildingManager.getNonMalfunctioningBuildings(administrationBuildings);
            administrationBuildings = BuildingManager.getLeastCrowdedBuildings(administrationBuildings);

            if (administrationBuildings.size() > 0) {
                Map<Building, Double> administrationBuildingProbs = BuildingManager.getBestRelationshipBuildings(
                        person, administrationBuildings);
                result = RandomUtil.getWeightedRandomObject(administrationBuildingProbs);
            }
        }

        return result;
    }

    @Override
    protected BuildingFunction getRelatedBuildingFunction() {
        return BuildingFunction.ADMINISTRATION;
    }

    /**
     * Performs the responding to invitation phase.
     * @param time the time (millisols) to perform the phase.
     * @return the remaining time (millisols) after performing the phase.
     */
    private double respondingToInvitationPhase(double time) {

        if (isDone()) {
            return time;
        }

        // If duration, send respond to invitation.
        if (getDuration() <= (getTimeCompleted() + time)) {

            study.respondingInvitedResearcher(person);
            Job job = person.getMind().getJob();

            // Get relationship between invitee and primary researcher.
            Person primaryResearcher = study.getPrimaryResearcher();
            RelationshipManager relationshipManager = Simulation.instance().getRelationshipManager();
            Relationship relationship = relationshipManager.getRelationship(person, primaryResearcher);

            // Decide response to invitation.
            if (decideResponse()) {
                ScienceType science = ScienceType.getJobScience(job);
                study.addCollaborativeResearcher(person, science);

                // Add 10 points to primary researcher's opinion of invitee for accepting invitation.
                if (relationship != null) {
                    double currentOpinion = relationship.getPersonOpinion(primaryResearcher);
                    relationship.setPersonOpinion(primaryResearcher, currentOpinion + 10D);
                }

                logger.fine(job.getName(person.getGender()) + " " + person.getName() + 
                        " accepting invitation from " + primaryResearcher.getName() + 
                        " to collaborate on " + study.toString());
            }
            else {

                // Subtract 10 points from primary researcher's opinion of invitee for rejecting invitation.
                if (relationship != null) {
                    double currentOpinion = relationship.getPersonOpinion(primaryResearcher);
                    relationship.setPersonOpinion(primaryResearcher, currentOpinion - 10D);
                }

                logger.fine(job.getName(person.getGender()) + " " + person.getName() + 
                        " rejecting invitation from " + primaryResearcher.getName() + 
                        " to collaborate on " + study.toString());
            }
        }

        return 0D;
    }

    /**
     * Decides is the researcher accepts or rejects invitation.
     * @return true if accepts, false if rejects.
     */
    private boolean decideResponse() {
        boolean result = false;

        ScienceType studyScience = study.getScience();
        ScienceType jobScience = ScienceType.getJobScience(person.getMind().getJob());
        if (jobScience != null) {
            boolean isPrimaryScience = studyScience.equals(jobScience);
            boolean isCollaborativeScience = ScienceType.isCollaborativeScience(studyScience, jobScience);
            if (isPrimaryScience || isCollaborativeScience) {
                double acceptChance = 50D;

                // Modify based on study primary researcher's achievement.
                double primaryAchievement = study.getPrimaryResearcher().getScientificAchievement(studyScience);
                acceptChance += primaryAchievement;

                // Modify based on study collaborative researchers' achievements.
                Iterator<Person> i = study.getCollaborativeResearchers().keySet().iterator();
                while (i.hasNext()) {
                    Person collaborator = i.next();
                    ScienceType collaborativeScience = study.getCollaborativeResearchers().get(collaborator);
                    acceptChance += (collaborator.getScientificAchievement(collaborativeScience) / 2D);
                }

                // Modify if researcher's job science is collaborative.
                if (isCollaborativeScience) {
                    acceptChance/= 2D;
                }

                // Modify by how many studies researcher is already collaborating on.
                ScientificStudyManager manager = Simulation.instance().getScientificStudyManager();
                int numCollabStudies = manager.getOngoingCollaborativeStudies(person).size();
                acceptChance /= (numCollabStudies + 1D);

                // Modify based on difficulty level of study vs researcher's skill.
                SkillType skill = jobScience.getSkill();
                int skillLevel = person.getMind().getSkillManager().getSkillLevel(skill);
                if (skillLevel == 0) {
                    skillLevel = 1;
                }
                int difficultyLevel = study.getDifficultyLevel();
                if (difficultyLevel == 0) {
                    difficultyLevel = 1;
                }
                acceptChance *= ((double) difficultyLevel / (double) skillLevel);

                // Modify based on researchers opinion of primary researcher.
                RelationshipManager relationshipManager = Simulation.instance().getRelationshipManager();
                double researcherOpinion = relationshipManager.getOpinionOfPerson(person, 
                        study.getPrimaryResearcher());
                acceptChance *= (researcherOpinion / 50D);

                // Modify based on if researcher and primary researcher are at same settlement.
                Settlement researcherSettlement = person.getAssociatedSettlement();
                Settlement primarySettlement = study.getPrimaryResearcher().getAssociatedSettlement();
                if ((researcherSettlement != null) && researcherSettlement.equals(primarySettlement)) {
                    acceptChance *= 2D;
                }

                result = (RandomUtil.getRandomDouble(100D) < acceptChance);
            }
        }

        return result;
    }

    @Override
    protected void addExperience(double time) {
        // Add experience to relevant science skill
        // 1 base experience point per 25 millisols of proposal writing time.
        double newPoints = time / 25D;

        // Experience points adjusted by person's "Academic Aptitude" attribute.
        int academicAptitude = person.getNaturalAttributeManager().getAttribute(NaturalAttribute.ACADEMIC_APTITUDE);
        newPoints += newPoints * ((double) academicAptitude - 50D) / 100D;
        newPoints *= getTeachingExperienceModifier();

        ScienceType jobScience = ScienceType.getJobScience(person.getMind().getJob());
        person.getMind().getSkillManager().addExperience(jobScience.getSkill(), newPoints);
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> result = new ArrayList<SkillType>(1);
        ScienceType jobScience = ScienceType.getJobScience(person.getMind().getJob());
        if (jobScience != null) {
            result.add(jobScience.getSkill());
        }
        return result;
    }

    @Override
    public int getEffectiveSkillLevel() {
        ScienceType jobScience = ScienceType.getJobScience(person.getMind().getJob());
        return person.getMind().getSkillManager().getEffectiveSkillLevel(jobScience.getSkill());
    }

    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (RESPONDING_INVITATION.equals(getPhase())) {
            return respondingToInvitationPhase(time);
        }
        else {
            return time;
        }
    }

    @Override
    public void destroy() {
        super.destroy();

        study = null;
    }
}