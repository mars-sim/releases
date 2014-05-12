/**
 * Mars Simulation Project
 * PrescribeMedication.java
 * @version 3.06 2014-02-26
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.Doctor;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.medical.AntiStressMedication;
import org.mars_sim.msp.core.person.medical.Medication;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * A task in which a doctor prescribes (and provides) a medication to a patient.
 */
public class PrescribeMedication
extends Task
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(PrescribeMedication.class.getName());

	// TODO Task phase should be an enum.
	private static final String MEDICATING = "Medicating";

	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = 0D;

	// Data members.
	private Person patient = null;
	private Medication medication = null;

	/**
	 * Constructor.
	 * @param person the person performing the task.
	 */
	public PrescribeMedication(Person person) {
        // Use task constructor.
        super("Prescribing Medication", person, true, false, STRESS_MODIFIER, true, 10D);
        
        // Determine patient needing medication.
        patient = determinePatient(person);
        if (patient != null) {
            // Determine medication to prescribe.
            medication = determineMedication(patient);
            
            // If in settlement, move doctor to building patient is in.
            if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
                
                // Walk to patient's building.
                walkToPatientBuilding(BuildingManager.getBuilding(patient));
            }
            
            logger.info(person.getName() + " prescribing " + medication.getName() + 
                    " to " + patient.getName());
        }
        else {
            endTask();
        }
        
        // Initialize phase
        addPhase(MEDICATING);
        setPhase(MEDICATING);
    }
    
    /** 
     * Returns the weighted probability that a person might perform this task.
     * It should return a 0 if there is no chance to perform this task given 
     * the person and his/her situation.
     * @param person the person to perform the task
     * @return the weighted probability that a person might perform this task
     */
    public static double getProbability(Person person) {
        double result = 0D;

        // Only doctor job allowed to perform this task.
        Job job = person.getMind().getJob();
        if (job instanceof Doctor) {
            
            // Determine patient needing medication.
            Person patient = determinePatient(person);
            if (patient != null) {
                result = 100D;
            }
        }
        
        // Effort-driven task modifier.
        result *= person.getPerformanceRating();

        return result;
    }
    
    /**
     * Walk to patient's building.
     * @param patientBuilding the patient's building.
     */
    private void walkToPatientBuilding(Building patientBuilding) {
        
        // Determine location within patient's building.
        // TODO: Use action point rather than random internal location.
        Point2D.Double buildingLoc = LocalAreaUtil.getRandomInteriorLocation(patientBuilding);
        Point2D.Double settlementLoc = LocalAreaUtil.getLocalRelativeLocation(buildingLoc.getX(), 
                buildingLoc.getY(), patientBuilding);
        
        if (Walk.canWalkAllSteps(person, settlementLoc.getX(), settlementLoc.getY(), 
                patientBuilding)) {
            
            // Add subtask for walking to patient building.
            addSubTask(new Walk(person, settlementLoc.getX(), settlementLoc.getY(), 
                    patientBuilding));
        }
        else {
            logger.fine(person.getName() + " unable to walk to patient building " + 
                    patientBuilding.getName());
            endTask();
        }
    }
    
    /**
     * Determines if there is a patient nearby needing medication.
     * @param doctor the doctor prescribing the medication.
     * @return patient if one found, null otherwise.
     */
    private static Person determinePatient(Person doctor) {
        Person result = null;
        
        // Get possible patient list.
        // Note: Doctor can also prescribe medication for himself.
        Collection<Person> patientList = null;
        LocationSituation loc = doctor.getLocationSituation();
        if (loc == LocationSituation.IN_SETTLEMENT) {
            patientList = doctor.getSettlement().getInhabitants();
        }
        else if (loc == LocationSituation.IN_VEHICLE) {
            Vehicle vehicle = doctor.getVehicle();
            if (vehicle instanceof Crewable) {
                Crewable crewVehicle = (Crewable) vehicle;
                patientList = crewVehicle.getCrew();
            }
        }
        
        // Determine patient.
        if (patientList != null) {
            Iterator<Person> i = patientList.iterator();
            while (i.hasNext() && (result == null)) {
                Person person = i.next();
                PhysicalCondition condition = person.getPhysicalCondition();
                if (!condition.isDead() && (condition.getStress() >= 100D)) {
                    // Only prescribing anti-stress medication at the moment.
                    if (!condition.hasMedication(AntiStressMedication.NAME)) {
                        result = person;
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Determines a medication for the patient.
     * @param patient the patient to medicate.
     * @return medication.
     */
    private Medication determineMedication(Person patient) {
        // Only allow anti-stress medication for now.
        return new AntiStressMedication(patient);
    }
    
    /**
     * Performs the medicating phase.
     * @param time the amount of time (millisols) to perform the phase.
     * @return the amount of time (millisols) left over after performing the phase.
     */
    private double medicatingPhase(double time) {
        
        // If duration, provide medication.
        if (getDuration() <= (getTimeCompleted() + time)) {
            if (patient != null) {
                if (medication != null) {
                    PhysicalCondition condition = patient.getPhysicalCondition();
                    
                    // Check if patient already has taken medication.
                    if (!condition.hasMedication(medication.getName())) {
                        // Medicate patient.
                        condition.addMedication(medication);
                    }
                }
                else throw new IllegalStateException("medication is null");
            }
            else throw new IllegalStateException ("patient is null");
        }
        
        // Add experience.
        addExperience(time);
        
        return 0D;
    }
    
    @Override
    protected void addExperience(double time) {
        // Add experience to "Medical" skill
        // (1 base experience point per 10 millisols of work)
        // Experience points adjusted by person's "Experience Aptitude" attribute.
        double newPoints = time / 10D;
        int experienceAptitude = person.getNaturalAttributeManager().getAttribute(
            NaturalAttribute.EXPERIENCE_APTITUDE);
        newPoints += newPoints * ((double) experienceAptitude - 50D) / 100D;
        newPoints *= getTeachingExperienceModifier();
		person.getMind().getSkillManager().addExperience(SkillType.MEDICINE, newPoints);
    }

    @Override
	public List<SkillType> getAssociatedSkills() {
		List<SkillType> results = new ArrayList<SkillType>(1);
		results.add(SkillType.MEDICINE);
        return results;
    }

    @Override
    public int getEffectiveSkillLevel() {
        SkillManager manager = person.getMind().getSkillManager();
		return manager.getEffectiveSkillLevel(SkillType.MEDICINE);
    }

    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (MEDICATING.equals(getPhase())) {
            return medicatingPhase(time);
        }
        else {
            return time;
        }
    }
    
    @Override
    public void destroy() {
        super.destroy();
        
        patient = null;
        medication = null;
    }
}