/**
 * Mars Simulation Project
 * InhabitableBuilding.java
 * @version 2.75 2003-01-05
 * @author Scott Davis
 */
 
package org.mars_sim.msp.simulation.structure.building;

import java.util.*;
import org.mars_sim.msp.simulation.person.Person;

/**
 * The InhabitableBuilding class is an abstract class representing a 
 * building capable of inhabitation.
 */
public abstract class InhabitableBuilding extends Building {
   
    protected int occupantCapacity;
    protected Collection occupants;
   
    /**
     * Constructor
     *
     * @param name the building's name.
     * @param manager the building's building manager.
     * @param populationCapacity the number of people that can occupy 
     *        the building at one time.
     */
    public InhabitableBuilding(String name, BuildingManager manager, int occupantCapacity) {
        // Use Building constructor.
        super(name, manager);
        
        this.occupantCapacity = occupantCapacity;
        occupants = new ArrayList();
    }
    
    /**
     * Gets the occupant capacity of the building.
     *
     * @return occupant capacity
     */
    public int getOccupantCapacity() {
        return occupantCapacity;
    }
    
    /**
     * Gets the current occupant number of the building.
     *
     * @return occupant number
     */
    public int getCurrentOccupantNumber() {
        return occupants.size();
    }
    
    /**
     * Adds a person to the building.
     *
     * @param person new person to add to building.
     */
    public void addPerson(Person person) {
        if (!occupants.contains(person) && (occupants.size() < occupantCapacity)) 
            occupants.add(person);
    }
    
    /**
     * Removes a person from the building.
     *
     * @param person occupant to remove from building.
     */
    public void removePerson(Person person) {
        if (occupants.contains(person))
            occupants.remove(person);
    }
}
