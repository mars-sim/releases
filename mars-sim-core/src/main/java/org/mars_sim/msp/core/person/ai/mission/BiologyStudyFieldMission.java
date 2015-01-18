/**
 * Mars Simulation Project
 * BiologyStudyFieldMission.java
 * @version 3.07 2015-01-04
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.mission;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Direction;
import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LifeSupport;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.mars.Mars;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.task.BiologyStudyFieldWork;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.Resource;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.science.ScientificStudyManager;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.vehicle.Rover;

/** 
 * A mission to do biology research at a remote field location for a
 * scientific study.
 * TODO externalize strings
 */
public class BiologyStudyFieldMission
extends RoverMission 
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(BiologyStudyFieldMission.class.getName());

	/** Default description. */
	public static final String DEFAULT_DESCRIPTION = Msg.getString(
			"Mission.description.biologyStudyFieldMission"); //$NON-NLS-1$

	/** Mission phase. */
	final public static MissionPhase RESEARCH_SITE = new MissionPhase(Msg.getString(
			"Mission.phase.researchingFieldSite")); //$NON-NLS-1$

	/** Minimum number of people to do mission. */
	public final static int MIN_PEOPLE = 2;

	/** Amount of time to field a site. */
	public final static double FIELD_SITE_TIME = 1000D;

	// Data members
	/** The start time at the field site. */
	private MarsClock fieldSiteStartTime;
	/** External flag for ending research at the field site. */
	private boolean endFieldSite;
	/** The field site location. */
	private Coordinates fieldSite;
	/** Scientific study to research. */
	private ScientificStudy study;
	/** The person leading the biology research. */
	private Person leadResearcher;

	/**
	 * Constructor.
	 * @param startingPerson {@link Person} the person starting the mission.
	 * @throws MissionException if problem constructing mission.
	 */
	public BiologyStudyFieldMission(Person startingPerson) {

		// Use RoverMission constructor.
		super(DEFAULT_DESCRIPTION, startingPerson, MIN_PEOPLE);

		if (!isDone()) {
			// Set the lead biology researcher and study.
			leadResearcher = startingPerson;
			study = determineStudy(leadResearcher);
			if (study == null) endMission("Scientific study could not be determined.");

			// Set mission capacity.
			if (hasVehicle()) setMissionCapacity(getRover().getCrewCapacity());
			int availableSuitNum = Mission.getNumberAvailableEVASuitsAtSettlement(
					startingPerson.getSettlement());
			if (availableSuitNum < getMissionCapacity()) setMissionCapacity(availableSuitNum);

			// Initialize data members.
			setStartingSettlement(startingPerson.getSettlement());

			// Recruit additional people to mission.
			recruitPeopleForMission(startingPerson);

			// Determine field site location.
			if (hasVehicle()) {
				double tripTimeLimit = getTotalTripTimeLimit(getRover(), getPeopleNumber(), true);
				determineFieldSite(getVehicle().getRange(), tripTimeLimit);
			}

			// Add home settlement
			addNavpoint(new NavPoint(getStartingSettlement().getCoordinates(), 
					getStartingSettlement(), getStartingSettlement().getName()));

			// Check if vehicle can carry enough supplies for the mission.
			if (hasVehicle() && !isVehicleLoadable()) 
				endMission("Vehicle is not loadable. (BiologyStudyFieldMission)");
		}

		// Add researching site phase.
		addPhase(RESEARCH_SITE);

		// Set initial mission phase.
		setPhase(VehicleMission.EMBARKING);
		setPhaseDescription(Msg.getString(
				"Mission.phase.embarking.description" //$NON-NLS-1$
				,getStartingSettlement().getName()));
	}

	/**
	 * Constructor with explicit information.
	 * @param members the mission members.
	 * @param startingSettlement the settlement the mission starts at.
	 * @param leadResearcher the lead researcher
	 * @param study the scientific study.
	 * @param rover the rover used by the mission.
	 * @param fieldSite the field site to research.
	 * @param description the mission description.
	 * @throws MissionException if error creating mission.
	 */
	public BiologyStudyFieldMission(Collection<Person> members, Settlement startingSettlement, 
			Person leadResearcher, ScientificStudy study, Rover rover, Coordinates fieldSite, 
			String description) {

		// Use RoverMission constructor.
		super(description, leadResearcher, MIN_PEOPLE, rover);

		setStartingSettlement(startingSettlement);
		this.study = study;
		this.leadResearcher = leadResearcher;
		this.fieldSite = fieldSite;
		addNavpoint(new NavPoint(fieldSite, "field research site"));

		// Set mission capacity.
		setMissionCapacity(getRover().getCrewCapacity());
		int availableSuitNum = Mission.getNumberAvailableEVASuitsAtSettlement(startingSettlement);
		if (availableSuitNum < getMissionCapacity()) setMissionCapacity(availableSuitNum);

		// Add mission members.
		Iterator<Person> i = members.iterator();
		while (i.hasNext()) i.next().getMind().setMission(this);

		// Add home settlement
		addNavpoint(new NavPoint(getStartingSettlement().getCoordinates(), 
				getStartingSettlement(), getStartingSettlement().getName()));

		// Add researching site phase.
		addPhase(RESEARCH_SITE);

		// Set initial mission phase.
		setPhase(VehicleMission.EMBARKING);
		setPhaseDescription(Msg.getString(
				"Mission.phase.embarking.description" //$NON-NLS-1$
				,getStartingSettlement().getName()));

		// Check if vehicle can carry enough supplies for the mission.
		if (hasVehicle() && !isVehicleLoadable()) 
			endMission("Vehicle is not loadable. (BiologyStudyFieldMission)");
	}

	/**
	 * Gets the scientific study for the mission.
	 * @return scientific study.
	 */
	public ScientificStudy getScientificStudy() {
		return study;
	}

	/**
	 * Gets the lead researcher for the mission. 
	 * @return the researcher.
	 */
	public Person getLeadResearcher() {
		return leadResearcher;
	}

	/**
	 * Determine the scientific study used for the mission.
	 * @param researcher the science researcher.
	 * @return scientific study or null if none determined.
	 */
	private ScientificStudy determineStudy(Person researcher) {
		ScientificStudy result = null;

		ScienceType biology = ScienceType.BIOLOGY;
		List<ScientificStudy> possibleStudies = new ArrayList<ScientificStudy>();

		// Add primary study if in research phase.
		ScientificStudyManager manager = Simulation.instance().getScientificStudyManager();
		ScientificStudy primaryStudy = manager.getOngoingPrimaryStudy(researcher);
		if (primaryStudy != null) {
			if (ScientificStudy.RESEARCH_PHASE.equals(primaryStudy.getPhase()) && 
					!primaryStudy.isPrimaryResearchCompleted()) {
				if (biology == primaryStudy.getScience()) {
					// Primary study added twice to double chance of random selection.
					possibleStudies.add(primaryStudy);
					possibleStudies.add(primaryStudy);
				}
			}
		}

		// Add all collaborative studies in research phase.
		Iterator<ScientificStudy> i = manager.getOngoingCollaborativeStudies(researcher).iterator();
		while (i.hasNext()) {
			ScientificStudy collabStudy = i.next();
			if (ScientificStudy.RESEARCH_PHASE.equals(collabStudy.getPhase()) && 
					!collabStudy.isCollaborativeResearchCompleted(researcher)) {
				if (biology == collabStudy.getCollaborativeResearchers().get(researcher))
					possibleStudies.add(collabStudy);
			}
		}

		// Randomly select study.
		if (possibleStudies.size() > 0) {
			int selected = RandomUtil.getRandomInt(possibleStudies.size() - 1);
			result = possibleStudies.get(selected);
		}

		return result;
	}

	/**
	 * Gets the time limit of the trip based on life support capacity.
	 * @param useBuffer use time buffer in estimation if true.
	 * @return time (millisols) limit.
	 * @throws MissionException if error determining time limit.
	 */
	public static double getTotalTripTimeLimit(Rover rover, int memberNum, boolean useBuffer) {

		Inventory vInv = rover.getInventory();

		double timeLimit = Double.MAX_VALUE;

		PersonConfig config = SimulationConfig.instance().getPersonConfiguration();

		// Check food capacity as time limit.
		AmountResource food = AmountResource.findAmountResource(LifeSupport.FOOD);
		double foodConsumptionRate = config.getFoodConsumptionRate();
		double foodCapacity = vInv.getAmountResourceCapacity(food, false);
		double foodTimeLimit = foodCapacity / (foodConsumptionRate * memberNum);
		if (foodTimeLimit < timeLimit) timeLimit = foodTimeLimit;

        // 2015-01-04 Added Soymilk
        // Check dessert1 capacity as time limit.
        AmountResource dessert1 = AmountResource.findAmountResource("Soymilk");
        double dessert1ConsumptionRate = config.getFoodConsumptionRate() / 6D;
        double dessert1Capacity = vInv.getAmountResourceCapacity(dessert1, false);
        double dessert1TimeLimit = dessert1Capacity / (dessert1ConsumptionRate * memberNum);
        if (dessert1TimeLimit < timeLimit)
            timeLimit = dessert1TimeLimit;
        
		
		// Check water capacity as time limit.
		AmountResource water = AmountResource.findAmountResource(LifeSupport.WATER);
		double waterConsumptionRate = config.getWaterConsumptionRate();
		double waterCapacity = vInv.getAmountResourceCapacity(water, false);
		double waterTimeLimit = waterCapacity / (waterConsumptionRate * memberNum);
		if (waterTimeLimit < timeLimit) timeLimit = waterTimeLimit;

		// Check oxygen capacity as time limit.
		AmountResource oxygen = AmountResource.findAmountResource(LifeSupport.OXYGEN);
		double oxygenConsumptionRate = config.getOxygenConsumptionRate();
		double oxygenCapacity = vInv.getAmountResourceCapacity(oxygen, false);
		double oxygenTimeLimit = oxygenCapacity / (oxygenConsumptionRate * memberNum);
		if (oxygenTimeLimit < timeLimit) timeLimit = oxygenTimeLimit;

		// Convert timeLimit into millisols and use error margin.
		timeLimit = (timeLimit * 1000D);
		if (useBuffer) timeLimit /= Rover.LIFE_SUPPORT_RANGE_ERROR_MARGIN;

		return timeLimit;
	}

	/** 
	 * Determine the location of the research site.
	 * @param roverRange the rover's driving range
	 * @param tripTimeLimit the time limit (millisols) of the trip.
	 * @throws MissionException of site can not be determined.
	 */
	private void determineFieldSite(double roverRange, double tripTimeLimit) 
	{

		// Determining the actual traveling range.
		double range = roverRange;
		double timeRange = getTripTimeRange(tripTimeLimit, true);
		if (timeRange < range) range = timeRange;

		//        try {
		// Get the current location.
		Coordinates startingLocation = getCurrentMissionLocation();

		// Determine the research site.
		Direction direction = new Direction(RandomUtil.getRandomDouble(2 * Math.PI));
		double limit = range / 4D;
		double siteDistance = RandomUtil.getRandomDouble(limit);
		fieldSite = startingLocation.getNewLocation(direction, siteDistance);
		addNavpoint(new NavPoint(fieldSite, "field research site"));
		//        }
		//        catch (Exception e) {
		//            throw new MissionException(getPhase(), e);
		//        }
	}

	/**
	 * Gets the range of a trip based on its time limit.
	 * @param tripTimeLimit time (millisols) limit of trip.
	 * @param useBuffer Use time buffer in estimations if true.
	 * @return range (km) limit.
	 */
	private double getTripTimeRange(double tripTimeLimit, boolean useBuffer) {
		double timeAtSite = FIELD_SITE_TIME;
		double tripTimeTravelingLimit = tripTimeLimit - timeAtSite;
		double averageSpeed = getAverageVehicleSpeedForOperators();
		double millisolsInHour = MarsClock.convertSecondsToMillisols(60D * 60D);
		double averageSpeedMillisol = averageSpeed / millisolsInHour;
		return tripTimeTravelingLimit * averageSpeedMillisol;
	}

	@Override
	protected void recruitPeopleForMission(Person startingPerson) {
		super.recruitPeopleForMission(startingPerson);

		// Make sure there is at least one person left at the starting settlement.
		if (!atLeastOnePersonRemainingAtSettlement(getStartingSettlement(), startingPerson)) {
			// Remove last person added to the mission.
			Person lastPerson = (Person) getPeople().toArray()[getPeopleNumber() - 1];
			if (lastPerson != null) {
				lastPerson.getMind().setMission(null);
				if (getPeopleNumber() < getMinPeople()) endMission("Not enough members.");
			}
		}
	}

	@Override
	protected double getMissionQualification(Person person) {  
		double result = 0D;

		if (isCapableOfMission(person)) {

			// Get base result for job modifier.
			Job job = person.getMind().getJob();
			if (job != null) result = job.getJoinMissionProbabilityModifier(this.getClass());

			// Add modifier if person is a researcher on the same scientific study.
			ScienceType biology = ScienceType.BIOLOGY;
			if (study != null) {
				if (person == study.getPrimaryResearcher()) {
					result += 2D;

					// Check if study's primary science is biology.
					if (biology == study.getScience()) result += 1D;
				}
				else if (study.getCollaborativeResearchers().containsKey(person)) {
					result += 1D;

					// Check if study collaboration science is in biology.
					ScienceType collabScience = study.getCollaborativeResearchers().get(person);
					if (biology == collabScience) result += 1D;
				}
			}
		}

		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map<Class, Integer> getEquipmentNeededForRemainingMission(
			boolean useBuffer) {
		if (equipmentNeededCache != null) return equipmentNeededCache;
		else {
			Map<Class, Integer> result = new HashMap<Class, Integer>();      
			equipmentNeededCache = result;
			return result;
		}
	}

	@Override
	public Settlement getAssociatedSettlement() {
		return getStartingSettlement();
	}

	@Override
	protected void determineNewPhase() {
		if (EMBARKING.equals(getPhase())) {
			startTravelToNextNode();
			setPhase(VehicleMission.TRAVELLING);
			setPhaseDescription(Msg.getString(
					"Mission.phase.travelling.description" //$NON-NLS-1$
					,getNextNavpoint().getDescription()));
		}
		else if (TRAVELLING.equals(getPhase())) {
			if (getCurrentNavpoint().isSettlementAtNavpoint()) {
				setPhase(VehicleMission.DISEMBARKING);
				setPhaseDescription(Msg.getString(
						"Mission.phase.disembarking.description" //$NON-NLS-1$
						,getCurrentNavpoint().getSettlement().getName()));
			}
			else {
				setPhase(RESEARCH_SITE);
				setPhaseDescription(Msg.getString(
						"Mission.phase.researchingFieldSite.description" //$NON-NLS-1$
						,getCurrentNavpoint().getDescription()));
			}
		}
		else if (RESEARCH_SITE.equals(getPhase())) {
			startTravelToNextNode();
			setPhase(VehicleMission.TRAVELLING);
			setPhaseDescription(Msg.getString(
					"Mission.phase.travelling.description" //$NON-NLS-1$
					,getNextNavpoint().getDescription()));
		}
		else if (DISEMBARKING.equals(getPhase())) endMission("Successfully disembarked.");
	}

	@Override
	protected void performPhase(Person person) {
		super.performPhase(person);
		if (RESEARCH_SITE.equals(getPhase())) researchFieldSitePhase(person);
	}

	/**
	 * Ends the research at a field site.
	 */
	public void endResearchAtFieldSite() {
		logger.info("Research field site phase ended due to external trigger.");
		endFieldSite = true;

		// End each member's biology field work task.
		Iterator<Person> i = getPeople().iterator();
		while (i.hasNext()) {
			Task task = i.next().getMind().getTaskManager().getTask();
			if (task instanceof BiologyStudyFieldWork) ((BiologyStudyFieldWork) task).endEVA();
		}
	}

	/** 
	 * Performs the research field site phase of the mission.
	 * @param person the person currently performing the mission
	 * @throws MissionException if problem performing phase.
	 */
	private void researchFieldSitePhase(Person person) {

		// Check if field site research has just started.
		if (fieldSiteStartTime == null) {
			fieldSiteStartTime = (MarsClock) Simulation.instance().getMasterClock().
					getMarsClock().clone();
		}

		// Check if crew has been at site for more than required length of time.
		boolean timeExpired = false;
		MarsClock currentTime = (MarsClock) Simulation.instance().getMasterClock().getMarsClock().clone();
		if (MarsClock.getTimeDiff(currentTime, fieldSiteStartTime) >= FIELD_SITE_TIME) 
			timeExpired = true;

		if (isEveryoneInRover()) {

			// Check if end field site flag is set.
			if (endFieldSite) {
				endFieldSite = false;
				setPhaseEnded(true);
			}

			// Check if crew has been at site for more than required length of time, then end this phase.
			if (timeExpired) setPhaseEnded(true);

			// Determine if no one can start the field work task.
			boolean nobodyFieldWork = true;
			Iterator<Person> j = getPeople().iterator();
			while (j.hasNext()) {
				if (BiologyStudyFieldWork.canResearchSite(j.next(), getRover())) nobodyFieldWork = false;
			}

			// If no one can research the site and this is not due to it just being
			// night time, end the field work phase.
			//            try {
			Mars mars = Simulation.instance().getMars();
			boolean inDarkPolarRegion = mars.getSurfaceFeatures().inDarkPolarRegion(
					getCurrentMissionLocation());
			double sunlight = mars.getSurfaceFeatures().getSurfaceSunlight(getCurrentMissionLocation());
			if (nobodyFieldWork && ((sunlight > 0D) || inDarkPolarRegion)) setPhaseEnded(true);
			//            }
			//            catch (Exception e) {
			//                throw new MissionException(getPhase(), e);
			//            }

			// Anyone in the crew or a single person at the home settlement has a dangerous illness, end phase.
			if (hasEmergency()) setPhaseEnded(true);

			//            try {
			// Check if enough resources for remaining trip.
			if (!hasEnoughResourcesForRemainingMission(false)) {
				// If not, determine an emergency destination.
				determineEmergencyDestination(person);
				setPhaseEnded(true);
			}
			//            }
			//            catch (Exception e) {
			//                throw new MissionException(getPhase(), e.getMessage());
			//            }
		}
		else {
			// If research time has expired for the site, have everyone end their field work tasks.
			if (timeExpired) {
				Iterator<Person> i = getPeople().iterator();
				while (i.hasNext()) {
					Task task = i.next().getMind().getTaskManager().getTask();
					if ((task != null) && (task instanceof BiologyStudyFieldWork))
						((BiologyStudyFieldWork) task).endEVA();
				}
			}
		}

		if (!getPhaseEnded()) {

			if (!endFieldSite && !timeExpired) {
				// If person can research the site, start that task.
				if (BiologyStudyFieldWork.canResearchSite(person, getRover())) {
					//                    try {
					assignTask(person, new BiologyStudyFieldWork(person, leadResearcher, study, 
							(Rover) getVehicle()));
					//                    }
					//                    catch(Exception e) {
					//                        throw new MissionException(getPhase(), e);
					//                    }
				}
			}
		}
	}

	@Override
	protected boolean isCapableOfMission(Person person) {
		if (super.isCapableOfMission(person)) {
			if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
				if (person.getSettlement() == getStartingSettlement()) return true;
			}
		}
		return false;
	}

	@Override
	public double getEstimatedRemainingMissionTime(boolean useBuffer) {
		double result = super.getEstimatedRemainingMissionTime(useBuffer);
		result += getEstimatedRemainingFieldSiteTime();
		return result;
	}

	/**
	 * Gets the estimated time remaining for the field site in the mission.
	 * @return time (millisols)
	 * @throws MissionException if error estimating time.
	 */
	private double getEstimatedRemainingFieldSiteTime() {
		double result = 0D;

		// Add estimated remaining field work time at field site if still there.
		if (RESEARCH_SITE.equals(getPhase())) {
			MarsClock currentTime = Simulation.instance().getMasterClock().getMarsClock();
			double timeSpentAtExplorationSite = MarsClock.getTimeDiff(currentTime, fieldSiteStartTime);
			double remainingTime = FIELD_SITE_TIME - timeSpentAtExplorationSite;
			if (remainingTime > 0D) result += remainingTime;
		}

		// If field site hasn't been visited yet, add full field work time.
		if (fieldSiteStartTime == null) result += FIELD_SITE_TIME;

		return result;
	}

	@Override
	public Map<Resource, Number> getResourcesNeededForRemainingMission(boolean useBuffer) {
		Map<Resource, Number> result = super.getResourcesNeededForRemainingMission(useBuffer);

		double fieldSiteTime = getEstimatedRemainingFieldSiteTime();
		double timeSols = fieldSiteTime / 1000D;

		int crewNum = getPeopleNumber();

		// Determine life support supplies needed for trip.
		AmountResource oxygen = AmountResource.findAmountResource(LifeSupport.OXYGEN);
		double oxygenAmount = PhysicalCondition.getOxygenConsumptionRate() * timeSols * crewNum;
		if (result.containsKey(oxygen)) 
			oxygenAmount += (Double) result.get(oxygen);
		result.put(oxygen, oxygenAmount);

		AmountResource water = AmountResource.findAmountResource(LifeSupport.WATER);
		double waterAmount = PhysicalCondition.getWaterConsumptionRate() * timeSols * crewNum;
		if (result.containsKey(water)) 
			waterAmount += (Double) result.get(water);
		result.put(water, waterAmount);

		AmountResource food = AmountResource.findAmountResource(LifeSupport.FOOD);
		double foodAmount = PhysicalCondition.getFoodConsumptionRate() * timeSols * crewNum;
		if (result.containsKey(food)) 
			foodAmount += (Double) result.get(food);
		result.put(food, foodAmount);

	       // 2015-01-04 Added Soymilk
	     AmountResource dessert1 = AmountResource.findAmountResource("Soymilk");
	     double dessert1Amount = PhysicalCondition.getFoodConsumptionRate() / 6D * timeSols * crewNum;
	     if (result.containsKey(dessert1))
	         dessert1Amount += (Double) result.get(dessert1);
	     result.put(dessert1, dessert1Amount);
	     
		return result;
	}

	@Override
	public void destroy() {
		super.destroy();

		fieldSiteStartTime = null;
		fieldSite = null;
		study = null;
		leadResearcher = null;
	}
}