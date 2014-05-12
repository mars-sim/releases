/**
 * Mars Simulation Project
 * ManufactureUtil.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */

package org.mars_sim.msp.core.manufacture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.equipment.Equipment;
import org.mars_sim.msp.core.equipment.EquipmentFactory;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ItemResource;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.resource.Type;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.Manufacture;
import org.mars_sim.msp.core.structure.goods.Good;
import org.mars_sim.msp.core.structure.goods.GoodsManager;
import org.mars_sim.msp.core.structure.goods.GoodsUtil;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.core.vehicle.VehicleConfig;

/**
 * Utility class for getting manufacturing processes.
 */
public final class ManufactureUtil {
	
	/** Private constructor. */
	private ManufactureUtil() {}
	
	/**
	 * Gets all manufacturing processes.
	 * @return list of processes.
	 * @throws Exception if error getting processes.
	 */
	public static List<ManufactureProcessInfo> getAllManufactureProcesses() {
		return SimulationConfig
		.instance()
		.getManufactureConfiguration()
		.getManufactureProcessList();
	}

	/**
	 * gives back an alphabetically ordered map of all manufacturing processes.
	 * @return {@link TreeMap}<{@link String},{@link ManufactureProcessInfo}>
	 */
	public static TreeMap<String,ManufactureProcessInfo> getAllManufactureProcessesMap() {
		TreeMap<String,ManufactureProcessInfo> map = new TreeMap<String,ManufactureProcessInfo>();
		for (ManufactureProcessInfo item : getAllManufactureProcesses()) {
			map.put(item.getName(),item);
		}
		return map;
	}

	/**
	 * Gets manufacturing processes within the capability of a tech level.
	 * @param techLevel the tech level.
	 * @return list of processes.
	 * @throws Exception if error getting processes.
	 */
	public static List<ManufactureProcessInfo> getManufactureProcessesForTechLevel(
			int techLevel) {
		List<ManufactureProcessInfo> result = new ArrayList<ManufactureProcessInfo>();
		 
		ManufactureConfig config = SimulationConfig.instance().getManufactureConfiguration();
		Iterator<ManufactureProcessInfo> i = config.getManufactureProcessList().iterator();
		while (i.hasNext()) {
			ManufactureProcessInfo process = i.next();
			if (process.getTechLevelRequired() <= techLevel) result.add(process);
		}
		
		return result;
	}

	/**
	 * gets manufacturing processes with given output.
	 * @param item {@link String} name of desired output
	 * @return {@link List}<{@link ManufactureProcessItem}> list of processes
	 */
	public static List<ManufactureProcessInfo> getManufactureProcessesWithGivenOutput(
		String name
	) {
		List<ManufactureProcessInfo> result = new ArrayList<ManufactureProcessInfo>();
		Iterator<ManufactureProcessInfo> i = SimulationConfig
		.instance().getManufactureConfiguration()
		.getManufactureProcessList().iterator();
		while (i.hasNext()) {
			ManufactureProcessInfo process = i.next();
			if (process.getOutputNames().contains(name)) result.add(process);
		}
		return result;
	}

	/**
	 * gets manufacturing processes with given input.
	 * @param item {@link String} desired input
	 * @return {@link List}<{@link ManufactureProcessItem}> list of processes
	 */
	public static List<ManufactureProcessInfo> getManufactureProcessesWithGivenInput(
		String item
	) {
		List<ManufactureProcessInfo> result = new ArrayList<ManufactureProcessInfo>();
		Iterator<ManufactureProcessInfo> i = SimulationConfig
		.instance().getManufactureConfiguration()
		.getManufactureProcessList().iterator();
		while (i.hasNext()) {
			ManufactureProcessInfo process = i.next();
			if (process.getInputNames().contains(item)) result.add(process);
		}
		return result;
	}

