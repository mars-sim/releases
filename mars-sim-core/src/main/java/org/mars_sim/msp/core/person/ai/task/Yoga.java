/**
 * Mars Simulation Project
 * Yoga.java
 * @version 3.06 2014-02-27
 * @author Sebastien Venot
 */

package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;

/** 
 * The Yoga class is a task for practicing yoga to reduce stress.
 */
public class Yoga
extends Task
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// TODO Task phase should be an enum.
	private static final String DOING_YOGA = "Doing Yoga";

	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = -.7D;

	/** 
	 * constructor.
	 * @param person the person to perform the task
	 */
	public Yoga(Person person) {
        super(DOING_YOGA, person, false, false, STRESS_MODIFIER, true, 
                10D + RandomUtil.getRandomDouble(30D));

        // Initialize phase
        addPhase(DOING_YOGA);
        setPhase(DOING_YOGA);
    }

    /** 
     * Returns the weighted probability that a person might perform this task.
     * @param person the person to perform the task
     * @return the weighted probability that a person might perform this task
     */
    public static double getProbability(Person person) {

        double result = 0D;

        // Stress modifier
        result += person.getPhysicalCondition().getStress() / 2D;
        
        // No yoga outside.
        if (person.getLocationSituation() == LocationSituation.OUTSIDE) {
            result = 0D;
        }

        return result;

    }

    @Override
    protected void addExperience(double time) {
        // Do nothing
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(0);
        return results;
    }

    @Override
    public int getEffectiveSkillLevel() {
        return 0;
    }

    @Override
    protected double performMappedPhase(double time) {
        return 0;
    }
}