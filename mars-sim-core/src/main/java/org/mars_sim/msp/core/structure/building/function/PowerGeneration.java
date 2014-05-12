/**
 * Mars Simulation Project
 * PowerGeneration.java
 * @version 3.06 2014-03-08
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingConfig;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.goods.Good;
import org.mars_sim.msp.core.structure.goods.GoodsUtil;

/**
 * The PowerGeneration class is a building function for generating power.
 */
public class PowerGeneration
extends Function
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** TODO Name of the building function needs to be internationalized. */
	private static final BuildingFunction FUNCTION = BuildingFunction.POWER_GENERATION;

	// Data members.
	private List<PowerSource> powerSources;

	/**
	 * Constructor.
	 * @param building the building this function is for.
	 * @throws BuildingException if error in constructing function.
	 */
	public PowerGeneration(Building building) {
		// Call Function constructor.
		super(FUNCTION, building);

		// Determine power sources.
		BuildingConfig config = SimulationConfig.instance()
				.getBuildingConfiguration();
		powerSources = config.getPowerSources(building.getName());
	}

	/**
	 * Gets the value of the function for a named building.
	 * @param buildingName the building name.
	 * @param newBuilding true if adding a new building.
	 * @param settlement the settlement.
	 * @return value (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String buildingName,
			boolean newBuilding, Settlement settlement) {

		double demand = settlement.getPowerGrid().getRequiredPower();

		double supply = 0D;
		boolean removedBuilding = false;
		Iterator<Building> i = settlement.getBuildingManager().getBuildings(
				FUNCTION).iterator();
		while (i.hasNext()) {
			Building building = i.next();
			if (!newBuilding
					&& building.getName().equalsIgnoreCase(buildingName)
					&& !removedBuilding) {
				removedBuilding = true;
			} else {
				PowerGeneration powerFunction = (PowerGeneration) building
						.getFunction(FUNCTION);
				double wearModifier = (building.getMalfunctionManager()
						.getWearCondition() / 100D) * .75D + .25D;
				supply += getPowerSourceSupply(powerFunction.powerSources,
						settlement) * wearModifier;
			}
		}

		double existingPowerValue = demand / (supply + 1D);

		BuildingConfig config = SimulationConfig.instance()
				.getBuildingConfiguration();
		double powerSupply = getPowerSourceSupply(config
				.getPowerSources(buildingName), settlement);

		return powerSupply * existingPowerValue;
	}

	/**
	 * Gets the supply value of a list of power sources.
	 * @param powerSources list of power sources.
	 * @param settlement the settlement.
	 * @return supply value.
	 * @throws Exception if error determining supply value.
	 */
	private static double getPowerSourceSupply(List<PowerSource> powerSources,
			Settlement settlement) {
		double result = 0D;

		Iterator<PowerSource> j = powerSources.iterator();
		while (j.hasNext()) {
			PowerSource source = j.next();
			result += source.getAveragePower(settlement);
			if (source instanceof StandardPowerSource)
				result += source.getMaxPower();
			else if (source instanceof FuelPowerSource) {
				FuelPowerSource fuelSource = (FuelPowerSource) source;
				double fuelPower = source.getMaxPower();
				AmountResource fuelResource = fuelSource.getFuelResource();
				Good fuelGood = GoodsUtil.getResourceGood(fuelResource);
				double fuelValue = settlement.getGoodsManager()
						.getGoodValuePerItem(fuelGood);
				fuelValue *= fuelSource.getFuelConsumptionRate();
				fuelPower -= fuelValue;
				if (fuelPower < 0D)
					fuelPower = 0D;
				result += fuelPower;
			} else if (source instanceof SolarPowerSource) {
				result += source.getMaxPower() / 2D;
			} else if (source instanceof SolarThermalPowerSource) {
				result += source.getMaxPower() / 2.5D;
			} else if (source instanceof WindPowerSource) {
				// TODO: Base on current wind speed at settlement.
				result += source.getMaxPower() / 3D;
			} else if (source instanceof AreothermalPowerSource) {
				double areothermalHeat = Simulation.instance().getMars().getSurfaceFeatures()
						.getAreothermalPotential(settlement.getCoordinates());
				result += source.getMaxPower() * (areothermalHeat / 100D);
			}
		}

		return result;
	}

	/**
	 * Gets the amount of electrical power generated.
	 * @return power generated in kW
	 */
	public double getGeneratedPower() {
		double result = 0D;

		// Building should only produce power if it has no current malfunctions.
		if (!getBuilding().getMalfunctionManager().hasMalfunction()) {
			Iterator<PowerSource> i = powerSources.iterator();
			while (i.hasNext()) {
				result += i.next().getCurrentPower(getBuilding());
			}
		}

		return result;
	}

	/**
	 * Time passing for the building.
	 * @param time amount of time passing (in millisols)
	 * @throws BuildingException if error occurs.
	 */
	public void timePassing(double time) {
		for (PowerSource source : powerSources) {
			if (source instanceof FuelPowerSource) {
				FuelPowerSource fuelSource = (FuelPowerSource) source;
				if (fuelSource.isToggleON()) {
					fuelSource.consumeFuel(time, getBuilding().getInventory());
				}
			}
		}
	}

	/**
	 * Gets the amount of power required when function is at full power.
	 * @return power (kW)
	 */
	public double getFullPowerRequired() {
		return 0D;
	}

	/**
	 * Gets the amount of power required when function is at power down level.
	 * @return power (kW)
	 */
	public double getPowerDownPowerRequired() {
		return 0D;
	}

	@Override
	public String[] getMalfunctionScopeStrings() {
		String[] result = new String[powerSources.size() + 1];
		// TODO take care to properly internationalize malfunction scope "strings"
		result[0] = getFunction().getName();

		for (int x = 0; x < powerSources.size(); x++) {
			result[x + 1] = powerSources.get(x).getType().getString();
		}
			
		return result;
	}

	/**
	 * Gets the power sources for the building.
	 * @return list of power sources.
	 */
	public List<PowerSource> getPowerSources() {
		return new ArrayList<PowerSource>(powerSources);
	}
	
    @Override
    public double getMaintenanceTime() {
        
        double result = 0D;
        
        Iterator<PowerSource> i = powerSources.iterator();
        while (i.hasNext()) {
            result += i.next().getMaintenanceTime();
        }
        
        return result;
    }

	@Override
	public void destroy() {
		super.destroy();

		Iterator<PowerSource> i = powerSources.iterator();
		while (i.hasNext()) {
			i.next().destroy();
		}
		powerSources.clear();
	}
}