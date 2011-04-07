/**
 * Mars Simulation Project
 * Yoga.java
 * @version 3.00 2010-08-10
 * @author Sebastien Venot
 */

package org.mars_sim.msp.core.person.ai.task;

import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Yoga extends Task implements Serializable {

    private static final long serialVersionUID = 1L;

    // Task phase
    private static final String DOING_YOGA = "Doing Yoga";

    // The stress modified per millisol.
    private static final double STRESS_MODIFIER = -.7D;

    public Yoga(Person person) {
        super(DOING_YOGA, person, false, false, STRESS_MODIFIER, true, RandomUtil.getRandomInt(100));

        // Initialize phase
        addPhase(DOING_YOGA);
        setPhase(DOING_YOGA);
    }

    public static double getProbability(Person person) {

        double result = 10D;

        // Stress modifier
        result += person.getPhysicalCondition().getStress();

        // Check if current settlement is a crowded one
        Settlement settlement = person.getSettlement();

        if (settlement != null) {
            int currentPopulation = settlement.getCurrentPopulationNum();
            int maxPopulation = settlement.getPopulationCapacity();
            if (maxPopulation <= 0) maxPopulation = 1;
            int percentange = (currentPopulation * 100) / maxPopulation;
            if (percentange > 70) {
                result = result + 10;

                if (result > 100) {
                    result = 100;
                }
            }
        }

        return result;

    }

    @Override
    protected void addExperience(double time) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<String> getAssociatedSkills() {
        List<String> results = new ArrayList<String>(0);
        return results;
    }

    @Override
    public int getEffectiveSkillLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected double performMappedPhase(double time) {
        // TODO Auto-generated method stub
        return 0;
    }

}
