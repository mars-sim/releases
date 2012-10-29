/**
 * Mars Simulation Project
 * Farming.java
 * @version 3.03 2012-07-19
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.person.ai.task.TendGreenhouse;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingConfig;
import org.mars_sim.msp.core.time.MarsClock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
 
/**
 * The Farming class is a building function for greenhouse farming.
 */
public class Farming extends Function implements Serializable {
        
	// Unit update events
	public static final String CROP_EVENT = "crop event";
	
    public static final String NAME = "Farming";
    public static final double HARVEST_MULTIPLIER = 10D;
    
    private int cropNum;
    private double powerGrowingCrop;
    private double powerSustainingCrop;
    private double growingArea;
    private double maxHarvest;
    private List<Crop> crops;
    
    /**
     * Constructor
     * @param building the building the function is for.
     * @throws BuildingException if error in constructing function.
     */
    public Farming(Building building) {
    	// Use Function constructor.
    	super(NAME, building);
    	
    	BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();

    	cropNum = config.getCropNum(building.getName());
    	powerGrowingCrop = config.getPowerForGrowingCrop(building.getName());
    	powerSustainingCrop = config.getPowerForSustainingCrop(building.getName());
    	growingArea = config.getCropGrowingArea(building.getName());

    	// Determine maximum harvest.
    	maxHarvest = growingArea * HARVEST_MULTIPLIER;

    	// Create initial crops.
    	crops = new ArrayList<Crop>();
    	Settlement settlement = building.getBuildingManager().getSettlement();
    	for (int x=0; x < cropNum; x++) {
    		Crop crop = new Crop(Crop.getRandomCropType(), (maxHarvest / (double) cropNum), 
    				this, settlement, false);
    		crops.add(crop);
    		building.getBuildingManager().getSettlement().fireUnitUpdate(CROP_EVENT, crop);
    	}
    }
    
    /**
     * Gets the value of the function for a named building.
     * @param buildingName the building name.
     * @param newBuilding true if adding a new building.
     * @param settlement the settlement.
     * @return value (VP) of building function.
     * @throws Exception if error getting function value.
     */
    public static double getFunctionValue(String buildingName, boolean newBuilding,
            Settlement settlement) {
        
        // Demand is value of estimated food needed by population per orbit.
        double foodPerSol = SimulationConfig.instance().getPersonConfiguration().getFoodConsumptionRate();
        int solsInOrbit = MarsClock.SOLS_IN_ORBIT_NON_LEAPYEAR;
        double foodPerOrbit = foodPerSol * solsInOrbit;
        double demand = foodPerOrbit * settlement.getAllAssociatedPeople().size();
        
        // Supply is total estimate harvest per orbit.
        double supply = 0D;
        boolean removedBuilding = false;
        Iterator<Building> i = settlement.getBuildingManager().getBuildings(NAME).iterator();
        while (i.hasNext()) {
            Building building = i.next();
            if (!newBuilding && building.getName().equalsIgnoreCase(buildingName) && !removedBuilding) {
                removedBuilding = true;
            }
            else {
                Farming farmingFunction = (Farming) building.getFunction(NAME);
                double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
                supply += farmingFunction.getEstimatedHarvestPerOrbit() * wearModifier;
            }
        }
        
        // Add food in settlement inventory to supply.
        AmountResource food = AmountResource.findAmountResource("food");
        supply += settlement.getInventory().getAmountResourceStored(food, false);
        
        double growingAreaValue = demand / (supply + 1D);
        
        BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();
        double growingArea = config.getCropGrowingArea(buildingName);
        
        return growingArea * growingAreaValue;
    }
    
    /**
     * Gets the farm's current crops.
     * @return collection of crops
     */
    public List<Crop> getCrops() {
    	return crops;
    }
    
    /**
     * Checks if farm currently requires work.
     * @return true if farm requires work
     */
    public boolean requiresWork() {
		boolean result = false;
		Iterator<Crop> i = crops.iterator();
		while (i.hasNext()) {
			if (i.next().requiresWork()) result = true;
		}
		return result;
    }
    
    /**
     * Adds work time to the crops current phase.
     * @param workTime - Work time to be added (millisols)
     * @return workTime remaining after working on crop (millisols)
     * @throws Exception if error adding work.
     */
    public double addWork(double workTime) {
		double workTimeRemaining = workTime;
		int needyCrops = 0;
		// Scott - I used the comparison criteria 00001D rather than 0D
		// because sometimes math anomalies result in workTimeRemaining
		// becoming very small double values and an endless loop occurs.
		while (((needyCrops = getNeedyCrops()) > 0) && (workTimeRemaining > 00001D)) {
			double maxCropTime = workTimeRemaining / (double) needyCrops;
			Iterator<Crop> i = crops.iterator();
			while (i.hasNext()) workTimeRemaining -= (maxCropTime - i.next().addWork(maxCropTime));
		}
 
		return workTimeRemaining;
    }
    
