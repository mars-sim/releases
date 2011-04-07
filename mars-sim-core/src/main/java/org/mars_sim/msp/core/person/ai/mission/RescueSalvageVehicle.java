/**
 * Mars Simulation Project
 * RescueSalvageVehicle.java
 * @version 3.00 2010-08-10
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.mission;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.equipment.EVASuit;
import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.job.Driver;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.Resource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/** 
 * The RescueSalvageRover class is a mission to rescue the crew of a vehicle 
 * that has an emergency beacon on and tow the vehicle back, or to simply tow 
 * the vehicle back if the crew is already dead.
 */
public class RescueSalvageVehicle extends RoverMission implements Serializable {
    
	private static String CLASS_NAME = 
	    "org.mars_sim.msp.simulation.person.ai.mission.RescueSalvageVehicle";
	
    private static Logger logger = Logger.getLogger(CLASS_NAME);

	// Default description.
	public static final String DEFAULT_DESCRIPTION = "Rescue/Salvage Vehicle";
	
	// Static members
	private static final int MISSION_MIN_MEMBERS = 2;
	private static final int MISSION_MAX_MEMBERS = 3;
	private static final double BASE_RESCUE_MISSION_WEIGHT = 1000D;
	private static final double BASE_SALVAGE_MISSION_WEIGHT = 100D;
	private static final double RESCUE_RESOURCE_BUFFER = 1D;
	
	// Mission phases
	final protected static String RENDEZVOUS = "Rendezvous with vehicle";
	
	// Data members
    private Vehicle vehicleTarget;
    private boolean rescue = false;
    
    /** 
     * Constructor
     * @param startingPerson the person starting the mission.
     * @throws MissionException if error constructing mission.
     */
    public RescueSalvageVehicle(Person startingPerson) {
    	// Use RoverMission constructor
        super(DEFAULT_DESCRIPTION, startingPerson, MISSION_MIN_MEMBERS);   
        
        if (!isDone()) {
        	setStartingSettlement(startingPerson.getSettlement());
        	setMissionCapacity(MISSION_MAX_MEMBERS);
        	
//        	try {
        		if (hasVehicle()) {
        			vehicleTarget = findAvailableBeaconVehicle(getStartingSettlement(), getVehicle().getRange());
        			
        			int capacity = getRover().getCrewCapacity();
        			if (capacity < MISSION_MAX_MEMBERS) setMissionCapacity(capacity);
        			
        			int availableSuitNum = Mission.getNumberAvailableEVASuitsAtSettlement(startingPerson.getSettlement());
                	if (availableSuitNum < getMissionCapacity()) setMissionCapacity(availableSuitNum);
        		}
//        	}
//        	catch (Exception e) {
//        		e.printStackTrace(System.err);
//        		throw new MissionException(getPhase(), e);
//        	}
        	
        	if (vehicleTarget != null) {
        		if (getRescuePeopleNum(vehicleTarget) > 0) {
        			rescue = true;
        			setMinPeople(1);
        			setDescription("Rescue " + vehicleTarget.getName());
        		}
        		else setDescription("Salvage " + vehicleTarget.getName());
        		
        		// Add navpoints for target vehicle and back home again.
        		addNavpoint(new NavPoint(vehicleTarget.getCoordinates(), vehicleTarget.getName()));
        		addNavpoint(new NavPoint(getStartingSettlement().getCoordinates(), getStartingSettlement(), getStartingSettlement().getName()));
        		
        		// Recruit additional people to mission.
            	if (!isDone()) recruitPeopleForMission(startingPerson);
            	
            	// Check if vehicle can carry enough supplies for the mission.
//            	try {
            		if (hasVehicle() && !isVehicleLoadable()) endMission("Vehicle is not loadable. (RescueSalvageVehicle)");
//            	}
//            	catch (Exception e) {
//            		throw new MissionException(getPhase(), e);
//            	}
            	
        		// Add rendezvous phase.
        		addPhase(RENDEZVOUS);
            	
            	// Set initial phase
                setPhase(VehicleMission.EMBARKING);
                setPhaseDescription("Embarking from " + getStartingSettlement().getName());
        	}
        	else endMission("No vehicle target.");
        }
    }
    
