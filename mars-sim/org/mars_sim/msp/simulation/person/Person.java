/**
 * Mars Simulation Project
 * Person.java
 * @version 2.74 2002-03-13
 * @author Scott Davis
 */

package org.mars_sim.msp.simulation.person;

import java.util.*;
import java.io.Serializable;

import org.mars_sim.msp.simulation.*;
import org.mars_sim.msp.simulation.structure.*;
import org.mars_sim.msp.simulation.vehicle.Vehicle;
import org.mars_sim.msp.simulation.person.ai.*;
import org.mars_sim.msp.simulation.person.medical.*;

/** The Person class represents a person on the virtual Mars. It keeps
 *  track of everything related to that person and provides
 *  information about him/her.
 */
public class Person extends Unit implements Serializable {

    /**
     * Status string used when Person resides in settlement
     */
    public final static String INSETTLEMENT = "In Settlement";

    /**
     * Status string used when Person resides in a vehicle
     */
    public final static String INVEHICLE = "In Vehicle";

    /**
     * Status string used when Person is outside
     */
    public final static String OUTSIDE = "Outside";

    /**
     * Status string used when Person has been buried
     */
    public final static String BURIED = "Buried";


    // Data members
    private NaturalAttributeManager attributes; // Manager for Person's natural attributes
    private SkillManager skills; // Manager for Person's skills
    private Mind mind; // Person's mind
    private PhysicalCondition health; // Person's physical
    private boolean isBuried; // True if person is dead and buried.

    /** Constructs a Person object at a given settlement
     *  @param name the person's name
     *  @param settlement the settlement the person is at
     *  @param mars the virtual Mars
     */
    Person(String name, Settlement settlement, Mars mars) {
        // Use Unit constructor
        super(name, settlement.getCoordinates(), mars);

        initPersonData(mars);
	settlement.getInventory().addUnit(this);
    }

    /** Constructs a Person object
     *  @param name the person's name
     *  @param mars the virtual Mars
     *  @param manager the unit manager
     *  @throws Exception if no suitable settlement is found
     */
    Person(String name, Mars mars, UnitManager manager) throws Exception {
        // Use Unit constructor
        super(name, new Coordinates(0D, 0D), mars);

        initPersonData(mars);

        Settlement leastPeople = null;
        int least = Integer.MAX_VALUE;
        SettlementIterator i = manager.getSettlements().iterator();
        while (i.hasNext()) {
            Settlement settlement = i.next();
            if (settlement.getAvailablePopulationCapacity() > 0) {
                if (settlement.getCurrentPopulationNum() < least) {
                    least = settlement.getCurrentPopulationNum();
                    leastPeople = settlement;
                }
            }
        }

        if (leastPeople != null) leastPeople.getInventory().addUnit(this);
        else throw new Exception("No suitable settlements available");

    }

    /** Initialize person data */
    private void initPersonData(Mars mars) {
        // Initialize data members
        attributes = new NaturalAttributeManager();
        skills = new SkillManager(this);
        mind = new Mind(this, mars);
	isBuried = false;
        health = new PhysicalCondition(this, mars);

	// Set base mass of person.
        baseMass = 70D;

	// Set inventory total mass capacity.
	inventory.setTotalCapacity(100D);
    }

    /** Returns a string for the person's relative location "In
     *  Settlement", "In Vehicle", "Outside" or "Buried"
     *  @return the person's location
     */
    public String getLocationSituation() {
        String location = null;

	if (isBuried) location = BURIED;
	else {
	    Unit container = getContainerUnit();
	    if (container == null) location = OUTSIDE;
	    else if (container instanceof Settlement) location = INSETTLEMENT;
	    else if (container instanceof Vehicle) location = INVEHICLE;
	}

        return location;
    }

    /** Get settlement person is at, null if person is not at
     *  a settlement
     *  @return the person's settlement
     */
    public Settlement getSettlement() {

        Unit topUnit = getTopContainerUnit();
        if ((topUnit != null) && (topUnit instanceof Settlement)) {
	        return (Settlement) topUnit;
        }
        else return null;
    }

    /** Get vehicle person is in, null if person is not in vehicle
     *  @return the person's vehicle
     */
    public Vehicle getVehicle() {

        if ((containerUnit != null) && (containerUnit instanceof Vehicle)) {
	    return (Vehicle) containerUnit;
	}
	else return null;
    }

    /** Sets the unit's container unit.
     *  Overridden from Unit class.
     *  @param containerUnit the unit to contain this unit.
     */
    public void setContainerUnit(Unit containerUnit) {
        super.setContainerUnit(containerUnit);

	    if (containerUnit instanceof Settlement) {
	        health.canStartRecovery(getAccessibleAid());
	    }
    }

