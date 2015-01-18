/**
 * Mars Simulation Project
 * Person.java
 * @version 3.07 2015-01-04
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.mars_sim.msp.core.LifeSupport;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.person.ai.Mind;
import org.mars_sim.msp.core.person.medical.MedicalAid;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.time.EarthClock;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.Medical;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.core.vehicle.VehicleOperator;

/**
 * The Person class represents a person on Mars. It keeps track of everything
 * related to that person and provides information about him/her.
 */
public class Person
extends Unit
implements VehicleOperator, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /* default logger.
	private static transient Logger logger = Logger.getLogger(Person.class.getName());
     */

    /** The base carrying capacity (kg) of a person. */
    private final static double BASE_CAPACITY = 60D;

    // Data members
    /** Manager for Person's natural attributes. */
    private NaturalAttributeManager attributes;
    /** Person's mind. */
    private Mind mind;
    /** Person's physical condition. */
    private PhysicalCondition health;
    /** True if person is dead and buried. */
    private boolean isBuried;
    /** The gender of the person (male or female). */
    private PersonGender gender;
    /** The height of the person (in cm). */
    private int height;
    /** The birthplace of the person. */
    private String birthplace;
    /** The birth time of the person. */
    private EarthClock birthTimeStamp;
    /** The settlement the person is currently associated with. */
    private Settlement associatedSettlement;
    /** The person's achievement in scientific fields. */
    private Map<ScienceType, Double> scientificAchievement;
    /** Settlement X location (meters) from settlement center. */
    private double xLoc;
    /** Settlement Y location (meters) from settlement center. */
    private double yLoc;

    /**
     * Constructs a Person object at a given settlement.
     * @param name the person's name
     * @param gender {@link PersonGender} the person's gender
     * @param birthplace the location of the person's birth
     * @param settlement {@link Settlement} the settlement the person is at
     * @throws Exception if no inhabitable building available at settlement.
     */
    public Person(String name, PersonGender gender, String birthplace, Settlement settlement) {
        // Use Unit constructor
        super(name, settlement.getCoordinates());

        // Initialize data members
        xLoc = 0D;
        yLoc = 0D;
        this.gender = gender;
        this.birthplace = birthplace;
        String timeString = createTimeString();
        birthTimeStamp = new EarthClock(timeString);
        attributes = new NaturalAttributeManager(this);
        mind = new Mind(this);
        isBuried = false;
        health = new PhysicalCondition(this);
        scientificAchievement = new HashMap<ScienceType, Double>(0);

        // Set base mass of person from 58 to 76, peaking at 67.
        setBaseMass(56D + (RandomUtil.getRandomInt(100) + RandomUtil.getRandomInt(100))/10D);

        // Set height of person as gender-correlated curve.
        height = (this.gender == PersonGender.MALE ?
                156 + (RandomUtil.getRandomInt(22) + RandomUtil.getRandomInt(22)) :
                    146 + (RandomUtil.getRandomInt(15) + RandomUtil.getRandomInt(15))
                );

        // Set inventory total mass capacity based on the person's strength.
        int strength = attributes.getAttribute(NaturalAttribute.STRENGTH);
        getInventory().addGeneralCapacity(BASE_CAPACITY + strength);

        // Put person in proper building.
        settlement.getInventory().storeUnit(this);
        BuildingManager.addToRandomBuilding(this, settlement);
        associatedSettlement = settlement;
    }

    /**
     * Create a string representing the birth time of the person.
     * @return birth time string.
     */
    private String createTimeString() {
        // Set a birth time for the person
        int year = 2003 + RandomUtil.getRandomInt(10)
                + RandomUtil.getRandomInt(10);
        int month = RandomUtil.getRandomInt(11) + 1;
        int day;
        if (month == 2) {
            if (((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0)) {
                day = RandomUtil.getRandomInt(28) + 1;
            } else {
                day = RandomUtil.getRandomInt(27) + 1;
            }
        } else {
            if (month % 2 == 1) {
                day = RandomUtil.getRandomInt(30) + 1;
            } else {
                day = RandomUtil.getRandomInt(29) + 1;
            }
        }

        int hour = RandomUtil.getRandomInt(23);
        int minute = RandomUtil.getRandomInt(59);
        int second = RandomUtil.getRandomInt(59);

        return month + "/" + day + "/" + year + " " + hour + ":"
        + minute + ":" + second;
    }

    /**
     * @return {@link LocationSituation} the person's location
     */
    public LocationSituation getLocationSituation() {
        if (isBuried)
            return LocationSituation.BURIED;
        else {
            Unit container = getContainerUnit();
            if (container == null)
                return LocationSituation.OUTSIDE;
            else if (container instanceof Settlement)
                return LocationSituation.IN_SETTLEMENT;
            else if (container instanceof Vehicle)
                return LocationSituation.IN_VEHICLE;
        }
        return null;
    }

    /**
     * Gets the person's X location at a settlement.
     * @return X distance (meters) from the settlement's center.
     */
    public double getXLocation() {
        return xLoc;
    }

    /**
     * Sets the person's X location at a settlement.
     * @param xLocation the X distance (meters) from the settlement's center.
     */
    public void setXLocation(double xLocation) {
        this.xLoc = xLocation;
    }

    /**
     * Gets the person's Y location at a settlement.
     * @return Y distance (meters) from the settlement's center.
     */
    public double getYLocation() {
        return yLoc;
    }

    /**
     * Sets the person's Y location at a settlement.
     * @param yLocation
     */
    public void setYLocation(double yLocation) {
        this.yLoc = yLocation;
    }

    /**
     * Get settlement person is at, null if person is not at a settlement
     * @return the person's settlement
     */
    public Settlement getSettlement() {
        if (LocationSituation.IN_SETTLEMENT == getLocationSituation())
            return (Settlement) getContainerUnit();
        else
            return null;
    }

    /**
     * Get vehicle person is in, null if person is not in vehicle
     * 
     * @return the person's vehicle
     */
    public Vehicle getVehicle() {
        if (LocationSituation.IN_VEHICLE == getLocationSituation())
            return (Vehicle) getContainerUnit();
        else
            return null;
    }

    /**
     * Sets the unit's container unit. Overridden from Unit class.
     * @param containerUnit
     *            the unit to contain this unit.
     */
    public void setContainerUnit(Unit containerUnit) {
        super.setContainerUnit(containerUnit);
    }

    /**
     * Bury the Person at the current location. The person is removed from any
     * containing Settlements or Vehicles. The body is fixed at the last
     * location of the containing unit.
     */
    public void buryBody() {
        Unit containerUnit = getContainerUnit();
        if (containerUnit != null) {
            containerUnit.getInventory().retrieveUnit(this);
        }
        isBuried = true;
        setAssociatedSettlement(null);
    }

    /**
     * Person has died. Update the status to reflect the change and remove this
     * Person from any Task and remove the associated Mind.
     */
    void setDead() {
        mind.setInactive();
        buryBody();
    }

    /**
     * Person can take action with time passing
     * @param time amount of time passing (in millisols).
     */
    public void timePassing(double time) {

        // If Person is dead, then skip
        if (health.getDeathDetails() == null) {

            PersonConfig config = SimulationConfig.instance()
                    .getPersonConfiguration();
            LifeSupport support = getLifeSupport();

            // Pass the time in the physical condition first as this may
            // result in death.
            if (health.timePassing(time, support, config)) {

                // Mental changes with time passing.
                mind.timePassing(time);
            } 
            else {
                // Person has died as a result of physical condition
                setDead();
            }
        }

    }

    /**
     * Returns a reference to the Person's natural attribute manager
     * @return the person's natural attribute manager
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

    /**
     * Returns a reference to the Person's physical condition
     * @return the person's physical condition
     */
    public PhysicalCondition getPhysicalCondition() {
        return health;
    }

    /**
     * Find a medical aid according to the current location.
     * @return Accessible aid.
     */
    MedicalAid getAccessibleAid() {
        MedicalAid found = null;

        LocationSituation location = getLocationSituation();
        if (location == LocationSituation.IN_SETTLEMENT) {
            Settlement settlement = getSettlement();
            List<Building> infirmaries = settlement.getBuildingManager().getBuildings(BuildingFunction.MEDICAL_CARE);
            if (infirmaries.size() > 0) {
                int rand = RandomUtil.getRandomInt(infirmaries.size() - 1);
                Building foundBuilding = infirmaries.get(rand);
                found = (MedicalAid) foundBuilding.getFunction(BuildingFunction.MEDICAL_CARE);
            }
        }
        if (location  == LocationSituation.IN_VEHICLE) {
            Vehicle vehicle = getVehicle();
            if (vehicle instanceof Medical) {
                found = ((Medical) vehicle).getSickBay();
            }
        }

        return found;
    }

    /**
     * Returns the person's mind
     * @return the person's mind
     */
    public Mind getMind() {
        return mind;
    }

    /**
     * Returns the person's age
     * @return the person's age
     */
    public int getAge() {
        EarthClock simClock = Simulation.instance().getMasterClock().getEarthClock();
        int age = simClock.getYear() - birthTimeStamp.getYear() - 1;
        if (simClock.getMonth() >= birthTimeStamp.getMonth()
                && simClock.getMonth() >= birthTimeStamp.getMonth()) {
            age++;
        }

        return age;
    }

    /**
     * Returns the person's height in cm
     * @return the person's height
     */
    public int getHeight() {
        return height;
    }


    /**
     * Returns the person's birth date
     * @return the person's birth date
     */
    public String getBirthDate() {
        return birthTimeStamp.getDateString();
    }

    /**
     * Get the LifeSupport system supporting this Person. This may be from the
     * Settlement, Vehicle or Equipment.
     * @return Life support system.
     */
    private LifeSupport getLifeSupport() {

        LifeSupport result = null;
        List<LifeSupport> lifeSupportUnits = new ArrayList<LifeSupport>();

        Settlement settlement = getSettlement();
        if (settlement != null) {
            lifeSupportUnits.add(settlement);
        }
        else {
            Vehicle vehicle = getVehicle();
            if ((vehicle != null) && (vehicle instanceof LifeSupport)) {
                
                if (BuildingManager.getBuilding(vehicle) != null) {
                    lifeSupportUnits.add(vehicle.getSettlement());
                }
                else {
                    lifeSupportUnits.add((LifeSupport) vehicle);
                }
            }
        }

        // Get all contained units.
        Iterator<Unit> i = getInventory().getContainedUnits().iterator();
        while (i.hasNext()) {
            Unit contained = i.next();
            if (contained instanceof LifeSupport) {
                lifeSupportUnits.add((LifeSupport) contained);
            }
        }

        // Get first life support unit that checks out.
        Iterator<LifeSupport> j = lifeSupportUnits.iterator();
        while (j.hasNext() && (result == null)) {
            LifeSupport goodUnit = j.next();
            if (goodUnit.lifeSupportCheck()) {
                result = goodUnit;
            }
        }

        // If no good units, just get first life support unit.
        if ((result == null) && (lifeSupportUnits.size() > 0)) {
            result = lifeSupportUnits.get(0);
        }

        return result;
    }

    /**
     * Person consumes given amount of food.
     * @param amount the amount of food to consume (in kg)
     * @param takeFromInv is food taken from local inventory?
     */
    public void consumeFood(double amount, boolean takeFromInv) {
        if (takeFromInv) {
        	// takeFrom Inv is true if meal == null, meaning that the person is on an excursion
            //System.out.println(this.getName() + " is calling consumeFood() in Person.java");
        	health.consumeFood(amount, getContainerUnit());
        }
        else { 	// The person is in a settlement, a cookedMeal has been eaten 
        		// no need to call health.consumeFood()   
            //health.consumeFood(amount);
        }
    }

    /**
     * Person consumes given amount of food.
     * @param amount the amount of food to consume (in kg)
     * @param takeFromInv 
      */
    // 2014-11-28 Added consumeDessert()
    public void consumeDessert(double amount, boolean takeFromInv) {
        if (takeFromInv) {
        	// takeFrom Inv is true if meal == null, meaning that the person is on an excursion
            //System.out.println(this.getName() + " is is calling consumeDessert() in Person.java");
            health.consumeDessert(amount, getContainerUnit());
        }
    }
   
    /**
     * Person consumes given amount of water.
     * @param amount the amount of water to consume (in kg)
     * @param takeFromInv is water taken from local inventory?
     
    //2014-11-06 ****NOT USED **** Added consumeWater()
    public void consumeLiquid(double amount, boolean takeFromInv) {
        if (takeFromInv) {
            health.consumeLiquid(amount, getContainerUnit());
        }
        else {
            health.consumeLiquid(amount);
        }
    }
    */
    /**
     * Gets the gender of the person.
     * @return the gender
     */
    public PersonGender getGender() {
        return gender;
    }

    /**
     * Gets the birthplace of the person
     * @return the birthplace
     * @deprecated
     * TODO internationalize the place of birth for display in user interface.
     */
    public String getBirthplace() {
        return birthplace;
    }

    /**
     * Gets the person's local group of people (in building or rover)
     * @return collection of people in person's location.
     */
    public Collection<Person> getLocalGroup() {
        Collection<Person> localGroup = new ConcurrentLinkedQueue<Person>();

        if (getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            Building building = BuildingManager.getBuilding(this);
            if (building != null) {
                if (building.hasFunction(BuildingFunction.LIFE_SUPPORT)) {
                    org.mars_sim.msp.core.structure.building.function.LifeSupport lifeSupport = 
                            (org.mars_sim.msp.core.structure.building.function.LifeSupport) 
                            building.getFunction(BuildingFunction.LIFE_SUPPORT);
                    localGroup = new ConcurrentLinkedQueue<Person>(lifeSupport.getOccupants());
                }
            }
        } else if (getLocationSituation() == LocationSituation.IN_VEHICLE) {
            Crewable crewableVehicle = (Crewable) getVehicle();
            localGroup = new ConcurrentLinkedQueue<Person>(crewableVehicle.getCrew());
        }

        if (localGroup.contains(this)) {
            localGroup.remove(this);
        }
        return localGroup;
    }

    /**
     * Checks if the vehicle operator is fit for operating the vehicle.
     * @return true if vehicle operator is fit.
     */
    public boolean isFitForOperatingVehicle() {
        return !health.hasSeriousMedicalProblems();
    }

    /**
     * Gets the name of the vehicle operator
     * @return vehicle operator name.
     */
    public String getOperatorName() {
        return getName();
    }

    /**
     * Gets the settlement the person is currently associated with.
     * @return associated settlement or null if none.
     */
    public Settlement getAssociatedSettlement() {
        return associatedSettlement;
    }

    /**
     * Sets the associated settlement for a person.
     * @param newSettlement the new associated settlement or null if none.
     */
    public void setAssociatedSettlement(Settlement newSettlement) {
        if (associatedSettlement != newSettlement) {
            Settlement oldSettlement = associatedSettlement;
            associatedSettlement = newSettlement;
            fireUnitUpdate(UnitEventType.ASSOCIATED_SETTLEMENT_EVENT, associatedSettlement);
            if (oldSettlement != null) {
                oldSettlement.fireUnitUpdate(UnitEventType.REMOVE_ASSOCIATED_PERSON_EVENT, this);
            }
            if (newSettlement != null) {
                newSettlement.fireUnitUpdate(UnitEventType.ADD_ASSOCIATED_PERSON_EVENT, this);
            }
        }
    }

    /**
     * Gets the person's achievement credit for a given scientific field.
     * @param science the scientific field.
     * @return achievement credit.
     */
    public double getScientificAchievement(ScienceType science) {
        double result = 0D;
        if (scientificAchievement.containsKey(science)) {
            result = scientificAchievement.get(science);
        }
        return result;
    }

    /**
     * Gets the person's total scientific achievement credit.
     * @return achievement credit.
     */
    public double getTotalScientificAchievement() {
        double result = 0d;
        for (double value : scientificAchievement.values()) {
            result += value;
        }
        return result;
    }

    /**
     * Add achievement credit to the person in a scientific field.
     * @param achievementCredit the achievement credit.
     * @param science the scientific field.
     */
    public void addScientificAchievement(double achievementCredit, ScienceType science) {
        if (scientificAchievement.containsKey(science)) {
            achievementCredit += scientificAchievement.get(science);
        }
        scientificAchievement.put(science, achievementCredit);
    }

    @Override
    public void destroy() {
        super.destroy();
        attributes.destroy();
        attributes = null;
        mind.destroy();
        mind = null;
        health.destroy();
        health = null;
        gender = null;
        birthTimeStamp = null;
        associatedSettlement = null;
        scientificAchievement.clear();
        scientificAchievement = null;
    }
}