	/**
	 * Gets manufacturing processes within the capability of a tech level and a skill level.
	 * @param techLevel the tech level.
	 * @param skillLevel the skill level.
	 * @return list of processes.
	 * @throws Exception if error getting processes.
	 */
	public static List<ManufactureProcessInfo> getManufactureProcessesForTechSkillLevel(
			int techLevel, int skillLevel) {
		List<ManufactureProcessInfo> result = new ArrayList<ManufactureProcessInfo>();
		
		ManufactureConfig config = SimulationConfig.instance().getManufactureConfiguration();
		Iterator<ManufactureProcessInfo> i = config.getManufactureProcessList().iterator();
		while (i.hasNext()) {
			ManufactureProcessInfo process = i.next();
			if ((process.getTechLevelRequired() <= techLevel) && 
					(process.getSkillLevelRequired() <= skillLevel)) result.add(process);
		}
		
		return result;
	}
	
	/**
     * Gets salvage processes info within the capability of a tech level and a skill level.
     * @param techLevel the tech level.
     * @param skillLevel the skill level.
     * @return list of salvage processes info.
     * @throws Exception if error getting salvage processes info.
     */
	public static List<SalvageProcessInfo> getSalvageProcessesForTechSkillLevel(
	        int techLevel, int skillLevel) {
	    List<SalvageProcessInfo> result = new ArrayList<SalvageProcessInfo>();
	    
	    ManufactureConfig config = SimulationConfig.instance().getManufactureConfiguration();
        Iterator<SalvageProcessInfo> i = config.getSalvageList().iterator();
        while (i.hasNext()) {
            SalvageProcessInfo process = i.next();
            if ((process.getTechLevelRequired() <= techLevel) && 
                    (process.getSkillLevelRequired() <= skillLevel)) result.add(process);
        }
	    
	    return result;
	}
	
	/**
	 * Gets salvage processes info within the capability of a tech level.
	 * @param techLevel the tech level.
	 * @return list of salvage processes info.
	 * @throws Exception if error get salvage processes info.
	 */
	public static List<SalvageProcessInfo> getSalvageProcessesForTechLevel(int techLevel)
	        {
	    List<SalvageProcessInfo> result = new ArrayList<SalvageProcessInfo>();
        
        ManufactureConfig config = SimulationConfig.instance().getManufactureConfiguration();
        Iterator<SalvageProcessInfo> i = config.getSalvageList().iterator();
        while (i.hasNext()) {
            SalvageProcessInfo process = i.next();
            if (process.getTechLevelRequired() <= techLevel) result.add(process);
        }
        
        return result;
	}
	
	/**
	 * Gets the goods value of a manufacturing process at a settlement.
	 * @param process the manufacturing process.
	 * @param settlement the settlement.
	 * @return goods value of output goods minus input goods.
	 * @throws Exception if error determining good values.
	 */
	public static double getManufactureProcessValue(ManufactureProcessInfo process,
			Settlement settlement) {
		
		double inputsValue = 0D;
		Iterator<ManufactureProcessItem> i = process.getInputList().iterator();
		while (i.hasNext()) inputsValue += getManufactureProcessItemValue(i.next(), settlement);
		
		double outputsValue = 0D;
		Iterator<ManufactureProcessItem> j = process.getOutputList().iterator();
		while (j.hasNext()) outputsValue += getManufactureProcessItemValue(j.next(), settlement);
		
        // Subtract power value.
        double hoursInMillisol = MarsClock.convertMillisolsToSeconds(1D) / 60D / 60D;
        double powerHrsRequiredPerMillisol = process.getPowerRequired() * hoursInMillisol;
        double powerValue = powerHrsRequiredPerMillisol * settlement.getPowerGrid().getPowerValue();
        
		return outputsValue - inputsValue - powerValue;
	}
	
