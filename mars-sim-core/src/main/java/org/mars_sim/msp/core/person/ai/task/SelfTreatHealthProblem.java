/**
 * Mars Simulation Project
 * SelfTreatHealthProblem.java
 * @version 3.07 2015-01-06
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.EventType;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.medical.HealthProblem;
import org.mars_sim.msp.core.person.medical.MedicalAid;
import org.mars_sim.msp.core.person.medical.Treatment;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.MedicalCare;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.SickBay;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * A task for performing a medical self-treatment at a medical station.
 */
public class SelfTreatHealthProblem extends Task implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;
    
    /** default logger. */
    private static Logger logger = Logger.getLogger(SelfTreatHealthProblem.class.getName());
    
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.selfTreatHealthProblem"); //$NON-NLS-1$
    
    /** Task phases. */
    private static final TaskPhase TREATMENT = new TaskPhase(Msg.getString(
            "Task.phase.treatingHealthProblem")); //$NON-NLS-1$
    
    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = 1D;
    
    // Data members.
    private MedicalAid medicalAid;
    private double duration;
    private double treatmentTime;
    private HealthProblem healthProblem;
    
    /** 
     * Constructor.
     * @param person the person to perform the task
     */
    public SelfTreatHealthProblem(Person person) {
        super(NAME, person, false, true, STRESS_MODIFIER, false, 0D);
        
        treatmentTime = 0D;
        
        // Choose available medical aid for treatment.
        medicalAid = determineMedicalAid();
        
        if (medicalAid != null) {
            
            // Determine health problem to treat.
            healthProblem = determineHealthProblemToTreat();
            if (healthProblem != null) {
                
                // Get the person's medical skill.
                int skill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.MEDICINE);
                
                // Determine medical treatment.
                Treatment treatment = healthProblem.getIllness().getRecoveryTreatment();
                if (treatment != null) {
                    duration = treatment.getAdjustedDuration(skill);
                    setStressModifier(STRESS_MODIFIER * treatment.getSkill());
                }
                else {
                    logger.severe(healthProblem + " does not have treatment.");
                    endTask();
                }
            }
            else {
                logger.severe(person + " could not self-treat a health problem at " + medicalAid);
                endTask();
            }
            
            // Walk to medical aid.
            if (medicalAid instanceof MedicalCare) {
                // Walk to medical care building.
                MedicalCare medicalCare = (MedicalCare) medicalAid;

                // Walk to medical care building.
                walkToActivitySpotInBuilding(medicalCare.getBuilding(), false);
            }
            else if (medicalAid instanceof SickBay) {
                // Walk to medical activity spot in rover.
                Vehicle vehicle = ((SickBay) medicalAid).getVehicle();
                if (vehicle instanceof Rover) {
                    
                    // Walk to rover sick bay activity spot.
                    walkToSickBayActivitySpotInRover((Rover) vehicle, false);
                }
            }
        }
        else {
            logger.severe("Medical aid could not be determined.");
            endTask();
        }
        
        // Initialize phase.
        addPhase(TREATMENT);
        setPhase(TREATMENT);
    }
    
    /**
     * Determine a local medical aid to use for self-treating a health problem.
     * @return medical aid or null if none found.
     */
    private MedicalAid determineMedicalAid() {
        
        MedicalAid result = null;
        
        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            result = determineMedicalAidAtSettlement();
        }
        else if (person.getLocationSituation() == LocationSituation.IN_VEHICLE) {
            result = determineMedicalAidInVehicle();
        }
        
        return result;
    }
    
    /**
     * Determine a medical aid at a settlement to use for self-treating a health problem.
     * @return medical aid or null if none found.
     */
    private MedicalAid determineMedicalAidAtSettlement() {
        
        MedicalAid result = null;
        
        List<MedicalAid> goodMedicalAids = new ArrayList<MedicalAid>();
        
        // Check all medical care buildings.
        Iterator<Building> i = person.getSettlement().getBuildingManager().getBuildings(
                BuildingFunction.MEDICAL_CARE).iterator();
        while (i.hasNext()) {
            Building building = i.next();
            
            // Check if building currently has a malfunction.
            boolean malfunction = building.getMalfunctionManager().hasMalfunction();
            
            // Check if enough beds for patient.
            MedicalCare medicalCare = (MedicalCare) building.getFunction(BuildingFunction.MEDICAL_CARE);
            int numPatients = medicalCare.getPatientNum();
            int numBeds = medicalCare.getSickBedNum();
            
            if ((numPatients < numBeds) && !malfunction) {
                
                // Check if any of person's self-treatable health problems can be treated in building.
                boolean canTreat = false;
                Iterator<HealthProblem> j = getSelfTreatableHealthProblems().iterator();
                while (j.hasNext() && !canTreat) {
                    HealthProblem problem = j.next();
                    if (medicalCare.canTreatProblem(problem)) {
                        canTreat = true;
                    }
                }
                
                if (canTreat) {
                    goodMedicalAids.add(medicalCare);
                }
            }
        }
        
        // Randomly select an valid medical care building.
        if (goodMedicalAids.size() > 0) {
            int index = RandomUtil.getRandomInt(goodMedicalAids.size() - 1);
            result = goodMedicalAids.get(index);
        }
        
        return result;
    }
    
    /**
     * Determine a medical aid on a vehicle to use for self-treating a health problem.
     * @return medical aid or null if none found.
     */
    private MedicalAid determineMedicalAidInVehicle() {
        
        MedicalAid result = null;
        
        if (person.getVehicle() instanceof Rover) {
            Rover rover = (Rover) person.getVehicle();
            if (rover.hasSickBay()) {
                SickBay sickBay = rover.getSickBay();
                
                // Check if enough beds for patient.
                int numPatients = sickBay.getPatientNum();
                int numBeds = sickBay.getSickBedNum();
                
                if (numPatients < numBeds) {
                    
                    // Check if any of person's self-treatable health problems can be treated in sick bay.
                    boolean canTreat = false;
                    Iterator<HealthProblem> j = getSelfTreatableHealthProblems().iterator();
                    while (j.hasNext() && !canTreat) {
                        HealthProblem problem = j.next();
                        if (sickBay.canTreatProblem(problem)) {
                            canTreat = true;
                        } 
                    }
                    
                    if (canTreat) {
                        result = sickBay;
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Determines the health problem to self-treat.
     * @return health problem or null if none found.
     */
    private HealthProblem determineHealthProblemToTreat() {
        
        HealthProblem result = null;
        
        if (medicalAid != null) {
            
            // Choose most severe health problem that can be self-treated.
            int highestSeverity = Integer.MIN_VALUE;
            Iterator<HealthProblem> i = getSelfTreatableHealthProblems().iterator();
            while (i.hasNext()) {
                HealthProblem problem = i.next();
                if (medicalAid.canTreatProblem(problem)) {
                    int severity = problem.getIllness().getSeriousness();
                    if (severity > highestSeverity) {
                        result = problem;
                        highestSeverity = severity;
                    }
                }
            }
        }
        else {
            logger.severe("Medical aid is null.");
        }
        
        return result;
    }
    
    /**
     * Gets a list of health problems the person can self-treat.
     * @return list of health problems (may be empty).
     */
    private List<HealthProblem> getSelfTreatableHealthProblems() {
        
        List<HealthProblem> result = new ArrayList<HealthProblem>();
        
        Iterator<HealthProblem> i = person.getPhysicalCondition().getProblems().iterator();
        while (i.hasNext()) {
            HealthProblem problem = i.next();
            if (problem.getDegrading()) {
                Treatment treatment = problem.getIllness().getRecoveryTreatment();
                if (treatment != null) {
                    boolean selfTreatable = treatment.getSelfAdminister();
                    int skill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.MEDICINE);
                    int requiredSkill = treatment.getSkill();
                    if (selfTreatable && (skill >= requiredSkill)) {
                        result.add(problem);
                    }
                }
            }
        }
        
        return result;
    }
    
    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (TREATMENT.equals(getPhase())) {
            return treatmentPhase(time);
        }
        else {
            return time;
        }
    }
    
    /**
     * Performs the treatment phase of the task.
     * @param time the amount of time (millisol) to perform the phase.
     * @return the amount of time (millisol) left over after performing the phase.
     */
    private double treatmentPhase(double time) {
        
        double timeLeft = 0D;
        
        // If medical aid has malfunction, end task.
        if (getMalfunctionable().getMalfunctionManager().hasMalfunction()) {
            endTask();
        }

        if (isDone()) {
            return time;
        }

        // Start treatment if not already started.
        if (!medicalAid.getProblemsBeingTreated().contains(healthProblem)) {
            medicalAid.requestTreatment(healthProblem);
            medicalAid.startTreatment(healthProblem, duration);
            logger.info(person.getName() + " self-treating " + healthProblem.getIllness().getName());

            // Create starting task event if needed.
            if (getCreateEvents()) {
                TaskEvent startingEvent = new TaskEvent(person, this, EventType.TASK_START, "");
                Simulation.instance().getEventManager().registerNewEvent(startingEvent);
            }
        }
        
        // Check for accident in medical aid.
        checkForAccident(time);

        treatmentTime += time;
        if (treatmentTime >= duration) {
            healthProblem.startRecovery();
            timeLeft = treatmentTime - duration;
            endTask();
        }

        // Add experience.
        addExperience(time);

        return timeLeft;
    }
    
    @Override
    protected BuildingFunction getRelatedBuildingFunction() {
        return BuildingFunction.MEDICAL_CARE;
    }

    @Override
    public int getEffectiveSkillLevel() {
        SkillManager manager = person.getMind().getSkillManager();
        return manager.getEffectiveSkillLevel(SkillType.MEDICINE);
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(1);
        results.add(SkillType.MEDICINE);
        return results;
    }

    @Override
    protected void addExperience(double time) {
        
        // Add experience to "Medical" skill
        // (1 base experience point per 25 millisols of work)
        // Experience points adjusted by person's "Experience Aptitude" attribute.
        double newPoints = time / 25D;
        int experienceAptitude = person.getNaturalAttributeManager().getAttribute(
                NaturalAttribute.EXPERIENCE_APTITUDE);
        newPoints += newPoints * ((double) experienceAptitude - 50D) / 100D;
        newPoints *= getTeachingExperienceModifier();
        person.getMind().getSkillManager().addExperience(SkillType.MEDICINE, newPoints);
    }
    
    /**
     * Gets the malfunctionable associated with the medical aid.
     * @return the associated Malfunctionable
     */
    private Malfunctionable getMalfunctionable() {
        Malfunctionable result = null;

        if (medicalAid instanceof SickBay) {
            result = ((SickBay) medicalAid).getVehicle();
        }
        else if (medicalAid instanceof MedicalCare) {
            result = ((MedicalCare) medicalAid).getBuilding();
        }
        else {
            result = (Malfunctionable) medicalAid;
        }

        return result;
    }
    
    /**
     * Check for accident in the medical aid.
     * @param time the amount of time working (in millisols)
     */
    private void checkForAccident(double time) {

        Malfunctionable entity = getMalfunctionable();

        double chance = .001D;

        // Medical skill modification.
        int skill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.MEDICINE);
        if (skill <= 3) {
            chance *= (4 - skill);
        }
        else {
            chance /= (skill - 2);
        }

        // Modify based on the entity's wear condition.
        chance *= entity.getMalfunctionManager().getWearConditionAccidentModifier();

        if (RandomUtil.lessThanRandPercent(chance * time)) {
            logger.info(person.getName() + " has accident during medical treatment.");
            entity.getMalfunctionManager().accident();
        }
    }
    
    @Override
    public void endTask() {
        super.endTask();

        // Stop treatment.
        if (medicalAid.getProblemsBeingTreated().contains(healthProblem)) {
            medicalAid.stopTreatment(healthProblem);
        }
    }
    
    @Override
    public void destroy() {
        super.destroy();

        medicalAid = null;
        healthProblem = null;
    }
}