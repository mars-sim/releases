/**
 * Mars Simulation Project
 * TradeMeta.java
 * @version 3.07 2014-09-18
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.mission.meta;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.RoverMission;
import org.mars_sim.msp.core.person.ai.mission.Trade;
import org.mars_sim.msp.core.person.ai.mission.TradeUtil;
import org.mars_sim.msp.core.person.ai.mission.VehicleMission;
import org.mars_sim.msp.core.person.ai.mission.Trade.TradeProfitInfo;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * A meta mission for the Trade mission.
 */
public class TradeMeta implements MetaMission {

    /** Mission name */
    private static final String NAME = Msg.getString(
            "Mission.description.trade"); //$NON-NLS-1$
    
    /** default logger. */
    private static Logger logger = Logger.getLogger(TradeMeta.class.getName());
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Mission constructInstance(Person person) {
        return new Trade(person);
    }

    @Override
    public double getProbability(Person person) {
        
        double missionProbability = 0D;

        // Check if person is in a settlement.
        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {

            // Check if mission is possible for person based on their circumstance.
            boolean missionPossible = true;
            Settlement settlement = person.getSettlement();

            // Check if available rover.
            if (!RoverMission.areVehiclesAvailable(settlement, false)) {
                missionPossible = false;
            }

            // Check if available backup rover.
            if (!RoverMission.hasBackupRover(settlement)) {
                missionPossible = false;
            }

            // Check if minimum number of people are available at the settlement.
            // Plus one to hold down the fort.
            if (!RoverMission.minAvailablePeopleAtSettlement(settlement, RoverMission.MIN_PEOPLE + 1)) {
                missionPossible = false;
            }

            // Check if min number of EVA suits at settlement.
            if (Mission.getNumberAvailableEVASuitsAtSettlement(settlement) < RoverMission.MIN_PEOPLE) {
                missionPossible = false;
            }

            // Check for the best trade settlement within range.
            double tradeProfit = 0D;
            try {
                Rover rover = (Rover) RoverMission.getVehicleWithGreatestRange(settlement, false);
                if (rover != null) {
                    // Only check every couple of Sols, else use cache.
                    // Note: this method is very CPU intensive.
                    boolean useCache = false;
                    MarsClock currentTime = Simulation.instance().getMasterClock().getMarsClock();
                    if (Trade.TRADE_PROFIT_CACHE.containsKey(settlement)) {
                        TradeProfitInfo profitInfo = Trade.TRADE_PROFIT_CACHE.get(settlement);
                        double timeDiff = MarsClock.getTimeDiff(currentTime, profitInfo.time);
                        if (timeDiff < 2000D) {
                            tradeProfit = profitInfo.profit;
                            useCache = true;
                        }
                    }
                    else {
                        Trade.TRADE_PROFIT_CACHE.put(settlement, new TradeProfitInfo(tradeProfit,
                                (MarsClock) currentTime.clone()));
                        useCache = true;
                    }

                    if (!useCache) {
                        double startTime = System.currentTimeMillis();
                        tradeProfit = TradeUtil.getBestTradeProfit(settlement, rover);
                        double endTime = System.currentTimeMillis();
                        logger.info(settlement.getName() + " getBestTradeProfit: " + (endTime - startTime)
                                + " milliseconds - TP: " + (int) tradeProfit + " VP");
                        Trade.TRADE_PROFIT_CACHE.put(settlement, new TradeProfitInfo(tradeProfit,
                                (MarsClock) currentTime.clone()));
                        Trade.TRADE_SETTLEMENT_CACHE.put(settlement, TradeUtil.bestTradeSettlementCache);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error finding vehicles at settlement.", e);
            }

            // Check for embarking missions.
            if (VehicleMission.hasEmbarkingMissions(settlement)) {
                missionPossible = false;
            }

            // Check if settlement has enough basic resources for a rover mission.
            if (!RoverMission.hasEnoughBasicResources(settlement)) {
                missionPossible = false;
            }
            
            // Check if starting settlement has minimum amount of methane fuel.
            AmountResource methane = AmountResource.findAmountResource("methane");
            if (settlement.getInventory().getAmountResourceStored(methane, false) < 
                    RoverMission.MIN_STARTING_SETTLEMENT_METHANE) {
                missionPossible = false;
            }

            // Determine mission probability.
            if (missionPossible) {

                // Trade value modifier.
                missionProbability = tradeProfit / 1000D;
                if (missionProbability > Trade.MAX_STARTING_PROBABILITY) {
                    missionProbability = Trade.MAX_STARTING_PROBABILITY;
                }

                // Crowding modifier.
                int crowding = settlement.getCurrentPopulationNum() - settlement.getPopulationCapacity();
                if (crowding > 0) {
                    missionProbability *= (crowding + 1);
                }

                // Job modifier.
                Job job = person.getMind().getJob();
                if (job != null) {
                    missionProbability *= job.getStartMissionProbabilityModifier(Trade.class);
                }
            }
        }

        return missionProbability;
    }
}