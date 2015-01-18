/**
 * Mars Simulation Project
 * Building.java
 * @version 3.07 2014-12-28
 * @author Scott Davis
 */

package org.mars_sim.msp.core.structure.building;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LocalBoundedObject;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.Maintenance;
import org.mars_sim.msp.core.person.ai.task.Repair;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.structure.BuildingTemplate;
import org.mars_sim.msp.core.structure.building.connection.InsidePathLocation;
import org.mars_sim.msp.core.structure.building.function.Administration;
import org.mars_sim.msp.core.structure.building.function.AstronomicalObservation;
import org.mars_sim.msp.core.structure.building.function.BuildingConnection;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.Communication;
import org.mars_sim.msp.core.structure.building.function.EVA;
import org.mars_sim.msp.core.structure.building.function.EarthReturn;
import org.mars_sim.msp.core.structure.building.function.Exercise;
import org.mars_sim.msp.core.structure.building.function.Farming;
import org.mars_sim.msp.core.structure.building.function.FoodProduction;
import org.mars_sim.msp.core.structure.building.function.Function;
import org.mars_sim.msp.core.structure.building.function.GroundVehicleMaintenance;
import org.mars_sim.msp.core.structure.building.function.ThermalGeneration;
import org.mars_sim.msp.core.structure.building.function.HeatMode;
import org.mars_sim.msp.core.structure.building.function.ThermalStorage;
import org.mars_sim.msp.core.structure.building.function.LifeSupport;
import org.mars_sim.msp.core.structure.building.function.LivingAccommodations;
import org.mars_sim.msp.core.structure.building.function.Management;
import org.mars_sim.msp.core.structure.building.function.Manufacture;
import org.mars_sim.msp.core.structure.building.function.MedicalCare;
import org.mars_sim.msp.core.structure.building.function.PowerGeneration;
import org.mars_sim.msp.core.structure.building.function.PowerMode;
import org.mars_sim.msp.core.structure.building.function.PowerStorage;
import org.mars_sim.msp.core.structure.building.function.Recreation;
import org.mars_sim.msp.core.structure.building.function.Research;
import org.mars_sim.msp.core.structure.building.function.ResourceProcessing;
import org.mars_sim.msp.core.structure.building.function.Storage;
import org.mars_sim.msp.core.structure.building.function.cooking.Cooking;
import org.mars_sim.msp.core.structure.building.function.cooking.Dining;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparingDessert;

/**
 * The Building class is a settlement's building.
 */
