/**
 * Mars Simulation Project
 * Rover.java
 * @version 3.07 2015-01-09
 * @author Scott Davis
 */

package org.mars_sim.msp.core.vehicle;

import org.mars_sim.msp.core.*;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/** 
 * The Rover class represents the rover type of ground vehicle.  It
 * contains information about the rover.
 */
public class Rover
extends GroundVehicle
implements Crewable, LifeSupport, Airlockable, Medical, Towing {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Static data members
	/** Normal air pressure (Pa). */
	private double NORMAL_AIR_PRESSURE = 101325D;
	/** Normal temperature (celsius). */
	private double NORMAL_TEMP = 25D;

	/** The amount of work time to perform maintenance (millisols) */
    public static final double MAINTENANCE_WORK_TIME = 500D;
	
	public static final double LIFE_SUPPORT_RANGE_ERROR_MARGIN = 3.0D;

	// Data members
	/** The rover's capacity for crew members. */
	private int crewCapacity = 0;
	/** The rover's airlock. */
	private Airlock airlock;
	/** The rover's lab. */
	private Lab lab;
	/** The rover's sick bay. */
	private SickBay sickbay;
	/** The vehicle the rover is currently towing. */
	private Vehicle towedVehicle;
	
	private List<Point2D> labActivitySpots;
	private List<Point2D> sickBayActivitySpots;

    /** 
     * Constructs a Rover object at a given settlement
     * @param name the name of the rover
     * @param description the configuration description of the vehicle.
     * @param settlement the settlement the rover is parked at
     */
    public Rover(String name, String description, Settlement settlement) {
        // Use GroundVehicle constructor
        super(name, description, settlement, MAINTENANCE_WORK_TIME);
		
		// Get vehicle configuration.
		VehicleConfig config = SimulationConfig.instance().getVehicleConfiguration();
		
		// Add scope to malfunction manager.
		malfunctionManager.addScopeString("Rover");
		malfunctionManager.addScopeString("Crewable");
		malfunctionManager.addScopeString("Life Support");
		malfunctionManager.addScopeString(description);
		if (config.hasLab(description)) malfunctionManager.addScopeString("Laboratory");
		if (config.hasSickbay(description)) malfunctionManager.addScopeString("Sickbay");
        
		// Set crew capacity
		crewCapacity = config.getCrewSize(description);

		Inventory inv = getInventory();
		
		// Set inventory total mass capacity.
		inv.addGeneralCapacity(config.getTotalCapacity(description));
	
		// Set inventory resource capacities.
		AmountResource methane = AmountResource.findAmountResource("methane");
		inv.addAmountResourceTypeCapacity(methane, config.getCargoCapacity(description, "methane"));
		AmountResource oxygen = AmountResource.findAmountResource(LifeSupport.OXYGEN);
		inv.addAmountResourceTypeCapacity(oxygen, config.getCargoCapacity(description, LifeSupport.OXYGEN));
		AmountResource water = AmountResource.findAmountResource(LifeSupport.WATER);
		inv.addAmountResourceTypeCapacity(water, config.getCargoCapacity(description, LifeSupport.WATER));
		AmountResource food = AmountResource.findAmountResource(LifeSupport.FOOD);
		inv.addAmountResourceTypeCapacity(food, config.getCargoCapacity(description, LifeSupport.FOOD));
		  // 2015-01-04 Added Soymilk
		AmountResource dessert = AmountResource.findAmountResource("Soymilk");
		inv.addAmountResourceTypeCapacity(dessert, config.getCargoCapacity(description, "Soymilk"));

		AmountResource rockSamples = AmountResource.findAmountResource("rock samples");
		inv.addAmountResourceTypeCapacity(rockSamples, config.getCargoCapacity(description, "rock samples"));
		AmountResource ice = AmountResource.findAmountResource("ice");
		inv.addAmountResourceTypeCapacity(ice, config.getCargoCapacity(description, "ice"));
		
		// Construct sick bay.
		if (config.hasSickbay(description)) {
			sickbay = new SickBay(this, config.getSickbayTechLevel(description), config.getSickbayBeds(description));
			
			// Initialize sick bay activity spots.
			sickBayActivitySpots = new ArrayList<Point2D>(config.getSickBayActivitySpots(description));
		}
			
		// Construct lab.
		if (config.hasLab(description)) {
			lab = new MobileLaboratory(1, config.getLabTechLevel(description), config.getLabTechSpecialties(description));
			
			// Initialize lab activity spots.
            labActivitySpots = new ArrayList<Point2D>(config.getLabActivitySpots(description));
		}
		// Set rover terrain modifier
		setTerrainHandlingCapability(0D);

		// Create the rover's airlock.
		double airlockXLoc = config.getAirlockXLocation(description);
		double airlockYLoc = config.getAirlockYLocation(description);
		double airlockInteriorXLoc = config.getAirlockInteriorXLocation(description);
        double airlockInteriorYLoc = config.getAirlockInteriorYLocation(description);
        double airlockExteriorXLoc = config.getAirlockExteriorXLocation(description);
        double airlockExteriorYLoc = config.getAirlockExteriorYLocation(description);
		try { airlock = new VehicleAirlock(this, 2, airlockXLoc, airlockYLoc, airlockInteriorXLoc, airlockInteriorYLoc, 
		        airlockExteriorXLoc, airlockExteriorYLoc); }
		catch (Exception e) { e.printStackTrace(System.err); }
    }
    
    /**
     * Sets the vehicle this rover is currently towing.
     * @param towedVehicle the vehicle being towed.
     */
    public void setTowedVehicle(Vehicle towedVehicle) {
    	if (this == towedVehicle) throw new IllegalArgumentException("Rover cannot tow itself.");
    	this.towedVehicle = towedVehicle;
    	updatedTowedVehicleSettlementLocation();
    }
    
    /**
     * Gets the vehicle this rover is currently towing.
     * @return towed vehicle.
     */
    public Vehicle getTowedVehicle() {
    	return towedVehicle;
    }

    /**
     * Gets the number of crewmembers the vehicle can carry.
     * @return capacity
     */
    public int getCrewCapacity() {
        return crewCapacity;
    }
    
    /**
     * Gets the current number of crewmembers.
     * @return number of crewmembers
     */
    public int getCrewNum() {
        return getCrew().size();
    }

    /**
     * Gets a collection of the crewmembers.
     * @return crewmembers as Collection
     */
    public Collection<Person> getCrew() {
        return CollectionUtils.getPerson(getInventory().getContainedUnits());
    }

    /**
     * Checks if person is a crewmember.
     * @param person the person to check
     * @return true if person is a crewmember
     */
    public boolean isCrewmember(Person person) {
        return getInventory().containsUnit(person);
    }
    
    /** Returns true if life support is working properly and is not out
     *  of oxygen or water.
     *  @return true if life support is OK
     *  @throws Exception if error checking life support.
     */
    public boolean lifeSupportCheck() {
        boolean result = true;

        AmountResource oxygen = AmountResource.findAmountResource(LifeSupport.OXYGEN);
        if (getInventory().getAmountResourceStored(oxygen, false) <= 0D) result = false;
        AmountResource water = AmountResource.findAmountResource(LifeSupport.WATER);
        if (getInventory().getAmountResourceStored(water, false) <= 0D) result = false;
        if (malfunctionManager.getOxygenFlowModifier() < 100D) result = false;
        if (malfunctionManager.getWaterFlowModifier() < 100D) result = false;
        if (getAirPressure() != NORMAL_AIR_PRESSURE) result = false;
        if (getTemperature() != NORMAL_TEMP) result = false;
	
        return result;
    }

    /** Gets the number of people the life support can provide for.
     *  @return the capacity of the life support system
     */
    public int getLifeSupportCapacity() {
        return crewCapacity;
    }

    /** Gets oxygen from system.
     *  @param amountRequested the amount of oxygen requested from system (kg)
     *  @return the amount of oxgyen actually received from system (kg)
     *  @throws Exception if error providing oxygen.
     */
    public double provideOxygen(double amountRequested) {
    	AmountResource oxygen = AmountResource.findAmountResource(LifeSupport.OXYGEN);
    	double oxygenTaken = amountRequested;
    	double oxygenLeft = getInventory().getAmountResourceStored(oxygen, false);
    	if (oxygenTaken > oxygenLeft) oxygenTaken = oxygenLeft;
    	getInventory().retrieveAmountResource(oxygen, oxygenTaken);

    	// 2015-01-09 Added addDemandTotalRequest()
    	getInventory().addDemandTotalRequest(oxygen);
    	// 2015-01-09 addDemandRealUsage()
    	getInventory().addDemandAmount(oxygen, oxygenTaken);
       	
        return oxygenTaken * (malfunctionManager.getOxygenFlowModifier() / 100D);
    }

    /** Gets water from system.
     *  @param amountRequested the amount of water requested from system (kg)
     *  @return the amount of water actually received from system (kg)
     *  @throws Exception if error providing water.
     */
    public double provideWater(double amountRequested) {
    	AmountResource water = AmountResource.findAmountResource(LifeSupport.WATER);
    	double waterTaken = amountRequested;
    	double waterLeft = getInventory().getAmountResourceStored(water, false);
    	if (waterTaken > waterLeft) waterTaken = waterLeft;
    	getInventory().retrieveAmountResource(water, waterTaken);

    	// 2015-01-09 Added addDemandTotalRequest()
    	getInventory().addDemandTotalRequest(water);
    	// 2015-01-09 addDemandRealUsage()
    	getInventory().addDemandAmount(water, waterTaken);    	
    	
        return waterTaken * (malfunctionManager.getWaterFlowModifier() / 100D);
    }

    /** Gets the air pressure of the life support system.
     *  @return air pressure (Pa)
     */
    public double getAirPressure() {
        double result = NORMAL_AIR_PRESSURE * 
	        (malfunctionManager.getAirPressureModifier() / 100D);
        double ambient = Simulation.instance().getMars().getWeather().getAirPressure(getCoordinates());
        if (result < ambient) return ambient;
        else return result;
    }

    /** Gets the temperature of the life support system.
     *  @return temperature (degrees C)
     */
    public double getTemperature() {
        double result = NORMAL_TEMP *
	        (malfunctionManager.getTemperatureModifier() / 100D);
        double ambient = Simulation.instance().getMars().getWeather().getTemperature(getCoordinates());
        if (result < ambient) return ambient;
        else return result;
    }

    /** 
     * Gets the rover's airlock.
     * @return rover's airlock
     */
    public Airlock getAirlock() {
        return airlock;
    }

    /** 
     * Perform time-related processes
     * @param time the amount of time passing (in millisols)
     * @throws exception if error during time.
     */
    public void timePassing(double time) {
        super.timePassing(time);
        
        airlock.timePassing(time);
    }

    /**
     * Gets a collection of people affected by this entity.
     * @return person collection
     */
    public Collection<Person> getAffectedPeople() {
        Collection<Person> people = super.getAffectedPeople();
        
        Collection<Person> crew = getCrew();
        Iterator<Person> i = crew.iterator();
        while (i.hasNext()) {
            Person person = i.next();
            if (!people.contains(person)) people.add(person);
        }

        return people;
    }
    
    /**
     * Checks if the rover has a laboratory.
     * @return true if lab.
     */
    public boolean hasLab() {
        return lab != null;
    }

	/**
	 * Gets the rover's laboratory
	 * @return lab
	 */
	public Lab getLab() {
		return lab;
	}
	
	/**
     * Gets a list of lab activity spots.
     * @return list of activity spots as Point2D objects.
     */
    public List<Point2D> getLabActivitySpots() {
        return labActivitySpots;
    }
	
	/**
	 * Checks if the rover has a sick bay.
	 * @return true if sick bay
	 */
	public boolean hasSickBay() {
        return sickbay != null;
	}
	
	/**
	 * Gets the rover's sick bay.
	 * @return sick bay
	 */
	public SickBay getSickBay() {
		return sickbay;
	}
	
	/**
     * Gets a list of sick bay activity spots.
     * @return list of activity spots as Point2D objects.
     */
    public List<Point2D> getSickBayActivitySpots() {
        return sickBayActivitySpots;
    }
	
    /**
     * Checks if a particular operator is appropriate for a vehicle.
     * @param operator the operator to check
     * @return true if appropriate operator for this vehicle.
     */
    public boolean isAppropriateOperator(VehicleOperator operator) {
        return (operator instanceof Person) && (getInventory().containsUnit((Unit) operator));
    }
    
    /**
     * Gets the resource type that this vehicle uses for fuel.
     * @return resource type as a string
     */
    public AmountResource getFuelType() {
    	try {
    		return AmountResource.findAmountResource("methane");
    	}
    	catch (Exception e) {
    		return null;
    	}
    }
    
    /** 
     * Sets unit's location coordinates 
     * @param newLocation the new location of the unit
     */
    public void setCoordinates(Coordinates newLocation) {
    	super.setCoordinates(newLocation);
        
    	// Set towed vehicle (if any) to new location.
        if (towedVehicle != null) towedVehicle.setCoordinates(newLocation);
    }
    
    /** 
     * Gets the range of the vehicle
     * @return the range of the vehicle (in km)
     * @throws Exception if error getting range.
     */
    public double getRange() {
    	double range = super.getRange();
    	
    	double distancePerSol = getEstimatedTravelDistancePerSol();
    	
    	PersonConfig config = SimulationConfig.instance().getPersonConfiguration();
    		
    	// Check food capacity as range limit.
    	AmountResource food = AmountResource.findAmountResource(LifeSupport.FOOD);
    	double foodConsumptionRate = config.getFoodConsumptionRate();
    	double foodCapacity = getInventory().getAmountResourceCapacity(food, false);
    	double foodSols = foodCapacity / (foodConsumptionRate * crewCapacity);
    	double foodRange = distancePerSol * foodSols / LIFE_SUPPORT_RANGE_ERROR_MARGIN;
    	if (foodRange < range) range = foodRange;
    		
	       // 2015-01-04 Added Soymilk
    	// Check dessert capacity as range limit.
    	AmountResource dessert = AmountResource.findAmountResource("Soymilk");
    	double dessertConsumptionRate = config.getFoodConsumptionRate() / 6D;
    	double dessertCapacity = getInventory().getAmountResourceCapacity(dessert, false);
    	double dessertSols = dessertCapacity / (dessertConsumptionRate * crewCapacity);
    	double dessertRange = distancePerSol * dessertSols / LIFE_SUPPORT_RANGE_ERROR_MARGIN;
    	if (dessertRange < range) range = dessertRange;
    	
    	// Check water capacity as range limit.
    	AmountResource water = AmountResource.findAmountResource(LifeSupport.WATER);
    	double waterConsumptionRate = config.getWaterConsumptionRate();
    	double waterCapacity = getInventory().getAmountResourceCapacity(water, false);
    	double waterSols = waterCapacity / (waterConsumptionRate * crewCapacity);
    	double waterRange = distancePerSol * waterSols / LIFE_SUPPORT_RANGE_ERROR_MARGIN;
    	if (waterRange < range) range = waterRange;
    		
    	// Check oxygen capacity as range limit.
    	AmountResource oxygen = AmountResource.findAmountResource(LifeSupport.OXYGEN);
    	double oxygenConsumptionRate = config.getOxygenConsumptionRate();
    	double oxygenCapacity = getInventory().getAmountResourceCapacity(oxygen, false);
    	double oxygenSols = oxygenCapacity / (oxygenConsumptionRate * crewCapacity);
    	double oxygenRange = distancePerSol * oxygenSols / LIFE_SUPPORT_RANGE_ERROR_MARGIN;
    	if (oxygenRange < range) range = oxygenRange;
    	
    	return range;
    }
    
    @Override
    public void setParkedLocation(double xLocation, double yLocation, double facing) {
        super.setParkedLocation(xLocation, yLocation, facing);
        
        // Update towed vehicle locations.
        updatedTowedVehicleSettlementLocation();
    }
    
    /**
     * Updates the settlement location of any towed vehicles.
     */
    private void updatedTowedVehicleSettlementLocation() {
        
        Vehicle towedVehicle = getTowedVehicle();
        if (towedVehicle != null) {
            if (towedVehicle instanceof Rover) {
                // Towed rovers should be located behind this rover with same facing.
                double distance = (getLength() + towedVehicle.getLength()) / 2D;
                double towedX = 0D;
                double towedY = 0D - distance;
                Point2D.Double towedLoc = LocalAreaUtil.getLocalRelativeLocation(towedX, towedY, this);
                towedVehicle.setParkedLocation(towedLoc.getX(), towedLoc.getY(), getFacing());
            }
            else if (towedVehicle instanceof LightUtilityVehicle) {
                // Towed light utility vehicles should be attached to back of the rover
                // sideways and facing to the right.
                double distance = (getLength() + towedVehicle.getWidth()) / 2D;
                double towedX = 0D;
                double towedY = 0D - distance;
                Point2D.Double towedLoc = LocalAreaUtil.getLocalRelativeLocation(towedX, towedY, this);
                towedVehicle.setParkedLocation(towedLoc.getX(), towedLoc.getY(), getFacing() + 90D);
            }
        }
    }
    
    @Override
    public void destroy() {
        super.destroy();
        
        if (airlock != null) airlock.destroy();
        airlock = null;
        if (lab != null) lab.destroy();
        lab = null;
        if (sickbay != null) sickbay.destroy();
        sickbay = null;
        towedVehicle = null;
    }
}