    /**
     * Constructor with explicit data.
     * @param members collection of mission members.
     * @param startingSettlement the starting settlement.
     * @param vehicleTarget the vehicle to rescue/salvage.
     * @param rover the rover to use.
     * @param description the mission's description.
     * @throws MissionException if error constructing mission.
     */
    public RescueSalvageVehicle(Collection<Person> members, Settlement startingSettlement, 
    		Vehicle vehicleTarget, Rover rover, String description) {
    	
       	// Use RoverMission constructor.
    	super(description, (Person) members.toArray()[0], 1, rover);
    	
    	setStartingSettlement(startingSettlement);
    	this.vehicleTarget = vehicleTarget;
    	setMissionCapacity(getRover().getCrewCapacity());
    	
    	if (getRescuePeopleNum(vehicleTarget) > 0) rescue = true;
    	
		// Add navpoints for target vehicle and back home again.
		addNavpoint(new NavPoint(vehicleTarget.getCoordinates(), vehicleTarget.getName()));
		addNavpoint(new NavPoint(startingSettlement.getCoordinates(), startingSettlement, startingSettlement.getName()));
		
    	// Add mission members.
    	Iterator<Person> i = members.iterator();
    	while (i.hasNext()) i.next().getMind().setMission(this);
    	
		// Add rendezvous phase.
		addPhase(RENDEZVOUS);
    	
    	// Set initial phase
        setPhase(VehicleMission.EMBARKING);
        setPhaseDescription("Embarking from " + startingSettlement.getName());
        
    	// Check if vehicle can carry enough supplies for the mission.
//    	try {
    		if (hasVehicle() && !isVehicleLoadable()) endMission("Vehicle is not loadable. (RescueSalvageVehicle)");
//    	}
//    	catch (Exception e) {
//    		throw new MissionException(getPhase(), e);
//    	}
    }
    
    /** 
     * Gets the weighted probability that a given person would start this mission.
     * @param person the given person
     * @return the weighted probability
     */
    public static double getNewMissionProbability(Person person) {

    	double missionProbability = 0D;
    	
        if (person.getLocationSituation().equals(Person.INSETTLEMENT)) {
        	
        	// Check if mission is possible for person based on their circumstance.
        	boolean missionPossible = true;
            Settlement settlement = person.getSettlement();
	    
	    	// Check if available rover.
	    	if (!areVehiclesAvailable(settlement, true)) missionPossible = false;
	    	
			// Check if min number of EVA suits at settlement.
			if (Mission.getNumberAvailableEVASuitsAtSettlement(settlement) < MISSION_MIN_MEMBERS) 
				missionPossible = false;
	    	
	    	// Check if there are any beacon vehicles within range that need help.
	    	Vehicle vehicleTarget = null;
	    	try {
	    		Vehicle vehicle = getVehicleWithGreatestRange(settlement, true);
		    	if (vehicle != null) {
		    		vehicleTarget = findAvailableBeaconVehicle(settlement, vehicle.getRange());
		    		if (vehicleTarget == null) missionPossible = false;
		    		else if (!isClosestCapableSettlement(settlement, vehicleTarget)) missionPossible = false;
		    	}
	    	}
	    	catch (Exception e) {}
	    	
	    	// Check if person is last remaining person at settlement (for salvage mission but not rescue mission).
            // Also check for backup rover for salvage mission.
	    	boolean rescue = false;
	    	if (vehicleTarget != null) {
	    		rescue = (getRescuePeopleNum(vehicleTarget) > 0);
	    		if (rescue) {
	    			// if (!atLeastOnePersonRemainingAtSettlement(settlement, person)) missionPossible = false;
	    		}
		    	else {
					// Check if minimum number of people are available at the settlement.
					// Plus one to hold down the fort.
					if (!minAvailablePeopleAtSettlement(settlement, (MISSION_MIN_MEMBERS + 1))) missionPossible = false;
                    
                    // Check if available backup rover.
                    if (!hasBackupRover(settlement)) missionPossible = false;
		    	}
	    	}
	    	
			// Check for embarking missions.
			if (VehicleMission.hasEmbarkingMissions(settlement)) missionPossible = false;
	    	
	    	// Determine mission probability.
	        if (missionPossible) {
	        	if (rescue) missionProbability = BASE_RESCUE_MISSION_WEIGHT;
	        	else missionProbability = BASE_SALVAGE_MISSION_WEIGHT;
	            
	            // Crowding modifier.
	            int crowding = settlement.getCurrentPopulationNum() - settlement.getPopulationCapacity();
	            if (crowding > 0) missionProbability *= (crowding + 1);
	        	
	    		// Job modifier.
	        	Job job = person.getMind().getJob();
	        	if (job != null) missionProbability *= job.getStartMissionProbabilityModifier(RescueSalvageVehicle.class);	
	        }
        }

        return missionProbability;
    }
    
