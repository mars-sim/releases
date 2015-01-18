/**
 * Mars Simulation Project
 * Function.java
 * @version 3.07 2014-06-19
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;

/**
 * A settlement building function.
 */
public abstract class Function
implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    private BuildingFunction function;
    private Building building;
    private List<Point2D> activitySpots;

    /**
     * Constructor.
     * @param function the function name.
     */
    public Function(BuildingFunction function, Building building) {
        this.function = function;
        this.building = building;
    }

    /**
     * Gets the function.
     * @return {@link BuildingFunction}
     */
    public BuildingFunction getFunction() {
        return function;
    }

    /**
     * Gets the function's building.
     * @return building
     */
    public Building getBuilding() {
        return building;
    }

    /**
     * Gets the maintenance time for this building function.
     * @return maintenance work time (millisols).
     */
    public abstract double getMaintenanceTime();

    /**
     * Gets the function's malfunction scope strings.
     * @return array of scope strings.
     * @deprecated
     * TODO malfunction scope strings should be internationalized.
     */
    public String[] getMalfunctionScopeStrings() {
        String[] result = {function.getName()};
        return result;
    }

    /**
     * Time passing for the building.
     * @param time amount of time passing (in millisols)
     */
    public abstract void timePassing(double time) ;

    /*
     * Gets the amount of heat required when function is at full heat.
     * @return heat (kJ/s)
    */
    //2014-10-17 mkung: Added getFullHeatRequired()
    public abstract double getFullHeatRequired();
    /**
     * Gets the amount of heat required when function is at heat down level.
     * @return heat (kJ/s)
*/
    //2014-10-17 mkung: Added getHeatDownHeatRequired()
    public abstract double getPoweredDownHeatRequired();
     /**
     * Gets the amount of power required when function is at full power.
     * @return power (kW)
*/
    public abstract double getFullPowerRequired();
     /**  
     * Gets the amount of power required when function is at power down level.
     * @return power (kW)
  */
    public abstract double getPoweredDownPowerRequired();
   
    /**
     * Perform any actions needed when removing this building function from
     * the settlement.
     */
    public void removeFromSettlement() {
        // Override as needed.
    }

    /**
     * Loads activity spots into the building function.
     * @param newActivitySpots activity spots to add.
     */
    protected void loadActivitySpots(Collection<Point2D> newActivitySpots) {

        if (activitySpots == null) {
            activitySpots = new ArrayList<Point2D>(newActivitySpots);
        }
        else {
            activitySpots.addAll(newActivitySpots);
        }
    }

    /**
     * Gets an available activity spot for the person.
     * @param person the person looking for the activity spot.
     * @return activity spot as Point2D object or null if none found.
     */
    public Point2D getAvailableActivitySpot(Person person) {

        Point2D result = null;

        if (activitySpots != null) {

            List<Point2D> availableActivitySpots = new ArrayList<Point2D>();
            Iterator<Point2D> i = activitySpots.iterator();
            while (i.hasNext()) {
                Point2D activitySpot = i.next();
                // Convert activity spot from building local to settlement local.
                Point2D settlementActivitySpot = LocalAreaUtil.getLocalRelativeLocation(
                        activitySpot.getX(), activitySpot.getY(), getBuilding());

                // Check if spot is unoccupied.
                boolean available = true;
                Settlement settlement = getBuilding().getBuildingManager().getSettlement();
                Iterator<Person> j = settlement.getInhabitants().iterator();
                while (j.hasNext() && available) {
                    Person tempPerson = j.next();
                    if (!tempPerson.equals(person)) {
                        
                        // Check if person's location is very close to activity spot.
                        Point2D personLoc = new Point2D.Double(tempPerson.getXLocation(), tempPerson.getYLocation());
                        if (LocalAreaUtil.areLocationsClose(settlementActivitySpot, personLoc)) {
                            available = false;
                        }
                    }
                }
                
                // If available, add activity spot to available list.
                if (available) {
                    availableActivitySpots.add(settlementActivitySpot);
                }
            }
            
            if (!availableActivitySpots.isEmpty()) {
                
                // Choose a random available activity spot.
                int index = RandomUtil.getRandomInt(availableActivitySpots.size() - 1);
                result = availableActivitySpots.get(index);
            }
        }

        return result;
    }

    /**
     * Prepare object for garbage collection.
     */
    public void destroy() {
        function = null;
        building = null;
    }
}