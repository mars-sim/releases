/**
 * Mars Simulation Project
 * VehicleTableModel.java
 * @version 3.00 2010-08-10
 * @author Barry Evans
 */

package org.mars_sim.msp.ui.swing.tool.monitor;

import org.mars_sim.msp.core.*;
import org.mars_sim.msp.core.malfunction.Malfunction;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.mission.*;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.Vehicle;

import javax.swing.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The VehicleTableModel that maintains a list of Vehicle objects.
 * It maps key attributes of the Vehicle into Columns.
 */
public class VehicleTableModel extends UnitTableModel {
    
    private static String CLASS_NAME = "org.mars_sim.msp.ui.standard.tool.monitor.VehicleTableModel";
	
    private static Logger logger = Logger.getLogger(CLASS_NAME);

    // Column indexes
    private final static int NAME = 0;
    private final static int TYPE = 1;
    private final static int LOCATION = 2;
    private final static int DESTINATION = 3;
    private final static int DESTDIST = 4;
    private final static int MISSION = 5;
    private final static int CREW = 6;
    private final static int DRIVER = 7;
    private final static int STATUS = 8;
    private final static int BEACON = 9;
    private final static int RESERVED = 10;
    private final static int SPEED = 11;
    private final static int MALFUNCTION = 12;
    private final static int OXYGEN = 13;
    private final static int METHANE = 14;
    private final static int WATER = 15;
    private final static int FOOD = 16;
    private final static int ROCK_SAMPLES = 17;
    private final static int ICE = 18;
    private final static int COLUMNCOUNT = 19; // The number of Columns
    private static String columnNames[]; // Names of Columns
    private static Class columnTypes[]; // Names of Columns

    /**
     * Class initialiser creates the static names and classes.
     */
    static {
        columnNames = new String[COLUMNCOUNT];
        columnTypes = new Class[COLUMNCOUNT];
        columnNames[NAME] = "Name";
        columnTypes[NAME] = String.class;
        columnNames[TYPE] = "Type";
        columnTypes[TYPE] = String.class;
        columnNames[DRIVER] = "Driver";
        columnTypes[DRIVER] = String.class;
        columnNames[STATUS] = "Status";
        columnTypes[STATUS] = String.class;
        columnNames[BEACON] = "Beacon";
        columnTypes[BEACON] = String.class;
        columnNames[RESERVED] = "Reserved";
        columnTypes[RESERVED] = String.class;
        columnNames[LOCATION] = "Location";
        columnTypes[LOCATION] = String.class;
        columnNames[SPEED] = "Speed";
        columnTypes[SPEED] = Integer.class;
        columnNames[MALFUNCTION] = "Malfunction";
        columnTypes[MALFUNCTION] = String.class;
        columnNames[CREW] = "Crew";
        columnTypes[CREW] = Integer.class;
        columnNames[DESTINATION] = "Destination";
        columnTypes[DESTINATION] = Coordinates.class;
        columnNames[DESTDIST] = "Dest. Dist.";
        columnTypes[DESTDIST] = Integer.class;
        columnNames[MISSION] = "Mission";
        columnTypes[MISSION] = String.class;
        columnNames[FOOD] = "Food";
        columnTypes[FOOD] = Integer.class;
        columnNames[OXYGEN] = "Oxygen";
        columnTypes[OXYGEN] = Integer.class;
        columnNames[WATER] = "Water";
        columnTypes[WATER] = Integer.class;
        columnNames[METHANE] = "Methane";
        columnTypes[METHANE] = Integer.class;
	    columnNames[ROCK_SAMPLES] = "Rock Samples";
	    columnTypes[ROCK_SAMPLES] = Integer.class;
	    columnNames[ICE] = "Ice";
	    columnTypes[ICE] = Integer.class;
    }
    
    // Data members
    private UnitManagerListener unitManagerListener;
    private LocalMissionManagerListener missionManagerListener;
    private Map<Unit, Map<AmountResource, Integer>> resourceCache;

    /**
     * Constructs a VehicleTableModel object. It creates the list of possible
     * Vehicles from the Unit manager.
     *
     * @param unitManager Proxy manager contains displayable Vehicles.
     */
    public VehicleTableModel(UnitManager unitManager) {
        super("All Vehicles", " vehicles", columnNames, columnTypes);

		setSource(unitManager.getVehicles());
		unitManagerListener = new LocalUnitManagerListener();
        unitManager.addUnitManagerListener(unitManagerListener);
        
        missionManagerListener = new LocalMissionManagerListener();
    }