    @Override
    protected boolean isUsableVehicle(Vehicle newVehicle) {
        if (newVehicle != null) {
            boolean usable = true;
            
            if (!(newVehicle instanceof Rover)) usable = false;
            if (newVehicle.isReservedForMission()) usable = false;
            String status = newVehicle.getStatus();
            if (!status.equals(Vehicle.PARKED) && !status.equals(Vehicle.MAINTENANCE)) 
                usable = false;
//            try {
                if (newVehicle.getInventory().getTotalInventoryMass() > 0D) usable = false;
//            }
//            catch (InventoryException e) {
//                throw new MissionException(getPhase(), e);
//            }
            
            return usable;
        }
        else throw new IllegalArgumentException("isUsableVehicle: newVehicle is null.");
    }
    
    @Override
    protected void setVehicle(Vehicle newVehicle) {
        super.setVehicle(newVehicle);
        if (getVehicle() == newVehicle) {
            if (newVehicle.isReservedForMaintenance()) {
                newVehicle.setReservedForMaintenance(false);
            }
        }
    }
    
    /**
     * Check if mission is a rescue mission or a salvage mission.
     * @return true if rescue mission
     */
    public boolean isRescueMission() {
    	return rescue;
    }
    
    /**
     * Gets the vehicle being rescued/salvaged by this mission.
     * @return vehicle
     */
    public Vehicle getVehicleTarget() {
    	return vehicleTarget;
    }
    
    /**
     * Determines a new phase for the mission when the current phase has ended.
     * @throws MissionException if problem setting a new phase.
     */
    protected void determineNewPhase() {
    	if (EMBARKING.equals(getPhase())) {
    		startTravelToNextNode();
    		setPhase(VehicleMission.TRAVELLING);
    		setPhaseDescription("Driving to " + getNextNavpoint().getDescription());
    		 if (rescue) logger.info(getVehicle().getName() + " starting rescue mission for " + vehicleTarget.getName());
        	 else logger.info(getVehicle().getName() + " starting salvage mission for " + vehicleTarget.getName());
    	}
		else if (TRAVELLING.equals(getPhase())) {
			if (getCurrentNavpoint().isSettlementAtNavpoint()) {
				setPhase(VehicleMission.DISEMBARKING);
				setPhaseDescription("Disembarking at " + getCurrentNavpoint().getSettlement().getName());
			}
			else {
				setPhase(RENDEZVOUS);
				if (rescue) setPhaseDescription("Rescuing " + vehicleTarget.getName());
				else setPhaseDescription("Salvaging " + vehicleTarget.getName());
			}
		}
		else if (RENDEZVOUS.equals(getPhase())) {
			startTravelToNextNode();
			setPhase(VehicleMission.TRAVELLING);
			setPhaseDescription("Driving to " + getNextNavpoint().getDescription());
		}
		else if (DISEMBARKING.equals(getPhase())) endMission("Successfully disembarked.");
    }
    
    /**
     * The person performs the current phase of the mission.
     * @param person the person performing the phase.
     * @throws MissionException if problem performing the phase.
     */
    protected void performPhase(Person person) {
    	super.performPhase(person);
    	if (RENDEZVOUS.equals(getPhase())) rendezvousPhase(person);
    }
    
