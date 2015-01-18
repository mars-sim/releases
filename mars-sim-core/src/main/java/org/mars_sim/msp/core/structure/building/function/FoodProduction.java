/**
 * Mars Simulation Project
 * FoodProduction.java
 * @version 3.07 2015-01-09
 * @author Manny Kung
 */
package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.UnitType;
import org.mars_sim.msp.core.equipment.Equipment;
import org.mars_sim.msp.core.equipment.EquipmentFactory;
import org.mars_sim.msp.core.foodProduction.FoodProductionProcess;
import org.mars_sim.msp.core.foodProduction.FoodProductionProcessInfo;
import org.mars_sim.msp.core.foodProduction.FoodProductionProcessItem;
import org.mars_sim.msp.core.foodProduction.FoodProductionUtil;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ItemResource;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.resource.Type;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingConfig;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.goods.GoodsManager;

/**
 * A building function for foodProduction.
 */
public class FoodProduction
extends Function
implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default logger. */
    private static Logger logger = Logger.getLogger(FoodProduction.class.getName());

    private static final BuildingFunction FUNCTION = BuildingFunction.FOOD_PRODUCTION;

    private static final double PROCESS_MAX_VALUE = 100D;

    // Data members.
    private int techLevel;
    private int concurrentProcesses;
    private List<FoodProductionProcess> processes;

    /**
     * Constructor.
     * @param building the building the function is for.
     * @throws BuildingException if error constructing function.
     */
    public FoodProduction(Building building) {
        // Use Function constructor.
        super(FUNCTION, building);

        BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();

        techLevel = config.getFoodProductionTechLevel(building.getName());
        concurrentProcesses = config.getFoodProductionConcurrentProcesses(building.getName());

        // Load activity spots
        loadActivitySpots(config.getCookingActivitySpots(building.getName()));

        processes = new ArrayList<FoodProductionProcess>();
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

        double result = 0D;

        BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();
        int buildingTech = config.getFoodProductionTechLevel(buildingName);

        double demand = 0D;
        Iterator<Person> i = settlement.getAllAssociatedPeople().iterator();
        while (i.hasNext()) {
            demand += i.next().getMind().getSkillManager().getSkillLevel(SkillType.COOKING);
        }

        double supply = 0D;
        int highestExistingTechLevel = 0;
        boolean removedBuilding = false;
        Iterator<Building> j = settlement.getBuildingManager().getBuildings(FUNCTION).iterator();
        while (j.hasNext()) {
            Building building = j.next();
            if (!newBuilding && building.getName().equalsIgnoreCase(buildingName) && !removedBuilding) {
                removedBuilding = true;
            }
            else {
                FoodProduction manFunction = (FoodProduction) building.getFunction(FUNCTION);
                int tech = manFunction.techLevel;
                double processes = manFunction.concurrentProcesses;
                double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
                supply += (tech * tech) * processes * wearModifier;

                if (tech > highestExistingTechLevel) {
                    highestExistingTechLevel = tech;
                }
            }
        }

        double baseFoodProductionValue = demand / (supply + 1D);

        double processes = config.getFoodProductionConcurrentProcesses(buildingName);
        double foodProductionValue = (buildingTech * buildingTech) * processes;

        result = foodProductionValue * baseFoodProductionValue;

        // If building has higher tech level than other buildings at settlement, 
        // add difference between best foodProduction processes.
        if (buildingTech > highestExistingTechLevel) {
            double bestExistingProcessValue = 0D;
            if (highestExistingTechLevel > 0D) {
                bestExistingProcessValue = getBestFoodProductionProcessValue(highestExistingTechLevel, settlement);
            }
            double bestBuildingProcessValue = getBestFoodProductionProcessValue(buildingTech, settlement);
            double processValueDiff = bestBuildingProcessValue - bestExistingProcessValue;

            if (processValueDiff < 0D) {
                processValueDiff = 0D;
            }

            if (processValueDiff > PROCESS_MAX_VALUE) {
                processValueDiff = PROCESS_MAX_VALUE;
            }

            result += processValueDiff;
        }

        return result;
    }

    /**
     * Gets the best foodProduction process value for a given foodProduction tech level at a settlement.
     * @param techLevel the foodProduction tech level.
     * @param settlement the settlement
     * @return best foodProduction process value.
     */
    private static double getBestFoodProductionProcessValue(int techLevel, Settlement settlement) {

        double result = 0D;

        Iterator<FoodProductionProcessInfo> i = FoodProductionUtil.getAllFoodProductionProcesses().iterator();
        while (i.hasNext()) {
            FoodProductionProcessInfo process = i.next();
            if (process.getTechLevelRequired() <= techLevel) {
                double value = FoodProductionUtil.getFoodProductionProcessValue(process, settlement);
                if (value > result) {
                    result = value;
                }
            }
        }

        return result;
    }

    /**
     * Gets the foodProduction tech level of the building.
     * @return tech level.
     */
    public int getTechLevel() {
        return techLevel;
    }

    /**
     * Gets the maximum concurrent foodProduction processes supported by the building.
     * @return maximum concurrent processes.
     */
    public int getConcurrentProcesses() {
        return concurrentProcesses;
    }

    /**
     * Gets the total food production processes currently in this building.
     * @return total process number.
     */
    public int getTotalProcessNumber() {
        //return processes.size() + salvages.size();
        return processes.size();       
    }

    /**
     * Gets a list of the current foodProduction processes.
     * @return unmodifiable list of processes.
     */
    public List<FoodProductionProcess> getProcesses() {
        return Collections.unmodifiableList(processes);
    }

    /**
     * Adds a new foodProduction process to the building.
     * @param process the new foodProduction process.
     * @throws BuildingException if error adding process.
     */
    public void addProcess(FoodProductionProcess process) {
        if (process == null) {
            throw new IllegalArgumentException("process is null");
        }
        if (getTotalProcessNumber() >= concurrentProcesses) {
            throw new IllegalStateException("No space to add new foodProduction process.");
        }
        processes.add(process);

        // Consume inputs.
        Inventory inv = getBuilding().getInventory();
        for (FoodProductionProcessItem item : process.getInfo().getInputList()) {
            if (Type.AMOUNT_RESOURCE.equals(item.getType())) {
                AmountResource resource = AmountResource.findAmountResource(item.getName());               
                inv.retrieveAmountResource(resource, item.getAmount());
                
            	// 2015-01-09 Added addDemandTotalRequest()
                inv.addDemandTotalRequest(resource);
            	// 2015-01-09 addDemandUsage()
               	inv.addDemandAmount(resource,item.getAmount());     
            }
            else if (Type.PART.equals(item.getType())) {
                Part part = (Part) ItemResource.findItemResource(item.getName());
                inv.retrieveItemResources(part, (int) item.getAmount());
            }
            else throw new IllegalStateException(
                    "FoodProduction process input: " +
                            item.getType() +
                            " not a valid type."
                    );

            // Recalculate settlement good value for input item.
            GoodsManager goodsManager = getBuilding().getBuildingManager().getSettlement().getGoodsManager();
            goodsManager.updateGoodValue(FoodProductionUtil.getGood(item), false);
        }


        // Log foodProduction process starting.
        if (logger.isLoggable(Level.FINEST)) {
            Settlement settlement = getBuilding().getBuildingManager().getSettlement();
            logger.finest(
                    getBuilding() + " at " 
                            + settlement
                            + " starting food production process: " 
                            + process.getInfo().getName()
                    );
        }
    }

    @Override
    public double getFullPowerRequired() {
        double result = 0D;
        Iterator<FoodProductionProcess> i = processes.iterator();
        while (i.hasNext()) {
            FoodProductionProcess process = i.next();
            if (process.getProcessTimeRemaining() > 0D)
                result += process.getInfo().getPowerRequired();
        }
        return result;
    }

    @Override
    public double getPoweredDownPowerRequired() {
        double result = 0D;
        Iterator<FoodProductionProcess> i = processes.iterator();
        while (i.hasNext()) {
            FoodProductionProcess process = i.next();
            if (process.getProcessTimeRemaining() > 0D)
                result += process.getInfo().getPowerRequired();
        }
        return result;
    }

    @Override
    public void timePassing(double time) {

        List<FoodProductionProcess> finishedProcesses = new ArrayList<FoodProductionProcess>();

        Iterator<FoodProductionProcess> i = processes.iterator();
        while (i.hasNext()) {
            FoodProductionProcess process = i.next();
            process.addProcessTime(time);

            if ((process.getProcessTimeRemaining() == 0D) && 
                    (process.getWorkTimeRemaining() == 0D)) {
                finishedProcesses.add(process);
            }
        }

        // End all processes that are done.
        Iterator<FoodProductionProcess> j = finishedProcesses.iterator();
        while (j.hasNext()) {
            endFoodProductionProcess(j.next(), false);
        }
    }

    /**
     * Checks if foodProduction function currently requires foodProduction work.
     * @param skill the person's materials science skill level.
     * @return true if foodProduction work.
     */
    public boolean requiresFoodProductionWork(int skill) {
        boolean result = false;

        if (concurrentProcesses > getTotalProcessNumber()) result = true;
        else {
            Iterator<FoodProductionProcess> i = processes.iterator();
            while (i.hasNext()) {
                FoodProductionProcess process = i.next();
                boolean workRequired = (process.getWorkTimeRemaining() > 0D);
                boolean skillRequired = (process.getInfo().getSkillLevelRequired() <= skill);
                if (workRequired && skillRequired) result = true;
            }
        }

        return result;
    }

    /**
     * Ends a foodProduction process.
     * @param process the process to end.
     * @param premature true if the process has ended prematurely.
     * @throws BuildingException if error ending process.
     */
    public void endFoodProductionProcess(FoodProductionProcess process, boolean premature) {

        if (!premature) {
            // Produce outputs.
            Settlement settlement = getBuilding().getBuildingManager().getSettlement();
            UnitManager manager = Simulation.instance().getUnitManager();
            Inventory inv = getBuilding().getInventory();

            Iterator<FoodProductionProcessItem> j = process.getInfo().getOutputList().iterator();
            while (j.hasNext()) {
                FoodProductionProcessItem item = j.next();
                if (FoodProductionUtil.getFoodProductionProcessItemValue(item, settlement, true) > 0D) {
                    if (Type.AMOUNT_RESOURCE.equals(item.getType())) {
                        // Produce amount resources.
                        AmountResource resource = AmountResource.findAmountResource(item.getName());
                        double amount = item.getAmount();
                        double capacity = inv.getAmountResourceRemainingCapacity(resource, true, false);
                        if (item.getAmount() > capacity) {
                            double overAmount = item.getAmount() - capacity;
                            logger.fine("Not enough storage capacity to store " + overAmount + " of " + 
                                    item.getName() + " from " + process.getInfo().getName() + " at " + 
                                    settlement.getName());
                            amount = capacity;
                        }
                        inv.storeAmountResource(resource, amount, true);
           			 	// 2015-01-15 Add addSupplyAmount()
                        inv.addSupplyAmount(resource, amount);
                    }
                    else if (Type.PART.equals(item.getType())) {
                        // Produce parts.
                        Part part = (Part) ItemResource.findItemResource(item.getName());
                        double mass = item.getAmount() * part.getMassPerItem();
                        double capacity = inv.getGeneralCapacity();
                        if (mass <= capacity) {
                            inv.storeItemResources(part, (int) item.getAmount());
                        }
                    }
                    else if (Type.EQUIPMENT.equals(item.getType())) {
                        // Produce equipment.
                        String equipmentType = item.getName();
                        int number = (int) item.getAmount();
                        for (int x = 0; x < number; x++) {
                            Equipment equipment = EquipmentFactory.getEquipment(equipmentType, settlement.getCoordinates(), false);
                            equipment.setName(manager.getNewName(UnitType.EQUIPMENT, equipmentType, null));
                            inv.storeUnit(equipment);
                        }
                    }
                    /*else if (Type.VEHICLE.equals(item.getType())) {
                        // Produce vehicles.
                        String vehicleType = item.getName();
                        int number = (int) item.getAmount();
                        for (int x = 0; x < number; x++) {
                            if (LightUtilityVehicle.NAME.equalsIgnoreCase(vehicleType)) {
                                String name = manager.getNewName(UnitType.VEHICLE, "LUV", null);
                                manager.addUnit(new LightUtilityVehicle(name, vehicleType, settlement));
                            }
                            else {
                                String name = manager.getNewName(UnitType.VEHICLE, null, null);
                                manager.addUnit(new Rover(name, vehicleType, settlement));
                            }
                        }
                    } */
                    else throw new IllegalStateException("FoodProduction.addProcess(): output: " +
                            item.getType() + " not a valid type.");

                    // Recalculate settlement good value for output item.
                    GoodsManager goodsManager = getBuilding().getBuildingManager().getSettlement().getGoodsManager();
                    goodsManager.updateGoodValue(FoodProductionUtil.getGood(item), false);
                }
            }
        }
        else {

            // Premature end of process.  Return all input materials.
            Settlement settlement = getBuilding().getBuildingManager().getSettlement();
            UnitManager manager = Simulation.instance().getUnitManager();
            Inventory inv = getBuilding().getInventory();

            Iterator<FoodProductionProcessItem> j = process.getInfo().getInputList().iterator();
            while (j.hasNext()) {
                FoodProductionProcessItem item = j.next();
                if (FoodProductionUtil.getFoodProductionProcessItemValue(item, settlement, false) > 0D) {
                    if (Type.AMOUNT_RESOURCE.equals(item.getType())) {
                        // Produce amount resources.
                        AmountResource resource = AmountResource.findAmountResource(item.getName());
                        double amount = item.getAmount();
                        double capacity = inv.getAmountResourceRemainingCapacity(resource, true, false);
                        if (item.getAmount() > capacity) {
                            double overAmount = item.getAmount() - capacity;
                            logger.severe("Not enough storage capacity to store " + overAmount + " of " + 
                                    item.getName() + " from " + process.getInfo().getName() + " at " + 
                                    settlement.getName());
                            amount = capacity;
                        }
                        inv.storeAmountResource(resource, amount, true);
                        // 2015-01-15 Add addSupplyAmount()
                        inv.addSupplyAmount(resource, amount);
                    }
                    else if (Type.PART.equals(item.getType())) {
                        // Produce parts.
                        Part part = (Part) ItemResource.findItemResource(item.getName());
                        double mass = item.getAmount() * part.getMassPerItem();
                        double capacity = inv.getGeneralCapacity();
                        if (mass <= capacity) {
                            inv.storeItemResources(part, (int) item.getAmount());
                        }
                    }
                    else if (Type.EQUIPMENT.equals(item.getType())) {
                        // Produce equipment.
                        String equipmentType = item.getName();
                        int number = (int) item.getAmount();
                        for (int x = 0; x < number; x++) {
                            Equipment equipment = EquipmentFactory.getEquipment(equipmentType, settlement.getCoordinates(), false);
                            equipment.setName(manager.getNewName(UnitType.EQUIPMENT, equipmentType, null));
                            inv.storeUnit(equipment);
                        }
                    }
                    /*else if (Type.VEHICLE.equals(item.getType())) {
                        // Produce vehicles.
                        String vehicleType = item.getName();
                        int number = (int) item.getAmount();
                        for (int x = 0; x < number; x++) {
                            if (LightUtilityVehicle.NAME.equalsIgnoreCase(vehicleType)) {
                                String name = manager.getNewName(UnitType.VEHICLE, "LUV", null);
                                manager.addUnit(new LightUtilityVehicle(name, vehicleType, settlement));
                            }
                            else {
                                String name = manager.getNewName(UnitType.VEHICLE, null, null);
                                manager.addUnit(new Rover(name, vehicleType, settlement));
                            }
                        }
                    }*/
                    else throw new IllegalStateException("FoodProduction.addProcess(): output: " +
                            item.getType() + " not a valid type.");

                    // Recalculate settlement good value for output item.
                    GoodsManager goodsManager = getBuilding().getBuildingManager().getSettlement().getGoodsManager();
                    goodsManager.updateGoodValue(FoodProductionUtil.getGood(item), false);
                }
            }
        }

        processes.remove(process);

        // Log process ending.
        if (logger.isLoggable(Level.FINEST)) { 
            Settlement settlement = getBuilding().getBuildingManager().getSettlement();
            logger.finest(getBuilding() + " at " + settlement + " ending foodProduction process: " + 
                    process.getInfo().getName());
        }
    }

    @Override
    public double getMaintenanceTime() {
        double result = 0D;

        // Add maintenance for tech level.
        result += techLevel * 10D;

        // Add maintenance for concurrect process capacity.
        result += concurrentProcesses * 10D;

        return result;
    }

    @Override
    public void destroy() {
        super.destroy();

        Iterator<FoodProductionProcess> i = processes.iterator();
        while (i.hasNext()) {
            i.next().destroy();
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