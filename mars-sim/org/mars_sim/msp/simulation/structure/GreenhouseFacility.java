/**
 * Mars Simulation Project
 * GreenhouseFacility.java
 * @version 2.74 2002-03-11
 * @author Scott Davis
 */

package org.mars_sim.msp.simulation.structure;

import org.mars_sim.msp.simulation.*;
import java.io.Serializable;

/**
 * The GreenhouseFacility class represents the greenhouses in a settlement.
 * It defines the amount of fresh and dried foods generated by the greenhouses.
 */

public class GreenhouseFacility extends Facility implements Serializable {

    // Data members
    private double workLoad; // Amount of work time (in millisols) tending greenhouse required during growth period for full harvest.
    private double growingWork; // Amount of work time (in millisols) completed for growing phase.
    private double workCompleted; // Amount of work time (in millisols) completed for current phase.
    private double growthPeriodCompleted; // Amount of time completed in current growth period.
    private String phase; // "Inactive", "Planting", "Growing" or "Harvesting"

    /** Constructor for random creation. 
     *  @param manager of the greenhouse facility
     */
    public GreenhouseFacility(FacilityManager manager) {

        // Use Facility's constructor.
        super(manager, "Greenhouse");

        // Initialize data members
        workCompleted = 0D;
        growthPeriodCompleted = 0D;
        phase = "Inactive";

        // Determine work load based on full harvest amount.
        // (3200 mSols for 200 kg food - 4800 mSols for 300 kg food)
        workLoad = 16D * getFullHarvestAmount();
    }

    /** Returns the harvest amount of the greenhouse. 
     *  @return Amount of food (kg) the greenhouse can produce at full harvest
     */
    public double getFullHarvestAmount() {
        SimulationProperties properties = manager.getMars().getSimulationProperties();
        return properties.getGreenhouseFullHarvest();
    }

    /** Returns the work load of the greenhouse.  
     *  @return tending work required for a full harvest (in work time (millisols))
     */
    public double getWorkLoad() {
        return workLoad;
    }

    /** Returns the work completed in this cycle in the growing phase. 
     *  @return work completed so far in the growing phase (in work time (millisols))
     */
    public double getGrowingWork() {
        return growingWork;
    }

    /** Returns the growth period of the greenhouse. (in millisols) 
     *  @return time required to grow crops (in millisols)
     */
    public double getGrowthPeriod() {
        SimulationProperties properties = manager.getMars().getSimulationProperties();
        return properties.getGreenhouseGrowingCycle();
    }

    /** Returns the current work completed on the current phase.  
     *  @return work completed so far in this phase (in work time (millisols))
     */
    public double getWorkCompleted() {
        return workCompleted;
    }

    /** Returns the time completed of the current growth cycle. (millisols) 
     *  @return time completed in growth cycle (millisols)
     */
    public double getTimeCompleted() {
        return growthPeriodCompleted;
    }

    /** Returns true if a harvest cycle has been started. 
     *  @return current phase
     */
    public String getPhase() {
        return phase;
    }

    /** Adds work to the work completed on a growth cycle. 
     *  @param work time added to growth cycle (in millisols)
     */
    public void addWorkToGrowthCycle(double time) {

        double plantingWork = 1000D;
        double harvestingWork = 10D * getFullHarvestAmount();
        double workInPhase = workCompleted + time;

        if (phase.equals("Inactive"))
            phase = "Planting";

        if (phase.equals("Planting")) {
            if (workInPhase >= plantingWork) {
                workInPhase -= plantingWork;
                phase = "Growing";
            }
        }

        if (phase.equals("Growing"))
            growingWork = workInPhase;

        if (phase.equals("Harvesting")) {
            if (workInPhase >= harvestingWork) {
                workInPhase -= harvestingWork;
                double foodProduced = getFullHarvestAmount() * (growingWork / workLoad);
		manager.getSettlement().getInventory().addResource(Inventory.FOOD, foodProduced);
                phase = "Planting";
                growingWork = 0D;
                growthPeriodCompleted = 0D;
            }
        }

        workCompleted = workInPhase;
    }

    /** Override Facility's timePasses method to allow for harvest cycle. 
     *  @param time the amount of time passing (in millisols) 
     */
    void timePassing(double time) {

        if (phase.equals("Growing")) {
            growthPeriodCompleted += time;
            if (growthPeriodCompleted >= getGrowthPeriod()) {
                phase = "Harvesting";
                workCompleted = 0D;
            }
        }
    }
}
