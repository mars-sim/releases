/**
 * Mars Simulation Project
 * MetaMission.java
 * @version 3.07 2014-08-12
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.mission.meta;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.mission.Mission;

/**
 * Interface for a meta mission, responsible for determining mission probability 
 * and constructing mission instances.
 */
public interface MetaMission {

    /**
     * Gets the associated mission name.
     * @return mission name string.
     */
    public String getName();
    
    /**
     * Constructs an instance of the associated mission.
     * @param person the person to perform the mission.
     * @return mission instance.
     */
    public Mission constructInstance(Person person);
    
    /**
     * Gets the weighted probability value that the person might perform this mission.
     * A probability weight of zero means that the mission has no chance of being performed by the person.
     * @param person the person to perform the mission.
     * @return weighted probability value (0 -> positive value).
     */
    public double getProbability(Person person);
}