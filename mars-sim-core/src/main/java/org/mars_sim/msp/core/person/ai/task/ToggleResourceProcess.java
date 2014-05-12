/**
 * Mars Simulation Project
 * ToggleResourceProcess.java
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
import org.mars_sim.msp.core.structure.building.function.ResourceProcess;
import org.mars_sim.msp.core.structure.building.function.ResourceProcessing;
import org.mars_sim.msp.core.structure.goods.GoodsUtil;
import org.mars_sim.msp.core.time.MarsClock;

/** 
 * The ToggleResourceProcess class is an EVA task for toggling a particular
 * automated resource process on or off.
 */
public class ToggleResourceProcess
extends EVAOperation
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(ToggleResourceProcess.class.getName());

	// TODO Task phase should be an enum.
	private static final String TOGGLE_PROCESS = "toggle process";

	// Data members
	/** True if toggling process is EVA operation. */
	private boolean isEVA;
	/** The resource process to toggle. */
	private ResourceProcess process;
	/** The building the resource process is in. */
	private Building building;
	/** True if process is to be turned on, false if turned off. */
	private boolean toggleOn;

	/**
	 * Constructor.
	 * @param person the person performing the task.
	 */
	public ToggleResourceProcess(Person person) {
		super("Turning on resource process", person, false, 0D);

		building = getResourceProcessingBuilding(person);
		if (building != null) {
			process = getResourceProcess(building);
			toggleOn = !process.isProcessRunning();
			if (!toggleOn) {
				setName("Turning off resource process");
				setDescription("Turning off resource process");
			}
			isEVA = !building.hasFunction(BuildingFunction.LIFE_SUPPORT);

			// If habitable building, add person to building.
			if (!isEVA) {
				// Walk to building.
				walkToProcessBuilding(building);
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

		addPhase(TOGGLE_PROCESS);

		if (!isEVA) {
			setPhase(TOGGLE_PROCESS);
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

			// Check if settlement has resource process override set.
			if (!settlement.getResourceProcessOverride()) {
				try {
					Building building = getResourceProcessingBuilding(person);
					if (building != null) {
						ResourceProcess process = getResourceProcess(building);
						isEVA = !building.hasFunction(BuildingFunction.LIFE_SUPPORT);
						double diff = getResourcesValueDiff(settlement, process);
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
				result *= job.getStartTaskProbabilityModifier(ToggleResourceProcess.class);    
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
	 * Walk to process toggle building.
	 * @param processBuilding the process toggle building.
	 */
	private void walkToProcessBuilding(Building processBuilding) {

		// Determine location within process toggle building.
		// TODO: Use action point rather than random internal location.
		Point2D.Double buildingLoc = LocalAreaUtil.getRandomInteriorLocation(processBuilding);
		Point2D.Double settlementLoc = LocalAreaUtil.getLocalRelativeLocation(buildingLoc.getX(), 
				buildingLoc.getY(), processBuilding);

		if (Walk.canWalkAllSteps(person, settlementLoc.getX(), settlementLoc.getY(), 
				processBuilding)) {

			// Add subtask for walking to process building.
			addSubTask(new Walk(person, settlementLoc.getX(), settlementLoc.getY(), 
					processBuilding));
		}
		else {
			logger.fine(person.getName() + " unable to walk to process building " + 
					processBuilding.getName());
			endTask();
		}
	}

	/**
	 * Gets the building at a person's settlement with the resource process that needs toggling.
	 * @param person the person.
	 * @return building with resource process to toggle, or null if none.
	 */
	private static Building getResourceProcessingBuilding(Person person) {
		Building result = null;

		Settlement settlement = person.getSettlement();
		if (settlement != null) {
			BuildingManager manager = settlement.getBuildingManager();
			double bestDiff = 0D;
			Iterator<Building> i = manager.getBuildings(BuildingFunction.RESOURCE_PROCESSING).iterator();
			while (i.hasNext()) {
				Building building = i.next();
				ResourceProcess process = getResourceProcess(building);
				if (process != null) {
					double diff = getResourcesValueDiff(settlement, process);
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
	 * Gets the resource process to toggle at a building.
	 * @param building the building
	 * @return the resource process to toggle or null if none.
	 */
	private static ResourceProcess getResourceProcess(Building building) {
		ResourceProcess result = null;

		Settlement settlement = building.getBuildingManager().getSettlement();
		if (building.hasFunction(BuildingFunction.RESOURCE_PROCESSING)) {
			double bestDiff = 0D;
			ResourceProcessing processing = (ResourceProcessing) building.getFunction(BuildingFunction.RESOURCE_PROCESSING);
			Iterator<ResourceProcess> i = processing.getProcesses().iterator();
			while (i.hasNext()) {
				ResourceProcess process = i.next();
				double diff = getResourcesValueDiff(settlement, process);
				if (diff > bestDiff) {
					bestDiff = diff;
					result = process;
				}
			}
		}

		return result;
	}

	/**
	 * Gets the resources value diff between inputs and outputs for a resource process.
	 * @param settlement the settlement the resource process is at.
	 * @param process the resource process.
	 * @return the resource value diff (value points)
	 */
	private static double getResourcesValueDiff(Settlement settlement, 
			ResourceProcess process) {
		double inputValue = getResourcesValue(settlement, process, true);
		double outputValue = getResourcesValue(settlement, process, false);
		double diff = outputValue - inputValue;

		// Subtract power required per millisol.
		double hoursInMillisol = MarsClock.convertMillisolsToSeconds(1D) / 60D / 60D;
		double powerHrsRequiredPerMillisol = process.getPowerRequired() * hoursInMillisol;
		double powerValue = powerHrsRequiredPerMillisol * settlement.getPowerGrid().getPowerValue();
		diff -= powerValue;

		if (process.isProcessRunning()) {
			diff *= -1D;
		}

		// Check if settlement doesn't have one or more of the input resources.
		if (isEmptyInputResourceInProcess(settlement, process)) {
			if (process.isProcessRunning()) {
				diff = 1D;
			}
			else {
				diff = 0D;
			}
		}
		return diff;
	}

	/**
	 * Gets the total value of a resource process's input or output.
	 * @param settlement the settlement for the resource process.
	 * @param process the resource process.
	 * @param input is the resource value for the input?
	 * @return the total value for the input or output.
	 */
	private static double getResourcesValue(Settlement settlement, ResourceProcess process, 
			boolean input) {

		double result = 0D;

		Iterator<AmountResource> i = null;
		if (input) i = process.getInputResources().iterator();
		else i = process.getOutputResources().iterator();

		while (i.hasNext()) {
			AmountResource resource = i.next();
			boolean useResource = true;
			if (input && process.isAmbientInputResource(resource)) {
				useResource = false;
			}
			if (!input && process.isWasteOutputResource(resource)) {
				useResource = false;
			}
			if (useResource) {
				double value = settlement.getGoodsManager().getGoodValuePerItem(
						GoodsUtil.getResourceGood(resource));
				double rate = 0D;
				if (input) {
					rate = process.getMaxInputResourceRate(resource);
				}
				else {
					rate = process.getMaxOutputResourceRate(resource);
				}
				result += (value * rate);
			}
		}

		return result;
	}

	/**
	 * Checks if a resource process has no input resources.
	 * @param settlement the settlement the resource is at.
	 * @param process the resource process.
	 * @return true if any input resources are empty.
	 */
	private static boolean isEmptyInputResourceInProcess(Settlement settlement, 
			ResourceProcess process) {
		boolean result = false;

		Iterator<AmountResource> i = process.getInputResources().iterator();
		while (i.hasNext()) {
			AmountResource resource = i.next();
			if (!process.isAmbientInputResource(resource)) {
				double stored = settlement.getInventory().getAmountResourceStored(resource, false);
				if (stored == 0D) {
					result = true;
				}
			}
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

		// If phase is toggle process, add experience to mechanics skill.
		if (TOGGLE_PROCESS.equals(getPhase())) {
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
		return TOGGLE_PROCESS;
	}

	@Override
	protected double performMappedPhase(double time) {

		time = super.performMappedPhase(time);

		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		}
		else if (TOGGLE_PROCESS.equals(getPhase())) {
			return toggleProcessPhase(time);
		}
		else {
			return time;
		}
	}

	/**
	 * Performs the toggle process phase.
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double toggleProcessPhase(double time) {

		// If person is incapacitated, enter airlock.
		if (person.getPerformanceRating() == 0D) {
			if (isEVA) {
				setPhase(WALK_BACK_INSIDE);
			}
			else {
				endTask();
			}
		}

		// Check if process has already been completed.
		if (process.isProcessRunning() == toggleOn) {
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
		if (mechanicSkill > 1) {
			workTime += workTime * (.2D * mechanicSkill);
		}

		// Add work to the toggle process.
		process.addToggleWorkTime(workTime);

		// Add experience points
		addExperience(time);

		// Check if process has already been completed.
		if (process.isProcessRunning() == toggleOn) {
			if (isEVA) {
				setPhase(WALK_BACK_INSIDE);
			}
			else {
				endTask();
			}

			Settlement settlement = building.getBuildingManager().getSettlement();
			String toggle = "off";
			if (toggleOn) {
				toggle = "on";
			}
			logger.fine(person.getName() + " turning " + toggle + " " + process.getProcessName() + 
					" at " + settlement.getName() + ": " + building.getName());
		}

		// Check if an accident happens during toggle process.
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

		process = null;
		building = null;
	}
}