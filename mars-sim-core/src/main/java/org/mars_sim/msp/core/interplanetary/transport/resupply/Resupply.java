/**
 * Mars Simulation Project
 * Resupply.java
 * @version 3.06 2014-04-11
 * @author Scott Davis
 */
package org.mars_sim.msp.core.interplanetary.transport.resupply;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.UnitType;
import org.mars_sim.msp.core.equipment.Equipment;
import org.mars_sim.msp.core.equipment.EquipmentFactory;
import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.interplanetary.transport.TransitState;
import org.mars_sim.msp.core.interplanetary.transport.TransportEvent;
import org.mars_sim.msp.core.interplanetary.transport.Transportable;
import org.mars_sim.msp.core.person.EventType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.PersonGender;
import org.mars_sim.msp.core.person.ai.social.RelationshipManager;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.structure.BuildingTemplate;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingConfig;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * Resupply mission from Earth for a settlement.
 */
public class Resupply
implements Serializable, Transportable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(Resupply.class.getName());
	
	// Default distance between buildings for resupply placement.
    private static final double DEFAULT_INHABITABLE_BUILDING_DISTANCE = 5D;
    private static final double DEFAULT_NONINHABITABLE_BUILDING_DISTANCE = 2D;
    
    // Default width and length for variable size buildings if not otherwise determined.
    private static final double DEFAULT_VARIABLE_BUILDING_WIDTH = 10D;
    private static final double DEFAULT_VARIABLE_BUILDING_LENGTH = 10D;
	
	// Data members
	private Settlement settlement;
	private TransitState state;
	private MarsClock launchDate;
	private MarsClock arrivalDate;
	private List<BuildingTemplate> newBuildings;
	private List<String> newVehicles;
	private Map<String, Integer> newEquipment;
	private int newImmigrantNum;
	private Map<AmountResource, Double> newResources;
	private Map<Part, Integer> newParts;

	/**
	 * Constructor.
	 * @param arrivalDate the arrival date of the supplies. 
	 * @param settlement the settlement receiving the supplies.
	 */
	public Resupply(MarsClock arrivalDate, Settlement settlement) {
		
		// Initialize data members.
		this.arrivalDate = arrivalDate;
		this.settlement = settlement;
	}
	
	@Override
	public MarsClock getLaunchDate() {
	    return (MarsClock) launchDate.clone();
	}
	
	/**
	 * Sets the launch date of the resupply mission.
	 * @param launchDate the launch date.
	 */
	public void setLaunchDate(MarsClock launchDate) {
	    this.launchDate = (MarsClock) launchDate.clone();
	}

    /**
     * Gets a list of the resupply buildings.
     * @return list of building types.
     */
    public List<BuildingTemplate> getNewBuildings() {
        return newBuildings;
    }

    /**
     * Sets the list of resupply buildings. 
     * @param newBuildings list of building types.
     */
    public void setNewBuildings(List<BuildingTemplate> newBuildings) {
        this.newBuildings = newBuildings;
    }

    /**
     * Gets a list of the resupply vehicles.
     * @return list of vehicle types.
     */
    public List<String> getNewVehicles() {
        return newVehicles;
    }

    /**
     * Sets the list of resupply vehicles.
     * @param newVehicles list of vehicle types.
     */
    public void setNewVehicles(List<String> newVehicles) {
        this.newVehicles = newVehicles;
    }

    /**
     * Gets a map of the resupply equipment.
     * @return map of equipment type and number.
     */
    public Map<String, Integer> getNewEquipment() {
        return newEquipment;
    }

    /**
     * Sets the map of resupply equipment.
     * @param newEquipment map of equipment type and number.
     */
    public void setNewEquipment(Map<String, Integer> newEquipment) {
        this.newEquipment = newEquipment;
    }

    /**
     * Gets the number of immigrants in the resupply mission.
     * @return the number of immigrants.
     */
    public int getNewImmigrantNum() {
        return newImmigrantNum;
    }

    /**
     * Sets the number of immigrants in the resupply mission.
     * @param newImmigrantNum the number of immigrants.
     */
    public void setNewImmigrantNum(int newImmigrantNum) {
        this.newImmigrantNum = newImmigrantNum;
    }

    /**
     * Gets a map of the resupply resources.
     * @return map of resource and amount (kg).
     */
    public Map<AmountResource, Double> getNewResources() {
        return newResources;
    }

    /**
     * Sets the map of resupply resources.
     * @param newResources map of resource and amount (kg).
     */
    public void setNewResources(Map<AmountResource, Double> newResources) {
        this.newResources = newResources;
    }

    /**
     * Gets a map of resupply parts.
     * @return map of part and number. 
     */
    public Map<Part, Integer> getNewParts() {
        return newParts;
    }

    /**
     * Sets the map of resupply parts.
     * @param newParts map of part and number.
     */
    public void setNewParts(Map<Part, Integer> newParts) {
        this.newParts = newParts;
    }

    @Override
	public MarsClock getArrivalDate() {
		return (MarsClock) arrivalDate.clone();
	}
	
	/**
	 * Sets the arrival date of the resupply mission.
	 * @param arrivalDate the arrival date.
	 */
	public void setArrivalDate(MarsClock arrivalDate) {
	    this.arrivalDate = (MarsClock) arrivalDate.clone();
	}
	
	/**
	 * Gets the destination settlement.
	 * @return destination settlement.
	 */
	public Settlement getSettlement() {
	    return settlement;
	}
	
	/**
	 * Sets the destination settlement.
	 * @param settlement the destination settlement.
	 */
    public void setSettlement(Settlement settlement) {
        this.settlement = settlement;
    }
    
    /**
     * Commits a set of modifications for the resupply mission.
     */
    public void commitModification() {
        HistoricalEvent newEvent = new TransportEvent(this, EventType.TRANSPORT_ITEM_MODIFIED, 
                "Resupply mission modified");
        Simulation.instance().getEventManager().registerNewEvent(newEvent);  
    }

    @Override
    public void destroy() {
        settlement = null;
        launchDate = null;
        arrivalDate = null;
        newBuildings.clear();
        newBuildings = null;
        newVehicles.clear();
        newVehicles = null;
        newEquipment.clear();
        newEquipment = null;
        newResources.clear();
        newResources = null;
        newParts.clear();
        newParts = null;
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(getSettlement().getName());
        buff.append(": ");
        buff.append(getArrivalDate().getDateString());
        return buff.toString();
    }
    
    @Override
    public int compareTo(Transportable o) {
        int result = 0;
        
        double arrivalTimeDiff = MarsClock.getTimeDiff(arrivalDate, o.getArrivalDate());
        if (arrivalTimeDiff < 0D) {
            result = -1;
        }
        else if (arrivalTimeDiff > 0D) {
            result = 1;
        }
        else {
            // If arrival time is the same, compare by name alphabetically.
            result = getName().compareTo(o.getName());
        }
        
        return result;
    }

	@Override
	public String getName() {
		return getSettlement().getName();
	}

	@Override
	public TransitState getTransitState() {
		return state;
	}

	@Override
	public void setTransitState(TransitState transitState) {
		this.state = transitState;
	}

	@Override
	public void performArrival() {
		// Deliver supplies to the destination settlement.
		deliverSupplies();
	}
	
	/**
     * Delivers supplies to the destination settlement.
     */
    private void deliverSupplies() {
        
        // Deliver buildings.
        BuildingManager buildingManager = settlement.getBuildingManager();
        Iterator<BuildingTemplate> buildingI = getNewBuildings().iterator();
        while (buildingI.hasNext()) {
            BuildingTemplate template = buildingI.next();
            
            
            // Check if building template position/facing collides with any 
            // existing buildings/vehicles/construction sites.
            if (checkBuildingTemplatePosition(template)) {
                
                // Correct length and width in building template.
                int buildingID = settlement.getBuildingManager().getUniqueBuildingIDNumber();
                
                // Replace width and length defaults to deal with variable width and length buildings.
                double width = SimulationConfig.instance().getBuildingConfiguration().getWidth(template.getType());
                if (template.getWidth() > 0D) {
                    width = template.getWidth();
                }
                if (width <= 0D) {
                    width = DEFAULT_VARIABLE_BUILDING_WIDTH;
                }
                
                double length = SimulationConfig.instance().getBuildingConfiguration().getLength(template.getType());
                if (template.getLength() > 0D) {
                    length = template.getLength();
                }
                if (length <= 0D) {
                    length = DEFAULT_VARIABLE_BUILDING_LENGTH;
                }
                
                BuildingTemplate correctedTemplate = new BuildingTemplate(buildingID, template.getType(), width, 
                        length, template.getXLoc(), template.getYLoc(), template.getFacing());
                
                buildingManager.addBuilding(correctedTemplate,  true);
            }
            else {
                // Determine location and facing for the new building.
                BuildingTemplate positionedTemplate = positionNewResupplyBuilding(template.getType());
                buildingManager.addBuilding(positionedTemplate, true);
            }
        }
        
        // Deliver vehicles.
        UnitManager unitManager = Simulation.instance().getUnitManager();
        Iterator<String> vehicleI = getNewVehicles().iterator();
        while (vehicleI.hasNext()) {
            String vehicleType = vehicleI.next();
            Vehicle vehicle = null;
            if (LightUtilityVehicle.NAME.equalsIgnoreCase(vehicleType)) {
                String name = unitManager.getNewName(UnitType.VEHICLE, "LUV", null);
                vehicle = new LightUtilityVehicle(name, vehicleType, settlement);
            } else {
                String name = unitManager.getNewName(UnitType.VEHICLE, null, null);
                vehicle = new Rover(name, vehicleType, settlement);
            }
            unitManager.addUnit(vehicle);
        }
        
        Inventory inv = settlement.getInventory();
        
        // Deliver equipment.
        Iterator<String> equipmentI = getNewEquipment().keySet().iterator();
        while (equipmentI.hasNext()) {
            String equipmentType = equipmentI.next();
            int number = getNewEquipment().get(equipmentType);
            for (int x=0; x < number; x++) {
                Equipment equipment = EquipmentFactory.getEquipment(equipmentType, 
                        settlement.getCoordinates(), false);
                equipment.setName(unitManager.getNewName(UnitType.EQUIPMENT, equipmentType, null));
                inv.storeUnit(equipment);
            }
        }
        
        // Deliver resources.
        Iterator<AmountResource> resourcesI = getNewResources().keySet().iterator();
        while (resourcesI.hasNext()) {
            AmountResource resource = resourcesI.next();
            double amount = getNewResources().get(resource);
            double capacity = inv.getAmountResourceRemainingCapacity(resource, true, false);
            if (amount > capacity) amount = capacity;
            inv.storeAmountResource(resource, amount, true);
        }
        
        // Deliver parts.
        Iterator<Part> partsI = getNewParts().keySet().iterator();
        while (partsI.hasNext()) {
            Part part = partsI.next();
            int number = getNewParts().get(part);
            inv.storeItemResources(part, number);
        }
        
        // Deliver immigrants.
        Collection<Person> immigrants = new ConcurrentLinkedQueue<Person>();
        RelationshipManager relationshipManager = Simulation.instance().getRelationshipManager();
        for (int x = 0; x < getNewImmigrantNum(); x++) {
            PersonConfig personConfig = SimulationConfig.instance().getPersonConfiguration();
            PersonGender gender = PersonGender.FEMALE;
            if (RandomUtil.getRandomDouble(1.0D) <= personConfig.getGenderRatio()) gender = PersonGender.MALE;
            String birthplace = "Earth"; //TODO: randomize from list of countries/federations.
            String immigrantName = unitManager.getNewName(UnitType.PERSON, null, gender);
            Person immigrant = new Person(immigrantName, gender, birthplace, settlement); //TODO: read from file
            unitManager.addUnit(immigrant);
            relationshipManager.addNewImmigrant(immigrant, immigrants);
            immigrants.add(immigrant);
            logger.info(immigrantName + " arrives on Mars at " + settlement.getName());
        }
    }
    
    /**
     * Checks if a building template's position is clear of collisions with any existing structures.
     * @param template the building template.
     * @return true if building template position is clear.
     */
    private boolean checkBuildingTemplatePosition(BuildingTemplate template) {
        
        boolean result = true;
        
        // Replace width and length defaults to deal with variable width and length buildings.
        double width = SimulationConfig.instance().getBuildingConfiguration().getWidth(template.getType());
        if (template.getWidth() > 0D) {
            width = template.getWidth();
        }
        if (width <= 0D) {
            width = DEFAULT_VARIABLE_BUILDING_WIDTH;
        }
        
        double length = SimulationConfig.instance().getBuildingConfiguration().getLength(template.getType());
        if (template.getLength() > 0D) {
            length = template.getLength();
        }
        if (length <= 0D) {
            length = DEFAULT_VARIABLE_BUILDING_LENGTH;
        }
        
        result = settlement.getBuildingManager().checkIfNewBuildingLocationOpen(template.getXLoc(), 
                template.getYLoc(), width, length, template.getFacing());
        
        return result;
    }
    
    /**
     * Determines and sets the position of a new resupply building.
     * @param building type the new building type.
     * @return the repositioned building template.
     */
    private BuildingTemplate positionNewResupplyBuilding(String buildingType) {
        
        BuildingTemplate newPosition = null;
        
        BuildingConfig buildingConfig = SimulationConfig.instance().getBuildingConfiguration();
        boolean isBuildingConnector = buildingConfig.hasBuildingConnection(buildingType);
        boolean hasLifeSupport = buildingConfig.hasLifeSupport(buildingType);
        
        if (isBuildingConnector) {
            // Try to find best location to connect two buildings.
            newPosition = positionNewBuildingConnectorBuilding(buildingType);
        }
        else if (hasLifeSupport) {
            // Try to put building next to another inhabitable building.
            List<Building> inhabitableBuildings = settlement.getBuildingManager().getBuildings(BuildingFunction.LIFE_SUPPORT);
            Collections.shuffle(inhabitableBuildings);
            Iterator<Building> i = inhabitableBuildings.iterator();
            while (i.hasNext()) {
                Building building = i.next();
                newPosition = positionNextToBuilding(buildingType, building, DEFAULT_INHABITABLE_BUILDING_DISTANCE, false);
                if (newPosition != null) {
                    break;
                }
            }
        }
        else {
            // Try to put building next to the same building type.
            List<Building> sameBuildings = settlement.getBuildingManager().getBuildingsOfName(buildingType);
            Collections.shuffle(sameBuildings);
            Iterator<Building> j = sameBuildings.iterator();
            while (j.hasNext()) {
                Building building = j.next();
                newPosition = positionNextToBuilding(buildingType, building, DEFAULT_NONINHABITABLE_BUILDING_DISTANCE, false);
                if (newPosition != null) {
                    break;
                }
            }
        }
        
        if (newPosition == null) {
            // Try to put building next to another building.
            // If not successful, try again 10m from each building and continue out at 10m increments 
            // until a location is found.
            BuildingManager buildingManager = settlement.getBuildingManager();
            if (buildingManager.getBuildingNum() > 0) {
                for (int x = 10; newPosition == null; x+= 10) {
                    List<Building> allBuildings = buildingManager.getBuildings();
                    Collections.shuffle(allBuildings);
                    Iterator<Building> i = allBuildings.iterator();
                    while (i.hasNext()) {
                        Building building = i.next();
                        newPosition = positionNextToBuilding(buildingType, building, (double) x, false);
                        if (newPosition != null) {
                            break;
                        }
                    }
                }
            }
            else {
                // Replace width and length defaults to deal with variable width and length buildings.
                double width = SimulationConfig.instance().getBuildingConfiguration().getWidth(buildingType);
                if (width <= 0D) {
                    width = DEFAULT_VARIABLE_BUILDING_WIDTH;
                }
                double length = SimulationConfig.instance().getBuildingConfiguration().getLength(buildingType);
                if (length <= 0D) {
                    length = DEFAULT_VARIABLE_BUILDING_LENGTH;
                }
                
                // If no buildings at settlement, position new building at 0,0 with random facing.
                int buildingID = settlement.getBuildingManager().getUniqueBuildingIDNumber();
                newPosition = new BuildingTemplate(buildingID, buildingType, width, length, 0D, 0D, 
                        RandomUtil.getRandomDouble(360D));
            }
        }
        
        return newPosition;
    }
    
    /**
     * Determine the position and length (for variable length) of a new building connector building.
     * @param newBuildingType the new building type.
     * @return new building template with position/length, or null if none found.
     */
    private BuildingTemplate positionNewBuildingConnectorBuilding(String newBuildingType) {
        
        BuildingTemplate newTemplate = null;
       
        BuildingManager manager = settlement.getBuildingManager();
        List<Building> inhabitableBuildings = manager.getBuildings(BuildingFunction.LIFE_SUPPORT);
        Collections.shuffle(inhabitableBuildings);
        
        // Try to find a connection between an inhabitable building without access to airlock and
        // another inhabitable building with access to an airlock.
        if (settlement.getAirlockNum() > 0) {
            Iterator<Building> i = inhabitableBuildings.iterator();
            while (i.hasNext()) {
                Building startingBuilding = i.next();
                if (!settlement.hasWalkableAvailableAirlock(startingBuilding)) {

                    // Find a different inhabitable building that has walkable access to an airlock.
                    double leastDistance = Double.MAX_VALUE;
                    Building endingBuilding = null;
                    Iterator<Building> k = inhabitableBuildings.iterator();
                    while (k.hasNext()) {
                        Building building = k.next();
                        if (!building.equals(startingBuilding)) {
                            if (settlement.hasWalkableAvailableAirlock(building)) {
                                double distance = Point2D.distance(startingBuilding.getXLocation(), 
                                        startingBuilding.getYLocation(), building.getXLocation(), 
                                        building.getYLocation());
                                if ((distance < leastDistance) && (distance >= 1D)) {
                                    endingBuilding = building;
                                    leastDistance = distance;
                                }
                            }
                        }
                    }
                    
                    if (endingBuilding != null) {
                        
                        // Determine new location/length between the two buildings.
                        newTemplate = positionConnectorBetweenTwoBuildings(newBuildingType, startingBuilding, 
                                endingBuilding);
                        break;
                    }
                }
            }
        }
        
        // Try to find valid connection location between two inhabitable buildings that are not directly connected.
        if (newTemplate == null) {
            Iterator<Building> j = inhabitableBuildings.iterator();
            while (j.hasNext()) {
                Building startingBuilding = j.next();
                
                // Find a different inhabitable building.
                double leastDistance = Double.MAX_VALUE;
                Building endingBuilding = null;
                Iterator<Building> k = inhabitableBuildings.iterator();
                while (k.hasNext()) {
                    Building building = k.next();
                    boolean directlyConnected = (settlement.getBuildingConnectorManager().getBuildingConnections(
                            startingBuilding, building).size() > 0);
                    if (!building.equals(startingBuilding) && !directlyConnected) {
                        double distance = Point2D.distance(startingBuilding.getXLocation(), 
                                startingBuilding.getYLocation(), building.getXLocation(), 
                                building.getYLocation());
                        if ((distance < leastDistance) && (distance >= 1D)) {
                            endingBuilding = building;
                            leastDistance = distance;
                        }
                    }
                }
                
                if (endingBuilding != null) {
                    
                    // Determine new location/length between the two buildings.
                    newTemplate = positionConnectorBetweenTwoBuildings(newBuildingType, startingBuilding, 
                            endingBuilding);
                    break;
                }
            }
        }
        
        // Try to find connection to existing inhabitable building.
        if (newTemplate == null) {
            Iterator<Building> l = inhabitableBuildings.iterator();
            while (l.hasNext()) {
                Building building = l.next();
                // Make connector building face away from building.
                newTemplate = positionNextToBuilding(newBuildingType, building, 0D, true);

                if (newTemplate != null) {
                    break;
                }
            }
        }
        
        return newTemplate;
    }
    
    /**
     * Positions a new building near an existing building.
     * @param newBuildingType the new building type.
     * @param building the existing building.
     * @param separationDistance the separation distance (meters) from the building.
     * @param faceAway true if new building should face away from other building.
     * @return new building template with determined position, or null if none found.
     */
    private BuildingTemplate positionNextToBuilding(String newBuildingType, Building building, 
            double separationDistance, boolean faceAway) {
        BuildingTemplate newPosition = null;
        
        // Replace width and length defaults to deal with variable width and length buildings.
        double width = SimulationConfig.instance().getBuildingConfiguration().getWidth(newBuildingType);
        if (width <= 0D) {
            width = DEFAULT_VARIABLE_BUILDING_WIDTH;
        }
        double length = SimulationConfig.instance().getBuildingConfiguration().getLength(newBuildingType);
        if (length <= 0D) {
            length = DEFAULT_VARIABLE_BUILDING_LENGTH;
        }
        
        final int front = 0;
        final int back = 1;
        final int right = 2;
        final int left = 3;
        
        List<Integer> directions = new ArrayList<Integer>(4);
        directions.add(front);
        directions.add(back);
        directions.add(right);
        directions.add(left);
        Collections.shuffle(directions);
        
        double direction = 0D;
        double structureDistance = 0D;
        double rectRotation = building.getFacing();
        
        for (int x = 0; x < directions.size(); x++) {
            switch (directions.get(x)) {
                case front: direction = building.getFacing();
                            structureDistance = (building.getLength() / 2D) + (length / 2D);
                            break;
                case back: direction = building.getFacing() + 180D;
                            structureDistance = (building.getLength() / 2D) + (length / 2D);
                            if (faceAway) {
                                rectRotation = building.getFacing() + 180D;
                            }
                            break;
                case right:  direction = building.getFacing() + 90D;
                            structureDistance = (building.getWidth() / 2D) + (width / 2D);
                            if (faceAway) {
                                structureDistance = (building.getWidth() / 2D) + (length / 2D);
                                rectRotation = building.getFacing() + 90D;
                            }
                            break;
                case left:  direction = building.getFacing() + 270D;
                            structureDistance = (building.getWidth() / 2D) + (width / 2D);
                            if (faceAway) {
                                structureDistance = (building.getWidth() / 2D) + (length / 2D);
                                rectRotation = building.getFacing() + 270D;
                            }
            }
            
            if (rectRotation > 360D) {
                rectRotation -= 360D;
            }
            
            double distance = structureDistance + separationDistance;
            double radianDirection = Math.toRadians(direction);
            double rectCenterX = building.getXLocation() - (distance * Math.sin(radianDirection));
            double rectCenterY = building.getYLocation() + (distance * Math.cos(radianDirection));
            
            // Check to see if proposed new building position intersects with any existing buildings 
            // or construction sites.
            if (settlement.getBuildingManager().checkIfNewBuildingLocationOpen(rectCenterX, 
                    rectCenterY, width, length, rectRotation)) {
                // Set the new building here.
                int buildingID = settlement.getBuildingManager().getUniqueBuildingIDNumber();
                newPosition = new BuildingTemplate(buildingID, newBuildingType, width, length, rectCenterX, 
                        rectCenterY, rectRotation);
                break;
            }
        }
        
        return newPosition;
    }
    
    /**
     * Determine the position and length (for variable length) for a connector building between two existing
     * buildings.
     * @param newBuildingType the new connector building type.
     * @param firstBuilding the first of the two existing buildings.
     * @param secondBuilding the second of the two existing buildings.
     * @return new building template with determined position, or null if none found.
     */
    private BuildingTemplate positionConnectorBetweenTwoBuildings(String newBuildingType, Building firstBuilding, 
            Building secondBuilding) {
        
        BuildingTemplate newPosition = null;
        
        // Determine valid placement lines for connector building.
        List<Line2D> validLines = new ArrayList<Line2D>();
        
        // Check each building side for the two buildings for a valid line unblocked by obstacles.
        double width = SimulationConfig.instance().getBuildingConfiguration().getWidth(newBuildingType);
        List<Point2D> firstBuildingPositions = getFourPositionsSurroundingBuilding(firstBuilding, .1D);
        List<Point2D> secondBuildingPositions = getFourPositionsSurroundingBuilding(secondBuilding, .1D);
        for (int x = 0; x < firstBuildingPositions.size(); x++) {
            for (int y = 0; y < secondBuildingPositions.size(); y++) {
                
                Point2D firstBuildingPos = firstBuildingPositions.get(x);
                Point2D secondBuildingPos = secondBuildingPositions.get(y);
                
                double distance = Point2D.distance(firstBuildingPos.getX(), firstBuildingPos.getY(), 
                        secondBuildingPos.getX(), secondBuildingPos.getY());
                
                if (distance > 1D) {
                    // Check line rect between positions for obstacle collision.
                    Line2D line = new Line2D.Double(firstBuildingPos.getX(), firstBuildingPos.getY(), 
                            secondBuildingPos.getX(), secondBuildingPos.getY());
                    boolean clearPath = LocalAreaUtil.checkLinePathCollision(line, settlement.getCoordinates());
                    if (clearPath) {
                        validLines.add(new Line2D.Double(firstBuildingPos, secondBuildingPos));
                    }
                }
            }
        }
        
        if (validLines.size() > 0) {
            
            // Find shortest valid line.
            double shortestLineLength = Double.MAX_VALUE;
            Line2D shortestLine = null;
            Iterator<Line2D> i = validLines.iterator();
            while (i.hasNext()) {
                Line2D line = i.next();
                double length = Point2D.distance(line.getX1(), line.getY1(), line.getX2(), line.getY2());
                if (length < shortestLineLength) {
                    shortestLine = line;
                    shortestLineLength = length;
                }
            }
            
            // Create building template with position, facing, width and length for the connector building.
            double shortestLineFacingDegrees = LocalAreaUtil.getDirection(shortestLine.getP1(), shortestLine.getP2());
            Point2D p1 = adjustConnectorEndPoint(shortestLine.getP1(), shortestLineFacingDegrees, firstBuilding, width);
            Point2D p2 = adjustConnectorEndPoint(shortestLine.getP2(), shortestLineFacingDegrees, secondBuilding, width);
            double centerX = (p1.getX() + p2.getX()) / 2D;
            double centerY = (p1.getY() + p2.getY()) / 2D;
            double newLength = p1.distance(p2);
            double facingDegrees = LocalAreaUtil.getDirection(p1, p2);
            int buildingID = settlement.getBuildingManager().getUniqueBuildingIDNumber();
            newPosition = new BuildingTemplate(buildingID, newBuildingType, width, newLength, centerX, 
                    centerY, facingDegrees);
        }
        
        return newPosition;
    }
    
    /**
     * Adjust the connector end point based on relative angle of the connection.
     * @param point the initial connector location.
     * @param lineFacing the facing of the connector line (degrees).
     * @param building the existing building being connected to.
     * @param connectorWidth the width of the new connector.
     * @return point adjusted location for connector end point.
     */
    private Point2D adjustConnectorEndPoint(Point2D point, double lineFacing, Building building, double connectorWidth) {
        
        double lineFacingRad = Math.toRadians(lineFacing);
        double angleFromBuildingCenterDegrees = LocalAreaUtil.getDirection(new Point2D.Double(building.getXLocation(), 
                building.getYLocation()), point);
        double angleFromBuildingCenterRad = Math.toRadians(angleFromBuildingCenterDegrees);
        double offsetAngle = angleFromBuildingCenterRad - lineFacingRad;
        double offsetDistance = Math.abs(Math.sin(offsetAngle)) * (connectorWidth / 2D);
        
        double newXLoc = (-1D * Math.sin(angleFromBuildingCenterRad) * offsetDistance) + point.getX();
        double newYLoc = (Math.cos(angleFromBuildingCenterRad) * offsetDistance) + point.getY();
        
        return new Point2D.Double(newXLoc, newYLoc);
    }
    
    /**
     * Gets four positions surrounding a building with a given distance from its edge.
     * @param building the building.
     * @param distanceFromSide distance (distance) for positions from the edge of the building.
     * @return list of four positions.
     */
    private List<Point2D> getFourPositionsSurroundingBuilding(Building building, double distanceFromSide) {
        
        List<Point2D> result = new ArrayList<Point2D>(4);
        
        final int front = 0;
        final int back = 1;
        final int right = 2;
        final int left = 3;
        
        for (int x = 0; x < 4; x++) {
            double xPos = 0D;
            double yPos = 0D;
            
            switch(x) {
                case front: xPos = 0D;
                             yPos = (building.getLength() / 2D) + distanceFromSide;
                             break;
                case back:  xPos = 0D;
                             yPos = 0D - (building.getLength() / 2D) - distanceFromSide;
                             break;
                case right: xPos = 0D - (building.getWidth() / 2D) - distanceFromSide;
                             yPos = 0D;
                             break;
                case left:  xPos = (building.getWidth() / 2D) + distanceFromSide;
                             yPos = 0D;
                             break;
            }
            
            Point2D position = LocalAreaUtil.getLocalRelativeLocation(xPos, yPos, building);
            result.add(position);
        }
        
        return result;
    }
}