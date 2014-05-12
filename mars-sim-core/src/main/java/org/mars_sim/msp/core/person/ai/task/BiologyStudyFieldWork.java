/**
 * Mars Simulation Project
 * BiologyFieldWork.java
 * @version 3.06 2014-03-04
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * A task for the EVA operation of performing biology field work at a research site 
 * for a scientific study.
 */
public class BiologyStudyFieldWork
extends EVAOperation
implements Serializable {

    /** default serial id.*/
    private static final long serialVersionUID = 1L;

    // TODO Task phases should be enums
    private static final String FIELD_WORK = "Performing Field Work";

    // Data members
    private Person leadResearcher;
    private ScientificStudy study;
    private Rover rover;

    /**
     * Constructor.
     * @param person the person performing the task.
     * @param leadResearcher the researcher leading the field work.
     * @param study the scientific study the field work is for.
     * @param rover the rover
     */
    public BiologyStudyFieldWork(Person person, Person leadResearcher, ScientificStudy study, 
            Rover rover) {

        // Use EVAOperation parent constructor.
        super("Biology Study Field Work", person, true, RandomUtil.getRandomDouble(50D) + 10D);

        // Initialize data members.
        this.leadResearcher = leadResearcher;
        this.study = study;
        this.rover = rover;

        // Determine location for field work.
        Point2D fieldWorkLoc = determineFieldWorkLocation();
        setOutsideSiteLocation(fieldWorkLoc.getX(), fieldWorkLoc.getY());
        
        // Add task phases
        addPhase(FIELD_WORK);
    }

    /**
     * Determine location for field work.
     * @return field work X and Y location outside rover.
     */
    private Point2D determineFieldWorkLocation() {

        Point2D newLocation = null;
        boolean goodLocation = false;
        for (int x = 0; (x < 5) && !goodLocation; x++) {
            for (int y = 0; (y < 10) && !goodLocation; y++) {

                double distance = RandomUtil.getRandomDouble(100D) + (x * 100D) + 50D;
                double radianDirection = RandomUtil.getRandomDouble(Math.PI * 2D);
                double newXLoc = rover.getXLocation() - (distance * Math.sin(radianDirection));
                double newYLoc = rover.getYLocation() + (distance * Math.cos(radianDirection));
                Point2D boundedLocalPoint = new Point2D.Double(newXLoc, newYLoc);

                newLocation = LocalAreaUtil.getLocalRelativeLocation(boundedLocalPoint.getX(), 
                        boundedLocalPoint.getY(), rover);
                goodLocation = LocalAreaUtil.checkLocationCollision(newLocation.getX(), newLocation.getY(), 
                        person.getCoordinates());
            }
        }

        return newLocation;
    }

    /**
     * Checks if a person can research a site.
     * @param person the person
     * @param rover the rover
     * @return true if person can research a site.
     */
    public static boolean canResearchSite(Person person, Rover rover) {
        // Check if person can exit the rover.
        boolean exitable = ExitAirlock.canExitAirlock(person, rover.getAirlock());

        SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();

        // Check if it is night time outside.
        boolean sunlight = surface.getSurfaceSunlight(rover.getCoordinates()) > 0;

        // Check if in dark polar region.
        boolean darkRegion = surface.inDarkPolarRegion(rover.getCoordinates());

        // Check if person's medical condition will not allow task.
        boolean medical = person.getPerformanceRating() < .5D;

        return (exitable && (sunlight || darkRegion) && !medical);
    }

    @Override
    protected String getOutsideSitePhase() {
        return FIELD_WORK;
    }
    
    @Override
    protected double performMappedPhase(double time) {
        
        time = super.performMappedPhase(time);
        
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (FIELD_WORK.equals(getPhase())) {
            return fieldWorkPhase(time);
        }
        else {
            return time;
        }
    }
    
    /**
     * Perform the field work phase of the task.
     * @param time the time available (millisols).
     * @return remaining time after performing phase (millisols).
     * @throws Exception if error performing phase.
     */
    private double fieldWorkPhase(double time) {

        // Check for an accident during the EVA operation.
        checkForAccident(time);

        // Check if site duration has ended or there is reason to cut the field 
        // work phase short and return to the rover.
        if (shouldEndEVAOperation() || addTimeOnSite(time)) {
            setPhase(WALK_BACK_INSIDE);
            return time;
        }

        // Add research work to the scientific study for lead researcher.
        addResearchWorkTime(time);

        // Add experience points
        addExperience(time);

        return 0D;
    }

    /**
     * Adds research work time to the scientific study for the lead researcher.
     * @param time the time (millisols) performing field work.
     */
    private void addResearchWorkTime(double time) {
        // Determine effective field work time.
        double effectiveFieldWorkTime = time;
        int skill = getEffectiveSkillLevel();
        if (skill == 0) {
            effectiveFieldWorkTime /= 2D;
        }
        else if (skill > 1) {
            effectiveFieldWorkTime += effectiveFieldWorkTime * (.2D * skill);
        }

        // If person isn't lead researcher, divide field work time by two.
        if (!person.equals(leadResearcher)) {
            effectiveFieldWorkTime /= 2D;
        }

        // Add research to study for primary or collaborative researcher.
        if (study.getPrimaryResearcher().equals(leadResearcher)) {
            study.addPrimaryResearchWorkTime(effectiveFieldWorkTime);
        }
        else {
            study.addCollaborativeResearchWorkTime(leadResearcher, effectiveFieldWorkTime);
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

        // If phase is performing field work, add experience to biology skill.
        if (FIELD_WORK.equals(getPhase())) {
            // 1 base experience point per 10 millisols of field work time spent.
            // Experience points adjusted by person's "Experience Aptitude" attribute.
            double biologyExperience = time / 10D;
            biologyExperience += biologyExperience * experienceAptitudeModifier;
            person.getMind().getSkillManager().addExperience(SkillType.BIOLOGY, biologyExperience);
        }
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(2);
        results.add(SkillType.EVA_OPERATIONS);
        results.add(SkillType.BIOLOGY);
        return results;
    }

    @Override
    public int getEffectiveSkillLevel() {
        SkillManager manager = person.getMind().getSkillManager();
        int EVAOperationsSkill = manager.getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
        int biologySkill = manager.getEffectiveSkillLevel(SkillType.BIOLOGY);
        return (int) Math.round((double)(EVAOperationsSkill + biologySkill) / 2D); 
    }

    @Override
    public void destroy() {
        super.destroy();

        leadResearcher = null;
        study = null;
        rover = null;
    }
}