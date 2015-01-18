/**
 * Mars Simulation Project
 * RepairMalfunction.java
 * @version 3.07 2015-01-14
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.malfunction.Malfunction;
import org.mars_sim.msp.core.malfunction.MalfunctionFactory;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * The RepairMalfunction class is a task to repair a malfunction.
 */
public class RepairMalfunction
extends Task
implements Repair, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default logger. */
    private static Logger logger = Logger.getLogger(RepairMalfunction.class.getName());

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.repairMalfunction"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase REPAIRING = new TaskPhase(Msg.getString(
            "Task.phase.repairing")); //$NON-NLS-1$

    // Static members
    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = .3D;

    // Data members
    /** Entity being repaired. */
    private Malfunctionable entity;

    /**
     * Constructor
     * @param person the person to perform the task
     */
    public RepairMalfunction(Person person) {
        super(NAME, person, true, false, STRESS_MODIFIER, true, 10D + 
                RandomUtil.getRandomDouble(50D));

        // Get the malfunctioning entity.
        entity = getMalfunctionEntity(person);
        if (entity != null) {
            // Add person to location of malfunction if possible.
            addPersonToMalfunctionLocation(entity);
        }
        else {
            endTask();
        }

        // Initialize phase
        addPhase(REPAIRING);
        setPhase(REPAIRING);

        logger.fine(person.getName() + " repairing malfunction.");
    }

    /**
     * Gets a malfunctional entity with a normal malfunction for a user.
     * @param person the person.
     * @return malfunctional entity.
     */
    private static Malfunctionable getMalfunctionEntity(Person person) {
        Malfunctionable result = null;

        Iterator<Malfunctionable> i = MalfunctionFactory.getMalfunctionables(person).iterator();
        while (i.hasNext() && (result == null)) {
            Malfunctionable entity = i.next();
            if (!requiresEVA(person, entity)) {
                if (hasMalfunction(person, entity)) {
                    result = entity;
                }
            }
        }

        return result;
    }
    
    /**
     * Check if a malfunctionable entity requires an EVA to repair.
     * @param person the person doing the repair.
     * @param entity the entity with a malfunction.
     * @return true if entity requires an EVA repair.
     */
    public static boolean requiresEVA(Person person, Malfunctionable entity) {
        
        boolean result = false;
        
        if (entity instanceof Vehicle) {
            // Requires EVA repair on outside vehicles that the person isn't inside.
            Vehicle vehicle = (Vehicle) entity;
            boolean outsideVehicle = BuildingManager.getBuilding(vehicle) == null;
            boolean personNotInVehicle = !vehicle.getInventory().containsUnit(person);
            if (outsideVehicle && personNotInVehicle) {
                result = true;
            }
        }
        else if (entity instanceof Building) {
            // Requires EVA repair on uninhabitable buildings.
            Building building = (Building) entity;
            if (!building.hasFunction(BuildingFunction.LIFE_SUPPORT)) {
                result = true;
            }
        }
        
        return result;
    }

    /**
     * Gets a malfunctional entity with a normal malfunction for a user.
     * @return malfunctional entity.
     */
    private static boolean hasMalfunction(Person person, Malfunctionable entity) {
        boolean result = false;

        MalfunctionManager manager = entity.getMalfunctionManager();
        Iterator<Malfunction> i = manager.getNormalMalfunctions().iterator();
        while (i.hasNext() && !result) {
            if (hasRepairPartsForMalfunction(person, i.next())) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Checks if there are enough repair parts at person's location to fix the malfunction.
     * @param person the person checking.
     * @param malfunction the malfunction.
     * @return true if enough repair parts to fix malfunction.
     */
    public static boolean hasRepairPartsForMalfunction(Person person, 
            Malfunction malfunction) {
        if (person == null) {
            throw new IllegalArgumentException("person is null");
        }
        if (malfunction == null) {
            throw new IllegalArgumentException("malfunction is null");
        }

        boolean result = false;    	
        Unit containerUnit = person.getTopContainerUnit();

        if (containerUnit != null) {
            result = true;
            Inventory inv = containerUnit.getInventory();

            Map<Part, Integer> repairParts = malfunction.getRepairParts();
            Iterator<Part> i = repairParts.keySet().iterator();
            while (i.hasNext() && result) {
                Part part = i.next();
                int number = repairParts.get(part);
                if (inv.getItemResourceNum(part) < number) {
                    result = false;
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
        else if (REPAIRING.equals(getPhase())) {
            return repairingPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Performs the repairing phase of the task.
     * @param time the amount of time (millisol) to perform the phase.
     * @return the amount of time (millisol) left after performing the phase.
     */
    private double repairingPhase(double time) {

        // Check if there are no more malfunctions.
        if (!hasMalfunction(person, entity)) {
            endTask();
        }

        if (isDone()) {
            return time;
        }

        // Determine effective work time based on "Mechanic" skill.
        double workTime = time;
        int mechanicSkill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.MECHANICS);
        if (mechanicSkill == 0) {
            workTime /= 2;
        }
        else if (mechanicSkill > 1) {
            workTime += workTime * (.2D * mechanicSkill);
        }

        // Get a local malfunction.
        Malfunction malfunction = null;
        Iterator<Malfunction> i = entity.getMalfunctionManager().getNormalMalfunctions().iterator();
        while (i.hasNext() && (malfunction == null)) {
            Malfunction tempMalfunction = i.next();
            if (hasRepairPartsForMalfunction(person, tempMalfunction)) {
                malfunction = tempMalfunction;
                setDescription(Msg.getString("Task.description.repairMalfunction.detail", 
                        malfunction.getName(), entity.getName())); //$NON-NLS-1$
            }
        }

        // Add repair parts if necessary.
        if (hasRepairPartsForMalfunction(person, malfunction)) {
            Inventory inv = person.getTopContainerUnit().getInventory();
            Map<Part, Integer> parts = new HashMap<Part, Integer>(malfunction.getRepairParts());
            Iterator<Part> j = parts.keySet().iterator();
            while (j.hasNext()) {
                Part part = j.next();
                int number = parts.get(part);
                inv.retrieveItemResources(part, number);
                malfunction.repairWithParts(part, number);
            }
        }
        else {
            endTask();
            return time;
        }

        // Add work to malfunction.
        // logger.info(description);
        double workTimeLeft = malfunction.addWorkTime(workTime);

        // Add experience
        addExperience(time);

        // Check if an accident happens during repair.
        checkForAccident(time);

        // Check if there are no more malfunctions.
        if (!hasMalfunction(person, entity)) {
            endTask();
        }

        return workTimeLeft;
    }

    @Override
    protected void addExperience(double time) {
        // Add experience to "Mechanics" skill
        // (1 base experience point per 20 millisols of work)
        // Experience points adjusted by person's "Experience Aptitude" attribute.
        double newPoints = time / 20D;
        int experienceAptitude = person.getNaturalAttributeManager().getAttribute(NaturalAttribute.EXPERIENCE_APTITUDE);
        newPoints += newPoints * ((double) experienceAptitude - 50D) / 100D;
        newPoints *= getTeachingExperienceModifier();
        person.getMind().getSkillManager().addExperience(SkillType.MECHANICS, newPoints);
    }

    /**
     * Check for accident with entity during maintenance phase.
     * @param time the amount of time (in millisols)
     */
    private void checkForAccident(double time) {

        double chance = .001D;

        // Mechanic skill modification.
        int skill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.MECHANICS);
        if (skill <= 3) {
            chance *= (4 - skill);
        }
        else {
            chance /= (skill - 2);
        }

        // Modify based on the entity's wear condition.
        chance *= entity.getMalfunctionManager().getWearConditionAccidentModifier();

        if (RandomUtil.lessThanRandPercent(chance * time)) {
            // logger.info(person.getName() + " has accident while " + description);
            if (entity != null) {
                entity.getMalfunctionManager().accident();
            }
        }
    }

    @Override
    public Malfunctionable getEntity() {
        return entity;
    }

    /**
     * Adds the person to building if malfunctionable is a building with life support.
     * Otherwise walk to random location.
     * @param malfunctionable the malfunctionable the person is repairing.
     */
    private void addPersonToMalfunctionLocation(Malfunctionable malfunctionable) {

        boolean isWalk = false;
        if (malfunctionable instanceof Building) {
            Building building = (Building) malfunctionable;
            if (building.hasFunction(BuildingFunction.LIFE_SUPPORT)) {

                // Walk to malfunctioning building.
                walkToRandomLocInBuilding(building, true);
                isWalk = true;
            }
        }
        else if (malfunctionable instanceof Rover) {
            // Walk to malfunctioning rover.
            walkToRandomLocInRover((Rover) malfunctionable, true);
            isWalk = true;
        }

        if (!isWalk) {
            walkToRandomLocation(true);
        }
    }

    @Override
    public int getEffectiveSkillLevel() {
        SkillManager manager = person.getMind().getSkillManager();
        return manager.getEffectiveSkillLevel(SkillType.MECHANICS);
    }  

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(1);
        results.add(SkillType.MECHANICS);
        return results;
    }

    @Override
    public void destroy() {
        super.destroy();

        entity = null;
    }
}