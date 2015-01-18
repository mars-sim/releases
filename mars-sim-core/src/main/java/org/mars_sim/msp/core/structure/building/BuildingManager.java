/**
 * Mars Simulation Project
 * BuildingManager.java
 * @version 3.07 2014-12-20
 * @author Scott Davis
 */

package org.mars_sim.msp.core.structure.building;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.interplanetary.transport.resupply.Resupply;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.social.RelationshipManager;
import org.mars_sim.msp.core.structure.BuildingTemplate;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.connection.BuildingConnectorManager;
import org.mars_sim.msp.core.structure.building.connection.InsideBuildingPath;
import org.mars_sim.msp.core.structure.building.function.Administration;
import org.mars_sim.msp.core.structure.building.function.AstronomicalObservation;
import org.mars_sim.msp.core.structure.building.function.BuildingConnection;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.Communication;
import org.mars_sim.msp.core.structure.building.function.EVA;
import org.mars_sim.msp.core.structure.building.function.EarthReturn;
import org.mars_sim.msp.core.structure.building.function.Exercise;
import org.mars_sim.msp.core.structure.building.function.Farming;
import org.mars_sim.msp.core.structure.building.function.FoodProduction;
import org.mars_sim.msp.core.structure.building.function.GroundVehicleMaintenance;
import org.mars_sim.msp.core.structure.building.function.ThermalGeneration;
import org.mars_sim.msp.core.structure.building.function.ThermalStorage;
import org.mars_sim.msp.core.structure.building.function.LifeSupport;
import org.mars_sim.msp.core.structure.building.function.LivingAccommodations;
import org.mars_sim.msp.core.structure.building.function.Management;
import org.mars_sim.msp.core.structure.building.function.Manufacture;
import org.mars_sim.msp.core.structure.building.function.MedicalCare;
import org.mars_sim.msp.core.structure.building.function.PowerGeneration;
import org.mars_sim.msp.core.structure.building.function.PowerStorage;
import org.mars_sim.msp.core.structure.building.function.Recreation;
import org.mars_sim.msp.core.structure.building.function.Research;
import org.mars_sim.msp.core.structure.building.function.ResourceProcessing;
import org.mars_sim.msp.core.structure.building.function.Storage;
import org.mars_sim.msp.core.structure.building.function.VehicleMaintenance;
import org.mars_sim.msp.core.structure.building.function.cooking.Cooking;
import org.mars_sim.msp.core.structure.building.function.cooking.Dining;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparingDessert;
import org.mars_sim.msp.core.structure.construction.ConstructionManager;
import org.mars_sim.msp.core.structure.construction.ConstructionSite;
import org.mars_sim.msp.core.structure.construction.ConstructionStageInfo;
import org.mars_sim.msp.core.structure.construction.ConstructionUtil;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.vehicle.GroundVehicle;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * The BuildingManager manages the settlement's buildings.
 */
public class BuildingManager implements Serializable {
//,ClockListener, BuildingListener{

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default serial id. */
    private static Logger logger = Logger.getLogger(BuildingManager.class.getName());

    // Data members
    private Settlement settlement; // The manager's settlement.
    private List<Building> buildings; 
    // 2014-10-29 Added buildingsNickNames
    // A list of the settlement's buildings with their nicknames.
    private List<Building> buildingsNickNames; 
    
    private Map<String, Double> buildingValuesNewCache;
    private Map<String, Double> buildingValuesOldCache;
    private MarsClock lastBuildingValuesUpdateTime;
    
    // 2014-12-23 Added resupply
    private Resupply resupply;

    /**
     * Constructor to construct buildings from settlement config template.
     * @param settlement the manager's settlement.
     * @throws Exception if buildings cannot be constructed.
     */
    public BuildingManager(Settlement settlement) {
        this(settlement, SimulationConfig.instance().getSettlementConfiguration().
                getSettlementTemplate(settlement.getTemplate()).getBuildingTemplates());
    }

    /**
     * Constructor to construct buildings from name list.
     * @param settlement the manager's settlement
     * @param buildingTemplates the settlement's building templates.
     * @throws Exception if buildings cannot be constructed.
     */
    public BuildingManager(Settlement settlement, List<BuildingTemplate> buildingTemplates) {

        this.settlement = settlement;

        // Construct all buildings in the settlement.
        buildings = new ArrayList<Building>();
        if (buildingTemplates != null) {
            Iterator<BuildingTemplate> i = buildingTemplates.iterator();
            while (i.hasNext()) {
                BuildingTemplate template = i.next();
                addBuilding(template, false);
            }
        }
        // Initialize building value caches.
        buildingValuesNewCache = new HashMap<String, Double>();
        buildingValuesOldCache = new HashMap<String, Double>();
        
        // 2014-12-19 Added listeners
    	//listeners = Collections.synchronizedList(new ArrayList<BuildingListener>());
    }