	/**
     * Gets the estimated goods value of a salvage process at a settlement.
     * @param process the salvage process.
     * @param settlement the settlement.
     * @return goods value of estimated salvaged parts minus salvaged unit.
     * @throws Exception if error determining good values.
     */
	public static double getSalvageProcessValue(SalvageProcessInfo process,
	        Settlement settlement, Person salvager) {
	    double result = 0D;
	    
	    Unit salvagedUnit = findUnitForSalvage(process, settlement);
	    if (salvagedUnit != null) {
	        GoodsManager goodsManager = settlement.getGoodsManager();
	        
	        double wearConditionModifier = 1D;
	        if (salvagedUnit instanceof Malfunctionable) {
	            Malfunctionable salvagedMalfunctionable = (Malfunctionable) salvagedUnit;
	            double wearCondition = salvagedMalfunctionable.getMalfunctionManager().getWearCondition();
	            wearConditionModifier = wearCondition / 100D;
	        }
	        
	        // Determine salvaged good value.
	        double salvagedGoodValue = 0D;
            Good salvagedGood = null;
            if (salvagedUnit instanceof Equipment) {
                salvagedGood = GoodsUtil.getEquipmentGood(salvagedUnit.getClass());
            }
            else if (salvagedUnit instanceof Vehicle) {
                salvagedGood = GoodsUtil.getVehicleGood(salvagedUnit.getDescription());
            }
            
            if (salvagedGood != null) salvagedGoodValue = goodsManager.getGoodValuePerItem(salvagedGood);
            else throw new IllegalStateException("Salvaged good is null");
            
            salvagedGoodValue *= (wearConditionModifier * .75D) + .25D;
	    
            // Determine total estimated parts salvaged good value.
            double totalPartsGoodValue = 0D;
            Iterator<PartSalvage> i = process.getPartSalvageList().iterator();
            while (i.hasNext()) {
                PartSalvage partSalvage = i.next();
                Part part = (Part) ItemResource.findItemResource(partSalvage.getName());
                Good partGood = GoodsUtil.getResourceGood(part);
                double partValue = goodsManager.getGoodValuePerItem(partGood) * partSalvage.getNumber();
                totalPartsGoodValue += partValue;
            }
            
            // Modify total parts good value by item wear and salvager skill.
            int skill = salvager.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.MATERIALS_SCIENCE);
            double valueModifier = .25D + (wearConditionModifier * .25D) + ((double) skill * .05D);
            totalPartsGoodValue *= valueModifier;
            
            // Determine process value.
            result = totalPartsGoodValue - salvagedGoodValue;
	    }
	    
