/**
 * Mars Simulation Project
 * TaskProbabilityUtil.java
 * @version 3.07 2014-08-04
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.util.Iterator;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.social.RelationshipManager;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.LifeSupport;

/**
 * Utility class for calculating task probabilities.
 */
public class TaskProbabilityUtil {
    
    /**
     * Private constructor for utility class.
     */
    private TaskProbabilityUtil() {};
    
    /**
     * Gets the probability modifier for a task if person needs to go to a new building.
     * @param person the person to perform the task.
     * @param newBuilding the building the person is to go to.
     * @return probability modifier
     */
    protected static double getCrowdingProbabilityModifier(Person person, Building newBuilding) {
        double modifier = 1D;

        Building currentBuilding = BuildingManager.getBuilding(person);
        if ((currentBuilding != null) && (newBuilding != null) && (currentBuilding != newBuilding)) {

            // Increase probability if current building is overcrowded.
            LifeSupport currentLS = (LifeSupport) currentBuilding.getFunction(BuildingFunction.LIFE_SUPPORT);
            int currentOverCrowding = currentLS.getOccupantNumber() - currentLS.getOccupantCapacity();
            if (currentOverCrowding > 0) {
                modifier *= ((double) currentOverCrowding + 2);
            }

            // Decrease probability if new building is overcrowded.
            LifeSupport newLS = (LifeSupport) newBuilding.getFunction(BuildingFunction.LIFE_SUPPORT);
            int newOverCrowding = newLS.getOccupantNumber() - newLS.getOccupantCapacity();
            if (newOverCrowding > 0) {
                modifier /= ((double) newOverCrowding + 2);
            }
        }

        return modifier;
    }
    
    /**
     * Gets the probability modifier for a person performing a task based on his/her 
     * relationships with the people in the room the task is to be performed in.
     * @param person the person to check for.
     * @param building the building the person will need to be in for the task.
     * @return probability modifier
     */
    protected static double getRelationshipModifier(Person person, Building building) {
        double result = 1D;

        RelationshipManager relationshipManager = Simulation.instance().getRelationshipManager();

        if ((person == null) || (building == null)) {
            throw new IllegalArgumentException("Task.getRelationshipModifier(): null parameter.");
        }
        else {
            if (building.hasFunction(BuildingFunction.LIFE_SUPPORT)) {
                LifeSupport lifeSupport = (LifeSupport) building.getFunction(BuildingFunction.LIFE_SUPPORT);
                double totalOpinion = 0D;
                Iterator<Person> i = lifeSupport.getOccupants().iterator();
                while (i.hasNext()) {
                    Person occupant = i.next();
                    if (person != occupant) {
                        totalOpinion+= ((relationshipManager.getOpinionOfPerson(person, occupant) - 50D) / 50D);
                    }
                }

                if (totalOpinion >= 0D) {
                    result*= (1D + totalOpinion);
                }
                else {
                    result/= (1D - totalOpinion); 
                }
            }
        }

        return result;
    }
}