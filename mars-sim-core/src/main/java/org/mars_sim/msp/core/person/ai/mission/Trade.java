/**
 * Mars Simulation Project
 * Trade.java
 * @version 3.07 2014-11-30
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.mission;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.equipment.EVASuit;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.task.LoadVehicleEVA;
import org.mars_sim.msp.core.person.ai.task.LoadVehicleGarage;
import org.mars_sim.msp.core.person.ai.task.NegotiateTrade;
import org.mars_sim.msp.core.person.ai.task.UnloadVehicleEVA;
import org.mars_sim.msp.core.person.ai.task.UnloadVehicleGarage;
import org.mars_sim.msp.core.person.ai.task.Walk;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ItemResource;
import org.mars_sim.msp.core.resource.Resource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.VehicleMaintenance;
import org.mars_sim.msp.core.structure.goods.CreditManager;
import org.mars_sim.msp.core.structure.goods.Good;
import org.mars_sim.msp.core.structure.goods.GoodType;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.vehicle.GroundVehicle;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * A mission for trading between two settlements.
 * TODO externalize strings
 */
public class Trade
extends RoverMission
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(Trade.class.getName());

	/** Default description. */
	public static final String DEFAULT_DESCRIPTION = Msg.getString(
            "Mission.description.trade"); //$NON-NLS-1$

	/** Mission phases. */
	final public static MissionPhase TRADE_DISEMBARKING = new MissionPhase(Msg.getString(
            "Mission.phase.tradeDisembarking")); //$NON-NLS-1$
	final public static MissionPhase TRADE_NEGOTIATING = new MissionPhase(Msg.getString(
            "Mission.phase.tradeNegotiating")); //$NON-NLS-1$
	final public static MissionPhase UNLOAD_GOODS = new MissionPhase(Msg.getString(
            "Mission.phase.unloadGoods")); //$NON-NLS-1$
	final public static MissionPhase LOAD_GOODS = new MissionPhase(Msg.getString(
            "Mission.phase.loadGoods")); //$NON-NLS-1$
	final public static MissionPhase TRADE_EMBARKING = new MissionPhase(Msg.getString(
            "Mission.phase.tradeEmbarking")); //$NON-NLS-1$

	// Static members
	static final int MAX_MEMBERS = 2;
	public static final double MAX_STARTING_PROBABILITY = 10D;

	// Static cache for holding trade profit info.
	public static final Map<Settlement, TradeProfitInfo> TRADE_PROFIT_CACHE = new HashMap<Settlement, TradeProfitInfo>();
	public static final Map<Settlement, Settlement> TRADE_SETTLEMENT_CACHE = new HashMap<Settlement, Settlement>();

	// Data members.
	private Settlement tradingSettlement;
	private Map<Good, Integer> sellLoad;
	private Map<Good, Integer> buyLoad;
	private double profit;
	private Map<Good, Integer> desiredBuyLoad;
	private double desiredProfit;
	private boolean outbound;
	private MarsClock startNegotiationTime;
	private NegotiateTrade negotiationTask;
	private boolean doNegotiation;

    /**
     * Constructor.
     * @param startingPerson the person starting the settlement.
     */
    public Trade(Person startingPerson) {
        // Use RoverMission constructor.
        super(DEFAULT_DESCRIPTION, startingPerson);

        // Set the mission capacity.
        setMissionCapacity(MAX_MEMBERS);
        int availableSuitNum = Mission.getNumberAvailableEVASuitsAtSettlement(startingPerson.getSettlement());
        if (availableSuitNum < getMissionCapacity()) {
            setMissionCapacity(availableSuitNum);
        }

        outbound = true;
        doNegotiation = true;

        if (!isDone()) {

            // Initialize data members
            setStartingSettlement(startingPerson.getSettlement());

            // Get trading settlement
            tradingSettlement = TRADE_SETTLEMENT_CACHE.get(getStartingSettlement());
            if ((tradingSettlement != null) && (tradingSettlement != getStartingSettlement())) {
                addNavpoint(new NavPoint(tradingSettlement.getCoordinates(), tradingSettlement,
                        tradingSettlement.getName()));
                setDescription(Msg.getString("Mission.description.trade.detail", 
                        tradingSettlement.getName())); //$NON-NLS-1$
                TRADE_PROFIT_CACHE.remove(getStartingSettlement());
                TRADE_PROFIT_CACHE.remove(tradingSettlement);
                TRADE_SETTLEMENT_CACHE.remove(getStartingSettlement());
                TRADE_SETTLEMENT_CACHE.remove(tradingSettlement);
            } else {
                endMission("Could not determine trading settlement.");
            }

            if (!isDone()) {
                // Get the credit that the starting settlement has with the destination settlement.
                CreditManager creditManager = Simulation.instance().getCreditManager();
                double credit = creditManager.getCredit(getStartingSettlement(), tradingSettlement);

                if (credit > (TradeUtil.SELL_CREDIT_LIMIT * -1D)) {
                    // Determine desired buy load,
                    desiredBuyLoad = TradeUtil.getDesiredBuyLoad(getStartingSettlement(), getRover(), tradingSettlement);
                }
                else {
                    // Cannot buy from settlement due to credit limit.
                    desiredBuyLoad = new HashMap<Good, Integer>(0);
                }

                if (credit < TradeUtil.SELL_CREDIT_LIMIT) {
                    // Determine sell load.
                    sellLoad = TradeUtil.determineBestSellLoad(getStartingSettlement(), getRover(), tradingSettlement);
                }
                else {
                    // Will not sell to settlement due to credit limit.
                    sellLoad = new HashMap<Good, Integer>(0);
                }

                // Determine desired trade profit.
                desiredProfit = estimateTradeProfit(desiredBuyLoad);
            }

            // Recruit additional people to mission.
            if (!isDone()) {
                recruitPeopleForMission(startingPerson);
            }
        }

        // Add trade mission phases.
        addPhase(TRADE_DISEMBARKING);
        addPhase(TRADE_NEGOTIATING);
        addPhase(UNLOAD_GOODS);
        addPhase(LOAD_GOODS);
        addPhase(TRADE_EMBARKING);

        // Set initial phase
        setPhase(VehicleMission.EMBARKING);
        setPhaseDescription(Msg.getString("Mission.phase.embarking.description", 
                getStartingSettlement().getName())); //$NON-NLS-1$
        if (logger.isLoggable(Level.INFO)) {
            if (startingPerson != null && getRover() != null) {
                logger.info(startingPerson.getName() + " starting Trade mission on " + getRover().getName());
            }
        }
    }

    /**
     * Constructor with explicit data.
     * @param members collection of mission members.
     * @param startingSettlement the starting settlement.
     * @param tradingSettlement the trading settlement.
     * @param rover the rover to use.
     * @param description the mission's description.
     * @param sellGoods map of mission sell goods and integer amounts.
     * @param buyGoods map of mission buy goods and integer amounts
     */
    public Trade(Collection<Person> members, Settlement startingSettlement, Settlement tradingSettlement,
            Rover rover, String description, Map<Good, Integer> sellGoods, Map<Good, Integer> buyGoods) {
        // Use RoverMission constructor.
        super(description, (Person) members.toArray()[0], 1, rover);

        outbound = true;
        doNegotiation = false;

        // Initialize data members
        setStartingSettlement(startingSettlement);

        // Sets the mission capacity.
        setMissionCapacity(MAX_MEMBERS);
        int availableSuitNum = Mission.getNumberAvailableEVASuitsAtSettlement(startingSettlement);
        if (availableSuitNum < getMissionCapacity()) {
            setMissionCapacity(availableSuitNum);
        }

        // Set mission destination.
        this.tradingSettlement = tradingSettlement;
        addNavpoint(new NavPoint(tradingSettlement.getCoordinates(), tradingSettlement,
                tradingSettlement.getName()));

        // Add mission members.
        Iterator<Person> i = members.iterator();
        while (i.hasNext()) {
            i.next().getMind().setMission(this);
        }

        // Set trade goods.
        sellLoad = sellGoods;
        buyLoad = buyGoods;
        desiredBuyLoad = new HashMap<Good, Integer>(buyGoods);
        profit = estimateTradeProfit(buyLoad);
        desiredProfit = profit;

        // Add trade mission phases.
        addPhase(TRADE_DISEMBARKING);
        addPhase(TRADE_NEGOTIATING);
        addPhase(UNLOAD_GOODS);
        addPhase(LOAD_GOODS);
        addPhase(TRADE_EMBARKING);

        // Set initial phase
        setPhase(VehicleMission.EMBARKING);
        setPhaseDescription(Msg.getString("Mission.phase.embarking.description", 
                getStartingSettlement().getName())); //$NON-NLS-1$
        if (logger.isLoggable(Level.INFO)) {
            Person startingPerson = (Person) members.toArray()[0];
            if (startingPerson != null && getRover() != null) {
                logger.info(startingPerson.getName() + " starting Trade mission on " + getRover().getName());
            }
        }
    }

    /**
     * Determines a new phase for the mission when the current phase has ended.
     */
    protected void determineNewPhase() {
        if (EMBARKING.equals(getPhase())) {
            startTravelToNextNode();
            setPhase(VehicleMission.TRAVELLING);
            setPhaseDescription(Msg.getString("Mission.phase.travelling.description", 
                    getNextNavpoint().getDescription())); //$NON-NLS-1$
        } else if (TRAVELLING.equals(getPhase())) {
            if (getCurrentNavpoint().isSettlementAtNavpoint()) {
                if (outbound) {
                    setPhase(TRADE_DISEMBARKING);
                    setPhaseDescription(Msg.getString("Mission.phase.disembarking.description", 
                            tradingSettlement.getName())); //$NON-NLS-1$
                } else {
                    setPhase(VehicleMission.DISEMBARKING);
                    setPhaseDescription(Msg.getString("Mission.phase.disembarking.description", 
                            getCurrentNavpoint().getDescription())); //$NON-NLS-1$
                }
            }
        } else if (TRADE_DISEMBARKING.equals(getPhase())) {
            setPhase(TRADE_NEGOTIATING);
            setPhaseDescription(Msg.getString("Mission.phase.tradeNegotiating.description", 
                    tradingSettlement.getName())); //$NON-NLS-1$
        } else if (TRADE_NEGOTIATING.equals(getPhase())) {
            setPhase(UNLOAD_GOODS);
            setPhaseDescription(Msg.getString("Mission.phase.unloadGoods.description", 
                    tradingSettlement.getName())); //$NON-NLS-1$
        } else if (UNLOAD_GOODS.equals(getPhase())) {
            setPhase(LOAD_GOODS);
            setPhaseDescription(Msg.getString("Mission.phase.loadGoods.description", 
                    tradingSettlement.getName())); //$NON-NLS-1$
        } else if (LOAD_GOODS.equals(getPhase())) {
            setPhase(TRADE_EMBARKING);
            setPhaseDescription(Msg.getString("Mission.phase.embarking.description", 
                    tradingSettlement.getName())); //$NON-NLS-1$
        } else if (TRADE_EMBARKING.equals(getPhase())) {
            startTravelToNextNode();
            setPhase(VehicleMission.TRAVELLING);
            setPhaseDescription(Msg.getString("Mission.phase.travelling.description", 
                    getNextNavpoint().getDescription())); //$NON-NLS-1$
        } else if (DISEMBARKING.equals(getPhase())) {
            endMission("Successfully disembarked.");
        }
    }

    /**
     * The person performs the current phase of the mission.
     * @param person the person performing the phase.
     */
    protected void performPhase(Person person) {
        super.performPhase(person);
        if (TRADE_DISEMBARKING.equals(getPhase())) {
            performTradeDisembarkingPhase(person);
        } else if (TRADE_NEGOTIATING.equals(getPhase())) {
            performTradeNegotiatingPhase(person);
        } else if (UNLOAD_GOODS.equals(getPhase())) {
            performUnloadGoodsPhase(person);
        } else if (LOAD_GOODS.equals(getPhase())) {
            performLoadGoodsPhase(person);
        } else if (TRADE_EMBARKING.equals(getPhase())) {
            performTradeEmbarkingPhase(person);
        }
    }

    /**
     * Performs the trade disembarking phase.
     * @param person the person performing the mission.
     */
    private void performTradeDisembarkingPhase(Person person) {

        // If rover is not parked at settlement, park it.
        if ((getVehicle() != null) && (getVehicle().getSettlement() == null)) {

            tradingSettlement.getInventory().storeUnit(getVehicle());
            getVehicle().determinedSettlementParkedLocationAndFacing();

            // Add vehicle to a garage if available.
            BuildingManager.addToRandomBuilding((GroundVehicle) getVehicle(), tradingSettlement);
        }

        // Have person exit rover if necessary.
        if (person.getLocationSituation() != LocationSituation.IN_SETTLEMENT) {
            
            // Get random inhabitable building at trading settlement.
            Building destinationBuilding = tradingSettlement.getBuildingManager().
                    getRandomAirlockBuilding();
            if (destinationBuilding != null) {
                Point2D destinationLoc = LocalAreaUtil.getRandomInteriorLocation(destinationBuilding);
                Point2D adjustedLoc = LocalAreaUtil.getLocalRelativeLocation(destinationLoc.getX(), 
                        destinationLoc.getY(), destinationBuilding);
                
                if (Walk.canWalkAllSteps(person, adjustedLoc.getX(), adjustedLoc.getY(), destinationBuilding)) {
                    assignTask(person, new Walk(person, adjustedLoc.getX(), adjustedLoc.getY(), destinationBuilding));
                }
                else {
                    logger.severe("Unable to walk to building " + destinationBuilding);
                }
            }
            else {
                logger.severe("No inhabitable buildings at " + tradingSettlement);
                endMission("No inhabitable buildings at " + tradingSettlement);
            }
        }

        // End the phase when everyone is out of the rover.
        if (isNoOneInRover()) {
            setPhaseEnded(true);
        }
    }

    /**
     * Perform the trade negotiating phase.
     * @param person the person performing the phase.
     */
    private void performTradeNegotiatingPhase(Person person) {
        if (doNegotiation) {
            if (person == getMissionTrader()) {
                if (negotiationTask != null) {
                    if (negotiationTask.isDone()) {
                        buyLoad = negotiationTask.getBuyLoad();
                        profit = estimateTradeProfit(buyLoad);
                        fireMissionUpdate(MissionEventType.BUY_LOAD_EVENT);
                        setPhaseEnded(true);
                    }
                } 
                else {
                    if (startNegotiationTime == null) {
                        startNegotiationTime = (MarsClock) Simulation.instance().getMasterClock().getMarsClock().clone();
                    }
                    Person settlementTrader = getSettlementTrader();
                    if (settlementTrader != null) {
                        negotiationTask = new NegotiateTrade(tradingSettlement, getStartingSettlement(), getRover(), 
                                sellLoad, person, settlementTrader);
                        assignTask(person, negotiationTask);
                    } 
                    else {
                        MarsClock currentTime = (MarsClock) Simulation.instance().getMasterClock().getMarsClock().clone();
                        double timeDiff = MarsClock.getTimeDiff(currentTime, startNegotiationTime);
                        if (timeDiff > 1000D) {
                            buyLoad = new HashMap<Good, Integer>(0);
                            profit = 0D;
                            fireMissionUpdate(MissionEventType.BUY_LOAD_EVENT);
                            setPhaseEnded(true);
                        }
                    }
                }
            }
        } else {
            setPhaseEnded(true);
        }

        if (getPhaseEnded()) {
            outbound = false;
            equipmentNeededCache = null;
            addNavpoint(new NavPoint(getStartingSettlement().getCoordinates(), getStartingSettlement(),
                    getStartingSettlement().getName()));
            TRADE_PROFIT_CACHE.remove(getStartingSettlement());
        }
    }

    /**
     * Perform the unload goods phase.
     * @param person the person performing the phase.
     */
    private void performUnloadGoodsPhase(Person person) {

        // Unload towed vehicle (if necessary).
        unloadTowedVehicle();

        // Unload rover if necessary.
        boolean roverUnloaded = getRover().getInventory().getTotalInventoryMass(false) == 0D;
        if (!roverUnloaded) {
            if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
                // Random chance of having person unload (this allows person to do other things sometimes)
                if (RandomUtil.lessThanRandPercent(50)) {
                    if (isRoverInAGarage()) {
                        assignTask(person, new UnloadVehicleGarage(person, getRover()));
                    }
                    else {
                        // Check if it is day time.
                        SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();
                        if ((surface.getSurfaceSunlight(person.getCoordinates()) > 0D) || 
                                surface.inDarkPolarRegion(person.getCoordinates())) {
                            assignTask(person, new UnloadVehicleEVA(person, getRover()));
                        }
                    }

                    return;
                }
            }
        } 
        else {
            setPhaseEnded(true);
        }
    }

    /**
     * Performs the load goods phase.
     * @param person the person performing the phase.
     */
    private void performLoadGoodsPhase(Person person) {

        if (!isDone()) {

            // Load towed vehicle (if necessary).
            loadTowedVehicle();
        }

        if (!isDone() && !isVehicleLoaded()) {

            // Check if vehicle can hold enough supplies for mission.
            if (isVehicleLoadable()) {
                if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
                    // Random chance of having person load (this allows person to do other things sometimes)
                    if (RandomUtil.lessThanRandPercent(50)) {
                        if (isRoverInAGarage()) {
                            assignTask(person, new LoadVehicleGarage(person, getVehicle(), getRequiredResourcesToLoad(),
                                    getOptionalResourcesToLoad(), getRequiredEquipmentToLoad(), getOptionalEquipmentToLoad()));
                        }
                        else {
                            // Check if it is day time.
                            SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();
                            if ((surface.getSurfaceSunlight(person.getCoordinates()) > 0D) || 
                                    surface.inDarkPolarRegion(person.getCoordinates())) {
                                assignTask(person, new LoadVehicleEVA(person, getVehicle(), getRequiredResourcesToLoad(),
                                        getOptionalResourcesToLoad(), getRequiredEquipmentToLoad(), getOptionalEquipmentToLoad()));
                            }
                        }
                    }
                }
            } else {
                endMission("Vehicle is not loadable (RoverMission).");
            }
        } else {
            setPhaseEnded(true);
        }
    }

    /**
     * Unload any towed vehicles.
     */
    private void unloadTowedVehicle() {
        Vehicle towed = getRover().getTowedVehicle();
        if (towed != null) {
            towed.setReservedForMission(false);
            getRover().setTowedVehicle(null);
            towed.setTowingVehicle(null);
            tradingSettlement.getInventory().storeUnit(towed);
            towed.determinedSettlementParkedLocationAndFacing();
        }
    }

    /**
     * Load the towed vehicle is not already loaded.
     */
    private void loadTowedVehicle() {
        if (!isDone() && (getRover().getTowedVehicle() == null)) {
            String vehicleType = getLoadVehicleType(true);
            if (vehicleType != null) {
                Vehicle buyVehicle = getInitialLoadVehicle(vehicleType, true);
                if (buyVehicle != null) {
                    buyVehicle.setReservedForMission(true);
                    getRover().setTowedVehicle(buyVehicle);
                    buyVehicle.setTowingVehicle(getRover());
                    tradingSettlement.getInventory().retrieveUnit(buyVehicle);
                } else {
                    endMission("Selling vehicle (" + vehicleType + ") is not available (Trade).");
                }
            }
        }
    }

    /**
     * Performs the trade embarking phase.
     * @param person the person performing the phase.
     */
    private void performTradeEmbarkingPhase(Person person) {

        // If person is not aboard the rover, board rover.
        if (!isDone() && person.getLocationSituation() != LocationSituation.IN_VEHICLE && 
                person.getLocationSituation() != LocationSituation.BURIED) {

            // Move person to random location within rover.
            Point2D.Double vehicleLoc = LocalAreaUtil.getRandomInteriorLocation(getVehicle());
            Point2D.Double adjustedLoc = LocalAreaUtil.getLocalRelativeLocation(vehicleLoc.getX(), 
                    vehicleLoc.getY(), getVehicle());
            if (Walk.canWalkAllSteps(person, adjustedLoc.getX(), adjustedLoc.getY(), getVehicle())) {
                assignTask(person, new Walk(person, adjustedLoc.getX(), adjustedLoc.getY(), getVehicle()));
            }
            else {
                logger.severe(person.getName() + " unable to enter rover " + getVehicle());
                endMission(person.getName() + " unable to enter rover " + getVehicle());
            }
            
            if (!isDone() && isRoverInAGarage()) {

                // Store one EVA suit for person (if possible).
                if (tradingSettlement.getInventory().findNumUnitsOfClass(EVASuit.class) > 0) {
                    EVASuit suit = (EVASuit) tradingSettlement.getInventory().findUnitOfClass(EVASuit.class);
                    if (getVehicle().getInventory().canStoreUnit(suit, false)) {
                        tradingSettlement.getInventory().retrieveUnit(suit);
                        getVehicle().getInventory().storeUnit(suit);
                    }
                    else {
                        endMission("Equipment " + suit + " cannot be loaded in rover " + getVehicle());
                        return;
                    }
                }
            }
        }

        // If rover is loaded and everyone is aboard, embark from settlement.
        if (!isDone() && isEveryoneInRover()) {

            // Remove from garage if in garage.
            Building garageBuilding = BuildingManager.getBuilding(getVehicle());
            if (garageBuilding != null) {
                VehicleMaintenance garage = (VehicleMaintenance) garageBuilding.getFunction(BuildingFunction.GROUND_VEHICLE_MAINTENANCE);
                garage.removeVehicle(getVehicle());
            }

            // Embark from settlement
            tradingSettlement.getInventory().retrieveUnit(getVehicle());
            setPhaseEnded(true);
        }
    }

    /** 
     * Performs the embark from settlement phase of the mission.
     * @param person the person currently performing the mission
     */
    protected void performEmbarkFromSettlementPhase(Person person) {
        super.performEmbarkFromSettlementPhase(person);

        if (!isDone() && (getRover().getTowedVehicle() == null)) {
            String vehicleType = getLoadVehicleType(false);
            if (vehicleType != null) {
                Vehicle sellVehicle = getInitialLoadVehicle(vehicleType, false);
                if (sellVehicle != null) {
                    sellVehicle.setReservedForMission(true);
                    getRover().setTowedVehicle(sellVehicle);
                    sellVehicle.setTowingVehicle(getRover());
                    getStartingSettlement().getInventory().retrieveUnit(sellVehicle);
                } else {
                    endMission("Selling vehicle (" + vehicleType + ") is not available (Trade).");
                }
            }
        }
    }

    /**
     * Performs the disembark to settlement phase of the mission.
     * @param person the person currently performing the mission.
     * @param disembarkSettlement the settlement to be disembarked to.
     */
    protected void performDisembarkToSettlementPhase(Person person, Settlement disembarkSettlement) {

        // Unload towed vehicle if any.
        if (!isDone() && (getRover().getTowedVehicle() != null)) {
            Vehicle towed = getRover().getTowedVehicle();
            towed.setReservedForMission(false);
            getRover().setTowedVehicle(null);
            towed.setTowingVehicle(null);
            disembarkSettlement.getInventory().storeUnit(towed);
            towed.determinedSettlementParkedLocationAndFacing();
        }

        super.performDisembarkToSettlementPhase(person, disembarkSettlement);
    }

    @Override
    public void endMission(String reason) {
        super.endMission(reason);

        // Unreserve any towed vehicles.
        if (getRover() != null) {
            if (getRover().getTowedVehicle() != null) {
                Vehicle towed = getRover().getTowedVehicle();
                towed.setReservedForMission(false);
            }
        }
    }

    /** 
     * Gets the type of vehicle in a load.
     * @param buy true if buy load, false if sell load.
     * @return vehicle type or null if none.
     */
    private String getLoadVehicleType(boolean buy) {
        String result = null;

        Map<Good, Integer> load = null;
        if (buy) {
            load = buyLoad;
        } else {
            load = sellLoad;
        }

        Iterator<Good> i = load.keySet().iterator();
        while (i.hasNext()) {
            Good good = i.next();
            if (good.getCategory().equals(GoodType.VEHICLE)) {
                result = good.getName();
            }
        }

        return result;
    }

    /**
     * Gets the initial load vehicle.
     * @param vehicleType the vehicle type string.
     * @param buy true if buying load, false if selling load.
     * @return load vehicle.
     */
    private Vehicle getInitialLoadVehicle(String vehicleType, boolean buy) {
        Vehicle result = null;

        if (vehicleType != null) {
            Settlement settlement = null;
            if (buy) {
                settlement = tradingSettlement;
            } else {
                settlement = getStartingSettlement();
            }

            Iterator<Vehicle> j = settlement.getParkedVehicles().iterator();
            while (j.hasNext()) {
                Vehicle vehicle = j.next();
                boolean isEmpty = vehicle.getInventory().isEmpty(false);
                if (vehicleType.equalsIgnoreCase(vehicle.getDescription())) {
                    if ((vehicle != getVehicle()) && !vehicle.isReserved() && isEmpty) {
                        result = vehicle;
                    }
                }
            }
        }

        return result;
    }

    @Override
    public Map<Class, Integer> getOptionalEquipmentToLoad() {

        Map<Class, Integer> result = super.getOptionalEquipmentToLoad();

        // Add buy/sell load.
        Map<Good, Integer> load = null;
        if (outbound) {
            load = sellLoad;
        } else {
            load = buyLoad;
        }

        Iterator<Good> i = load.keySet().iterator();
        while (i.hasNext()) {
            Good good = i.next();
            if (good.getCategory().equals(GoodType.EQUIPMENT)) {
                Class equipmentClass = good.getClassType();
                int num = load.get(good);
                if (result.containsKey(equipmentClass)) {
                    num += (Integer) result.get(equipmentClass);
                }
                result.put(equipmentClass, num);
            }
        }

        return result;
    }

    @Override
    public Map<Resource, Number> getOptionalResourcesToLoad() {

        Map<Resource, Number> result = super.getOptionalResourcesToLoad();

        // Add buy/sell load.
        Map<Good, Integer> load = null;
        if (outbound) {
            load = sellLoad;
        } else {
            load = buyLoad;
        }

        Iterator<Good> i = load.keySet().iterator();
        while (i.hasNext()) {
            Good good = i.next();
            if (good.getCategory().equals(GoodType.AMOUNT_RESOURCE)) {
                AmountResource resource = (AmountResource) good.getObject();
                double amount = load.get(good).doubleValue();
                if (result.containsKey(resource)) {
                    amount += (Double) result.get(resource);
                }
                result.put(resource, amount);
            } else if (good.getCategory().equals(GoodType.ITEM_RESOURCE)) {
                ItemResource resource = (ItemResource) good.getObject();
                int num = load.get(good);
                if (result.containsKey(resource)) {
                    num += (Integer) result.get(resource);
                }
                result.put(resource, num);
            }
        }

        return result;
    }

    @Override
    public Settlement getAssociatedSettlement() {
        return getStartingSettlement();
    }

    @Override
    protected boolean isCapableOfMission(Person person) {
        if (super.isCapableOfMission(person)) {
            if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
                if (person.getSettlement() == getStartingSettlement()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void recruitPeopleForMission(Person startingPerson) {
        super.recruitPeopleForMission(startingPerson);

        // Make sure there is at least one person left at the starting settlement.
        if (!atLeastOnePersonRemainingAtSettlement(getStartingSettlement(), startingPerson)) {
            // Remove last person added to the mission.
            Person lastPerson = null;
            int amount = getPeopleNumber() - 1;
            Object[] array = getPeople().toArray();

            if (amount >= 0 && amount < array.length) {
                lastPerson = (Person) array[amount];
            }

            if (lastPerson != null) {
                lastPerson.getMind().setMission(null);
                if (getPeopleNumber() < getMinPeople()) {
                    endMission("Not enough members.");
                }
            }
        }
    }

    @Override
    protected int compareVehicles(Vehicle firstVehicle, Vehicle secondVehicle) {
        int result = super.compareVehicles(firstVehicle, secondVehicle);

        if ((result == 0) && isUsableVehicle(firstVehicle) && isUsableVehicle(secondVehicle)) {
            // Check if one has more general cargo capacity than the other.
            double firstCapacity = firstVehicle.getInventory().getGeneralCapacity();
            double secondCapacity = secondVehicle.getInventory().getGeneralCapacity();
            if (firstCapacity > secondCapacity) {
                result = 1;
            } else if (secondCapacity > firstCapacity) {
                result = -1;
            }

            // Vehicle with superior range should be ranked higher.
            if (result == 0) {
                if (firstVehicle.getRange() > secondVehicle.getRange()) {
                    result = 1;
                } else if (firstVehicle.getRange() < secondVehicle.getRange()) {
                    result = -1;
                }
            }
        }

        return result;
    }

    /**
     * Gets the trader for the mission.
     * @return the trader.
     */
    private Person getMissionTrader() {
        Person bestTrader = null;
        int bestTradeSkill = -1;

        Iterator<Person> i = getPeople().iterator();
        while (i.hasNext()) {
            Person person = i.next();
            int tradeSkill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.TRADING);
            if (tradeSkill > bestTradeSkill) {
                bestTradeSkill = tradeSkill;
                bestTrader = person;
            }
        }

        return bestTrader;
    }

    /**
     * Gets the trader and the destination settlement for the mission.
     * @return the trader.
     */
    private Person getSettlementTrader() {
        Person bestTrader = null;
        int bestTradeSkill = -1;

        Iterator<Person> i = tradingSettlement.getInhabitants().iterator();
        while (i.hasNext()) {
            Person person = i.next();
            if (!getPeople().contains(person)) {
                int tradeSkill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.TRADING);
                if (tradeSkill > bestTradeSkill) {
                    bestTradeSkill = tradeSkill;
                    bestTrader = person;
                }
            }
        }

        return bestTrader;
    }

    /**
     * Gets the load that is being sold in the trade.
     * @return sell load.
     */
    public Map<Good, Integer> getSellLoad() {
        if (sellLoad != null) {
            return new HashMap<Good, Integer>(sellLoad);
        } else {
            return null;
        }
    }

    /**
     * Gets the load that is being bought in the trade.
     * @return buy load.
     */
    public Map<Good, Integer> getBuyLoad() {
        if (buyLoad != null) {
            return new HashMap<Good, Integer>(buyLoad);
        } else {
            return null;
        }
    }

    /**
     * Gets the profit for the settlement initiating the trade.
     * @return profit (VP).
     */
    public double getProfit() {
        return profit;
    }

    /**
     * Gets the load that the starting settlement initially desires to buy.
     * @return desired buy load.
     */
    public Map<Good, Integer> getDesiredBuyLoad() {
        if (desiredBuyLoad != null) {
            return new HashMap<Good, Integer>(desiredBuyLoad);
        } else {
            return null;
        }
    }

    /**
     * Gets the profit initially expected by the starting settlement.
     * @return desired profit (VP).
     */
    public double getDesiredProfit() {
        return desiredProfit;
    }

    /**
     * Gets the settlement that the starting settlement is trading with.
     * @return trading settlement.
     */
    public Settlement getTradingSettlement() {
        return tradingSettlement;
    }

    /**
     * Estimates the profit for the starting settlement for a given buy load.
     * @param buyingLoad the load to buy.
     * @return profit (VP).
     */
    private double estimateTradeProfit(Map<Good, Integer> buyingLoad) {
        double result = 0D;

        try {
            double sellingValueHome = TradeUtil.determineLoadValue(sellLoad, getStartingSettlement(), false);
            double sellingValueRemote = TradeUtil.determineLoadValue(sellLoad, tradingSettlement, true);
            double sellingProfit = sellingValueRemote - sellingValueHome;

            double buyingValueHome = TradeUtil.determineLoadValue(buyingLoad, getStartingSettlement(), true);
            double buyingValueRemote = TradeUtil.determineLoadValue(buyingLoad, tradingSettlement, false);
            double buyingProfit = buyingValueHome - buyingValueRemote;

            double totalProfit = sellingProfit + buyingProfit;

            double estimatedDistance = getStartingSettlement().getCoordinates().getDistance(
                    tradingSettlement.getCoordinates()) * 2D;
            double missionCost = TradeUtil.getEstimatedMissionCost(getStartingSettlement(), getRover(),
                    estimatedDistance);

            result = totalProfit - missionCost;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            endMission("Could not estimate trade profit.");
        }

        return result;
    }

    @Override
    public void destroy() {
        super.destroy();

        tradingSettlement = null;
        if (sellLoad != null) sellLoad.clear();
        sellLoad = null;
        if (buyLoad != null) buyLoad.clear();
        buyLoad = null;
        if (desiredBuyLoad != null) desiredBuyLoad.clear();
        desiredBuyLoad = null;
        startNegotiationTime = null;
        negotiationTask = null;
    }

    /**
     * Inner class for storing trade profit info.
     */
    public static class TradeProfitInfo {

        public double profit;
        public MarsClock time;

        public TradeProfitInfo(double profit, MarsClock time) {
            this.profit = profit;
            this.time = time;
        }
    }

    @Override
    public Map<Class, Integer> getEquipmentNeededForRemainingMission(
            boolean useBuffer) {
        if (equipmentNeededCache != null) return equipmentNeededCache;
        else {
            Map<Class, Integer> result = new HashMap<Class, Integer>(0);
            equipmentNeededCache = result;
            return result;
        }
    }
}