	/**
	 * Gets the number of crops that currently need work.
	 * @return number of crops requiring work
	 */
	private int getNeedyCrops() {
		int result = 0;
		Iterator<Crop> i = crops.iterator();
		while (i.hasNext()) {
			if (i.next().requiresWork()) result++;
		}
		return result;
	}
    
    /**
     * Adds harvested food to the farm.
     * @param harvest harvested food to add (kg.)
     */
    public void addHarvest(double harvest) {
    	try {
    		Inventory inv = getBuilding().getInventory();
    		AmountResource food = AmountResource.findAmountResource("food");
    		double remainingCapacity = inv.getAmountResourceRemainingCapacity(food, false, false);
    		if (remainingCapacity < harvest) harvest = remainingCapacity;
    		inv.storeAmountResource(food, harvest, false);
    	}
    	catch (Exception e) {}
    }
    
    /**
     * Gets the number of farmers currently working at the farm.
     * @return number of farmers
     */
    public int getFarmerNum() {
		int result = 0;
        
		if (getBuilding().hasFunction(LifeSupport.NAME)) {
			try {
				LifeSupport lifeSupport = (LifeSupport) getBuilding().getFunction(LifeSupport.NAME);
				Iterator<Person> i = lifeSupport.getOccupants().iterator();
				while (i.hasNext()) {
					Task task = i.next().getMind().getTaskManager().getTask();
					if (task instanceof TendGreenhouse) result++;
				}
			}
			catch (Exception e) {}
		}
        
		return result;
    }
    
	/**
	 * Time passing for the building.
	 * @param time amount of time passing (in millisols)
	 * @throws BuildingException if error occurs.
	 */
	public void timePassing(double time) {
        
		// Determine resource processing production level.
		double productionLevel = 0D;
		if (getBuilding().getPowerMode().equals(Building.FULL_POWER)) productionLevel = 1D;
		else if (getBuilding().getPowerMode().equals(Building.POWER_DOWN)) productionLevel = .5D;
        
		// Add time to each crop.
		Iterator<Crop> i = crops.iterator();
		int newCrops = 0;
		while (i.hasNext()) {
			Crop crop = i.next();
			crop.timePassing(time * productionLevel);

			// Remove old crops.
			if (crop.getPhase().equals(Crop.FINISHED)) {
				i.remove();
				newCrops++;
			}
		}

		// Add any new crops.
		Settlement settlement = getBuilding().getBuildingManager().getSettlement();
		for (int x=0; x < newCrops; x++) {
			Crop crop = new Crop(Crop.getRandomCropType(), (maxHarvest / (double) cropNum), 
					this, settlement, true);
			crops.add(crop);
			getBuilding().getBuildingManager().getSettlement().fireUnitUpdate(CROP_EVENT, crop);
		}
	}
	
	/**
	 * Gets the amount of power required when function is at full power.
	 * @return power (kW)
	 */
	public double getFullPowerRequired() {

		// Power (kW) required for normal operations.
		double powerRequired = 0D;
        
		Iterator<Crop> i = crops.iterator();
		while (i.hasNext()) {
			Crop crop = i.next();
			if (crop.getPhase().equals(Crop.GROWING))
				powerRequired += (crop.getMaxHarvest() * powerGrowingCrop);
		}
        
		return powerRequired;
	}
	
	/**
	 * Gets the amount of power required when function is at power down level.
	 * @return power (kW)
	 */
	public double getPowerDownPowerRequired() {
        
		// Get power required for occupant life support.
		double powerRequired = 0D;
        
		// Add power required to sustain growing or harvest-ready crops.
		Iterator<Crop> i = crops.iterator();
		while (i.hasNext()) {
			Crop crop = i.next();
			if (crop.getPhase().equals(Crop.GROWING) || crop.getPhase().equals(Crop.HARVESTING))
				powerRequired += (crop.getMaxHarvest() * powerSustainingCrop);
		}
        
		return powerRequired;
	}
	
	/**
	 * Gets the total growing area for all crops.
	 * @return growing area in square meters
	 */
	public double getGrowingArea() {
		return growingArea;
	}
	
	/**
	 * Gets the estimated maximum harvest for one orbit.
	 * @return max harvest (kg)
	 * @throws Exception if error determining harvest.
	 */
	public double getEstimatedHarvestPerOrbit() {
		double aveGrowingTime = Crop.getAverageCropGrowingTime();
		int solsInOrbit = MarsClock.SOLS_IN_ORBIT_NON_LEAPYEAR;
		double aveGrowingCyclesPerOrbit = solsInOrbit * 1000D / aveGrowingTime;
		return maxHarvest * aveGrowingCyclesPerOrbit;
	}
	
	@Override
	public void destroy() {
	    super.destroy();
	    
	    Iterator<Crop> i = crops.iterator();
	    while (i.hasNext()) {
	        i.next().destroy();
	    }
	}
}