	/** 
	 * Performs the rendezvous phase of the mission.
	 * @param person the person currently performing the mission
	 * @throws MissionException if problem performing rendezvous phase.
	 */
	private void rendezvousPhase(Person person) {
	
		 logger.info(getVehicle().getName() + " rendezvous with " + vehicleTarget.getName());
		
		// If rescuing vehicle crew, load rescue life support resources into vehicle (if possible).
		if (rescue) {
//			try {
				Map rescueResources = determineRescueResourcesNeeded(true);
				Iterator i = rescueResources.keySet().iterator();
				while (i.hasNext()) {
					AmountResource resource = (AmountResource) i.next();
					double amount = (Double) rescueResources.get(resource);
					Inventory roverInv = getRover().getInventory();
					Inventory targetInv = vehicleTarget.getInventory();
					double amountNeeded = amount - targetInv.getAmountResourceStored(resource);
					if ((amountNeeded > 0) && (roverInv.getAmountResourceStored(resource) > amountNeeded)) {
						roverInv.retrieveAmountResource(resource, amountNeeded);
						targetInv.storeAmountResource(resource, amountNeeded, true);
					}
				}
//			}
//			catch (Exception e) {
//				throw new MissionException(getPhase(), e);
//			}
		}
		
		// Turn off vehicle's emergency beacon.
		setEmergencyBeacon(person, vehicleTarget, false);
		
		// Hook vehicle up for towing.
		getRover().setTowedVehicle(vehicleTarget);
		vehicleTarget.setTowingVehicle(getRover());
		
		setPhaseEnded(true);
		
		// Set mission event.
		HistoricalEvent newEvent = new MissionHistoricalEvent(person, this, MissionHistoricalEvent.RENDEZVOUS);
		Simulation.instance().getEventManager().registerNewEvent(newEvent);
	}
	
    /**
     * Performs the disembark to settlement phase of the mission.
     * @param person the person currently performing the mission.
     * @param disembarkSettlement the settlement to be disembarked to.
     * @throws MissionException if error performing phase.
     */
    protected void performDisembarkToSettlementPhase(Person person, Settlement disembarkSettlement) 
{
    	
    	// Put towed vehicle and crew in settlement if necessary.
//    	try {
    		if (hasVehicle()) disembarkTowedVehicles(person, getRover(), disembarkSettlement);
//    	}
//    	catch (Exception e) {
//    		throw new MissionException(getPhase(), e);
//        }
    	
    	super.performDisembarkToSettlementPhase(person, disembarkSettlement);
    }
    
    /**
     * Stores the towed vehicle and any crew at settlement.
     * @param rover the towing rover.
     * @param disembarkSettlement the settlement to store the towed vehicle in.
     * @throws MissionException if error disembarking towed vehicle.
     */
    private void disembarkTowedVehicles(Person person, Rover rover, Settlement disembarkSettlement) 
{
    	
    	if (rover.getTowedVehicle() != null) {
    		Vehicle towedVehicle = rover.getTowedVehicle();
    		
    		// Unhook towed vehicle.
    		rover.setTowedVehicle(null);
    		towedVehicle.setTowingVehicle(null);
    		
    		// Store towed vehicle in settlement.
    		Inventory inv = disembarkSettlement.getInventory();
//    		try {
    			inv.storeUnit(towedVehicle);
//    		}
//    		catch (InventoryException e) {
//    			throw new MissionException(getPhase(), e);
//    		}
    		logger.info(towedVehicle + " salvaged at " + disembarkSettlement.getName());
    		HistoricalEvent salvageEvent = new MissionHistoricalEvent(person, this, MissionHistoricalEvent.SALVAGE_VEHICLE);
			Simulation.instance().getEventManager().registerNewEvent(salvageEvent);
    		
    		// Unload any crew at settlement.
    		if (towedVehicle instanceof Crewable) {
    			Crewable crewVehicle = (Crewable) towedVehicle;
				Iterator<Person> i = crewVehicle.getCrew().iterator();
				while (i.hasNext()) {
					Person crewmember = i.next();
//					try {
						towedVehicle.getInventory().retrieveUnit(crewmember);
						disembarkSettlement.getInventory().storeUnit(crewmember);
						BuildingManager.addToRandomBuilding(crewmember, disembarkSettlement);
//					}
//					catch (Exception e) {
//						throw new MissionException(getPhase(), e);
//					}
        			crewmember.setAssociatedSettlement(disembarkSettlement);
        		        logger.info(crewmember.getName() + " rescued.");
        			HistoricalEvent rescueEvent = new MissionHistoricalEvent(person, this, MissionHistoricalEvent.RESCUE_PERSON);
        			Simulation.instance().getEventManager().registerNewEvent(rescueEvent);
        		}
    		}
    		
    		// Unhook the towed vehicle this vehicle is towing if any.
    		if (towedVehicle instanceof Rover) {
    			disembarkTowedVehicles(person, (Rover) towedVehicle, disembarkSettlement);
    		}
    	}
    }
	
