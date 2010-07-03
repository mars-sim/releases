/**
 * Mars Simulation Project
 * ResupplyManager.java
 * @version 2.81 2007-08-27
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure;

import java.io.Serializable;
import java.util.*;

import org.mars_sim.msp.core.*;
import org.mars_sim.msp.core.time.MarsClock;

/**
 * Manages resupply missions from Earth for a settlement.
 */
public class ResupplyManager implements Serializable {
	
	// Data members
	private List<Resupply> resupplies;
	
	/**
	 * Constructor
	 * @param settlement the settlement the manager is for.
	 * @throws Exception if problem creating resupply missions.
	 */
	ResupplyManager(Settlement settlement) throws Exception {
		
		//Initialize data
		resupplies = new ArrayList<Resupply>();
		
		// Create resupply missions.
		SettlementConfig config = SimulationConfig.instance().getSettlementConfiguration();
		String templateName = settlement.getTemplate();
		MarsClock currentTime = Simulation.instance().getMasterClock().getMarsClock();
		
		try {
			for (int x = 0; x < config.getNumberOfTemplateResupplies(templateName); x++ ) {
				String resupplyName = config.getTemplateResupplyName(templateName, x);
				double timeUntilArrival = config.getTemplateResupplyArrivalTime(templateName, x);
				MarsClock arrivalDate = (MarsClock) currentTime.clone();
				arrivalDate.addTime(timeUntilArrival * 1000D);
				resupplies.add(new Resupply(arrivalDate, resupplyName, settlement));
			}
		}
		catch (Exception e) {
			throw new Exception("ResupplyManager.constructor: " + e.getMessage());
		}
	}
	
	/**
	 * Gets the settlement resupply missions.
	 * @return list of resupply missions.
	 */
	public List<Resupply> getResupplies() {
		return resupplies;
	}
	
	/**
	 * Time passing at settlement.
	 *
	 * @param time amount of time passing (in millisols)
	 * @throws Exception if error.
	 */
	public void timePassing(double time) throws Exception {
		try {
			Iterator<Resupply> i = resupplies.iterator();
			while (i.hasNext()) {
				Resupply resupply = i.next();
				if (!resupply.isDelivered()) {
					MarsClock currentDate = Simulation.instance().getMasterClock().getMarsClock();
					if (MarsClock.getTimeDiff(resupply.getArrivalDate(), currentDate) <= 0D) {
						// Deliver supplies
						resupply.deliverSupplies();
					}
				}
			} 
		}
		catch (Exception e) {
			throw new Exception("ResupplyManager.timePassing(): " + e.getMessage());
		}
	}   	
}