    /**
     * Gets the building manager's settlement.
     *
     * @return settlement
     */
    public Settlement getSettlement() {
        return settlement;
    }

 
    /**
     * Adds a new building to the settlement.
     * @param newBuilding the building to add.
     * @param createBuildingConnections true if automatically create building connections.
     */
    public void addBuilding(Building newBuilding, boolean createBuildingConnections) {
        if (!buildings.contains(newBuilding)) {
            buildings.add(newBuilding);
    		//logger.info("addBuilding() : a new building has just been added");
            // Create new building connections if needed.
            if (createBuildingConnections) {
                settlement.getBuildingConnectorManager().createBuildingConnections(newBuilding);
            }
             settlement.fireUnitUpdate(UnitEventType.ADD_BUILDING_EVENT, newBuilding);
            //unitManager.fireUnitManagerUpdate(UnitManagerEventType.ADD_UNIT, settlement);
            //unitManager.addUnit(newBuilding);
        }
    }

    /**
     * Removes a building from the settlement.
     * @param oldBuilding the building to remove.
     */
    public void removeBuilding(Building oldBuilding) {
        if (buildings.contains(oldBuilding)) {

            // Remove building connections (hatches) to old building.
            settlement.getBuildingConnectorManager().removeAllConnectionsToBuilding(oldBuilding);

            // Remove the building's functions from the settlement.
            oldBuilding.removeFunctionsFromSettlement();

            buildings.remove(oldBuilding);
    		logger.info("removeBuilding() : a new building has just been removed");

            settlement.fireUnitUpdate(UnitEventType.REMOVE_BUILDING_EVENT, oldBuilding);
        }
    }

    /**
     * Adds a building with a template to the settlement.
     * @param template the building template.
     * @param createBuildingConnections true if automatically create building connections.
     */
    public void addBuilding(BuildingTemplate template, boolean createBuildingConnections) {
        Building newBuilding = new Building(template, this); 
        addBuilding(newBuilding, createBuildingConnections);
    }
    
    /**
     * Adds a building with a template to the settlement.
     * @param template the building template.
     * @param createBuildingConnections true if automatically create building connections.
     * @return newBuilding
     */
    // 2014-12-19 Added addOneBuilding(). Called by confirmBuildingLocation() in Resupply.java
    public Building addOneBuilding(BuildingTemplate template, Resupply resupply, boolean createBuildingConnections) {
        this.resupply = resupply;
    	Building newBuilding = new Building(template, this);
		//System.out.println("BuildingManager.java : addBuilding() : isBuildingArrived is " + isBuildingArrived);
        //settlement.fireUnitUpdate(UnitEventType.PLACE_BUILDING_EVENT, newBuilding);            
    	//logger.info("addBuilding() : just fired PLACE_BUILDING_EVENT");
        addBuilding(newBuilding, createBuildingConnections);
        return newBuilding;
    }
    
    // 2014-12-23 Added getResupply()
    public Resupply getResupply() {
    	return resupply;
    }
    // 2014-12-23 Added setResupply()
    public void setResupply(Resupply resupply) {
    	this.resupply= resupply; 
    }
    /**
     * Gets the settlement's collection of buildings.
     *
     * @return collection of buildings
     */
    public List<Building> getBuildings() {
        return new ArrayList<Building>(buildings);
    }

    /**
     * Gets the settlement's collection of buildings (in their nicknames)
     *
     * @return collection of buildings (in their nicknames)
     */
    public List<Building> getBuildingsNickNames() {
        return new ArrayList<Building>(buildingsNickNames);
    }

  
    /**
     * Checks if the settlement contains a given building.
     * @param building the building.
     * @return true if settlement contains building.
     */
    public boolean containsBuilding(Building building) {
        return buildings.contains(building);
    }

    /**
     * Gets the building with a given ID number.
     * @param id the unique building ID number.
     * @return building or null if none found.
     */
    public Building getBuilding(int id) {

        Building result = null;

        Iterator<Building> i = buildings.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            if (building.getID() == id) {
                result = building;
            }
        }