    /**
     * Return the value of a Cell
     * @param rowIndex Row index of the cell.
     * @param columnIndex Column index of the cell.
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object result = null;
        
        if (rowIndex < getUnitNumber()) {
        	Vehicle vehicle = (Vehicle)getUnit(rowIndex);
        	Map<AmountResource, Integer> resourceMap = resourceCache.get(vehicle);

        	try {
        		// Invoke the appropriate method, switch is the best solution
        		// althought disliked by some
        		switch (columnIndex) {
            		case NAME : {
            			result = vehicle.getName();
            		} break;

            		case TYPE : {
            			result = vehicle.getDescription();
            		} break;
            
            		case CREW : {
            			if (vehicle instanceof Crewable)
            				result = ((Crewable) vehicle).getCrewNum();
            			else result = 0;
            		} break;

            		case WATER : {
            			result = resourceMap.get(AmountResource.findAmountResource("water"));
            		} break;

            		case FOOD : {
            			result = resourceMap.get(AmountResource.findAmountResource("food"));
            		} break;

            		case OXYGEN : {
            			result = resourceMap.get(AmountResource.findAmountResource("oxygen"));
            		} break;

            		case METHANE : {
            			result = resourceMap.get(AmountResource.findAmountResource("methane"));
            		} break;

            		case ROCK_SAMPLES : {
            			result = resourceMap.get(AmountResource.findAmountResource("rock samples"));
            		} break;

            		case SPEED : {
            			result = new Float(vehicle.getSpeed()).intValue();
            		} break;

            		case DRIVER : {
            			if (vehicle.getOperator() != null) {
            				result = vehicle.getOperator().getOperatorName();
            			}
            			else {
            				result = null;
            			}
            		} break;

            		// Status is a combination of Mechical failure and maintenance
            		case STATUS : {
            			result = vehicle.getStatus();
            		} break;
            
            		case BEACON : {
            			if (vehicle.isEmergencyBeacon()) result = "on";
            			else result = "off";
            		} break;
            
            		case RESERVED : {
            			if (vehicle.isReserved()) result = "true";
            			else result = "false";
            		} break;

            		case MALFUNCTION: {
            			Malfunction failure = vehicle.getMalfunctionManager().getMostSeriousMalfunction();
            			if (failure != null) result = failure.getName();
            		} break;

            		case LOCATION : {
            			Settlement settle = vehicle.getSettlement();
            			if (settle != null) {
            				result = settle.getName();
            			}
            			else {
            				result = vehicle.getCoordinates().getFormattedString();
            			}
            		} break;

            		case DESTINATION : {
            			result = null;
            			VehicleMission mission = (VehicleMission) 
            				Simulation.instance().getMissionManager().getMissionForVehicle(vehicle);
            			if (mission != null) {
            				if (mission.getTravelStatus().equals(TravelMission.TRAVEL_TO_NAVPOINT)) {
            					NavPoint destination = mission.getNextNavpoint();
            					if (destination.isSettlementAtNavpoint()) result = destination.getSettlement().getName();
            					else result = destination.getLocation().getFormattedString();
            				}
            			}
            		} break;

            		case DESTDIST : {
            			VehicleMission mission = (VehicleMission) 
            				Simulation.instance().getMissionManager().getMissionForVehicle(vehicle);
            			if (mission != null) {
            				try {
            					result = new Float(mission.getCurrentLegRemainingDistance()).intValue();
            				}
            				catch (Exception e) {
            					logger.log(Level.SEVERE,"Error getting current leg remaining distance.");
            					e.printStackTrace(System.err);
            				}
            			}
            			else result = null;
            		} break;
            
            		case MISSION : {
            			VehicleMission mission = (VehicleMission) 
            				Simulation.instance().getMissionManager().getMissionForVehicle(vehicle);
            			if (mission != null) {
            				result = mission.getName();
            			}
            			else result = null;
            		} break;
            
            		case ICE : {
            			result = resourceMap.get(AmountResource.findAmountResource("ice"));
            		} break;
        		}
        	}
        	catch (Exception e) {
        		logger.log(Level.SEVERE, "", e);
        	}
        }

        return result;
    }
    
	/**
	 * Catch unit update event.
	 * @param event the unit event.
	 */
	public void unitUpdate(UnitEvent event) {
		Unit unit = (Unit) event.getSource();
		int unitIndex = getUnitIndex(unit);
		Object target = event.getTarget();
		String eventType = event.getType();

		int columnNum = -1;
		if (eventType.equals(Unit.NAME_EVENT)) columnNum = NAME;
		else if (eventType.equals(Unit.LOCATION_EVENT)) columnNum = LOCATION;
		else if (eventType.equals(Inventory.INVENTORY_STORING_UNIT_EVENT) || 
				eventType.equals(Inventory.INVENTORY_RETRIEVING_UNIT_EVENT)) {
			if (target instanceof Person) columnNum = CREW;
		}
		else if (eventType.equals(Vehicle.OPERATOR_EVENT)) columnNum = DRIVER;
		else if (eventType.equals(Vehicle.STATUS_EVENT)) columnNum = STATUS;
		else if (eventType.equals(Vehicle.EMERGENCY_BEACON_EVENT)) columnNum = BEACON;
		else if (eventType.equals(Vehicle.RESERVED_EVENT)) columnNum = RESERVED;
		else if (eventType.equals(Vehicle.SPEED_EVENT)) columnNum = SPEED;
		else if (eventType.equals(MalfunctionManager.MALFUNCTION_EVENT)) columnNum = MALFUNCTION;
		else if (eventType.equals(Inventory.INVENTORY_RESOURCE_EVENT)) {
			try {
				int tempColumnNum = -1;
				
				if (target.equals(AmountResource.findAmountResource("oxygen"))) 
					tempColumnNum = OXYGEN;
				else if (target.equals(AmountResource.findAmountResource("methane"))) 
					tempColumnNum = METHANE;
				else if (target.equals(AmountResource.findAmountResource("food"))) 
					tempColumnNum = FOOD;
				else if (target.equals(AmountResource.findAmountResource("water"))) 
					tempColumnNum = WATER;
				else if (target.equals(AmountResource.findAmountResource("rock samples"))) 
					tempColumnNum = ROCK_SAMPLES;
				else if (target.equals(AmountResource.findAmountResource("ice"))) 
					tempColumnNum = ICE;
				
				if (tempColumnNum > -1) {
					// Only update cell if value as int has changed.
					int currentValue = (Integer) getValueAt(unitIndex, tempColumnNum);
					int newValue = getResourceStored(unit, (AmountResource) target);
					if (currentValue != newValue) {
						columnNum = tempColumnNum;
						Map<AmountResource, Integer> resourceMap = resourceCache.get(unit);
						resourceMap.put((AmountResource) target, newValue);
					}
				}
			}
			catch (Exception e) {
				logger.log(Level.SEVERE, "", e);
			}
		}
			
		if (columnNum > -1) {
			SwingUtilities.invokeLater(new VehicleTableCellUpdater(unitIndex, columnNum));
		}
	}
    
