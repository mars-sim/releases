package org.mars_sim.msp.core.structure.building.function;

/**
 * Mars Simulation Project
 * ThermalGeneration.java
 * @version 3.07 2014-10-23
 * @author Manny Kung
 */
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.structure.building.BuildingConfig;
import org.mars_sim.msp.core.structure.building.function.HeatMode;

/**
 * The HeatGeneration class handles how the buildings of a settlement  
 * generate heat and how the Thermal Control behaves
 */
public class ThermalGeneration
extends Function
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(ThermalGeneration.class.getName());

	DecimalFormat fmt = new DecimalFormat("#.#######"); 
	
	/** TODO Name of the building function needs to be internationalized. */
	private static final BuildingFunction FUNCTION = BuildingFunction.THERMAL_GENERATION;

	// Data members.
	private List<HeatSource> heatSources;
	private Building building;
	
	// 2014-11-02 Created heatGenerated and heatGeneratedCache
	private double heatGenerated;
	private double heatGeneratedCache;

	
	private boolean sufficientHeat;
	//private static int count;

	// 2014-10-25 Added heatSource
	private HeatSource heatSource;
	
  	//protected HeatMode heatMode;
	/**
	 * Constructor
	 */
	public ThermalGeneration(Building building) {
		// Call Function constructor.
		super(FUNCTION, building);	
		//count++;
		//logger.info("constructor : count is " + count);
		
		// Determine heat sources.
		BuildingConfig config = SimulationConfig.instance()
				.getBuildingConfiguration();
		heatSources = config.getHeatSources(building.getName());
		this.building = building;
		
	}

	/**
	 * Gets the value of the function for a named building.
	 * @param buildingName the building name.
	 * @param newBuilding true if adding a new building.
	 * @param settlement the settlement.
	 * @return value) (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String buildingName,
			boolean newBuilding, Settlement settlement) {

		double demand = settlement.getThermalSystem().getRequiredHeat();

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
				ThermalGeneration heatFunction = (ThermalGeneration) building.getFunction(FUNCTION);
				double wearModifier = (building.getMalfunctionManager()
						.getWearCondition() / 100D) * .75D + .25D;
				supply += getHeatSourceSupply(heatFunction.heatSources,
						settlement) * wearModifier;
			}
		}

		double existingHeatValue = demand / (supply + 1D);

		BuildingConfig config = SimulationConfig.instance()
				.getBuildingConfiguration();
		double heatSupply = getHeatSourceSupply(config
				.getHeatSources(buildingName), settlement);

		return heatSupply * existingHeatValue;
	}

	/**
	 * Gets the supply value of a list of heat sources.
	 * @param heatSources list of heat sources.
	 * @param settlement the settlement.
	 * @return supply value.
	 * @throws Exception if error determining supply value.
	 */
	private static double getHeatSourceSupply(List<HeatSource> heatSources,
			Settlement settlement) {
		double result = 0D;

		Iterator<HeatSource> j = heatSources.iterator();
		while (j.hasNext()) {
			HeatSource source = j.next();
			result += source.getAverageHeat(settlement);
			if (source instanceof ElectricHeatSource)
				result += source.getMaxHeat();
			 else if (source instanceof SolarHeatSource) {
				result += source.getMaxHeat() / 2D;
			}
		}

		return result;
	}
	/**
	 * Checks if there is enough heat in the grid for all 
	 * buildings to be set to full heat.
	 * @return true if sufficient heat
	 */
	public boolean isSufficientHeat() {
		return sufficientHeat;
	}


	/** 	
	 * Gets the total amount of heat that this building is capable of producing (regardless malfunctions).
	 * @return heat generated in kJ/s (heat flow rate)
	 */
	// get heat from HeatSource.java
	//2014-10-24 mkung: added getGeneratedCapacity()
	// Note: NOT affected by HeatMode.POWER_DOWN
	public double getGeneratedCapacity() {
		double result = 0D;	
			Iterator<HeatSource> i = heatSources.iterator();
			while (i.hasNext()) {
				result += i.next().getCurrentHeat(getBuilding());
			}
			//logger.info("getGeneratedHeat() : total heat gain is " + fmt.format(result) ); 
		return result;
	}
	
	/**
	 * Gets the total amount of heat that this building is CURRENTLY producing 
	 * @return heat generated in kJ/s (heat flow rate)
	 */
	// get heat from HeatSource.java
	// 2014-10-24 Modified getGeneratedHeat()
	public double getGeneratedHeat() {
		if ( heatGeneratedCache != heatGenerated) {
			// if heatGeneratedCache is different from the its last value
			heatGeneratedCache = heatGenerated;
		logger.info("heatGenerated is " + heatGenerated);
			//logger.info("getGeneratedHeat() : total heat gain is " + fmt.format(result) ); 
		}
		return heatGenerated; // = 0.0 if heatMode == HeatMode.POWER_DOWN
	}
	
	/**
	 * Calculate the total amount of heat that this building is CURRENTLY producing 
	 * @return heat generated in kW
	 */
	// 2014-11-02 Created calculateGeneratedHeat()
	public void calculateGeneratedHeat() {
		
		double result = 0D; 
		HeatMode heatMode = building.getHeatMode();
	
	// Building should only produce heat if it has no current malfunctions.
	if (!getBuilding().getMalfunctionManager().hasMalfunction() 
			&& heatMode == HeatMode.FULL_POWER) {
		// No heat if heatMode = HeatMode.POWER_DOWN
		Iterator<HeatSource> i = heatSources.iterator();
		while (i.hasNext()) {
			/// 2014-10-27 mkung: for testing
			//HeatSource heatSource = i.next();
		    //System.out.println(heatSource.toString());
		    ///
			result += i.next().getCurrentHeat(getBuilding());
		}
	}
	// store new result in heatGenerated 
	heatGenerated = result;
}
	
	/**
	 * Time passing for the building.
	 * @param time amount of time passing (in millisols)
	 * @throws BuildingException if error occurs.
	 */
	public void timePassing(double time) {

		// 2014-11-02 Added calculateGeneratedHeat()
		// Set heatGenerated at the building the furnace belongs
		calculateGeneratedHeat();
				
		if ( heatGeneratedCache != heatGenerated) {
			// if heatGeneratedCache is different from the its last value
			heatGeneratedCache = heatGenerated;
			building.setHeatGenerated(heatGenerated);
		}
			
		for (HeatSource source : heatSources) {
			/*
			if (source instanceof FuelHeatSource) {
				FuelHeatSource fuelSource = (FuelHeatSource) source;
				if (fuelSource.isToggleON()) {
					fuelSource.consumeFuel(time, getBuilding().getInventory());
				}
			}
			*/
		}
	}

	/**
	 * Gets the amount of heat required when function is at full power.
	 * @return heat (J)
	 */
	// 2014-11-02: temporarily set getFullHeatRequired() = heatGenerated
	// thus getGeneratedHeat() = getFullHeatRequired() 
	// will consolidate them into one in near future
	public double getFullHeatRequired() {
		return heatGenerated;
	}

	/**
	 * Gets the amount of heat required when function is at power down level.
	 * @return heat (J)
	 */
	public double getPoweredDownHeatRequired() {
		return 0D;
	}

	@Override
	public String[] getMalfunctionScopeStrings() {
		String[] result = new String[heatSources.size() + 1];
		// TODO take care to properly internationalize malfunction scope "strings"
		result[0] = getFunction().getName();

		for (int x = 0; x < heatSources.size(); x++) {
			result[x + 1] = heatSources.get(x).getType().getString();
		}
			
		return result;
	}

	/**
	 * Gets the heat sources for the building.
	 * @return list of heat sources.
	 */
	public HeatSource getHeatSource() {
		return heatSource;
	}
	
	
	/**
	 * Gets the heat sources for the building.
	 * @return list of heat sources.
	 */
	public List<HeatSource> getHeatSources() {
		return new ArrayList<HeatSource>(heatSources);
	}
	
    @Override
    public double getMaintenanceTime() {
        
        double result = 0D;
        
        Iterator<HeatSource> i = heatSources.iterator();
        while (i.hasNext()) {
            result += i.next().getMaintenanceTime();
        }
        
        return result;
    }

	@Override
	public void destroy() {
		super.destroy();

		Iterator<HeatSource> i = heatSources.iterator();
		while (i.hasNext()) {
			i.next().destroy();
		}
		heatSources.clear();
	}


	public double getFullPowerRequired() {
		
		return 0;
	}


	@Override
	public double getPoweredDownPowerRequired() {
		// TODO Auto-generated method stub
		return 0;
	}
	/**
	 * Switch on or off the heating system
	 * @param true if it's on
	
	public void setHeating(boolean isOn) {
		heating = isOn;
	}
 */
	/**
	 * get the state of the heating system
	 * @param true if it's on  
	
	public boolean isHeating() {
		return heating;
	}
	 */
	/**
	 * Gets the capacity of the furnace.
	 * @return heat in J
	 
	public double getCapacity() {
		return capacity;
	}
*/
	/**
	 * Gets the efficiency
	 * @return between 0 to 1 
	
	public double getEfficiency() {
		return efficiency;
	}
 */

	/**
	 * Gets the heat required from the grid.
	 * @return heat in J
	 
	public double getRequiredHeat() {
		return heatRequired;
	}
*/



}