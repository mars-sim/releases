/**
 * Mars Simulation Project
 * ToggleFuelPowerSource.java
 * @version 3.06 2014-02-27
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.FuelPowerSource;
import org.mars_sim.msp.core.structure.building.function.PowerGeneration;
import org.mars_sim.msp.core.structure.building.function.PowerSource;
import org.mars_sim.msp.core.structure.goods.GoodsUtil;
import org.mars_sim.msp.core.time.MarsClock;

/** 
 * The ToggleFuelPowerSource class is an EVA task for toggling a particular
 * fuel power source building on or off.
 */
public class ToggleFuelPowerSource
extends EVAOperation
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(ToggleFuelPowerSource.class.getName());

	// TODO Task phase should be an enum.
	private static final String TOGGLE_POWER_SOURCE = "Toggle Power Source";

	// Data members
	/** True if toggling process is EVA operation. */
	private boolean isEVA;
	/** The fuel power source to toggle. */
	private FuelPowerSource powerSource;
	/** The building the resource process is in. */
	private Building building;
	/** True if power source is to be turned on, false if turned off. */
	private boolean toggleOn;

	/**
	 * Constructor
	 * @param person the person performing the task.
	 * @throws Exception if error constructing the task.
	 */
	public ToggleFuelPowerSource(Person person) {
		super("Turning on fuel power source", person, false, 0D);

		building = getFuelPowerSourceBuilding(person);
		if (building != null) {
			powerSource = getFuelPowerSource(building);
			toggleOn = !powerSource.isToggleON();
			if (!toggleOn) {
				setName("Turning off fuel power source");
				setDescription("Turning off fuel power source");
			}
			isEVA = !building.hasFunction(BuildingFunction.LIFE_SUPPORT);

			// If habitable building, add person to building.
			if (!isEVA) {
				// Walk to power source building.
				walkToPowerSourceBuilding(building);
			}
			else {
				// Determine location for toggling power source.
				Point2D toggleLoc = determineToggleLocation();
				setOutsideSiteLocation(toggleLoc.getX(), toggleLoc.getY());
			}
		}
		else {
			endTask();
		}

		addPhase(TOGGLE_POWER_SOURCE);

		if (!isEVA) {
			setPhase(TOGGLE_POWER_SOURCE);
		}
	}

	/** 
	 * Gets the weighted probability that a person might perform this task.
	 * It should return a 0 if there is no chance to perform this task given the person and his/her situation.
	 * @param person the person to perform the task
	 * @return the weighted probability that a person might perform this task
	 */
	public static double getProbability(Person person) {
		double result = 0D;

		if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
			boolean isEVA = false;

			Settlement settlement = person.getSettlement();

			try {
				Building building = getFuelPowerSourceBuilding(person);
				if (building != null) {
					FuelPowerSource powerSource = getFuelPowerSource(building);
					isEVA = !building.hasFunction(BuildingFunction.LIFE_SUPPORT);
					double diff = getValueDiff(settlement, powerSource);
					double baseProb = diff * 10000D;
					if (baseProb > 100D) {
						baseProb = 100D;
					}
					result += baseProb;

					if (!isEVA) {
						// Factor in building crowding and relationship factors.
						result *= Task.getCrowdingProbabilityModifier(person, building);
						result *= Task.getRelationshipModifier(person, building);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}

			if (isEVA) {
				// Check if an airlock is available
				if (getWalkableAvailableAirlock(person) == null) {
					result = 0D;
				}

				// Check if it is night time.
				SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();
				if (surface.getSurfaceSunlight(person.getCoordinates()) == 0) {
					if (!surface.inDarkPolarRegion(person.getCoordinates())) {
						result = 0D;
					}
				} 

				// Crowded settlement modifier
				if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
					if (settlement.getCurrentPopulationNum() > settlement.getPopulationCapacity()) {
						result *= 2D;
					}
				}
			}

			// Effort-driven task modifier.
			result *= person.getPerformanceRating();

			// Job modifier.
			Job job = person.getMind().getJob();
			if (job != null) {
				result *= job.getStartTaskProbabilityModifier(ToggleFuelPowerSource.class);
			}
		}

		return result;
	}

	/**
	 * Determine location to toggle power source.
	 * @return location.
	 */
	private Point2D determineToggleLocation() {

		Point2D.Double newLocation = new Point2D.Double(0D, 0D);

		boolean goodLocation = false;
		for (int x = 0; (x < 50) && !goodLocation; x++) {
			Point2D.Double boundedLocalPoint = LocalAreaUtil.getRandomExteriorLocation(building, 1D);
			newLocation = LocalAreaUtil.getLocalRelativeLocation(boundedLocalPoint.getX(), 
					boundedLocalPoint.getY(), building);
			goodLocation = LocalAreaUtil.checkLocationCollision(newLocation.getX(), newLocation.getY(), 
					person.getCoordinates());
		}

		return newLocation;
	}

	/**
	 * Walk to power source building.
	 * @param powerBuilding the power source building.
	 */
	private void walkToPowerSourceBuilding(Building powerBuilding) {

		// Determine location within power source building.
		// TODO: Use action point rather than random internal location.
		Point2D.Double buildingLoc = LocalAreaUtil.getRandomInteriorLocation(powerBuilding);
		Point2D.Double settlementLoc = LocalAreaUtil.getLocalRelativeLocation(buildingLoc.getX(), 
				buildingLoc.getY(), powerBuilding);

		if (Walk.canWalkAllSteps(person, settlementLoc.getX(), settlementLoc.getY(), 
				powerBuilding)) {

			// Add subtask for walking to power building.
			addSubTask(new Walk(person, settlementLoc.getX(), settlementLoc.getY(), 
					powerBuilding));
		}
		else {
			logger.fine(person.getName() + " unable to walk to power building " + 
					powerBuilding.getName());
			endTask();
		}
	}

	/**
	 * Gets the building at a person's settlement with the fuel power source that needs toggling.
	 * @param person the person.
	 * @return building with fuel power source to toggle, or null if none.
	 */
	private static Building getFuelPowerSourceBuilding(Person person) {
		Building result = null;

		Settlement settlement = person.getSettlement();
		if (settlement != null) {
			BuildingManager manager = settlement.getBuildingManager();
			double bestDiff = 0D;
			Iterator<Building> i = manager.getBuildings(BuildingFunction.POWER_GENERATION).iterator();
			while (i.hasNext()) {
				Building building = i.next();
				FuelPowerSource powerSource = getFuelPowerSource(building);
				if (powerSource != null) {
					double diff = getValueDiff(settlement, powerSource);
					if (diff > bestDiff) {
						bestDiff = diff;
						result = building;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Gets the fuel power source to toggle at a building.
	 * @param building the building
	 * @return the fuel power source to toggle or null if none.
	 */
	private static FuelPowerSource getFuelPowerSource(Building building) {
		FuelPowerSource result = null;

		Settlement settlement = building.getBuildingManager().getSettlement();
		if (building.hasFunction(BuildingFunction.POWER_GENERATION)) {
			double bestDiff = 0D;
			PowerGeneration powerGeneration = (PowerGeneration) building.getFunction(BuildingFunction.POWER_GENERATION);
			Iterator<PowerSource> i = powerGeneration.getPowerSources().iterator();
			while (i.hasNext()) {
				PowerSource powerSource = i.next();
				if (powerSource instanceof FuelPowerSource) {
					FuelPowerSource fuelSource = (FuelPowerSource) powerSource;
					double diff = getValueDiff(settlement, fuelSource);
					if (diff > bestDiff) {
						bestDiff = diff;
						result = fuelSource;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Gets the value diff between inputs and outputs for a fuel power source.
	 * @param settlement the settlement the resource process is at.
	 * @param fuelSource the fuel power source.
	 * @return the value diff (value points)
	 */
	private static double getValueDiff(Settlement settlement, FuelPowerSource fuelSource) {

		double inputValue = getInputResourcesValue(settlement, fuelSource);
		double outputValue = getPowerOutputValue(settlement, fuelSource);
		double diff = outputValue - inputValue;
		if (fuelSource.isToggleON()) {
			diff *= -1D;
		}

		// Check if settlement doesn't have one or more of the input resources.
		if (isEmptyInputResource(settlement, fuelSource)) {
			if (fuelSource.isToggleON()) {
				diff = 1D;
			}
			else {
				diff = 0D;
			}
		}
		return diff;
	}

	/**
	 * Gets the total value of a fuel power sources input resources.
	 * @param settlement the settlement.
	 * @param fuel source the fuel power source.
	 * @return the total value for the input resources per Sol.
	 */
	private static double getInputResourcesValue(Settlement settlement, FuelPowerSource fuelSource) {

		AmountResource resource = fuelSource.getFuelResource();
		double massPerSol = fuelSource.getFuelConsumptionRate();
		double value = settlement.getGoodsManager().getGoodValuePerItem(GoodsUtil.getResourceGood(resource));

		return value * massPerSol;
	}

	/**
	 * Gets the total value of the power produced by the power source.
	 * @param settlement the settlement.
	 * @param fuelSource the fuel power source.
	 * @return the value of the power generated per Sol.
	 */
	private static double getPowerOutputValue(Settlement settlement, FuelPowerSource fuelSource) {

		// Get settlement value for kW hr produced.
		double power = fuelSource.getMaxPower();
		double hoursInSol = MarsClock.convertMillisolsToSeconds(1000D) / 60D / 60D;
		double powerPerSol = power * hoursInSol;
		double powerValue = powerPerSol * settlement.getPowerGrid().getPowerValue();

		return powerValue;
	}

	/**
	 * Checks if a fuel power source has no input resources.
	 * @param settlement the settlement the resource is at.
	 * @param fuelSource the fuel power source.
	 * @return true if any input resources are empty.
	 */
	private static boolean isEmptyInputResource(Settlement settlement, 
			FuelPowerSource fuelSource) {
		boolean result = false;

		AmountResource resource = fuelSource.getFuelResource();
		double stored = settlement.getInventory().getAmountResourceStored(resource, false);
		if (stored == 0D) {
			result = true;
		}

		return result;
	}

	@Override
	protected void addExperience(double time) {

		// Experience points adjusted by person's "Experience Aptitude" attribute.
		NaturalAttributeManager nManager = person.getNaturalAttributeManager();
		int experienceAptitude = nManager.getAttribute(NaturalAttribute.EXPERIENCE_APTITUDE);
		double experienceAptitudeModifier = (((double) experienceAptitude) - 50D) / 100D;

		if (isEVA) {
			// Add experience to "EVA Operations" skill.
			// (1 base experience point per 100 millisols of time spent)
			double evaExperience = time / 100D;
			evaExperience += evaExperience * experienceAptitudeModifier;
			evaExperience *= getTeachingExperienceModifier();
			person.getMind().getSkillManager().addExperience(SkillType.EVA_OPERATIONS, evaExperience);
		}

		// If phase is toggle power source, add experience to mechanics skill.
		if (TOGGLE_POWER_SOURCE.equals(getPhase())) {
			// 1 base experience point per 100 millisols of time spent.
			// Experience points adjusted by person's "Experience Aptitude" attribute.
			double mechanicsExperience = time / 100D;
			mechanicsExperience += mechanicsExperience * experienceAptitudeModifier;
			person.getMind().getSkillManager().addExperience(SkillType.MECHANICS, mechanicsExperience);
		}
	}

	@Override
	public List<SkillType> getAssociatedSkills() {
		List<SkillType> result = new ArrayList<SkillType>(2);
		result.add(SkillType.MECHANICS);
		if (isEVA) {
			result.add(SkillType.EVA_OPERATIONS);
		}
		return result;
	}

	@Override
	public int getEffectiveSkillLevel() {
		SkillManager manager = person.getMind().getSkillManager();
		int EVAOperationsSkill = manager.getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
		int mechanicsSkill = manager.getEffectiveSkillLevel(SkillType.MECHANICS);
		if (isEVA) {
			return (int) Math.round((double)(EVAOperationsSkill + mechanicsSkill) / 2D);
		}
		else {
			return (mechanicsSkill);
		}
	}

	@Override
	protected String getOutsideSitePhase() {
		return TOGGLE_POWER_SOURCE;
	}

	@Override
	protected double performMappedPhase(double time) {

		time = super.performMappedPhase(time);

		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		}
		else if (TOGGLE_POWER_SOURCE.equals(getPhase())) {
			return togglePowerSourcePhase(time);
		}
		else {
			return time;
		}
	}

	/**
	 * Performs the toggle power source phase.
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double togglePowerSourcePhase(double time) {

		// If person is incapacitated, end task.
		if (person.getPerformanceRating() == 0D) {
			if (isEVA) {
				setPhase(WALK_BACK_INSIDE);
			}
			else {
				endTask();
			}
		}

		// Check if toggle has already been completed.
		if (powerSource.isToggleON() == toggleOn) {
			if (isEVA) {
				setPhase(WALK_BACK_INSIDE);
			}
			else {
				endTask();
			}
		}

		if (isDone()) {
			return time;
		}

		// Determine effective work time based on "Mechanic" skill.
		double workTime = time;
		int mechanicSkill = getEffectiveSkillLevel();
		if (mechanicSkill == 0) {
			workTime /= 2;
		}
		else if (mechanicSkill > 1) {
			workTime += workTime * (.2D * mechanicSkill);
		}

		// Add work to the toggle power source.
		powerSource.addToggleWorkTime(workTime);

		// Add experience points
		addExperience(time);

		// Check if toggle has already been completed.
		if (powerSource.isToggleON() == toggleOn) {
			if (isEVA) {
				setPhase(WALK_BACK_INSIDE);
			}
			else {
				endTask();
			}

			Settlement settlement = building.getBuildingManager().getSettlement();
			String toggle = "off";
			if (toggleOn) toggle = "on";
			logger.fine(person.getName() + " turning " + toggle + " " + powerSource.getType() + 
					" at " + settlement.getName() + ": " + building.getName());
		}

		// Check if an accident happens during toggle power source.
		checkForAccident(time);

		return 0D;
	}

	/**
	 * Check for accident with entity during toggle resource phase.
	 * @param time the amount of time (in millisols)
	 */
	protected void checkForAccident(double time) {

		// Use EVAOperation checkForAccident() method.
		if (isEVA) {
			super.checkForAccident(time);
		}

		double chance = .001D;

		// Mechanic skill modification.
		int skill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.MECHANICS);
		if (skill <= 3) {
			chance *= (4 - skill);
		}
		else {
			chance /= (skill - 2);
		}

		// Modify based on the building's wear condition.
		chance *= building.getMalfunctionManager().getWearConditionAccidentModifier();

		if (RandomUtil.lessThanRandPercent(chance * time)) {
			building.getMalfunctionManager().accident();
		}
	}

	@Override
	public void destroy() {
		super.destroy();

		powerSource = null;
		building = null;
	}
}