	/**
	 * Gets the resources needed for the crew to be rescued.
	 * @param useBuffer use time buffers in estimation if true.
	 * @return map of amount resources and their amounts.
	 * @throws MissionException if error determining resources.
	 */
	private Map<Resource, Number> determineRescueResourcesNeeded(boolean useBuffer) {
		Map<Resource, Number> result = new HashMap<Resource, Number>(3);
		
    	// Determine estimate time for trip.
		double distance = vehicleTarget.getCoordinates().getDistance(getStartingSettlement().getCoordinates());
    	double time = getEstimatedTripTime(true, distance);
    	double timeSols = time / 1000D;
    	
    	int peopleNum = getRescuePeopleNum(vehicleTarget);
    	
    	// Determine life support supplies needed for trip.
//    	try {
    		AmountResource oxygen = AmountResource.findAmountResource("oxygen");
    		double oxygenAmount = PhysicalCondition.getOxygenConsumptionRate() * timeSols * peopleNum;
    		if (useBuffer) oxygenAmount *= Rover.LIFE_SUPPORT_RANGE_ERROR_MARGIN;
    		result.put(oxygen, oxygenAmount);
    		
    		AmountResource water = AmountResource.findAmountResource("water");
    		double waterAmount = PhysicalCondition.getWaterConsumptionRate() * timeSols * peopleNum;
    		if (useBuffer) waterAmount *= Rover.LIFE_SUPPORT_RANGE_ERROR_MARGIN;
    		result.put(water, waterAmount);
    		
    		AmountResource food = AmountResource.findAmountResource("food");
    		double foodAmount = PhysicalCondition.getFoodConsumptionRate() * timeSols * peopleNum;
    		if (useBuffer) foodAmount *= Rover.LIFE_SUPPORT_RANGE_ERROR_MARGIN;
    		result.put(food, foodAmount);
//    	}
//    	catch (Exception e) {
//    		throw new MissionException(getPhase(), e);
//    	}
		
		return result;
	}
    
    /**
     * Finds the closest available rescue or salvage vehicles within range.
     * @param settlement the starting settlement.
     * @param range the available range (km).
     * @return vehicle or null if none available.
     */
    public static Vehicle findAvailableBeaconVehicle(Settlement settlement, double range) {
    	Vehicle result = null;
    	double halfRange = range / 2D;
    	
    	Collection<Vehicle> emergencyBeaconVehicles = new ConcurrentLinkedQueue<Vehicle>();
    	Collection<Vehicle> vehiclesNeedingRescue = new ConcurrentLinkedQueue<Vehicle>();
    	
    	// Find all available vehicles.
    	Iterator<Vehicle> iV = Simulation.instance().getUnitManager().getVehicles().iterator();
    	while (iV.hasNext()) {
    		Vehicle vehicle = iV.next();
    		if (vehicle.isEmergencyBeacon() && !isVehicleAlreadyMissionTarget(vehicle)) {
    			emergencyBeaconVehicles.add(vehicle);
    			
    			if (vehicle instanceof Crewable) {
    				if (((Crewable) vehicle).getCrewNum() > 0) vehiclesNeedingRescue.add(vehicle);
    			}
    		}
    	}
    	
    	// Check for vehicles with crew needing rescue first.
    	if (vehiclesNeedingRescue.size() > 0) {
    		Vehicle vehicle = findClosestVehicle(settlement.getCoordinates(), vehiclesNeedingRescue);
    		if (vehicle != null) {
    			double vehicleRange = settlement.getCoordinates().getDistance(vehicle.getCoordinates());
    			if (vehicleRange <= halfRange) result = vehicle;
    		}
    	}
    	
    	// Check for vehicles needing salvage next.
    	if ((result == null) && (emergencyBeaconVehicles.size() > 0)) {
    		Vehicle vehicle = findClosestVehicle(settlement.getCoordinates(), emergencyBeaconVehicles);
    		if (vehicle != null) {
    			double vehicleRange = settlement.getCoordinates().getDistance(vehicle.getCoordinates());
    			if (vehicleRange <= halfRange) result = vehicle;
    		}
    	}
    	
    	return result;
    }
    
