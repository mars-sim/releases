/**
 * Mars Simulation Project
 * RepairEVAMalfunction.java
 * @version 2.75 2002-06-08
 * @author Scott Davis
 */

package org.mars_sim.msp.simulation.person.ai;

import org.mars_sim.msp.simulation.*;
import org.mars_sim.msp.simulation.person.*;
import org.mars_sim.msp.simulation.malfunction.*;
import java.io.Serializable;
import java.util.*;

/**
 * The RepairEVAMalfunction class is a task to repair a malfunction.
 */
public class RepairEVAMalfunction extends EVAOperation implements Repair, Serializable {

    // Phase names
    private static final String EXIT_AIRLOCK = "Exit Airlock";
    private static final String REPAIR_MALFUNCTION = "Repair Malfunction";
    private static final String ENTER_AIRLOCK = "Enter Airlock";
	
    // Data members
    private Malfunctionable entity; // The malfunctionable entity being repaired.
    private Airlockable airlocker; // The unit that provides the airlock.
    private Malfunctionable airlocker2; // The unit that provids the airlock.
    private double duration; // Duration of task in millisols.
	
    /**
     * Constructs a RepairEVAMalfunction object.
     * @param person the person to perform the task
     * @param mars the virtual Mars
     */
    public RepairEVAMalfunction(Person person, Mars mars) {
        super("Repairing EVA Malfunction", person, mars);

	// Randomly determine duration, from 0 - 500 millisols.
	duration = RandomUtil.getRandomDouble(500D);
	airlocker = (Airlockable) person.getContainerUnit();
	airlocker2 = (Malfunctionable) person.getContainerUnit();

	phase = EXIT_AIRLOCK;

	// System.out.println(person.getName() + " has started the RepairEVAMalfunction task.");
    }

    /**
     * Checks if the person has a local EVA malfunction.
     * @return true if malfunction, false if none.
     */
    public static boolean hasEVAMalfunction(Person person) {
   
        boolean result = false;

	Iterator i = MalfunctionFactory.getMalfunctionables(person).iterator();
	while (i.hasNext()) {
	    MalfunctionManager manager = ((Malfunctionable) i.next()).getMalfunctionManager();
	    if (manager.hasEVAMalfunction()) result = true;
	}

	return result;
    }

    /**
     * Checks if the malfunctionable entity has a local EVA malfunction.
     * @return true if malfunction, false if none.
     */
    public static boolean hasEVAMalfunction(Malfunctionable entity) {
   
        boolean result = false;

	Iterator i = MalfunctionFactory.getMalfunctionables(entity).iterator();
	while (i.hasNext()) {
	    MalfunctionManager manager = ((Malfunctionable) i.next()).getMalfunctionManager();
	    if (manager.hasEVAMalfunction()) result = true;
	}

	return result;
    }

    /** Returns the weighted probability that a person might perform this task.
     *  @param person the person to perform the task
     *  @param mars the virtual Mars
     *  @return the weighted probability that a person might perform this task
     */
    public static double getProbability(Person person, Mars mars) {
        double result = 0D;

        // Total probabilities for all malfunctionable entities in person's local.
        Iterator i = MalfunctionFactory.getMalfunctionables(person).iterator();
        while (i.hasNext()) {
            MalfunctionManager manager = ((Malfunctionable) i.next()).getMalfunctionManager();
            if (manager.hasEVAMalfunction()) result = 50D;
        }

        // Check if person is in airlockable unit.
	if (!(person.getContainerUnit() instanceof Airlockable)) result = 0D;

        // Check if it is night time.
	if (mars.getSurfaceFeatures().getSurfaceSunlight(person.getCoordinates()) == 0) result = 0D; 
	
        // Effort-driven task modifier.
        result *= person.getPerformanceRating();

        return result;
    }
    
