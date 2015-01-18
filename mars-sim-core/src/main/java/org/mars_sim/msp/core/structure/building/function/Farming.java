/**
 * Mars Simulation Project
 * Farming.java
 * @version 3.07 2015-01-14
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.person.ai.task.TendGreenhouse;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingConfig;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.time.MarsClock;

/**
 * The Farming class is a building function for greenhouse farming.
 */
// 2014-10-15 Fixed the crash by checking if there is any food available
// 	Added new method checkAmountOfFood() for CookMeal.java
// 2014-10-14 Implemented new way of calculating amount of crop harvest in kg, 
// Crop Yield or Edible Biomass, based on NASA Advanced Life Support Baseline Values and Assumptions CR-2004-208941 
// 2014-11-06 Added if clause to account for soybean harvest
// 2014-11-29 Added harvesting crops to turn into corresponding amount resource having the same name as the crop's name
// 2014-12-09 Added crop queue
public class Farming
extends Function
implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;
    /** default logger. */
	private static Logger logger = Logger.getLogger(Farming.class.getName());

    private static final BuildingFunction FUNCTION = BuildingFunction.FARMING;

    private Inventory inv;
    private Settlement settlement;
    
    private int cropNum;
    private double powerGrowingCrop;
    private double powerSustainingCrop;
    private double growingArea;
    private double maxHarvestinKg;
    private List<Crop> crops = new ArrayList<Crop>();
    
  	// 2014-12-09 Added cropInQueue, cropListInQueue
    private String cropInQueue;
    private List<CropType> cropListInQueue = new ArrayList<CropType>();

    /**
     * Constructor.
     * @param building the building the function is for.
     * @throws BuildingException if error in constructing function.
     */
    public Farming(Building building) {
        // Use Function constructor.
        super(FUNCTION, building);

        inv = getBuilding().getInventory();
        BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();

        cropNum = config.getCropNum(building.getBuildingType());
        powerGrowingCrop = config.getPowerForGrowingCrop(building.getBuildingType());
        powerSustainingCrop = config.getPowerForSustainingCrop(building.getBuildingType());
        growingArea = config.getCropGrowingArea(building.getBuildingType());

        // Load activity spots
        loadActivitySpots(config.getFarmingActivitySpots(building.getBuildingType()));

        settlement = building.getBuildingManager().getSettlement();
        
        // 2014-12-09 Added this.crop = crop
        //cropListInQueue.add();
        
        for (int x=0; x < cropNum; x++) {
        	// 2014-10-14 Implemented new way of calculating amount of food in kg, accounting for the Edible Biomass of a crop 
           	// 2014-12-09 Added cropInQueue and changed method name to getNewCrop()
        	CropType cropType = Crop.getNewCrop(null);
        	//System.out.println(" crop name : " + cropType.getName());
        	// edibleBiomass is in  [ gram / m^2 / day ]
        	double edibleBiomass = cropType.getEdibleBiomass();
        	// growing-time is in millisol vs. growingDay is in sol
        	double growingDay = cropType.getGrowingTime() / 1000 ;
        	// maxHarvest is in kg
        	maxHarvestinKg = edibleBiomass * growingDay * (growingArea / (double) cropNum)/1000;
        	     //logger.info("constructor :  max possible Harvest is set to "+ Math.round(maxHarvestinKg) + " kg");
            Crop crop = new Crop(cropType, maxHarvestinKg, this, settlement, false);
            crops.add(crop);
            
            // Retrieves the fertilizer and add to the soil for the crop
            provideFertilizer();
            
            building.getBuildingManager().getSettlement().fireUnitUpdate(UnitEventType.CROP_EVENT, crop);       
        }  
    }
    
    /**
     * Retrieves the fertilizer and add to the soil when planting the crop
     */
    //2015-01-14 provideFertilizer()
    public void provideFertilizer() {
	    String name = "fertilizer";
		//TODO: need to move the hardcoded amount to a xml file
		double requestedAmount = Crop.FERTILIZER_NEEDED;
	    retrieveResource(name, requestedAmount);    
    }
    
    /**
     * Retrieves the resource
     * @param name
     * @parama requestedAmount
     */
    //2015-01-14 Added retrieveResource()
    public void retrieveResource(String name, double requestedAmount) {
    	try {
	    	AmountResource nameAR = AmountResource.findAmountResource(name);  	
	        double remainingCapacity = inv.getAmountResourceStored(nameAR, false);
	    	inv.addDemandTotalRequest(nameAR);  
	        if (remainingCapacity < requestedAmount) {
	     		requestedAmount = remainingCapacity;
	    		logger.warning("Just used up all fertilizer");
	        }
	    	else if (remainingCapacity == 0)
	    		logger.warning("no more fertilizer in " + settlement.getName());

	    	else {
	    		inv.retrieveAmountResource(nameAR, requestedAmount);
	    		inv.addDemandAmount(nameAR, requestedAmount);
	    	}
	    }  catch (Exception e) {}
    }
    
    //2014-12-09 Added setCropInQueue()
    public void setCropInQueue(String cropInQueue) {
    	this.cropInQueue = cropInQueue;
    }
    
    //2014-12-09 Added getCropInQueue()
    public String getCropInQueue() {
    	return cropInQueue;
    }
    
    /**
     * Gets a collection of the CropType.
     * @return Collection of CropType
     */
    //2014-12-09 Added getCropListInQueue()
    public List<CropType> getCropListInQueue() {
        return cropListInQueue;
      //CollectionUtils.getPerson(getInventory().getContainedUnits());
    }
    
    /**
     * Adds a cropType to cropListInQueue.
     * @param cropType
     */
    //2014-12-09 Added addCropListInQueue()
    public void addCropListInQueue(CropType cropType) {
    	if (cropType != null) {
    		cropListInQueue.add(cropType);
    	}
    }
    
    /**
     * Deletes a cropType to cropListInQueue.
     * @param cropType
     */
    //2014-12-09 Added deleteCropListInQueue()
    public void deleteACropFromQueue(int index, CropType cropType) {
     	//cropListInQueue.remove(index);
     	// Safer removal than cropListInQueue.remove(index)
    	int size = cropListInQueue.size();
       	if ( size > 0) {
     		Iterator<CropType> j = cropListInQueue.iterator();
     		int i = 0;
    		while (j.hasNext()) {
    			CropType c = j.next();
    			if ( i == index) {
    		    	//System.out.println("Farming.java: deleteCropListInQueue() : i is at " + i);     	  
    				String name = c.getName();
    		    	//System.out.println("Farming.java: deleteCropListInQueue() : c is " + c);     	     		        
    	 			if (cropType.getName() != name) 
    	 				logger.log(java.util.logging.Level.SEVERE, "The crop queue encounters a problem removing a crop");
    	 			else {
    	 				j.remove();
    	 		       	//System.out.println("Farming.java: deleteCropListInQueue() : queue size is now " + cropListInQueue.size()); 
        				break; // remove the first entry only
    	 			}
    			}
    			i++;
    		}
    	}
    	
    }

    /**
     * Gets the value of the function for a named building.
     * @param buildingName the building name.
     * @param newBuilding true if adding a new building.
     * @param settlement the settlement.
     * @return value (VP) of building function.
     * @throws Exception if error getting function value.
     * Called by BuildingManager.java getBUildingValue() 
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
        Iterator<Building> i = settlement.getBuildingManager().getBuildings(FUNCTION).iterator();
        while (i.hasNext()) {
            Building building = i.next();
            if (!newBuilding && building.getBuildingType().equalsIgnoreCase(buildingName) && !removedBuilding) {
                removedBuilding = true;
            }
            else {
                Farming farmingFunction = (Farming) building.getFunction(FUNCTION);
                double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
                supply += farmingFunction.getEstimatedHarvestPerOrbit() * wearModifier;
            }
        }

        // TODO: investigating if other food group besides food should be added as well
        return  addSupply(supply, demand, settlement, buildingName);        
    }

    public static double addSupply(double supply, double demand, Settlement settlement, String buildingName) {
        AmountResource food = AmountResource.findAmountResource(org.mars_sim.msp.core.LifeSupport.FOOD);
        supply += settlement.getInventory().getAmountResourceStored(food, false);

        double growingAreaValue = demand / (supply + 1D);

        BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();
        double growingArea = config.getCropGrowingArea(buildingName);       
        //logger.info("addSupply() : growingArea is " + growingArea);
        //logger.info("addSupply() : growingAreaValue is " + growingAreaValue);
        
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
        Crop needyCrop = null;
        // Scott - I used the comparison criteria 00001D rather than 0D
        // because sometimes math anomalies result in workTimeRemaining
        // becoming very small double values and an endless loop occurs.
        while (((needyCrop = getNeedyCrop()) != null) && (workTimeRemaining > 00001D)) {
            workTimeRemaining = needyCrop.addWork(workTimeRemaining);
        }

        return workTimeRemaining;
    }

    /**
     * Gets a crop that needs planting, tending, or harvesting.
     * @return crop or null if none found.
     */
    private Crop getNeedyCrop() {
        Crop result = null;

        List<Crop> needyCrops = new ArrayList<Crop>(crops.size());
        Iterator<Crop> i = crops.iterator();
        while (i.hasNext()) {
            Crop crop = i.next();
            if (crop.requiresWork()) {
                needyCrops.add(crop);
            }
        }

        if (needyCrops.size() > 0) {
            result = needyCrops.get(RandomUtil.getRandomInt(needyCrops.size() - 1));
        }

        return result;
    }

    /**
     * Adds the crop harvest to the farm.
     * @param harvest: harvested food to add (kg.)
     * @param cropCategory
     
     */
    // Note: this method was called by Crop.java's addWork()
    // 2014-10-14 Added String cropCategory to the param list,
    // 2014-11-29 Used cropName instead of cropCategory when creating amount resource
    public void addHarvest(double harvestAmount, String cropName, String cropCategory) {

    	try {
            AmountResource harvestCropAR = AmountResource.findAmountResource(cropName);      
            double remainingCapacity = inv.getAmountResourceRemainingCapacity(harvestCropAR, false, false);

            if (remainingCapacity < harvestAmount) {
                // if the remaining capacity is smaller than the harvested amount, set remaining capacity to full
            	harvestAmount = remainingCapacity;
                 	//logger.info("addHarvest() : storage is full!");
                }
            // add the harvest to the remaining capacity
            // 2014-11-06 changed the last param from false to true
            inv.storeAmountResource(harvestCropAR, harvestAmount, true);
            // 2015-01-15 Add addSupplyAmount()
            inv.addSupplyAmount(harvestCropAR, harvestAmount);
  
        }  catch (Exception e) {}
    }

    /**
     * Gets the number of farmers currently working at the farm.
     * @return number of farmers
     */
    public int getFarmerNum() {
        int result = 0;

        if (getBuilding().hasFunction(BuildingFunction.LIFE_SUPPORT)) {
            try {
                LifeSupport lifeSupport = (LifeSupport) getBuilding().getFunction(BuildingFunction.LIFE_SUPPORT);
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
        if (getBuilding().getPowerMode() == PowerMode.FULL_POWER) productionLevel = 1D;
        else if (getBuilding().getPowerMode() ==  PowerMode.POWER_DOWN) productionLevel = .5D;

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
           	// 2014-12-09 Added cropInQueue and changed method name to getNewCrop()
        	//CropType cropType = Crop.getNewCrop();
        	CropType cropType = null;
        	int size = cropListInQueue.size();
          	if ( size > 0) {
         		// Safer to remove using iterator than just cropListInQueue.remove(0);
        		Iterator<CropType> j = cropListInQueue.iterator();
        		while (j.hasNext()) {
        			cropType = j.next();
        			cropInQueue = cropType.getName();
        			j.remove();
        			break; // remove the first entry only
        		}
          	} else
        		// TODO: put in the crop with highest demand into getNewCrop()
        		cropType = Crop.getNewCrop(null); //
        	
        	double edibleBiomassPerDay = cropType.getEdibleBiomass();
        	double growingDay = cropType.getGrowingTime() / 1000 ;
        	maxHarvestinKg = edibleBiomassPerDay * growingDay * (growingArea / (double) cropNum) /1000;
        	// logger.info("timePassing : seeding a new crop with maxHarvest "+ Math.round(maxHarvestinKg) + " kg");          
        	// Note: the last param of Crop must be set to TRUE
        	Crop crop = new Crop(cropType, maxHarvestinKg, this, settlement, true);
            //Crop crop = new Crop(Crop.getRandomCropType(), (maxHarvest / (double) cropNum), this, settlement, true);
            crops.add(crop);
            
            provideFertilizer();
            
            getBuilding().getBuildingManager().getSettlement().fireUnitUpdate(UnitEventType.CROP_EVENT, crop);
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
    public double getPoweredDownPowerRequired() {

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
        return maxHarvestinKg * aveGrowingCyclesPerOrbit;
    }

    @Override
    public double getMaintenanceTime() {
        return growingArea * 5D;
    }

    @Override
    public void destroy() {
        super.destroy();

        inv = null;
        settlement = null;
        
        Iterator<CropType> i = cropListInQueue.iterator();
        while (i.hasNext()) {
            i.next().destroy();
        }
        Iterator<Crop> ii = crops.iterator();
        while (ii.hasNext()) {
            ii.next().destroy();
        }
    }

	@Override
	public double getFullHeatRequired() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getPoweredDownHeatRequired() {
		// TODO Auto-generated method stub
		return 0;
	}
}