    /**
     * Checks if vehicle is already the target of a rescue/salvage vehicle mission.
     * @param vehicle the vehicle to check.
     * @return true if already mission target.
     */
    private static boolean isVehicleAlreadyMissionTarget(Vehicle vehicle) {
    	boolean result = false;
    	
    	MissionManager manager = Simulation.instance().getMissionManager();
    	Iterator i = manager.getMissions().iterator();
    	while (i.hasNext() && !result) {
    		Mission mission = (Mission) i.next();
    		if (mission instanceof RescueSalvageVehicle) {
    			Vehicle vehicleTarget = ((RescueSalvageVehicle) mission).vehicleTarget;
    			if (vehicle == vehicleTarget) result = true;
    		}
    	}
    	
    	return result;
    }
    
    /**
     * Gets the closest vehicle in a vehicle collection
     * @param location the location to measure from.
     * @param vehicles the vehicle collection.
     * @return closest vehicle.
     */
    private static Vehicle findClosestVehicle(Coordinates location, Collection<Vehicle> vehicles) {
    	Vehicle closest = null;
    	double closestDistance = Double.MAX_VALUE;
    	Iterator<Vehicle> i = vehicles.iterator();
    	while (i.hasNext()) {
    		Vehicle vehicle = i.next();
    		double vehicleDistance = location.getDistance(vehicle.getCoordinates());
    		if (vehicleDistance < closestDistance) {
    			closest = vehicle;
    			closestDistance = vehicleDistance;
    		}
    	}
    	return closest;
    }
    
    /**
     * Gets the number of people in the vehicle who need rescuing.
     * @param vehicle the vehicle.
     * @return number of people.
     */
    private static int getRescuePeopleNum(Vehicle vehicle) {
    	int result = 0;
    	
    	if (vehicle instanceof Crewable) {
    		result = ((Crewable) vehicle).getCrewNum();
    	}
    	
    	return result;
    }
    
	/**
	 * Gets the settlement associated with the mission.
	 * @return settlement or null if none.
	 */
	public Settlement getAssociatedSettlement() {
		return getStartingSettlement();
	}
	
    /**
	 * Gets the number and amounts of resources needed for the mission.
	 * @param useBuffer use time buffers in estimation if true.
	 * @param parts include parts.
	 * @return map of amount and item resources and their Double amount or Integer number.
	 * @throws MissionException if error determining needed resources.
	 */
    public Map<Resource, Number> getResourcesNeededForRemainingMission(boolean useBuffer, 
    		boolean parts) {
    	Map<Resource, Number> result = super.getResourcesNeededForRemainingMission(useBuffer, parts);
    	
    	// Include rescue resources if needed.
    	if (rescue && (getRover().getTowedVehicle() == null)) {
    		Map<Resource, Number> rescueResources = determineRescueResourcesNeeded(useBuffer);
    		Iterator<Resource> i = rescueResources.keySet().iterator();
    		while (i.hasNext()) {
    			Resource resource = i.next();
    			if (resource instanceof AmountResource) {
    				double amount = (Double) rescueResources.get(resource);
    				if (result.containsKey(resource)) amount += (Double) result.get(resource);
    				if (useBuffer) amount *= RESCUE_RESOURCE_BUFFER;
    				result.put(resource, amount);
    			}
    			else {
    				int num = (Integer) rescueResources.get(resource);
    				if (result.containsKey(resource)) num += (Integer) result.get(resource);
    				result.put(resource, num);
    			}
    		}
    	}
    	
    	return result;
    }
	
    /**
     * Gets the number and types of equipment needed for the mission.
     * @param useBuffer use time buffer in estimation if true.
     * @return map of equipment class and Integer number.
     * @throws MissionException if error determining needed equipment.
     */
    public Map<Class, Integer> getEquipmentNeededForRemainingMission(boolean useBuffer) {
    	if (equipmentNeededCache != null) return equipmentNeededCache;
    	else {
    		Map<Class, Integer> result = new HashMap<Class, Integer>();
    		
    		// Include two EVA suits.
        	result.put(EVASuit.class, 2);
    		
    		equipmentNeededCache = result;
    		return result;
    	}
    }
    
