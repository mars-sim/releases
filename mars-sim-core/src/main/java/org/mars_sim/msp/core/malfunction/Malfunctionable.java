/**
 * Mars Simulation Project
 * Malfunctionable.java
 * @version 3.07 2014-12-06
 * @author Scott Davis
 */

package org.mars_sim.msp.core.malfunction;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.person.Person;

import java.util.Collection;

/**
 * The Malfunctionable interface represents a Unit that can have malfunctions.
 */
public interface Malfunctionable {

    /**
     * Gets the entity's malfunction manager.
     * @return malfunction manager
     */
    public MalfunctionManager getMalfunctionManager();

    /**
     * Gets the name of the malfunctionable entity.
     * @return name the entity name
     */
    public String getName();

    /**
     * Gets a collection of people affected by this entity.
     * @return person collection
     */
    public Collection<Person> getAffectedPeople();

    /**
     * Gets the inventory associated with this entity.
     * @return inventory
     */
    public Inventory getInventory();
}