	/**
	 * Defines the source data from this table
	 */
	private void setSource(Collection<Vehicle> source) {
		Iterator<Vehicle> iter = source.iterator();
		while(iter.hasNext()) addUnit(iter.next());
	}
	
    /**
     * Add a unit to the model.
     * @param newUnit Unit to add to the model.
     */
    protected void addUnit(Unit newUnit) {
    	if (resourceCache == null) resourceCache = new HashMap<Unit, Map<AmountResource, Integer>>();
    	if (!resourceCache.containsKey(newUnit)) {
    		try {
    			Map<AmountResource, Integer> resourceMap = new HashMap<AmountResource, Integer>(6);
    			AmountResource food = AmountResource.findAmountResource("food");
    			resourceMap.put(food, getResourceStored(newUnit, food));
    			AmountResource oxygen = AmountResource.findAmountResource("oxygen");
    			resourceMap.put(oxygen, getResourceStored(newUnit, oxygen));
    			AmountResource water = AmountResource.findAmountResource("water");
    			resourceMap.put(water, getResourceStored(newUnit, water));
    			AmountResource methane = AmountResource.findAmountResource("methane");
    			resourceMap.put(methane, getResourceStored(newUnit, methane));
    			AmountResource rockSamples = AmountResource.findAmountResource("rock samples");
    			resourceMap.put(rockSamples, getResourceStored(newUnit, rockSamples));
    			AmountResource ice = AmountResource.findAmountResource("ice");
    			resourceMap.put(ice, getResourceStored(newUnit, ice));
    			resourceCache.put(newUnit, resourceMap);
    		}
    		catch (Exception e) {
    			logger.log(Level.SEVERE, "", e);
    		}
    	}
    	super.addUnit(newUnit);
    }
    
    /**
     * Remove a unit to the model.
     * @param oldUnit Unit to remove from the model.
     */
    protected void removeUnit(Unit oldUnit) {
    	if (resourceCache == null) resourceCache = new HashMap<Unit, Map<AmountResource, Integer>>();
    	if (resourceCache.containsKey(oldUnit)) {
    		Map<AmountResource, Integer> resourceMap = resourceCache.get(oldUnit);
    		resourceMap.clear();
    		resourceCache.remove(oldUnit);
    	}
    	super.removeUnit(oldUnit);
    }
    