	/**
	 * Gets the mission qualification value for the person.
	 * Person is qualified and interested in joining the mission if the value is larger than 0.
	 * The larger the qualification value, the more likely the person will be picked for the mission.
	 * Qualification values of zero or negative will not join missions.
	 * @param person the person to check.
	 * @return mission qualification value.
	 * @throws MissionException if problem finding mission qualification.
	 */
	protected double getMissionQualification(Person person) {
		double result = 0D;
		
		if (isCapableOfMission(person)) {
			result = super.getMissionQualification(person);
			
			// If person has the "Driver" job, add 1 to their qualification.
			if (person.getMind().getJob() instanceof Driver) result += 1D;
		}
		
		return result;
	}
	
	/**
	 * Checks to see if a person is capable of joining a mission.
	 * @param person the person to check.
	 * @return true if person could join mission.
	 */
	protected boolean isCapableOfMission(Person person) {
		if (super.isCapableOfMission(person)) {
			if (person.getLocationSituation().equals(Person.INSETTLEMENT)) {
				if (person.getSettlement() == getStartingSettlement()) return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if this is the closest settlement to a beacon vehicle that could rescue/salvage it.
	 * @param thisSettlement this settlement.
	 * @param thisVehicle the beacon vehicle.
	 * @return true if this is the closest settlement.
	 * @throws MissionException if error in checking settlements.
	 */
	public static boolean isClosestCapableSettlement(Settlement thisSettlement, Vehicle thisVehicle) 
			{
		boolean result = true;
		
		double distance = thisSettlement.getCoordinates().getDistance(thisVehicle.getCoordinates());
		
		Iterator<Settlement> iS = Simulation.instance().getUnitManager().getSettlements().iterator();
		while (iS.hasNext() && result) {
			Settlement settlement = iS.next();
			if (settlement != thisSettlement) {
				double settlementDistance = settlement.getCoordinates().getDistance(thisVehicle.getCoordinates());
				if (settlementDistance < distance) {
					if (settlement.getCurrentPopulationNum() >= MISSION_MIN_MEMBERS) {
						Iterator<Vehicle> iV = settlement.getParkedVehicles().iterator();
						while (iV.hasNext() && result) {
							Vehicle vehicle = iV.next();
							if (vehicle instanceof Rover) {
//								try {
									if (vehicle.getRange() >= (settlementDistance * 2D)) result = false;
//								}
//								catch (Exception e) {
//									throw new MissionException(null, e);
//								}
							}
						}
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Recruits new people into the mission.
	 * @param startingPerson the person starting the mission.
	 */
	protected void recruitPeopleForMission(Person startingPerson) {
		super.recruitPeopleForMission(startingPerson);
	
		// Make sure there is at least one person left at the starting settlement.
		// If salvage mission, otherwise ignore if rescue mission.
		if (!rescue) {
			if (!atLeastOnePersonRemainingAtSettlement(getStartingSettlement(), startingPerson)) {
				// Remove last person added to the mission.
			    	Object[] array = getPeople().toArray();
				Person lastPerson = null;
				int amount = getPeopleNumber() - 1;
				
				if(amount > 0 && amount < array.length){
				    lastPerson= (Person) array[amount];
				}
				
				if (lastPerson != null) {
					lastPerson.getMind().setMission(null);
					if (getPeopleNumber() < getMinPeople()) endMission("Not enough members.");
				}
			}
		}
	}
	
	/**
	 * Gets the resources needed for loading the vehicle.
	 * @return resources and their number.
	 * @throws MissionException if error determining resources.
	 */
	public Map<Resource, Number> getResourcesToLoad() {
		// Override and full rover with fuel and life support resources.
		Map<Resource, Number> result = new HashMap<Resource, Number>(4);
		Inventory inv = getVehicle().getInventory();
//		try {
			result.put(getVehicle().getFuelType(), inv.getAmountResourceCapacity(getVehicle().getFuelType()));
			AmountResource oxygen = AmountResource.findAmountResource("oxygen");
			result.put(oxygen, inv.getAmountResourceCapacity(oxygen));
			AmountResource water = AmountResource.findAmountResource("water");
			result.put(water, inv.getAmountResourceCapacity(water));
			AmountResource food = AmountResource.findAmountResource("food");
			result.put(food, inv.getAmountResourceCapacity(food));
//		}
//		catch (Exception e) {
//			throw new MissionException(getPhase(), e);
//		}
		
		// Get parts too.
		result.putAll(getPartsNeededForTrip(getTotalRemainingDistance()));
		
		return result;
	}
}