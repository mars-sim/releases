/**
 * Mars Simulation Project
 * MetaTask.java
 * @version 3.07 2014-08-04
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.Task;

/**
 * Interface for a meta task, responsible for determining task probability 
 * and constructing task instances.
 */
public interface MetaTask {

    /**
     * Gets the associated task name.
     * @return task name string.
     */
    public String getName();
    
    /**
     * Constructs an instance of the associated task.
     * @param person the person to perform the task.
     * @return task instance.
     */
    public Task constructInstance(Person person);
    
    /**
     * Gets the weighted probability value that the person might perform this task.
     * A probability weight of zero means that the task has no chance of being performed by the person.
     * @param person the person to perform the task.
     * @return weighted probability value (0 -> positive value).
     */
    public double getProbability(Person person);
}