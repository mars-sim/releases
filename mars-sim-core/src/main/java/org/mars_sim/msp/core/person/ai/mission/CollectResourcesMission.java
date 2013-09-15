/**
 * Mars Simulation Project
 * CollectResourcesMission.java
 * @version 3.05 2013-08-19
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.mission;

import org.mars_sim.msp.core.*;
import org.mars_sim.msp.core.equipment.EVASuit;
import org.mars_sim.msp.core.equipment.EquipmentFactory;
import org.mars_sim.msp.core.mars.Mars;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.task.CollectResources;
import org.mars_sim.msp.core.person.ai.task.EVAOperation;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.resource.Resource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.vehicle.Rover;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

/**
 * The CollectResourcesMission class is a mission to travel in a rover to several random locations around a settlement
 * and collect resources of a given type.
 */
public abstract class CollectResourcesMission extends RoverMission implements
        Serializable {

    private static Logger logger = Logger.getLogger(CollectResourcesMission.class.getName());

    // Mission phases
    final public static String COLLECT_RESOURCES = "Collecting Resources";

    // Estimated collection time multiplyer for EVA.
    final public static double EVA_COLLECTION_OVERHEAD = 20D;

    // Data members
    private AmountResource resourceType; // The type of resource to collect.
    private double siteCollectedResources; // The amount of resources (kg) collected at a collection site.
    private double collectingStart; // The starting amount of resources in a rover at a collection site.
    private double siteResourceGoal; // The goal amount of resources to collect at a site (kg).
    private double resourceCollectionRate; // The resource collection rate for a person (kg/millisol).
    private Class containerType; // The type of container needed for the mission or null if none.
    private int containerNum; // The number of containers needed for the mission.
    private MarsClock collectionSiteStartTime; // The start time at the current collection site.
    private boolean endCollectingSite; // External flag for ending collection at the current site.
    private double totalResourceCollected; // The total amount (kg) of resource collected.

    /**
     * Constructor
     * @param missionName The name of the mission.
     * @param startingPerson The person starting the mission.
     * @param resourceType The type of resource.
     * @param siteResourceGoal The goal amount of resources to collect at a site (kg) (or 0 if none).
     * @param resourceCollectionRate The resource collection rate for a person (kg/millisol).
     * @param containerType The type of container needed for the mission or null if none.
     * @param containerNum The number of containers needed for the mission.
     * @param numSites The number of collection sites.
     * @param minPeople The mimimum number of people for the mission.
     * @throws MissionException if problem constructing mission.
     */
    CollectResourcesMission(String missionName, Person startingPerson,
            AmountResource resourceType, double siteResourceGoal,
            double resourceCollectionRate, Class containerType,
            int containerNum, int numSites, int minPeople) {

        // Use RoverMission constructor
        super(missionName, startingPerson, minPeople);

        if (!isDone()) {

            // Set mission capacity.
            if (hasVehicle())
                setMissionCapacity(getRover().getCrewCapacity());
            int availableSuitNum = Mission
                    .getNumberAvailableEVASuitsAtSettlement(startingPerson
                    .getSettlement());
            if (availableSuitNum < getMissionCapacity())
                setMissionCapacity(availableSuitNum);

            // Initialize data members.
            setStartingSettlement(startingPerson.getSettlement());
            this.resourceType = resourceType;
            this.siteResourceGoal = siteResourceGoal;
            this.resourceCollectionRate = resourceCollectionRate;
            this.containerType = containerType;
            this.containerNum = containerNum;

            // Recruit additional people to mission.
            recruitPeopleForMission(startingPerson);

            // Determine collection sites
            if (hasVehicle()) determineCollectionSites(getVehicle().getRange(),
                    getTotalTripTimeLimit(getRover(), getPeopleNumber(),
                    true), numSites);

            // Add home settlement
            addNavpoint(new NavPoint(getStartingSettlement().getCoordinates(),
                    getStartingSettlement(), getStartingSettlement().getName()));

            // Check if vehicle can carry enough supplies for the mission.
            if (hasVehicle() && !isVehicleLoadable()) {
                endMission("Vehicle is not loadable. (CollectingResourcesMission)");
            }
        }

        // Add collecting phase.
        addPhase(COLLECT_RESOURCES);

        // Set initial mission phase.
        setPhase(VehicleMission.EMBARKING);
        setPhaseDescription("Embarking from " + getStartingSettlement().getName());

        // int emptyContainers = numCollectingContainersAvailable(getStartingSettlement(), containerType);
        // logger.info("Starting " + getName() + " with " + emptyContainers + " " + containerType);
    }

    /**
     * Constructor with explicit data
     * @param missionName The name of the mission.
     * @param members collection of mission members.
     * @param startingSettlement the starting settlement.
     * @param resourceType The type of resource.
     * @param siteResourceGoal The goal amount of resources to collect at a site (kg) (or 0 if none).
     * @param resourceCollectionRate The resource collection rate for a person (kg/millisol).
     * @param containerType The type of container needed for the mission or null if none.
     * @param containerNum The number of containers needed for the mission.
     * @param numSites The number of collection sites.
     * @param minPeople The mimimum number of people for the mission.
     * @param rover the rover to use.
     * @param iceCollectionSites the sites to collect ice.
     * @throws MissionException if problem constructing mission.
     */
    CollectResourcesMission(String missionName, Collection<Person> members,
            Settlement startingSettlement, AmountResource resourceType,
            double siteResourceGoal, double resourceCollectionRate,
            Class containerType, int containerNum, int numSites, int minPeople,
            Rover rover, List<Coordinates> collectionSites) {

        // Use RoverMission constructor
        super(missionName, (Person) members.toArray()[0], minPeople, rover);

        setStartingSettlement(startingSettlement);

        // Set mission capacity.
        setMissionCapacity(getRover().getCrewCapacity());
        int availableSuitNum = Mission
                .getNumberAvailableEVASuitsAtSettlement(startingSettlement);
        if (availableSuitNum < getMissionCapacity())
            setMissionCapacity(availableSuitNum);

        this.resourceType = resourceType;
        this.siteResourceGoal = siteResourceGoal;
        this.resourceCollectionRate = resourceCollectionRate;
        this.containerType = containerType;
        this.containerNum = containerNum;

        // Set collection navpoints.
        for (int x = 0; x < collectionSites.size(); x++)
            addNavpoint(new NavPoint(collectionSites.get(x),
                    getCollectionSiteDescription(x + 1)));

        // Add home navpoint.
        addNavpoint(new NavPoint(startingSettlement.getCoordinates(),
                startingSettlement, startingSettlement.getName()));

        // Add mission members.
        Iterator<Person> i = members.iterator();
        while (i.hasNext())
            i.next().getMind().setMission(this);

        // Add collecting phase.
        addPhase(COLLECT_RESOURCES);

        // Set initial mission phase.
        setPhase(VehicleMission.EMBARKING);
        setPhaseDescription("Embarking from "
                + getStartingSettlement().getName());

        // Check if vehicle can carry enough supplies for the mission.
        if (hasVehicle() && !isVehicleLoadable())
            endMission("Vehicle is not loadable. (CollectingResourcesMission)");
    }

    /**
     * Gets the weighted probability that a given person would start this mission.
     * @param person the given person
     * @param containerType = the required container class.
     * @param containerNum = the number of containers required.
     * @param minPeople = the minimum number of people required.
     * @param missionType the mission class.
     * @return the weighted probability
     */
    public static double getNewMissionProbability(Person person,
            Class containerType, int containerNum, int minPeople,
            Class missionType) {
        double result = 0D;

        if (person.getLocationSituation().equals(Person.INSETTLEMENT)) {
            Settlement settlement = person.getSettlement();

            // Check if a mission-capable rover is available.
            boolean reservableRover = areVehiclesAvailable(settlement, false);

            // Check if available backup rover.
            boolean backupRover = hasBackupRover(settlement);

            // Check if minimum number of people are available at the settlement.
            // Plus one to hold down the fort.
            boolean minNum = minAvailablePeopleAtSettlement(settlement,
                    (minPeople + 1));

            // Check if there are enough specimen containers at the settlement for collecting rock samples.
            boolean enoughContainers = (numCollectingContainersAvailable(
                    settlement, containerType) >= containerNum);

            // Check for embarking missions.
            boolean embarkingMissions = VehicleMission
                    .hasEmbarkingMissions(settlement);

            // Check if settlement has enough basic resources for a rover mission.
            boolean hasBasicResources = RoverMission
                    .hasEnoughBasicResources(settlement);
            
            // Check if starting settlement has minimum amount of methane fuel.
            AmountResource methane = AmountResource.findAmountResource("methane");
            boolean enoughMethane = settlement.getInventory().getAmountResourceStored(methane, false) >= 
                    RoverMission.MIN_STARTING_SETTLEMENT_METHANE;

            if (reservableRover && backupRover && minNum && enoughContainers
                    && !embarkingMissions && hasBasicResources && enoughMethane)
                result = 5D;

            // Crowding modifier
            int crowding = settlement.getCurrentPopulationNum()
                    - settlement.getPopulationCapacity();
            if (crowding > 0)
                result *= (crowding + 1);

            // Job modifier.
            Job job = person.getMind().getJob();
            if (job != null)
                result *= job.getStartMissionProbabilityModifier(missionType);
        }

        return result;
    }
    
    /**
     * Gets the total amount of resources collected so far in the mission.
     * @return resource amount (kg).
     */
    public double getTotalCollectedResources() {
        return totalResourceCollected;
    }

    /**
     * Determines a new phase for the mission when the current phase has ended.
     * @throws MissionException if problem setting a new phase.
     */
    protected void determineNewPhase() {
        if (EMBARKING.equals(getPhase())) {
            startTravelToNextNode();
            setPhase(VehicleMission.TRAVELLING);
            setPhaseDescription("Driving to "
                    + getNextNavpoint().getDescription());
        } else if (TRAVELLING.equals(getPhase())) {
            if (getCurrentNavpoint().isSettlementAtNavpoint()) {
                setPhase(VehicleMission.DISEMBARKING);
                setPhaseDescription("Disembarking at "
                        + getCurrentNavpoint().getSettlement().getName());
            } else {
                setPhase(COLLECT_RESOURCES);
                setPhaseDescription("Collecting resources at "
                        + getCurrentNavpoint().getDescription());
                collectionSiteStartTime = (MarsClock) Simulation.instance()
                        .getMasterClock().getMarsClock().clone();
            }
        } else if (COLLECT_RESOURCES.equals(getPhase())) {
            startTravelToNextNode();
            setPhase(VehicleMission.TRAVELLING);
            setPhaseDescription("Driving to "
                    + getNextNavpoint().getDescription());
        } else if (DISEMBARKING.equals(getPhase()))
            endMission("Successfully disembarked.");
    }

    /**
     * The person performs the current phase of the mission.
     * @param person the person performing the phase.
     * @throws MissionException if problem performing the phase.
     */
    protected void performPhase(Person person) {
        super.performPhase(person);
        if (COLLECT_RESOURCES.equals(getPhase()))
            collectingPhase(person);
    }

    public void endCollectingAtSite() {
        logger.info("Collecting phase ended due to external trigger.");
        endCollectingSite = true;

        // End each member's collection task.
        Iterator<Person> i = getPeople().iterator();
        while (i.hasNext()) {
            Task task = i.next().getMind().getTaskManager().getTask();
            if (task instanceof CollectResources)
                ((CollectResources) task).endEVA();
        }
    }

    /**
     * Performs the collecting phase of the mission.
     * @param person the person currently performing the mission
     * @throws MissionException if problem performing collecting phase.
     */
    private void collectingPhase(Person person) {
        Inventory inv = getRover().getInventory();
        double resourcesCollected = inv.getAmountResourceStored(resourceType, false);
        double resourcesCapacity = inv.getAmountResourceCapacity(resourceType, false);

        // Set total collected resources.
        totalResourceCollected = resourcesCollected;
        
        // Calculate resources collected at the site so far.
        siteCollectedResources = resourcesCollected - collectingStart;

        if (isEveryoneInRover()) {

            // Check if end collecting flag is set.
            if (endCollectingSite) {
                endCollectingSite = false;
                setPhaseEnded(true);
            }

            // Check if rover capacity for resources is met, then end this phase.
            if (resourcesCollected >= resourcesCapacity)
                setPhaseEnded(true);

            // If collected resources are sufficient for this site, end the collecting phase.
            if (siteCollectedResources >= siteResourceGoal)
                setPhaseEnded(true);

            // Determine if no one can start the collect resources task.
            boolean nobodyCollect = true;
            Iterator<Person> j = getPeople().iterator();
            while (j.hasNext()) {
                if (CollectResources.canCollectResources(j.next(), getRover(),
                        containerType, resourceType))
                    nobodyCollect = false;
            }

            // If no one can collect resources and this is not due to it just being
            // night time, end the collecting phase.
            Mars mars = Simulation.instance().getMars();
            boolean inDarkPolarRegion = mars.getSurfaceFeatures()
                    .inDarkPolarRegion(getCurrentMissionLocation());
            double sunlight = mars.getSurfaceFeatures().getSurfaceSunlight(
                    getCurrentMissionLocation());
            if (nobodyCollect && ((sunlight > 0D) || inDarkPolarRegion))
                setPhaseEnded(true);

            // Anyone in the crew or a single person at the home settlement has a dangerous illness, end phase.
            if (hasEmergency())
                setPhaseEnded(true);

            // Check if enough resources for remaining trip.
            if (!hasEnoughResourcesForRemainingMission(false)) {
                // If not, determine an emergency destination.
                determineEmergencyDestination(person);
                setPhaseEnded(true);
            }
        }

        if (!getPhaseEnded()) {
            if ((siteCollectedResources < siteResourceGoal)
                    && !endCollectingSite) {
                // If person can collect resources, start him/her on that task.
                if (CollectResources.canCollectResources(person, getRover(),
                        containerType, resourceType)) {
                    CollectResources collectResources = new CollectResources(
                            "Collecting Resources", person, getRover(),
                            resourceType, resourceCollectionRate,
                            siteResourceGoal - siteCollectedResources, inv
                                    .getAmountResourceStored(resourceType, false),
                            containerType);
                    assignTask(person, collectResources);
                }
            }
        } else {
            // If the rover is full of resources, head home.
            if (siteCollectedResources >= resourcesCapacity) {
                setNextNavpointIndex(getNumberOfNavpoints() - 2);
                updateTravelDestination();
                siteCollectedResources = 0D;
            }
        }
    }

    /**
     * Determine the locations of the sample collection sites.
     * @param roverRange the rover's driving range
     * @param numSites the number of collection sites
     * @throws MissionException of collection sites can not be determined.
     */
    private void determineCollectionSites(double roverRange,
            double tripTimeLimit, int numSites) {

        List<Coordinates> unorderedSites = new ArrayList<Coordinates>();

        // Determining the actual travelling range.
        double range = roverRange;
        double timeRange = getTripTimeRange(tripTimeLimit, numSites, true);
        if (timeRange < range)
            range = timeRange;

        // Get the current location.
        Coordinates startingLocation = getCurrentMissionLocation();

        // Determine the first collection site.
        Direction direction = new Direction(RandomUtil
                .getRandomDouble(2 * Math.PI));
        double limit = range / 4D;
        double siteDistance = RandomUtil.getRandomDouble(limit);
        Coordinates newLocation = startingLocation.getNewLocation(direction,
                siteDistance);
        unorderedSites.add(newLocation);
        Coordinates currentLocation = newLocation;

        // Determine remaining collection sites.
        double remainingRange = (range / 2D) - siteDistance;
        for (int x = 1; x < numSites; x++) {
            double currentDistanceToSettlement = currentLocation
                    .getDistance(startingLocation);
            if (remainingRange > currentDistanceToSettlement) {
                direction = new Direction(RandomUtil
                        .getRandomDouble(2D * Math.PI));
                double tempLimit1 = Math.pow(remainingRange, 2D)
                        - Math.pow(currentDistanceToSettlement, 2D);
                double tempLimit2 = (2D * remainingRange)
                        - (2D * currentDistanceToSettlement * direction
                                .getCosDirection());
                limit = tempLimit1 / tempLimit2;
                siteDistance = RandomUtil.getRandomDouble(limit);
                newLocation = currentLocation.getNewLocation(direction,
                        siteDistance);
                unorderedSites.add(newLocation);
                currentLocation = newLocation;
                remainingRange -= siteDistance;
            }
        }

        // Reorder sites for shortest distance.
        int collectionSiteNum = 1;
        currentLocation = startingLocation;
        while (unorderedSites.size() > 0) {
            Coordinates shortest = unorderedSites.get(0);
            Iterator<Coordinates> i = unorderedSites.iterator();
            while (i.hasNext()) {
                Coordinates site = i.next();
                if (currentLocation.getDistance(site) < currentLocation
                        .getDistance(shortest))
                    shortest = site;
            }
            addNavpoint(new NavPoint(shortest,
                    getCollectionSiteDescription(collectionSiteNum)));
            unorderedSites.remove(shortest);
            currentLocation = shortest;
            collectionSiteNum++;
        }
    }

    /**
     * Gets the range of a trip based on its time limit and collection sites.
     * @param tripTimeLimit time (millisols) limit of trip.
     * @param numSites the number of collection sites.
     * @param useBuffer Use time buffer in estimations if true.
     * @return range (km) limit.
     */
    private double getTripTimeRange(double tripTimeLimit, int numSites,
            boolean useBuffer) {
        double timeAtSites = getEstimatedTimeAtCollectionSite(useBuffer)
                * numSites;
        double tripTimeTravellingLimit = tripTimeLimit - timeAtSites;
        double averageSpeed = getAverageVehicleSpeedForOperators();
        double millisolsInHour = MarsClock.convertSecondsToMillisols(60D * 60D);
        double averageSpeedMillisol = averageSpeed / millisolsInHour;
        return tripTimeTravellingLimit * averageSpeedMillisol;
    }

    /**
     * Gets the settlement associated with the mission.
     * @return settlement or null if none.
     */
    public Settlement getAssociatedSettlement() {
        return getStartingSettlement();
    }

    /**
     * Checks to see if a person is capable of joining a mission.
     * @param person the person to check.
     * @return true if person could join mission.
     */
    protected boolean isCapableOfMission(Person person) {
        if (super.isCapableOfMission(person)) {
            if (person.getLocationSituation().equals(Person.INSETTLEMENT)) {
                if (person.getSettlement() == getStartingSettlement())
                    return true;
            }
        }
        return false;
    }

    /**
     * Recruits new people into the mission.
     * @param startingPerson the person starting the mission.
     */
    protected void recruitPeopleForMission(Person startingPerson) {
        super.recruitPeopleForMission(startingPerson);

        // Make sure there is at least one person left at the starting settlement.
        if (!atLeastOnePersonRemainingAtSettlement(getStartingSettlement(),
                startingPerson)) {
            // Remove last person added to the mission.
            Object[] array = getPeople().toArray();
            int amount = getPeopleNumber() - 1;
            Person lastPerson = null;

            if (amount >= 0 && amount < array.length) {
                lastPerson = (Person) array[amount];
            }

            if (lastPerson != null) {
                lastPerson.getMind().setMission(null);
                if (getPeopleNumber() < getMinPeople())
                    endMission("Not enough members.");
            }
        }
    }

    /**
     * Gets the number of empty containers of given type at the settlement.
     * @param settlement the settlement
     * @param containerType the type of container
     * @return number of empty containers.
     * @throws MissionException if error determining number.
     */
    protected static int numCollectingContainersAvailable(
            Settlement settlement, Class containerType) {
        return settlement.getInventory()
                .findNumEmptyUnitsOfClass(containerType, false);
    }

    /**
     * Gets the estimated time remaining for the mission.
     * @param useBuffer use time buffer in estimations if true.
     * @return time (millisols)
     * @throws MissionException
     */
    public double getEstimatedRemainingMissionTime(boolean useBuffer) {
        double result = super.getEstimatedRemainingMissionTime(useBuffer);

        result += getEstimatedRemainingCollectionSiteTime(useBuffer);

        return result;
    }

    /**
     * Gets the estimated time remaining for collection sites in the mission.
     * @param useBuffer use time buffer in estimations if true.
     * @return time (millisols)
     * @throws MissionException if error estimating time.
     */
    private double getEstimatedRemainingCollectionSiteTime(boolean useBuffer) {
        double result = 0D;

        // Add estimated remaining collection time at current site if still there.
        if (COLLECT_RESOURCES.equals(getPhase())) {
            MarsClock currentTime = Simulation.instance().getMasterClock()
                    .getMarsClock();
            double timeSpentAtCollectionSite = MarsClock.getTimeDiff(
                    currentTime, collectionSiteStartTime);
            double remainingTime = getEstimatedTimeAtCollectionSite(useBuffer)
                    - timeSpentAtCollectionSite;
            if (remainingTime > 0D)
                result += remainingTime;
        }

        // Add estimated collection time at sites that haven't been visited yet.
        int remainingCollectionSites = getNumCollectionSites()
                - getNumCollectionSitesVisited();
        result += getEstimatedTimeAtCollectionSite(useBuffer)
                * remainingCollectionSites;

        return result;
    }

    @Override
    public Map<Resource, Number> getResourcesNeededForRemainingMission(
            boolean useBuffer) {
        Map<Resource, Number> result = super.getResourcesNeededForRemainingMission(useBuffer);

        double collectionSitesTime = getEstimatedRemainingCollectionSiteTime(useBuffer);
        double timeSols = collectionSitesTime / 1000D;

        int crewNum = getPeopleNumber();

        // Determine life support supplies needed for trip.
        AmountResource oxygen = AmountResource.findAmountResource("oxygen");
        double oxygenAmount = PhysicalCondition.getOxygenConsumptionRate()
                * timeSols * crewNum;
        if (result.containsKey(oxygen))
            oxygenAmount += (Double) result.get(oxygen);
        result.put(oxygen, oxygenAmount);

        AmountResource water = AmountResource.findAmountResource("water");
        double waterAmount = PhysicalCondition.getWaterConsumptionRate()
                * timeSols * crewNum;
        if (result.containsKey(water))
            waterAmount += (Double) result.get(water);
        result.put(water, waterAmount);

        AmountResource food = AmountResource.findAmountResource("food");
        double foodAmount = PhysicalCondition.getFoodConsumptionRate()
                * timeSols * crewNum;
        if (result.containsKey(food))
            foodAmount += (Double) result.get(food);
        result.put(food, foodAmount);

        return result;
    }

    @Override
    protected Map<Resource, Number> getPartsNeededForTrip(double distance) {
        Map<Resource, Number> result = super.getPartsNeededForTrip(distance);

        // Determine repair parts for EVA Suits.
        double evaTime = getEstimatedRemainingCollectionSiteTime(false);
        double numberAccidents = evaTime * getPeopleNumber()
                * EVAOperation.BASE_ACCIDENT_CHANCE;

        // Average number malfunctions per accident is two.
        double numberMalfunctions = numberAccidents * 2D;

        // Get temporary EVA suit.
        EVASuit suit = (EVASuit) EquipmentFactory.getEquipment(EVASuit.class,
                new Coordinates(0, 0), true);

        // Determine needed repair parts for EVA suits.
        Map<Part, Double> parts = suit.getMalfunctionManager()
                .getRepairPartProbabilities();
        Iterator<Part> i = parts.keySet().iterator();
        while (i.hasNext()) {
            Part part = i.next();
            int number = (int) Math.round(parts.get(part) * numberMalfunctions);
            if (number > 0) {
                if (result.containsKey(part))
                    number += result.get(part).intValue();
                result.put(part, number);
            }
        }

        return result;
    }

    /**
     * Gets the total number of collection sites for this mission.
     * @return number of sites.
     */
    public final int getNumCollectionSites() {
        return getNumberOfNavpoints() - 2;
    }

    /**
     * Gets the number of collection sites that have been currently visited by the mission.
     * @return number of sites.
     */
    public final int getNumCollectionSitesVisited() {
        int result = getCurrentNavpointIndex();
        if (result == (getNumberOfNavpoints() - 1))
            result -= 1;
        return result;
    }

    /**
     * Gets the estimated time spent at a collection site.
     * @param useBuffer Use time buffer in estimation if true.
     * @return time (millisols)
     */
    protected double getEstimatedTimeAtCollectionSite(boolean useBuffer) {
        double timePerPerson = siteResourceGoal / resourceCollectionRate;
        if (useBuffer)
            timePerPerson *= EVA_COLLECTION_OVERHEAD;
        return timePerPerson / getPeopleNumber();
    }

    /**
     * Gets the time limit of the trip based on life support capacity.
     * @param useBuffer use time buffer in estimation if true.
     * @return time (millisols) limit.
     * @throws MissionException if error determining time limit.
     */
    public static double getTotalTripTimeLimit(Rover rover, int memberNum,
            boolean useBuffer) {

        Inventory vInv = rover.getInventory();

        double timeLimit = Double.MAX_VALUE;

        PersonConfig config = SimulationConfig.instance()
                .getPersonConfiguration();

        // Check food capacity as time limit.
        AmountResource food = AmountResource.findAmountResource("food");
        double foodConsumptionRate = config.getFoodConsumptionRate();
        double foodCapacity = vInv.getAmountResourceCapacity(food, false);
        double foodTimeLimit = foodCapacity / (foodConsumptionRate * memberNum);
        if (foodTimeLimit < timeLimit)
            timeLimit = foodTimeLimit;

        // Check water capacity as time limit.
        AmountResource water = AmountResource.findAmountResource("water");
        double waterConsumptionRate = config.getWaterConsumptionRate();
        double waterCapacity = vInv.getAmountResourceCapacity(water, false);
        double waterTimeLimit = waterCapacity
                / (waterConsumptionRate * memberNum);
        if (waterTimeLimit < timeLimit)
            timeLimit = waterTimeLimit;

        // Check oxygen capacity as time limit.
        AmountResource oxygen = AmountResource.findAmountResource("oxygen");
        double oxygenConsumptionRate = config.getOxygenConsumptionRate();
        double oxygenCapacity = vInv.getAmountResourceCapacity(oxygen, false);
        double oxygenTimeLimit = oxygenCapacity
                / (oxygenConsumptionRate * memberNum);
        if (oxygenTimeLimit < timeLimit)
            timeLimit = oxygenTimeLimit;

        // Convert timeLimit into millisols and use error margin.
        timeLimit = (timeLimit * 1000D);
        if (useBuffer)
            timeLimit /= Rover.LIFE_SUPPORT_RANGE_ERROR_MARGIN;

        return timeLimit;
    }

    @Override
    public Map<Class, Integer> getEquipmentNeededForRemainingMission(
            boolean useBuffer) {
        if (equipmentNeededCache != null) {
            return equipmentNeededCache;
        }
        else {
            Map<Class, Integer> result = new HashMap<Class, Integer>();
            
            // Include required number of containers.
            result.put(containerType, containerNum);

            equipmentNeededCache = result;
            return result;
        }
    }

    /**
     * Gets the description of a collection site.
     * @param siteNum the number of the site.
     * @return description
     */
    protected abstract String getCollectionSiteDescription(int siteNum);
    
    @Override
    public void destroy() {
        super.destroy();
        
        resourceType = null;
        containerType = null;
        collectionSiteStartTime = null;
    }
}