        return result;
    }

    /**
     * Gets the buildings in a settlement that has a given function.
     * @param function {@link BuildingFunction} the function of the building.
     * @return list of buildings.
     */
    public List<Building> getBuildings(BuildingFunction function) {
        List<Building> functionBuildings = new ArrayList<Building>();
        Iterator<Building> i = buildings.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            if (building.hasFunction(function)) {
                functionBuildings.add(building);
            }
        }
        return functionBuildings;
    }
    
    /**
     * Gets the buildings in a settlement have have all of a given array of functions.
     * @param functions the array of required functions {@link BuildingFunctions}.
     * @return list of buildings.
     */
    public List<Building> getBuildings(BuildingFunction[] functions) {
        
        List<Building> functionBuildings = new ArrayList<Building>();
        Iterator<Building> i = buildings.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            boolean hasFunctions = true;
            for (int x = 0; x < functions.length; x++) {
                if (!building.hasFunction(functions[x])) {
                    hasFunctions = false;
                }
            }
            if (hasFunctions) {
                functionBuildings.add(building);
            }
        }
        return functionBuildings;
    }

    /**
     * Gets the buildings in the settlement with a given building type.
     * @param buildingType the building type.
     * @return list of buildings.
     */
    // 2014-10-27: Changed method name from getBuildingsOfName() to getBuildingsOfSameType()
    // Called by Resupply.java and BuildingConstructionMission.java
    // for putting new building next to the same building "type".
    public List<Building> getBuildingsOfSameType(String buildingType) {
        List<Building> typeOfBuildings = new ArrayList<Building>();
        Iterator<Building> i = buildings.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            // WARNING: do NOT change getName() below to getNickName()
            // It is for comparing buildingType
            if (building.getName().equalsIgnoreCase(buildingType)){
            	typeOfBuildings.add(building);  
                	//System.out.println("BuildingManager.java : getBuildingsOfSameType() : buildingType is " +buildingType);
            }
        }
        return typeOfBuildings;
    }

    /**
     * Gets the number of buildings at the settlement.
     *
     * @return number of buildings
     */
    public int getBuildingNum() {
        return buildings.size();
    }

    /**
     * Time passing for all buildings.
     *
     * @param time amount of time passing (in millisols)
     * @throws Exception if error.
     */
    public void timePassing(double time) {
        Iterator<Building> i = buildings.iterator();
        while (i.hasNext()) {
            i.next().timePassing(time);
        }
    }

    /**
     * Gets a random inhabitable building.
     * @return inhabitable building.
     */
    public Building getRandomInhabitableBuilding() {

        Building result = null;

        List<Building> inhabitableBuildings = getBuildings(BuildingFunction.LIFE_SUPPORT);
        if (inhabitableBuildings.size() > 0) {
            int buildingIndex = RandomUtil.getRandomInt(inhabitableBuildings.size() - 1);
            result = inhabitableBuildings.get(buildingIndex);
        }

        return result;
    }
    
    /**
     * Gets a random building with an airlock.
     * @return random building.
     */
    public Building getRandomAirlockBuilding() {
        
        Building result = null;
        
        List<Building> evaBuildings = getBuildings(BuildingFunction.EVA);
        if (evaBuildings.size() > 0) {
            int buildingIndex = RandomUtil.getRandomInt(evaBuildings.size() - 1);
            result = evaBuildings.get(buildingIndex);
        }
        
        return result;
    }

    /**
     * Adds a person to a random inhabitable building within a settlement.
     *
     * @param person the person to add.
     * @param settlement the settlement to find a building.
     * @throws BuildingException if person cannot be added to any building.
     */
    public static void addToRandomBuilding(Person person, Settlement settlement) {

        List<Building> habs = settlement.getBuildingManager().getBuildings(
                new BuildingFunction[] { BuildingFunction.EVA, BuildingFunction.LIFE_SUPPORT });
        
        List<Building> goodHabs = getLeastCrowdedBuildings(habs);

        Building building = null;
        if (goodHabs.size() >= 1) {
            int rand = RandomUtil.getRandomInt(goodHabs.size() - 1);

            int count = 0;
            Iterator<Building> i = goodHabs.iterator();
            while (i.hasNext()) {
                Building tempBuilding = i.next();
                if (count == rand) {
                    building = tempBuilding;
                }
                count++;
            }
        }

        if (building != null) {
            addPersonToBuildingRandomLocation(person, building);
        }
        else {
            throw new IllegalStateException("No inhabitable buildings available for " + person.getName());
        }
    }

    /**
     * Adds a ground vehicle to a random ground vehicle maintenance building within a settlement.
     * @param vehicle the ground vehicle to add.
     * @param settlement the settlement to find a building.
     * @throws BuildingException if vehicle cannot be added to any building.
     */
    public static void addToRandomBuilding(GroundVehicle vehicle, Settlement settlement) {

        List<Building> garages = settlement.getBuildingManager().getBuildings(BuildingFunction.GROUND_VEHICLE_MAINTENANCE);
        List<VehicleMaintenance> openGarages = new ArrayList<VehicleMaintenance>();
        Iterator<Building> i = garages.iterator();
        while (i.hasNext()) {
            Building garageBuilding = i.next();
            VehicleMaintenance garage = (VehicleMaintenance) garageBuilding.getFunction(BuildingFunction.GROUND_VEHICLE_MAINTENANCE);
            if (garage.getCurrentVehicleNumber() < garage.getVehicleCapacity()) openGarages.add(garage);
        }

        if (openGarages.size() > 0) {
            int rand = RandomUtil.getRandomInt(openGarages.size() - 1);
            openGarages.get(rand).addVehicle(vehicle);
        }
        else {
            //logger.warning("No available garage space for " + vehicle.getName() + ", didn't add vehicle");
        }
    }

    /**
     * Gets the building a given person is in.
     * @return building or null if none.
     */
    public static Building getBuilding(Person person) {
        Building result = null;
        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            Settlement settlement = person.getSettlement();
            Iterator<Building> i = settlement.getBuildingManager().getBuildings(BuildingFunction.LIFE_SUPPORT).iterator();
            while (i.hasNext()) {
                Building building = i.next();
                try {
                    LifeSupport lifeSupport = (LifeSupport) building.getFunction(BuildingFunction.LIFE_SUPPORT);
                    if (lifeSupport.containsPerson(person)) {
                        if (result == null) { 
                            result = building;
                        }
                        else {
                            throw new IllegalStateException(person + " is located in more than one building: " + result + 
                                    " and " + building);
                        }
                    }
                }
                catch (Exception e) {
                    logger.log(Level.SEVERE,"BuildingManager.getBuilding(): " + e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * Gets the vehicle maintenance building a given vehicle is in.
     * @return building or null if none.
     */
    public static Building getBuilding(Vehicle vehicle) {
        if (vehicle == null) throw new IllegalArgumentException("vehicle is null");
        Building result = null;
        Settlement settlement = vehicle.getSettlement();
        if (settlement != null) {
            Iterator<Building> i = settlement.getBuildingManager().getBuildings(
                    BuildingFunction.GROUND_VEHICLE_MAINTENANCE).iterator();
            while (i.hasNext()) {
                Building garageBuilding = i.next();
                try {
                    VehicleMaintenance garage = (VehicleMaintenance) garageBuilding.getFunction(
                            BuildingFunction.GROUND_VEHICLE_MAINTENANCE);
                    if (garage.containsVehicle(vehicle)) {
                        result = garageBuilding;
                    }
                }
                catch (Exception e) {
                    logger.log(Level.SEVERE,"BuildingManager.getBuilding(): " + e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * Check what building a given local settlement position is within.
     * @param xLoc the local X position.
     * @param yLoc the local Y position.
     * @return building the position is within, or null if the position is not 
     * within any building.
     */
    public Building getBuildingAtPosition(double xLoc, double yLoc) {
        Building result = null;
        Iterator<Building> i = buildings.iterator();
        while (i.hasNext() && (result == null)) {
            Building building = i.next();
            if (LocalAreaUtil.checkLocationWithinLocalBoundedObject(xLoc, yLoc, building)) {
                result = building;
            }
        }

        return result;
    }

    /**
     * Gets a list of uncrowded buildings from a given list of buildings with life support.
     * @param buildingList list of buildings with the life support function.
     * @return list of buildings that are not at or above maximum occupant capacity.
     * @throws BuildingException if building in list does not have the life support function.
     */
    public static List<Building> getUncrowdedBuildings(List<Building> buildingList) {
        List<Building> result = new ArrayList<Building>();
        try {
            Iterator<Building> i = buildingList.iterator();
            while (i.hasNext()) {
                Building building = i.next();
                LifeSupport lifeSupport = (LifeSupport) building.getFunction(BuildingFunction.LIFE_SUPPORT);
                if (lifeSupport.getAvailableOccupancy() > 0) {
                    result.add(building);
                }
            }
        }
        catch (ClassCastException e) {
            throw new IllegalStateException("BuildingManager.getUncrowdedBuildings(): " +
                    "building isn't a life support building.");
        }
        return result;
    }

    /**
     * Gets a list of the least crowded buildings from a given list of buildings with life support.
     * @param buildingList list of buildings with the life support function.
     * @return list of least crowded buildings.
     * @throws BuildingException if building in list does not have the life support function.
     */
    public static List<Building> getLeastCrowdedBuildings(List<Building> buildingList) {
        List<Building> result = new ArrayList<Building>();
        // Find least crowded population.
        int leastCrowded = Integer.MAX_VALUE;
        Iterator<Building> i = buildingList.iterator();
        while (i.hasNext()) {
            LifeSupport lifeSupport = (LifeSupport) i.next().getFunction(BuildingFunction.LIFE_SUPPORT);
            int crowded = lifeSupport.getOccupantNumber() - lifeSupport.getOccupantCapacity();
            if (crowded < -1) crowded = -1;
            if (crowded < leastCrowded) leastCrowded = crowded;
        }

        // Add least crowded buildings to list.
        Iterator<Building> j = buildingList.iterator();
        while (j.hasNext()) {
            Building building = j.next();
            LifeSupport lifeSupport = (LifeSupport) building.getFunction(BuildingFunction.LIFE_SUPPORT);
            int crowded = lifeSupport.getOccupantNumber() - lifeSupport.getOccupantCapacity();
            if (crowded < -1) crowded = -1;
            if (crowded == leastCrowded) result.add(building);
        }

        return result;
    }

    /**
     * Gets a map of buildings and their probabilities for being chosen based on the best relationships 
     * for a given person from a list of buildings.
     * @param person the person to check for.
     * @param buildingList the list of buildings to filter.
     * @return map of buildings and their probabilities.
     */
    public static Map<Building, Double> getBestRelationshipBuildings(Person person, List<Building> buildingList) {
        Map<Building, Double> result = new HashMap<Building, Double>(buildingList.size());
        RelationshipManager relationshipManager = Simulation.instance().getRelationshipManager();	
        // Determine probabilities based on relationships in buildings.
        Iterator<Building> i = buildingList.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            LifeSupport lifeSupport = (LifeSupport) building.getFunction(BuildingFunction.LIFE_SUPPORT);
            double buildingRelationships = 0D;
            int numPeople = 0;
            Iterator<Person> j = lifeSupport.getOccupants().iterator();
            while (j.hasNext()) {
                Person occupant = j.next();
                if (person != occupant) {
                    buildingRelationships+= relationshipManager.getOpinionOfPerson(person, occupant);
                    numPeople++;
                }
            }
            double prob = 50D;
            if (numPeople > 0) {
                prob = buildingRelationships / numPeople;
                if (prob < 0D) {
                    prob = 0D;
                }
            }

            result.put(building, prob);
        }
        return result;
    }

    /**
     * Gets a list of buildings that don't have any malfunctions from a list of buildings.
     * @param buildingList the list of buildings.
     * @return list of buildings without malfunctions.
     */
    public static List<Building> getNonMalfunctioningBuildings(List<Building> buildingList) {
        List<Building> result = new ArrayList<Building>();

        Iterator<Building> i = buildingList.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            boolean malfunction = building.getMalfunctionManager().hasMalfunction();
            if (!malfunction) result.add(building);
        }

        return result;
    }

    /**
     * Gets a list of buildings that have a valid interior walking path from the person's current building.
     * @param person the person.
     * @param buildingList initial list of buildings.
     * @return list of buildings with valid walking path.
     */
    public static List<Building> getWalkableBuildings(Person person, List<Building> buildingList) {
        List<Building> result = new ArrayList<Building>();

        Building currentBuilding = BuildingManager.getBuilding(person);
        if (currentBuilding != null) {
            BuildingConnectorManager connectorManager = person.getSettlement().getBuildingConnectorManager();

            Iterator<Building> i = buildingList.iterator();
            while (i.hasNext()) {
                Building building = i.next();

                InsideBuildingPath validPath = connectorManager.determineShortestPath(currentBuilding, 
                        currentBuilding.getXLocation(), currentBuilding.getYLocation(), building, 
                        building.getXLocation(), building.getYLocation());

                if (validPath != null) {
                    result.add(building);
                }
            }
        }

        return result;
    }

    /**
     * Adds the person to the building the person's same location if possible.
     * @param person the person to add.
     * @param building the building to add the person to.
     */
    public static void addPersonToBuildingSameLocation(Person person, Building building)  {
        if (building != null) {
            try {
                LifeSupport lifeSupport = (LifeSupport) building.getFunction(BuildingFunction.LIFE_SUPPORT);
                if (!lifeSupport.containsPerson(person)) {
                    lifeSupport.addPerson(person); 
                }
            }
            catch (Exception e) {
                throw new IllegalStateException("BuildingManager.addPersonToBuilding(): " + e.getMessage());
            }
        }
        else throw new IllegalStateException("Building is null");
    }

    /**
     * Adds the person to the building at a given location if possible.
     * @param person the person to add.
     * @param building the building to add the person to.
     */
    public static void addPersonToBuilding(Person person, Building building, double xLocation, double yLocation)  {
        if (building != null) {

            if (!LocalAreaUtil.checkLocationWithinLocalBoundedObject(xLocation, yLocation, building)) {
                throw new IllegalArgumentException(building.getName() + " does not contain location x: " + 
                        xLocation + ", y: " + yLocation);
            }

            try {
                LifeSupport lifeSupport = (LifeSupport) building.getFunction(BuildingFunction.LIFE_SUPPORT);
                if (!lifeSupport.containsPerson(person)) {
                    lifeSupport.addPerson(person); 
                }

                person.setXLocation(xLocation);
                person.setYLocation(yLocation);
            }
            catch (Exception e) {
                throw new IllegalStateException("BuildingManager.addPersonToBuilding(): " + e.getMessage());
            }
        }
        else {
            throw new IllegalStateException("Building is null");
        }
    }

    /**
     * Adds the person to the building at a random location if possible.
     * @param person the person to add.
     * @param building the building to add the person to.
     */
    public static void addPersonToBuildingRandomLocation(Person person, Building building)  {
        if (building != null) {
            try {
                LifeSupport lifeSupport = (LifeSupport) building.getFunction(BuildingFunction.LIFE_SUPPORT);
                if (!lifeSupport.containsPerson(person)) {
                    lifeSupport.addPerson(person); 
                }

                // Add person to random location within building.
                // TODO: Modify this when implementing active locations in buildings.
                Point2D.Double buildingLoc = LocalAreaUtil.getRandomInteriorLocation(building);
                Point2D.Double settlementLoc = LocalAreaUtil.getLocalRelativeLocation(buildingLoc.getX(), 
                        buildingLoc.getY(), building);
                person.setXLocation(settlementLoc.getX());
                person.setYLocation(settlementLoc.getY());
            }
            catch (Exception e) {
                throw new IllegalStateException("BuildingManager.addPersonToBuilding(): " + e.getMessage());
            }
        }
        else {
            throw new IllegalStateException("Building is null");
        }
    }

    /**
     * Removes the person from a building if possible.
     * @param person the person to remove.
     * @param building the building to remove the person from.
     */
    public static void removePersonFromBuilding(Person person, Building building) {
        if (building != null) {
            try {
                LifeSupport lifeSupport = (LifeSupport) building.getFunction(BuildingFunction.LIFE_SUPPORT);
                if (lifeSupport.containsPerson(person)) {
                    lifeSupport.removePerson(person); 
                }
            }
            catch (Exception e) {
                throw new IllegalStateException("BuildingManager.removePersonFromBuilding(): " + e.getMessage());
            }
        }
        else {
            throw new IllegalStateException("Building is null");
        }
    }

    /**
     * Gets the value of a named building at the settlement.
     * @param buildingType the building name.
     * @param newBuilding true if adding a new building.
     * @return building value (VP).
     */
    public double getBuildingValue(String buildingType, boolean newBuilding) {

        // Make sure building name is lower case.
        buildingType = buildingType.toLowerCase().trim();

        // Update building values cache once per Sol.
        MarsClock currentTime = Simulation.instance().getMasterClock().getMarsClock();
        if ((lastBuildingValuesUpdateTime == null) || 
                (MarsClock.getTimeDiff(currentTime, lastBuildingValuesUpdateTime) > 1000D)) {
            buildingValuesNewCache.clear();
            buildingValuesOldCache.clear();
            lastBuildingValuesUpdateTime = (MarsClock) currentTime.clone();
        }

        if (newBuilding && buildingValuesNewCache.containsKey(buildingType)) {
            return buildingValuesNewCache.get(buildingType);
        }
        else if (!newBuilding && buildingValuesOldCache.containsKey(buildingType)) {
            return buildingValuesOldCache.get(buildingType);
        }
        else {
            double result = 0D;

            // Determine value of all building functions.
            BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();
            if (config.hasCommunication(buildingType))
                result += Communication.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasCooking(buildingType)) {
                result += Cooking.getFunctionValue(buildingType, newBuilding, settlement);
                // 2014-11-06 Added MakingSoy
                result += PreparingDessert.getFunctionValue(buildingType, newBuilding, settlement);                
            }
            if (config.hasDining(buildingType))
                result += Dining.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasEVA(buildingType))
                result += EVA.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasExercise(buildingType))
                result += Exercise.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasFarming(buildingType))
                result += Farming.getFunctionValue(buildingType, newBuilding, settlement);
          //2014-11-24 Added FoodProduction
            if (config.hasFoodProduction(buildingType)) 
                result += FoodProduction.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasGroundVehicleMaintenance(buildingType))
                result += GroundVehicleMaintenance.getFunctionValue(buildingType, newBuilding, settlement);
            //2014-10-17 Added the effect of heating requirement
            if (config.hasThermalGeneration(buildingType)) 
                result += ThermalGeneration.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasThermalStorage(buildingType))
                result += ThermalStorage.getFunctionValue(buildingType, newBuilding, settlement);
 
            if (config.hasLifeSupport(buildingType))
                result += LifeSupport.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasLivingAccommodations(buildingType))
                result += LivingAccommodations.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasManufacture(buildingType))
                result += Manufacture.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasMedicalCare(buildingType))
                result += MedicalCare.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasPowerGeneration(buildingType)) 
                result += PowerGeneration.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasPowerStorage(buildingType))
                result += PowerStorage.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasRecreation(buildingType))
                result += Recreation.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasResearchLab(buildingType))
                result += Research.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasResourceProcessing(buildingType))
                result += ResourceProcessing.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasStorage(buildingType))
                result += Storage.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasAstronomicalObservation(buildingType))
                result += AstronomicalObservation.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasManagement(buildingType))
                result += Management.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasEarthReturn(buildingType))
                result += EarthReturn.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasBuildingConnection(buildingType))
                result += BuildingConnection.getFunctionValue(buildingType, newBuilding, settlement);
            if (config.hasAdministration(buildingType))
                result += Administration.getFunctionValue(buildingType, newBuilding, settlement);

            // Multiply value.
            result *= 1000D;

            // TODO: mkung to subtract heating costs per Sol???
            
            // Subtract power costs per Sol.
            double power = config.getBasePowerRequirement(buildingType);
            double hoursInSol = MarsClock.convertMillisolsToSeconds(1000D) / 60D / 60D;
            double powerPerSol = power * hoursInSol;
            double powerValue = powerPerSol * settlement.getPowerGrid().getPowerValue();
            result -= powerValue;

            if (result < 0D) result = 0D;

            // Check if a new non-constructable building has a frame that already exists at the settlement. 
            if (newBuilding) {
                ConstructionStageInfo buildingConstInfo = ConstructionUtil.getConstructionStageInfo(buildingType);
                if (buildingConstInfo != null) {
                    ConstructionStageInfo frameConstInfo = ConstructionUtil.getPrerequisiteStage(buildingConstInfo);
                    if (frameConstInfo != null) {
                        // Check if frame is not constructable.
                        if (!frameConstInfo.isConstructable()) {
                            // Check if the building's frame exists at the settlement.
                            if (!hasBuildingFrame(frameConstInfo.getName())) {
                                // If frame doesn't exist and isn't constructable, the building has zero value.
                                result = 0D;
                            }
                        }
                    }
                }
            }

            //System.out.println("Building " + buildingType + " value: " + (int) result);

            if (newBuilding) buildingValuesNewCache.put(buildingType, result);
            else buildingValuesOldCache.put(buildingType, result);

            return result;
        }
    }

    /**
     * Gets the value of a building at the settlement.
     * @param building the building.
     * @return building value (VP).
     * @throws Exception if error getting building value.
     */
    // TODO: change getName() to getNickName() ?
    public double getBuildingValue(Building building) {
        double result = 0D;
        // 2014-10-29 TODO: Should we change getName() to getNickName()?
        result = getBuildingValue(building.getBuildingType(), false);
        
        // Modify building value by its wear condition.
        double wearCondition = building.getMalfunctionManager().getWearCondition();
        result *= (wearCondition / 100D) * .75D + .25D;

        logger.info("getBuildingValue() : value is " + result);

        return result;
    }

    /**
     * Checks if a new building's proposed location is open or intersects with existing 
     * buildings or construction sites.
     * @param xLoc the new building's X location.
     * @param yLoc the new building's Y location.
     * @param width the new building's width (meters).
     * @param length the new building's length (meters).
     * @param facing the new building's facing (degrees clockwise from North).
     * @return true if new building location is open.
     */
    public boolean checkIfNewBuildingLocationOpen(double xLoc, double yLoc, 
            double width, double length, double facing) {
        return checkIfNewBuildingLocationOpen(xLoc, yLoc, width, length, facing, null);
    }

    /**
     * Checks if a new building's proposed location is open or intersects with existing 
     * buildings or construction sites.
     * @param xLoc the new building's X location.
     * @param yLoc the new building's Y location.
     * @param width the new building's width (meters).
     * @param length the new building's length (meters).
     * @param facing the new building's facing (degrees clockwise from North).
     * @param site the new construction site or null if none.
     * @return true if new building location is open.
     */
    public boolean checkIfNewBuildingLocationOpen(double xLoc, double yLoc, 
            double width, double length, double facing, ConstructionSite site) {
        boolean goodLocation = true;

        goodLocation = LocalAreaUtil.checkObjectCollision(site, width, length, 
                xLoc, yLoc, facing, settlement.getCoordinates());

        return goodLocation;
    }

    /**
     * Checks if a building frame exists at the settlement.
     * Either with an existing building or at a construction site.
     * @param frameName the frame's name.
     * @return true if frame exists.
     */
    public boolean hasBuildingFrame(String frameName) {
        boolean result = false;

        // Check if any existing buildings have this frame.
        Iterator<Building> i = buildings.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            // 2014-10-29 TODO: determine if getName() needed to be changed to getNickName()
            ConstructionStageInfo buildingStageInfo = ConstructionUtil.getConstructionStageInfo(building.getName());
            if (buildingStageInfo != null) {
                ConstructionStageInfo frameStageInfo = ConstructionUtil.getPrerequisiteStage(buildingStageInfo);
                if (frameStageInfo != null) {
                    // 2014-10-29 TODO: determine if getName() needed to be changed to getNickName()
                	if (frameStageInfo.getName().equals(frameName)) {
                        result = true;
                        break;
                    }
                }
            }
        }

        // Check if any construction projects have this frame.
        if (!result) {
            ConstructionStageInfo frameStageInfo = ConstructionUtil.getConstructionStageInfo(frameName);
            if (frameStageInfo != null) {
                ConstructionManager constManager = settlement.getConstructionManager();
                Iterator<ConstructionSite> j = constManager.getConstructionSites().iterator();
                while (j.hasNext()) {
                    ConstructionSite site = j.next();
                    if (site.hasStage(frameStageInfo)) {
                        result = true;
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Gets a unique ID number for a new building.
     * @return ID integer.
     */
    public int getUniqueBuildingIDNumber() {

        int largestID = 0;
        Iterator<Building> i = buildings.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            int id = building.getID();
            if (id > largestID) {
                largestID = id;
            }
        }

        return largestID + 1;
    }
    /**
     * Gets a unique nick name for a new building
     * @return a unique nick name 
     */
    // 2014-10-29 Added getBuildingNickName()
    public String getBuildingNickName(String buildingType) {
      	int bid = getUniqueBuildingIDNumber();
    	int sid = getSettlement().getID();    	
    	String settlementID = getCharForNumber(sid + 1);
		String buildingID = bid + "";		
		String buildingNickName = buildingType + " " + settlementID + buildingID;
    	return buildingNickName;
    }
    
	// 2014-10-29 Added getCharForNumber()
	private String getCharForNumber(int i) {
		// NOTE: i must be > 1, if i = 0, return null
	    return i > 0 && i < 27 ? String.valueOf((char)(i + 'A' - 1)) : null;
	}
	

    /**
     * Prepare object for garbage collection.
     */
    public void destroy() {
        settlement = null;
        Iterator<Building> i = buildings.iterator();
        while (i.hasNext()) {
            i.next().destroy();
        }
        buildings.clear();
        buildings = null;
        buildingValuesNewCache.clear();
        buildingValuesNewCache = null;
        buildingValuesOldCache.clear();
        buildingValuesOldCache = null;
        lastBuildingValuesUpdateTime = null;
    }

}