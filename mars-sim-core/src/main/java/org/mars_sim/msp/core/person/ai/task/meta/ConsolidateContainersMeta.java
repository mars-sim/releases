/**
 * Mars Simulation Project
 * ConsolidateContainersMeta.java
 * @version 3.07 2014-09-18
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.ConsolidateContainers;
import org.mars_sim.msp.core.person.ai.task.Task;

/**
 * Meta task for the ConsolidateContainers task.
 */
public class ConsolidateContainersMeta implements MetaTask {
    
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.consolidateContainers"); //$NON-NLS-1$
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new ConsolidateContainers(person);
    }

    @Override
    public double getProbability(Person person) {
        
        double result = 0D;
        
        if (LocationSituation.IN_SETTLEMENT == person.getLocationSituation() || 
                LocationSituation.IN_VEHICLE == person.getLocationSituation()) {
        
            // Check if there are local containers that need resource consolidation.
            if (ConsolidateContainers.needResourceConsolidation(person)) {
                result = 10D;
            }
        
            // Effort-driven task modifier.
            result *= person.getPerformanceRating();
        }

        return result;
    }
}