	    return result;
	}
	
	/**
	 * Gets the good value of a manufacturing process item for a settlement.
	 * @param item the manufacturing process item.
	 * @param settlement the settlement.
	 * @return good value.
	 * @throws Exception if error getting good value.
	 */
	public static double getManufactureProcessItemValue(ManufactureProcessItem item,
			Settlement settlement) {
		double result = 0D;
		
		GoodsManager manager = settlement.getGoodsManager();
		
		if (item.getType().equals(Type.AMOUNT_RESOURCE)) {
			AmountResource resource = AmountResource.findAmountResource(item.getName());
			Good good = GoodsUtil.getResourceGood(resource);
			result = manager.getGoodValuePerItem(good) * item.getAmount();
		}
		else if (item.getType().equals(Type.PART)) {
			ItemResource resource = ItemResource.findItemResource(item.getName());
			Good good = GoodsUtil.getResourceGood(resource);
			result = manager.getGoodValuePerItem(good) * item.getAmount();
		}
		else if (item.getType().equals(Type.EQUIPMENT)) {
			Class<? extends Equipment> equipmentClass = EquipmentFactory.getEquipmentClass(item.getName());
			Good good = GoodsUtil.getEquipmentGood(equipmentClass);
			result = manager.getGoodValuePerItem(good) * item.getAmount();
		}
		else if (item.getType().equals(Type.VEHICLE)) {
			Good good = GoodsUtil.getVehicleGood(item.getName());
			result = manager.getGoodValuePerItem(good) * item.getAmount();
		}
		else throw new IllegalStateException("Item type: " + item.getType() + " not valid.");
        
		return result;
	}

	/**
	 * Checks to see if a manufacturing process can be started at a given manufacturing building.
	 * @param process the manufacturing process to start.
	 * @param workshop the manufacturing building.
	 * @return true if process can be started.
	 * @throws Exception if error determining if process can be started.
	 */
	public static boolean canProcessBeStarted(ManufactureProcessInfo process,
			Manufacture workshop) {
		boolean result = true;
		
		// Check to see if workshop is full of processes.
		if (workshop.getTotalProcessNumber() >= workshop.getConcurrentProcesses()) result = false;
		
		// Check to see if process tech level is above workshop tech level.
		if (workshop.getTechLevel() < process.getTechLevelRequired()) result = false;
		
		Inventory inv = workshop.getBuilding().getInventory();
		
		// Check to see if process input items are available at settlement.
		if (!areProcessInputsAvailable(process, inv)) result = false;
		
		// Check to see if room for process output items at settlement.
		// if (!canProcessOutputsBeStored(process, inv)) result = false;
		
		return result;
	}
	
	/**
     * Checks to see if a salvage process can be started at a given manufacturing building.
     * @param process the salvage process to start.
     * @param workshop the manufacturing building.
     * @return true if salvage process can be started.
     * @throws Exception if error determining if salvage process can be started.
     */
    public static boolean canSalvageProcessBeStarted(SalvageProcessInfo process,
            Manufacture workshop) {
        boolean result = true;
        
        // Check to see if workshop is full of processes.
        if (workshop.getTotalProcessNumber() >= workshop.getConcurrentProcesses()) result = false;
        
        // Check to see if process tech level is above workshop tech level.
        if (workshop.getTechLevel() < process.getTechLevelRequired()) result = false;
        
        // Check to see if a salvagable unit is available at the settlement.
        Settlement settlement = workshop.getBuilding().getBuildingManager().getSettlement();
        if (findUnitForSalvage(process, settlement) == null) result = false;
        
        return result;
    }
	
	/**
	 * Checks if process inputs are available in an inventory.
	 * @param process the manufacturing process.
	 * @param inv the inventory.
	 * @return true if process inputs are available.
	 * @throws Exception if error determining if process inputs are available.
	 */
	private static boolean areProcessInputsAvailable(ManufactureProcessInfo process, Inventory inv) {
		boolean result = true;
		
		Iterator<ManufactureProcessItem> i = process.getInputList().iterator();
		while (result && i.hasNext()) {
			ManufactureProcessItem item = i.next();
			if (Type.AMOUNT_RESOURCE.equals(item.getType())) {
				AmountResource resource = AmountResource.findAmountResource(item.getName());
				result = (inv.getAmountResourceStored(resource, false) >= item.getAmount());
			}
			else if (Type.PART.equals(item.getType())) {
				Part part = (Part) ItemResource.findItemResource(item.getName());
				result = (inv.getItemResourceNum(part) >= (int) item.getAmount());
			} else throw new IllegalStateException(
				"Manufacture process input: " +
				item.getType() +
				" not a valid type."
			);
		}
		
		return result;
	}
	
	/**
	 * Checks if enough storage room for process outputs in an inventory.
	 * @param process the manufacturing process.
	 * @param inv the inventory.
	 * @return true if storage room.
	 * @throws Exception if error determining storage room for outputs.
	 */
	/*
	private static final boolean canProcessOutputsBeStored(ManufactureProcessInfo process, Inventory inv)
			{
		boolean result = true;
		
		Iterator<ManufactureProcessItem> j = process.getOutputList().iterator();
		while (j.hasNext()) {
			ManufactureProcessItem item = j.next();
			if (ManufactureProcessItem.AMOUNT_RESOURCE.equalsIgnoreCase(item.getType())) {
				AmountResource resource = AmountResource.findAmountResource(item.getName());
				double capacity = inv.getAmountResourceRemainingCapacity(resource, true);
				if (item.getAmount() > capacity) result = false; 
			}
			else if (ManufactureProcessItem.PART.equalsIgnoreCase(item.getType())) {
				Part part = (Part) ItemResource.findItemResource(item.getName());
				double mass = item.getAmount() * part.getMassPerItem();
				double capacity = inv.getGeneralCapacity();
				if (mass > capacity) result = false;
			}
			else if (ManufactureProcessItem.EQUIPMENT.equalsIgnoreCase(item.getType())) {
				String equipmentType = item.getName();
				int number = (int) item.getAmount();
				Equipment equipment = EquipmentFactory.getEquipment(equipmentType, 
						new Coordinates(0D, 0D), true);
				double mass = equipment.getBaseMass() * number;
				double capacity = inv.getGeneralCapacity();
				if (mass > capacity) result = false;
			}
			else if (ManufactureProcessItem.VEHICLE.equalsIgnoreCase(item.getType())) {
				// Vehicles are stored outside a settlement.
			}
			else throw new BuildingException("Manufacture.addProcess(): output: " + 
					item.getType() + " not a valid type.");
		}
		
		return result;
	}
	*/
	
	/**
	 * Checks if settlement has buildings with manufacture function.
	 * @param settlement the settlement.
	 * @return true if buildings with manufacture function.
	 * @throws BuildingException if error checking for manufacturing buildings.
	 */
	public static boolean doesSettlementHaveManufacturing(Settlement settlement)
			{
		BuildingManager manager = settlement.getBuildingManager();
        return (manager.getBuildings(BuildingFunction.MANUFACTURE).size() > 0);
	}
	
	/**
	 * Gets the highest manufacturing tech level in a settlement.
	 * @param settlement the settlement.
	 * @return highest manufacturing tech level.
	 * @throws BuildingException if error determining highest tech level.
	 */
	public static int getHighestManufacturingTechLevel(Settlement settlement)
			{
		int highestTechLevel = 0;
		BuildingManager manager = settlement.getBuildingManager();
		Iterator<Building> i = manager.getBuildings(BuildingFunction.MANUFACTURE).iterator();
		while (i.hasNext()) {
			Building building = i.next();
			Manufacture manufacturingFunction = (Manufacture) building.getFunction(BuildingFunction.MANUFACTURE);
    		if (manufacturingFunction.getTechLevel() > highestTechLevel) 
    			highestTechLevel = manufacturingFunction.getTechLevel();
		}
		
		return highestTechLevel;
	}

	/**
	 * Gets a good for a manufacture process item.
	 * @param item the manufacture process item.
	 * @return good
	 * @throws Exception if error determining good.
	 */
	public static Good getGood(ManufactureProcessItem item) {
		Good result = null;
		if (Type.AMOUNT_RESOURCE.equals(item.getType())) {
			AmountResource resource = AmountResource.findAmountResource(item.getName());
			result = GoodsUtil.getResourceGood(resource);
		}
		else if (Type.PART.equals(item.getType())) {
			Part part = (Part) ItemResource.findItemResource(item.getName());
			result = GoodsUtil.getResourceGood(part);
		}
		else if (Type.EQUIPMENT.equals(item.getType())) {
			Class<? extends Equipment> equipmentClass = EquipmentFactory.getEquipmentClass(item.getName());
			result = GoodsUtil.getEquipmentGood(equipmentClass);
		}
		else if (Type.VEHICLE.equals(item.getType())) {
			result = GoodsUtil.getVehicleGood(item.getName());
		}
		
		return result;
	}
    
    /**
     * Gets the mass for a manufacturing process item.
     * @param item the manufacturing process item.
     * @return mass (kg).
     * @throws Exception if error determining the mass.
     */
    public static double getMass(ManufactureProcessItem item) {
        double mass = 0D;
        
        if (Type.AMOUNT_RESOURCE.equals(item.getType())) {
            mass = item.getAmount();
        }
        else if (Type.PART.equals(item.getType())) {
            Part part = (Part) ItemResource.findItemResource(item.getName());
            mass = item.getAmount() * part.getMassPerItem();
        }
        else if (Type.EQUIPMENT.equals(item.getType())) {
            double equipmentMass = EquipmentFactory.getEquipmentMass(item.getName());
            mass = item.getAmount() * equipmentMass;
        }
        else if (Type.VEHICLE.equals(item.getType())) {
            VehicleConfig config = SimulationConfig.instance().getVehicleConfiguration();
            mass = item.getAmount() * config.getEmptyMass(item.getName());
        }

        return mass;
    }
    
    /**
     * Finds an available unit to salvage of the type needed by a salvage process.
     * @param info the salvage process information.
     * @param settlement the settlement to find the unit.
     * @return available salvagable unit, or null if none found.
     * @throws Exception if problem finding salvagable unit.
     */
    public static Unit findUnitForSalvage(SalvageProcessInfo info, Settlement settlement)
{
        Unit result = null;
        Inventory inv = settlement.getInventory();
        Collection<Unit> salvagableUnits = new ArrayList<Unit>(0);
        
        if (info.getType().equalsIgnoreCase("vehicle")) {
            if (LightUtilityVehicle.NAME.equalsIgnoreCase(info.getItemName())) {
                salvagableUnits = inv.findAllUnitsOfClass(LightUtilityVehicle.class);
            }
            else {
                salvagableUnits = inv.findAllUnitsOfClass(Rover.class);
                
                // Remove rovers that aren't the right type.
                Iterator<Unit> i = salvagableUnits.iterator();
                while (i.hasNext()) {
                    Rover rover = (Rover) i.next();
                    if (!rover.getDescription().equalsIgnoreCase(info.getItemName())) i.remove();
                }
            }
            
            // Remove any reserved vehicles.
            Iterator<Unit> i = salvagableUnits.iterator();
            while (i.hasNext()) {
                Vehicle vehicle = (Vehicle) i.next();
                if (vehicle.isReserved()) i.remove();
            }
        }
        else if (info.getType().equalsIgnoreCase("equipment")) {
            Class<? extends Equipment> equipmentClass = EquipmentFactory.getEquipmentClass(info.getItemName());
            salvagableUnits = inv.findAllUnitsOfClass(equipmentClass);
        }
        
        // Make sure container unit is settlement.
        Iterator<Unit> i = salvagableUnits.iterator();
        while (i.hasNext()) {
            if (i.next().getContainerUnit() != settlement) i.remove();
        }
        
        // Make sure unit's inventory is empty.
        Iterator<Unit> j = salvagableUnits.iterator();
        while (j.hasNext()) {
            if (!j.next().getInventory().isEmpty(false)) j.remove();
        }
        
        // If malfunctionable, find most worn unit.
        if (salvagableUnits.size() > 0) {
            Unit firstUnit = (Unit) salvagableUnits.toArray()[0];
            if (firstUnit instanceof Malfunctionable) {
                Unit mostWorn = null;
                double lowestWearCondition = Double.MAX_VALUE;
                Iterator<Unit> k = salvagableUnits.iterator();
                while (k.hasNext()) {
                    Unit unit = k.next();
                    Malfunctionable malfunctionable = (Malfunctionable) unit;
                    double wearCondition = malfunctionable.getMalfunctionManager().getWearCondition();
                    if (wearCondition < lowestWearCondition) {
                        mostWorn = unit;
                        lowestWearCondition = wearCondition;
                    }
                }
                result = mostWorn;
            }
            else result = firstUnit;
        }
        
        return result;
    }
}