    /** Sets the person's fatigue level
     *  @param fatigue new fatigue level
     */
    public void setFatigue(double fatigue) {
        health.setFatigue(fatigue);
    }

    /** Sets the person's hunger level
     *  @param hunger new hunger level
     */
    public void setHunger(double hunger) {
        health.setHunger(hunger);
    }

    /**
     * Bury the Person at the current location. The person is removed from
     * any containing Settlements or Vehicles. The body is fixed at the last
     * of the containing unit.
     */
    public void buryBody() {

        containerUnit.getInventory().dropUnitOutside(this);
	isBuried = true;
    }

    /**
     * Person has died. Update the status to reflect the change and remove
     * this Person from any Task and remove the associated Mind.
     */
    void setDead() {
	System.out.println("");
	System.out.println(name + " is " + health.getHealthSituation());
        System.out.println(name + " mission: " + mind.getMission().getDescription() + " - " + mind.getMission().getPhase());
	System.out.println(name + " task: " + mind.getTaskManager().getTaskDescription() + " - " + mind.getTaskManager().getPhase());
	System.out.println("");
        mind.setInactive();
    }

    /** Person can take action with time passing
     *  @param time amount of time passing (in millisols)
     */
    public void timePassing(double time) {

        // If Person is dead, then skip
        if (health.getAlive()) {
            SimulationProperties props = mars.getSimulationProperties();
            LifeSupport support = getLifeSupport();

            // Pass the time in the physical condition first as this may kill
            // Person
            if (health.timePassing(time, support, props)) {
                // Mins action is descreased according to any illness
                mind.takeAction(time);
            }
            else {
                // Person has died as a result of physical condition
                setDead();
            }
        }
    }

    /** Returns a reference to the Person's natural attribute manager
     *  @return the person's natural attribute manager
     */
    public NaturalAttributeManager getNaturalAttributeManager() {
        return attributes;
    }

    /**
     * Get the performance factor that effect Person with the complaint.
     * @return The value is between 0 -> 1.
     */
    public double getPerformanceRating() {
        return health.getPerformanceFactor();
    }

    /** Returns a reference to the Person's physical condition
     *  @return the person's physical condition
     */
    public PhysicalCondition getPhysicalCondition() {
        return health;
    }

    /** Returns a reference to the Person's skill manager
     *  @return the person's skill manager
     */
    public SkillManager getSkillManager() {
        return skills;
    }

    /**
     * Find a medical aid according to the current location.
     * @return Accessible aid.
     */
    MedicalAid getAccessibleAid() {
        MedicalAid found = null;
        Unit topUnit = getTopContainerUnit();
        if ((topUnit != null) && (topUnit instanceof Settlement)) {
            Settlement settlement = (Settlement)topUnit;
	        found =
                (Infirmary)settlement.getFacilityManager().getFacility(Infirmary.NAME);
        }

        return found;
    }

    /** Returns the person's mind
     *  @return the person's mind
     */
    public Mind getMind() {
        return mind;
    }

    /**
     * Get the LifeSupport system supporting this Person. This may be from
     * the Settlement, Vehicle or Equipment.
     *
     * @return Life support system.
     */
    private LifeSupport getLifeSupport() {

        UnitCollection lifeSupportUnits = new UnitCollection();

	// Get all container units.
	Unit container = getContainerUnit();
	while (container != null) {
            if (container instanceof LifeSupport) lifeSupportUnits.add(container);
	    container = container.getContainerUnit();
	}

	// Get all contained units.
        UnitIterator i = inventory.getContainedUnits().iterator();
	while (i.hasNext()) {
	    Unit contained = i.next();
	    if (contained instanceof LifeSupport) lifeSupportUnits.add(contained);
	}

	// Get first life support unit that checks out.
	i = lifeSupportUnits.iterator();
	while (i.hasNext()) {
	    LifeSupport goodUnit = (LifeSupport) i.next();
	    if (goodUnit.lifeSupportCheck()) return goodUnit;
	}

	// If no good units, just get first life support unit.
	i = lifeSupportUnits.iterator();
	if (i.hasNext()) return (LifeSupport) i.next();

	// If no life support units at all, return null.
	return null;
    }

    /** Person consumes given amount of food
     *  @param amount amount of food to consume (in kg)
     */
    public void consumeFood(double amount) {
        health.consumeFood(amount, getContainerUnit(),
                           mars.getSimulationProperties());
    }
}