    /**
     * Gets the integer amount of resources stored in a unit.
     * @param unit the unit to check.
     * @param resource the resource to check.
     * @return integer amount of resource.
     */
    private Integer getResourceStored(Unit unit, AmountResource resource) {
    	Integer result = null;	
//    	try {
    		Inventory inv = unit.getInventory();
    		result = (int) inv.getAmountResourceStored(resource);
//    	}
//    	catch (InventoryException e) {
//    		e.printStackTrace(System.err);
//    	}
    	return result;
    }
	
    /**
     * Prepares the model for deletion.
     */
    public void destroy() {
    	super.destroy();
    	
    	UnitManager unitManager = Simulation.instance().getUnitManager();
    	unitManager.removeUnitManagerListener(unitManagerListener);
    	unitManagerListener = null;
    	
    	missionManagerListener.destroy();
    	missionManagerListener = null;
    	
    	resourceCache.clear();
    	resourceCache = null;
    }
	
    /**
     * UnitManagerListener inner class.
     */
    private class LocalUnitManagerListener implements UnitManagerListener {
    	
    	/**
    	 * Catch unit manager update event.
    	 * @param event the unit event.
    	 */
    	public void unitManagerUpdate(UnitManagerEvent event) {
    		Unit unit = event.getUnit();
    		String eventType = event.getEventType();
    		if (unit instanceof Vehicle) {
    			if (eventType.equals(UnitManagerEvent.ADD_UNIT)) {
    				if (!containsUnit(unit)) addUnit(unit);
    			}
    			else if (eventType.equals(UnitManagerEvent.REMOVE_UNIT)) {
    				if (containsUnit(unit)) removeUnit(unit);
    			}
    		}
    	}
    }
    
    private class LocalMissionManagerListener implements MissionManagerListener {
    	
    	private List<Mission> missions;
    	private MissionListener missionListener;
    	
    	LocalMissionManagerListener() {
    		missionListener = new LocalMissionListener();
    		missions = Simulation.instance().getMissionManager().getMissions();
    		Iterator<Mission> i = missions.iterator();
    		while (i.hasNext()) addMission(i.next());
    	}
    	
    	/**
    	 * Adds a new mission.
    	 * @param mission the new mission.
    	 */
    	public void addMission(Mission mission) {
    		mission.addMissionListener(missionListener);
    		updateVehicleMissionCell(mission);
    	}
    	
    	/**
    	 * Removes an old mission.
    	 * @param mission the old mission.
    	 */
    	public void removeMission(Mission mission){
    		mission.removeMissionListener(missionListener);
    		updateVehicleMissionCell(mission);
    	}
    	
    	private void updateVehicleMissionCell(Mission mission) {
    		if (mission instanceof VehicleMission) {
    			Vehicle vehicle = ((VehicleMission) mission).getVehicle();
    			if (vehicle != null) {
    				int unitIndex = getUnitIndex(vehicle);
    				SwingUtilities.invokeLater(new VehicleTableCellUpdater(unitIndex, MISSION));
    			}
    		}
    	}
    	
    	/**
    	 * Prepares for deletion.
    	 */
    	public void destroy() {
    		Iterator<Mission> i = missions.iterator();
    		while (i.hasNext()) removeMission(i.next());
    		missions = null;
    		missionListener = null;
    	}
    }
    
    /**
     * Inner class for updating a cell in the vehicle table.
     */
    private class VehicleTableCellUpdater implements Runnable {
    	
    	private int row;
    	private int column;
    	
    	private VehicleTableCellUpdater(int row, int column) {
    		this.row = row;
    		this.column = column;
    	}
    	
    	public void run() {
    		fireTableCellUpdated(row, column);
    	}
    }
    
    /**
     * MissionListener inner class.
     */
    private class LocalMissionListener implements MissionListener {
    	
    	/**
    	 * Catch mission update event.
    	 * @param event the mission event.
    	 */
    	public void missionUpdate(MissionEvent event) {
    		Mission mission = (Mission) event.getSource();
    		String eventType = event.getType();
    		int columnNum = -1;
    		if (eventType.equals(TravelMission.TRAVEL_STATUS_EVENT) || 
    				eventType.equals(TravelMission.NAVPOINTS_EVENT)) columnNum = DESTINATION;
    		else if (eventType.equals(TravelMission.DISTANCE_EVENT)) columnNum = DESTDIST;
    		else if (eventType.equals(VehicleMission.VEHICLE_EVENT)) columnNum = MISSION;
    		
    		if (columnNum > -1) {
    			Vehicle vehicle = ((VehicleMission) mission).getVehicle();
    			if (vehicle != null) {
    				int unitIndex = getUnitIndex(vehicle);
    				SwingUtilities.invokeLater(new VehicleTableCellUpdater(unitIndex, columnNum));
    			}
    		}
    	}
    }
}