public class Building
extends Unit
implements Malfunctionable, Serializable, // Comparable<Building>,
LocalBoundedObject, InsidePathLocation {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// default logger.
	private static Logger logger = Logger.getLogger(Building.class.getName());
	 
	DecimalFormat fmt = new DecimalFormat("###.####"); 
	
	// TODO Maintenance info should not be hard coded but initialized from some config files
	/** 3340 Sols (5 orbits). */
	private static final double WEAR_LIFETIME = 3340000D;
	/** Base amount of maintenance time for building. */
	private static final double BASE_MAINTENANCE_TIME = 50D;

    // Data members
	protected String buildingType;
	protected String nickName;
	protected BuildingManager manager; 	
	protected double width;
	protected double length;
	protected int baseLevel;
	protected double xLoc;
	protected double yLoc;
	protected double facing;
	protected PowerMode powerMode;
	protected double basePowerRequirement;
	protected double basePowerDownPowerRequirement;
	protected MalfunctionManager malfunctionManager;
	protected List<Function> functions;
	protected int id;
	protected double baseHeatRequirement;
	protected double basePowerDownHeatRequirement;

	//private static int count;
	// 2014-10-28  changed variable's name from "name" to "buildingType"
	protected ThermalGeneration furnace;
	// Specific Heat Capacity = 4.0 for a typical house
	protected double SHC = 4.0; 
	// Building Loss Coefficient = 1.0 for a typical house
	protected double BLC = 1.0; 
	protected double floorArea;
	protected double currentTemperature;
	//protected double deltaTemperature ;
    private static final double INITIAL_TEMPERATURE = 22.5D;
	//2014-10-23  Modified thermal control parameters in the building */
	protected HeatMode heatMode;
	// 2014-11-02 Added HeatModeCache
	protected HeatMode heatModeCache;
	// 2014-11-02 Added heatGenerated
	private double heatGenerated = 0; // the initial value is zero 
	private double heatGeneratedCache = 0; // the initial value is zero 
	// 2014-11-27 Added description
	protected String description = "Stay tuned";
	
	/** Unit location coordinates. */
	private Coordinates location;// = manager.getSettlement().getCoordinates();
	
	/** Constructor 1
	 * Constructs a Building object.
	 * @param template the building template.
	 * @param manager the building's building manager.
	 * @throws BuildingException if building can not be created.
	 */
	//2014-10-27  Added getNickName() 
	public Building(BuildingTemplate template, BuildingManager manager) {
		this(template.getID(), template.getBuildingType(), template.getNickName(), template.getWidth(), 
		        template.getLength(), template.getXLoc(), template.getYLoc(),
				template.getFacing(), manager);		
		
		this.currentTemperature = INITIAL_TEMPERATURE;
		// TODO: what do we need this for?
		LifeSupport lifeSupport;
		if (hasFunction(BuildingFunction.LIFE_SUPPORT))
			lifeSupport = (LifeSupport) getFunction(BuildingFunction.LIFE_SUPPORT);	
		
		this.manager = manager;
		this.location = manager.getSettlement().getCoordinates();
		
		heatMode = HeatMode.POWER_DOWN;
		//count++;
		//Logger.info("constructor 1 : count is " + count);
	}

	/** Constructor 2
	 * Constructs a Building object.
	 * @param id the building's unique ID number.
	 * @param buildingType the building Type.
	 * @param nickName the building's nick name.	  
	 * @param width the width (meters) of the building or -1 if not set.
	 * @param length the length (meters) of the building or -1 if not set.
	 * @param xLoc the x location of the building in the settlement.
	 * @param yLoc the y location of the building in the settlement.
	 * @param facing the facing of the building (degrees clockwise from North).
	 * @param manager the building's building manager.
	 * @throws BuildingException if building can not be created.
	 */
	//2014-10-27  changed variable "name" to "buildingType"
	public Building(int id, String buildingType, String nickName, double width, double length, 
	        double xLoc, double yLoc, double facing, BuildingManager manager) {
		
		super(nickName, manager.getSettlement().getCoordinates());
		
		//logger.info("constructor2 : purple width is " + width);	
		//logger.info("constructor2 : purple length is " + length);	
		//logger.info("constructor2 : blue width is " + this.width);
		//logger.info("constructor2 : blue length is " + this.length);
		this.id = id;
		this.buildingType = buildingType;
		this.nickName = nickName;
		this.manager = manager;
		powerMode = PowerMode.FULL_POWER;
		this.xLoc = xLoc;
		this.yLoc = yLoc;
		this.facing = facing;
		//count++;
		//logger.info("constructor2 : count is " + count);
	
		this.currentTemperature = INITIAL_TEMPERATURE;
		heatMode = HeatMode.POWER_DOWN;
			
		BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();
	// Get building's dimensions.
		if (width != -1D) {
			this.width = width;
			}
		else {
			this.width = config.getWidth(buildingType);
			}
		if (this.width <= 0D) {
			throw new IllegalStateException("Invalid building width: " + this.width + " m. for new building " + buildingType);
		}

		if (length != -1D) {
			this.length = length;
		}
		else {
			this.length = config.getLength(buildingType);
		}
		if (this.length <= 0D) {
			throw new IllegalStateException("Invalid building length: " + this.length + " m. for new building " + buildingType);
		}

		baseLevel = config.getBaseLevel(buildingType);
		
		description = config.getDescription(buildingType);
		
		// Get the building's functions
		functions = determineFunctions();

		// Get base power requirements.
		basePowerRequirement = config.getBasePowerRequirement(buildingType);
		basePowerDownPowerRequirement = config.getBasePowerDownPowerRequirement(buildingType);

		// TODO: determine the benefit of adding base heat requirements.
		//baseHeatRequirement = config.getBaseHeatRequirement(buildingType);
		//baseHeatDownHeatRequirement = config.getBasePowerDownHeatRequirement(buildingType);

		// Determine total maintenance time.
		double totalMaintenanceTime = BASE_MAINTENANCE_TIME;
		Iterator<Function> j = functions.iterator();
		while (j.hasNext()) {
			Function function = j.next();
			totalMaintenanceTime += function.getMaintenanceTime();
		}

		// Set up malfunction manager.
		malfunctionManager = new MalfunctionManager(this, WEAR_LIFETIME, totalMaintenanceTime);
		malfunctionManager.addScopeString("Building");

		// Add each function to the malfunction scope.
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) {
			Function function = i.next();
			for (int x = 0; x < function.getMalfunctionScopeStrings().length; x++) {
				malfunctionManager.addScopeString(function.getMalfunctionScopeStrings()[x]);
			}
		}
		//logger.info("constructor 2 : end of constructor 2");
	}

	//Constructor 3
	/** Empty constructor. */
	protected Building(BuildingManager manager) {
		super("Mock Building", new Coordinates(0D, 0D));
	}


	/**
     * Gets the description of a building.
     * @return String description
     */
	//2014-11-27  Added getDescription()
    public String getDescription() {
            return description;
    }
	/**
	 * Sets building nickname
	 * @return none
	 */
	// 2014-10-28  Added setBuildingNickName()
    public void setBuildingNickName(String nickName) {
        this.nickName = nickName;
    }
    
	/**
     * Gets the initial temperature of a building.
     * @return temperature (deg C)
     */
	//2014-10-23  Added getInitialTemperature()
    public double getInitialTemperature() {
            return INITIAL_TEMPERATURE;
    }
	
    /**
     * Gets the temperature of a building.
     * @return temperature (deg C)
     */
	//2014-10-17  Added getTemperature()
    public double getTemperature() {
            return currentTemperature;
    }
    
    /**
     * Sets the current temperature of a building due to heat gain
     * @return temperature (deg C)
     */
    public void setTemperature(double t) {
        currentTemperature = t;
    }
    
	//2014-10-17  Added getSHC() and getBLC()
    public double getSHC() {
    	return SHC;
    }
    public double getBLC() {
    	return BLC;
    }
    
    

	/**
	 * Determines the building functions.
	 * @return list of building functions.
	 * @throws Exception if error in functions.
	 */
	private List<Function> determineFunctions() {
		List<Function> buildingFunctions = new ArrayList<Function>();

		BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();

		// Set power generation function.
		if (config.hasPowerGeneration(buildingType)) buildingFunctions.add(new PowerGeneration(this));
		//2014-10-17 mkung 
		// Added thermal generation function.
		if (config.hasThermalGeneration(buildingType)) buildingFunctions.add(new ThermalGeneration(this));

		// Set life support function.
		if (config.hasLifeSupport(buildingType)) buildingFunctions.add(new LifeSupport(this));

		// Set living accommodations function.
		if (config.hasLivingAccommodations(buildingType)) buildingFunctions.add(new LivingAccommodations(this));

		// Set research function.
		if (config.hasResearchLab(buildingType)) buildingFunctions.add(new Research(this));

		// Set communication function.
		if (config.hasCommunication(buildingType)) buildingFunctions.add(new Communication(this));

		// Set EVA function.
		if (config.hasEVA(buildingType)) buildingFunctions.add(new EVA(this));

		// Set recreation function.
		if (config.hasRecreation(buildingType)) buildingFunctions.add(new Recreation(this));

		// Set dining function.
		if (config.hasDining(buildingType)) buildingFunctions.add(new Dining(this));

		// Set resource processing function.
		if (config.hasResourceProcessing(buildingType)) buildingFunctions.add(new ResourceProcessing(this));

		// Set storage function.
		if (config.hasStorage(buildingType)) buildingFunctions.add(new Storage(this));

		// Set medical care function.
		if (config.hasMedicalCare(buildingType)) buildingFunctions.add(new MedicalCare(this));

		// Set farming function.
		if (config.hasFarming(buildingType)) buildingFunctions.add(new Farming(this));

		// Set exercise function.
		if (config.hasExercise(buildingType)) buildingFunctions.add(new Exercise(this));

		// Set ground vehicle maintenance function.
		if (config.hasGroundVehicleMaintenance(buildingType)) buildingFunctions.add(new GroundVehicleMaintenance(this));

		// Set cooking function.
		if (config.hasCooking(buildingType)) buildingFunctions.add(new Cooking(this));
		if (config.hasCooking(buildingType)) buildingFunctions.add(new PreparingDessert(this));

		// Set manufacture function.
		if (config.hasManufacture(buildingType)) buildingFunctions.add(new Manufacture(this));

		//2014-11-23 Added food production
		if (config.hasFoodProduction(buildingType)) buildingFunctions.add(new FoodProduction(this));

		// Set power storage function.
		if (config.hasPowerStorage(buildingType)) buildingFunctions.add(new PowerStorage(this));

		//2014-10-17  Added and imported ThermalStorage
		// Set thermal storage function.
		if (config.hasThermalStorage(buildingType)) buildingFunctions.add(new ThermalStorage(this));
		
		// Set astronomical observation function
		if (config.hasAstronomicalObservation(buildingType)) buildingFunctions.add(new AstronomicalObservation(this));

		// Set management function.
		if (config.hasManagement(buildingType)) buildingFunctions.add(new Management(this));

		// Set Earth return function.
		if (config.hasEarthReturn(buildingType)) buildingFunctions.add(new EarthReturn(this));

		// Set building connection function.
		if (config.hasBuildingConnection(buildingType)) buildingFunctions.add(new BuildingConnection(this));

		// Set administration function.
		if (config.hasAdministration(buildingType)) buildingFunctions.add(new Administration(this));
		
		return buildingFunctions;
	}

	/**
	 * Checks if a building has a particular function.
	 * @param function the name of the function.
	 * @return true if function.
	 */
	public boolean hasFunction(BuildingFunction function) {
		boolean result = false;
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) {
			if (i.next().getFunction() == function) result = true;
		}
		return result;
	}

	/**
	 * Gets a function if the building has it.
	 * @param functionType {@link BuildingFunction} the function of the building.
	 * @return function.
	 * @throws BuildingException if building doesn't have the function.
	 */
	public Function getFunction(BuildingFunction functionType) {
		Function result = null;
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) {
			Function function = i.next();
			if (function.getFunction() == functionType) result = function;
		}
		if (result != null) return result;
		else throw new IllegalStateException(buildingType + " does not have " + functionType);
	}

	/**
	 * Remove the building's functions from the settlement.
	 */
	public void removeFunctionsFromSettlement() {

		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) {
			i.next().removeFromSettlement();
		}
	}

	/**
	 * Gets the building's building manager.
	 * @return building manager
	 */
	public BuildingManager getBuildingManager() {
		return manager;
	}

	/**
	 * Gets the building's unique ID number.
	 * @return ID integer.
	 */
	public int getID() {
		return id;
	}

	/**
	 * Sets the building's nickName
	 * @return none
	 * 2014-10-27  Called by TabPanelBuilding.java for building nickname change
	 */
	public void setNickName(String nickName) {
		//System.out.println("input nickName is " + nickName);
		this.nickName = nickName;
		//System.out.println("new nickName is " + this.nickName);
	}
	/**
	 * Gets the building's nickName
	 * @return building's nickName as a String
	 * 2014-10-27  Called by TabPanelBuilding.java for building nickname change
	 */
	public String getNickName() {
		return nickName;
	}
	
	/**
	 * Gets the building type.
	 * @return building type as a String.
	 * @deprecated
	 * TODO internationalize building names for display in user interface.
	 */
	// 2014-10-28 change data field from "name" to "buildingType"  
	// TODO: change getName() to getBuildingType()
	// getName() has 120 occurrences in MSP 
	// will retain its name for the time being
	public String getName() {
		return buildingType;
	}
	//2014-11-02  Added getBuildingType()
	public String getBuildingType() {
		return buildingType;
	}
	/**
	 * Sets the building's type (formerly name)
	 * @return none
	 * "buildingType" was formerly "name"
	 * 2014-10-28  Called by TabPanelBuilding.java for generating a building list
	 */
	public void setType(String type) {
		System.out.println("input nickName is " + nickName);
		this.buildingType = type;
		System.out.println("new buildingType is " + this.buildingType);
	}
	@Override
	public double getWidth() {
		return width;
	}

	@Override
	public double getLength() {
		return length;
	}
	
	@Override
	public double getXLocation() {
		return xLoc;
	}

	@Override
	public double getYLocation() {
		return yLoc;
	}

	@Override
	public double getFacing() {
		return facing;
	}

	/**
     * Gets the base level of the building.
     * @return -1 for in-ground, 0 for above-ground.
     */
    public int getBaseLevel() {
        return baseLevel;
    }
	
	/**
	 * Gets the power this building currently requires for full-power mode.
	 * @return power in kW.
	 */
	//2014-11-02  Modified getFullPowerRequired()
	public double getFullPowerRequired()  {
		double result = basePowerRequirement;

		// Determine power required for each function.
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) result += i.next().getFullPowerRequired();

		//2014-11-02 Added getFullHeatRequired()
		result = result + getFullHeatRequired();
		
		return result;
	}

	/**
	 * Gets the power the building requires for power-down mode.
	 * @return power in kW.
	 */
	public double getPoweredDownPowerRequired() {
		double result = basePowerDownPowerRequirement;

		// Determine power required for each function.
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) result += i.next().getPoweredDownPowerRequired();

		return result;
	}

	/**
	 * Gets the building's heat mode.
	 */
	public PowerMode getPowerMode() {
		return powerMode;
	}

	/**
	 * Sets the building's heat mode.
	 */
	public void setPowerMode(PowerMode powerMode) {
		this.powerMode = powerMode;
	}

	/**
	 * Gets the heat this building currently requires for full-power mode.
	 * @return heat in kJ/s.
	 */
	//2014-11-02  Modified getFullHeatRequired()
	public double getFullHeatRequired()  {
		//double result = baseHeatRequirement;	
		if ( heatGeneratedCache != heatGenerated) {
			// if heatGeneratedCache is different from the its last value
			heatGeneratedCache = heatGenerated;
		//logger.info("getFullHeatRequired() : heatGenerated is updated to " + 
				//heatGenerated + " kW");	
		}
		// Determine heat required for each function.
		//TODO: should I add power requirement inside
		// thermal generation function instead?
		//Iterator<Function> i = functions.iterator();
		//while (i.hasNext()) result += i.next().getFullHeatRequired();
		return heatGenerated;
	}
	//2014-11-02 Added setHeatGenerated()
	public void setHeatGenerated(double heatGenerated) {
		this.heatGenerated = heatGenerated;
	}
	
	/**
	 * Gets the heat the building requires for power-down mode.
	 * @return heat in kJ/s.
	*/
	//2014-10-17  Added heat mode
	public double getPoweredDownHeatRequired() {
		double result = basePowerDownHeatRequirement;

		// Determine heat required for each function.
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) result += i.next().getPoweredDownHeatRequired();

		return result;
	}

	/**
	 * Gets the building's power mode.
	 */
	//2014-10-17  Added heat mode
	public HeatMode getHeatMode() {
		return heatMode;
	}

	/**
	 * Sets the building's heat mode.
	 */
	//2014-10-17  Added heat mode
	public void setHeatMode(HeatMode heatMode) {
		//logger.info("setHeatMode() : heatMode was " + heatMode);		
		if ( heatModeCache != heatMode) {
			// if heatModeCache is different from the its last value
			heatModeCache = heatMode;
			this.heatMode = heatMode;
			//logger.info("setHeatMode() : heatMode is now " + heatMode);
		}
	}
	
	/**
	 * Gets the entity's malfunction manager.
	 * @return malfunction manager
	 */
	public MalfunctionManager getMalfunctionManager() {
		return malfunctionManager;
	}

	/**
	 * Gets a collection of people affected by this entity.
	 * Children buildings should add additional people as necessary.
	 * @return person collection
	 */
	public Collection<Person> getAffectedPeople() {
		Collection<Person> people = new ConcurrentLinkedQueue<Person>();

		// If building has life support, add all occupants of the building.
		if (hasFunction(BuildingFunction.LIFE_SUPPORT)) {
			LifeSupport lifeSupport = (LifeSupport) getFunction(BuildingFunction.LIFE_SUPPORT);
			Iterator<Person> i = lifeSupport.getOccupants().iterator();
			while (i.hasNext()) {
				Person occupant = i.next();
				if (!people.contains(occupant)) people.add(occupant);
			}
		}

		// Check all people in settlement.
		Iterator<Person> i = manager.getSettlement().getInhabitants().iterator();
		while (i.hasNext()) {
			Person person = i.next();
			Task task = person.getMind().getTaskManager().getTask();

			// Add all people maintaining this building. 
			if (task instanceof Maintenance) {
				if (((Maintenance) task).getEntity() == this) {
					if (!people.contains(person)) people.add(person);
				}
			}

			// Add all people repairing this facility.
			if (task instanceof Repair) {
				if (((Repair) task).getEntity() == this) {
					if (!people.contains(person)) people.add(person);
				}
			}
		}

		return people;
	}

	/**
	 * Gets the inventory associated with this entity.
	 * @return inventory
	 */
	public Inventory getInventory() {
		return manager.getSettlement().getInventory();
	}

	/**
	 * String representation of this building.
	 * @return The settlement and building's nickName.
	 */
	// TODO: To prevent crash, check which classes still rely on toString() to return buildingType
	// 2014-10-29 change buildingType to nickName
	public String toString() {
//		return buildingType;
		return nickName;	
	}

	/**
	 * Compares this object with the specified object for order.
	 * @param o the Object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object is less than, 
	 * equal to, or greater than the specified object.
	 */
	// TODO: find out if we should use nickName vs. buildingType 
	public int compareTo(Building o) {
		return buildingType.compareToIgnoreCase(o.buildingType);
	}
	
	
	/**
	 * Time passing for building.
	 * @param time amount of time passing (in millisols)
	 * @throws BuildingException if error occurs.
	 */

	public void timePassing(double time) {
			// Check for valid argument.
		if (time < 0D) throw new IllegalArgumentException("Time must be > 0D");

		// Send time to each building function.
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) i.next().timePassing(time);
			

		// Update malfunction manager.
		malfunctionManager.timePassing(time);
		// If powered up, active time passing.
		if (powerMode == PowerMode.FULL_POWER) malfunctionManager.activeTimePassing(time);	
		//2014-10-17  Added HeatMode
		// If heat is on, active time passing.
		if (heatMode == HeatMode.FULL_POWER) malfunctionManager.activeTimePassing(time);

		//logger.info("timePassing() : calling determineDeltaTemperature()");

	}

	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		buildingType = null;
		manager = null;
		powerMode = null;
		heatMode = null;
		malfunctionManager.destroy();
		malfunctionManager = null;
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) {
			i.next().destroy();
		}
	}
}