    /**
     * Perform the task.
     * @param time the amount of time (millisols) to perform the task
     * @return amount of time remaining after performing the task
     */
    double performTask(double time) {
        double timeLeft = super.performTask(time);
        if (subTask != null) return timeLeft;

        while ((timeLeft > 0D) && !done) {
            if (phase.equals(EXIT_AIRLOCK)) timeLeft = exitEVA(timeLeft);
            else if (phase.equals(REPAIR_MALFUNCTION)) timeLeft = repairMalfunction(timeLeft);
            else if (phase.equals(ENTER_AIRLOCK)) timeLeft = enterEVA(timeLeft);
	}					            
	
        // Add experience to "EVA Operations" skill.
        // (1 base experience point per 20 millisols of time spent)
        // Experience points adjusted by person's "Experience Aptitude" attribute.
	double experience = timeLeft / 50D;
        NaturalAttributeManager nManager = person.getNaturalAttributeManager();
        experience += experience * (((double) nManager.getAttribute("Experience Aptitude") - 50D) / 100D);
        person.getSkillManager().addExperience("EVA Operations", experience);

        return timeLeft;
    }

    /**
     * Perform the exit airlock phase of the task.
     * @param time the time to perform this phase (in millisols)
     * @return the time remaining after performing this phase (in millisols)
     */
    private double exitEVA(double time) {
        time = exitAirlock(time, airlocker);
        if (exitedAirlock) phase = REPAIR_MALFUNCTION;
        return time;
    }

    /**
     * Perform the repair malfunction phase of the task.
     * @param time the time to perform this phase (in millisols)
     * @return the time remaining after performing this phase (in millisols)
     */
    private double repairMalfunction(double time) {

        if (!hasEVAMalfunction(airlocker2) || shouldEndEVAOperation()) {
	    phase = ENTER_AIRLOCK;
	    return time;
	}
	    
        // Determine effective work time based on "Mechanic" skill.
	double workTime = time;
        int mechanicSkill = person.getSkillManager().getEffectiveSkillLevel("Mechanic");
        if (mechanicSkill == 0) workTime /= 2;
        if (mechanicSkill > 1) workTime += workTime * (.2D * mechanicSkill);

	// Get a local malfunction.
	Malfunction malfunction = null;
        Iterator i = MalfunctionFactory.getMalfunctionables(airlocker2).iterator();
	while (i.hasNext()) {
	    Malfunctionable e = (Malfunctionable) i.next();
	    MalfunctionManager manager = e.getMalfunctionManager();
	    if (manager.hasEVAMalfunction()) {
                malfunction = manager.getMostSeriousEVAMalfunction();
		description = "Repairing " + malfunction.getName() + " on " + e.getName();
		entity = e;
	    }
	}
	
	// Add EVA work to malfunction.
        double workTimeLeft = malfunction.addEVAWorkTime(workTime);

        // Add experience to "Mechanic" skill.
        // (1 base experience point per 20 millisols of time spent)
        // Experience points adjusted by person's "Experience Aptitude" attribute.
	double experience = time / 50D;
        NaturalAttributeManager nManager = person.getNaturalAttributeManager();
        experience += experience * (((double) nManager.getAttribute("Experience Aptitude") - 50D) / 100D);
        person.getSkillManager().addExperience("Mechanic", experience);
	
	// Check if there are no more malfunctions. 
        if (!hasEVAMalfunction(airlocker2)) phase = ENTER_AIRLOCK;

        // Keep track of the duration of the task.
        timeCompleted += time;
        if (timeCompleted >= duration) phase = ENTER_AIRLOCK;
	
        // Check if an accident happens during maintenance.
        checkForAccident(time);

	return (workTimeLeft / workTime) * time;
    }

    /**
     * Perform the enter airlock phase of the task.
     * @param time amount of time to perform the phase
     * @return time remaining after performing the phase
     */
    private double enterEVA(double time) {
        time = enterAirlock(time, airlocker);
        if (enteredAirlock) done = true;
	return time;
    }	

    /**
     * Gets the malfunctionable entity the person is currently repairing.
     * @returns null if none.
     * @return entity
     */
    public Malfunctionable getEntity() {
        return entity;
    }
}
