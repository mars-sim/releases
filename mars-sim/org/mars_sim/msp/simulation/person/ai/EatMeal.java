/**
 * Mars Simulation Project
 * EatMeal.java
 * @version 2.75 2003-04-15
 * @author Scott Davis
 */

package org.mars_sim.msp.simulation.person.ai;

import org.mars_sim.msp.simulation.*;
import org.mars_sim.msp.simulation.person.*;
import org.mars_sim.msp.simulation.structure.Settlement;
import org.mars_sim.msp.simulation.structure.building.*;
import org.mars_sim.msp.simulation.structure.building.function.*;
import java.util.*;
import java.io.Serializable;

/** The EatMeal class is a task for eating a meal.
 *  The duration of the task is 20 millisols.
 *
 *  Note: Eating a meal reduces hunger to 0.
 */
class EatMeal extends Task implements Serializable {

    // Data members
    private double duration = 20D; // The predetermined duration of task in millisols

    /** Constructs a EatMeal object
     *  @param person the person to perform the task
     *  @param mars the virtual Mars
     */
    public EatMeal(Person person, Mars mars) {
        super("Eating a meal", person, false, mars);
        
        String location = person.getLocationSituation();
        
        // If person is in a settlement, try to find a dining area.
        if (location.equals(Person.INSETTLEMENT)) {
            Dining diningroom = getAvailableDiningBuilding(person);
            InhabitableBuilding building = (InhabitableBuilding) diningroom;
            
            if (diningroom != null) {
                try {
                    if (!building.containsPerson(person)) building.addPerson(person);
                }
                catch (BuildingException e) {
                    System.out.println("EatMeal: " + e.getMessage());
                }
            }
            else {
                // Add stress increase later.
            }
        }
        else if (location.equals(Person.OUTSIDE)) done = true;
    }

    /** Returns the weighted probability that a person might perform this task.
     *
     *  @param person the person to perform the task
     *  @param mars the virtual Mars
     *  @return the weighted probability that a person might perform this task
     */
    public static double getProbability(Person person, Mars mars) {

        double result = person.getPhysicalCondition().getHunger() - 250D;
        if (result < 0D) result = 0D;
        
        if (person.getLocationSituation().equals(Person.OUTSIDE)) result = 0D;
	
        return result;
    }

    /** This task allows the person to eat for the duration.
     *  @param time the amount of time to perform this task (in millisols)
     *  @return amount of time remaining after finishing with task (in millisols)
     */
    double performTask(double time) {
        double timeLeft = super.performTask(time);
        if (subTask != null) return timeLeft;

        person.setHunger(0D);
        timeCompleted += time;
        if (timeCompleted > duration) {
            SimulationProperties properties = mars.getSimulationProperties();
            person.consumeFood(properties.getPersonFoodConsumption() * (1D / 3D));
            done = true;
            return timeCompleted - duration;
        }
        else return 0D;
    }
    
    /**
     * Gets an available dining building that the person can use.
     * Returns null if no dining building is currently available.
     *
     * @param person the person
     * @return available dining building
     */
    private static Dining getAvailableDiningBuilding(Person person) {
     
        Dining result = null;
     
        String location = person.getLocationSituation();
        if (location.equals(Person.INSETTLEMENT)) {
            Settlement settlement = person.getSettlement();
            List dininglist = new ArrayList();
            Iterator i = settlement.getBuildingManager().getBuildings(Dining.class).iterator();
            while (i.hasNext()) {
                Dining dining = (Dining) i.next();
                boolean malfunction = ((Building) dining).getMalfunctionManager().hasMalfunction();
                if (!malfunction) dininglist.add(dining);
            }
            
            if (dininglist.size() > 0) {
                // Pick random dining building from list.
                int rand = RandomUtil.getRandomInt(dininglist.size() - 1);
                result = (Dining) dininglist.get(rand);
            }
        }
        
        return result;
    }
}
