/**
 * Mars Simulation Project
 * TransportManager.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */
package org.mars_sim.msp.core.interplanetary.transport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.interplanetary.transport.resupply.ResupplyUtil;
import org.mars_sim.msp.core.interplanetary.transport.settlement.ArrivingSettlementUtil;
import org.mars_sim.msp.core.time.MarsClock;

/**
 * A manager for interplanetary transportation.
 */
public class TransportManager implements Serializable {

	private static Logger logger = Logger.getLogger(TransportManager.class.getName());
    
	// Data members
	private Collection<Transportable> transportItems;
	
	/**
	 * Constructor
	 */
	public TransportManager() {
		
		//Initialize data
		transportItems = new ConcurrentLinkedQueue<Transportable>();
		
		// Create initial arriving settlements.
		transportItems.addAll(ArrivingSettlementUtil.createInitialArrivingSettlements());
		
		// Create initial resupply missions.
		transportItems.addAll(ResupplyUtil.createInitialResupplyMissions());
	}
	
	/**
	 * Adds a new transport item.
	 * @param transportItem the new transport item.
	 */
	public void addNewTransportItem(Transportable transportItem) {
	    transportItems.add(transportItem);
	    HistoricalEvent newEvent = new TransportEvent(transportItem, TransportEvent.TRANSPORT_ITEM_CREATED,
            "Transport item created: " + transportItem.getName());
	    Simulation.instance().getEventManager().registerNewEvent(newEvent);
	    logger.info("New transport item created: " + transportItem.toString());
	}
	
	/**
	 * Gets all of the transport items.
	 * @return list of all transport items.
	 */
	public List<Transportable> getAllTransportItems() {
		return new ArrayList<Transportable>(transportItems);
	}
	
	/**
	 * Gets the transport items that are planned or in transit.
	 * @return transportables.
	 */
	public List<Transportable> getIncomingTransportItems() {
	    List<Transportable> incoming = new ArrayList<Transportable>(transportItems.size());
	    
	    Iterator<Transportable> i = transportItems.iterator();
	    while (i.hasNext()) {
	    	Transportable transportItem = i.next();
	        String state = transportItem.getTransitState();
	        if (Transportable.PLANNED.equals(state) || Transportable.IN_TRANSIT.equals(state)) {
	            incoming.add(transportItem);
	        }
	    }
	    
	    return incoming;
	}
	
	/**
	 * Gets the transport items that have already arrived.
	 * @return transportables.
	 */
	public List<Transportable> getArrivedTransportItems() {
	    List<Transportable> arrived = new ArrayList<Transportable>(transportItems.size());
        
	    Iterator<Transportable> i = transportItems.iterator();
        while (i.hasNext()) {
        	Transportable transportItem = i.next();
            String state = transportItem.getTransitState();
            if (Transportable.ARRIVED.equals(state)) {
                arrived.add(transportItem);
            }
        }
        
        return arrived;
	}
	
	/**
	 * Cancels a transport item.
	 * @param transportItem the transport item.
	 */
	public void cancelTransportItem(Transportable transportItem) {
	    transportItem.setTransitState(Transportable.CANCELED);
	    HistoricalEvent cancelEvent = new TransportEvent(transportItem, TransportEvent.TRANSPORT_ITEM_CANCELLED,
                "Transport item cancelled");
	    Simulation.instance().getEventManager().registerNewEvent(cancelEvent);
	    logger.info("Transport item cancelled: " + transportItem.toString());
	}
	
	/**
	 * Time passing.
	 *
	 * @param time amount of time passing (in millisols)
	 * @throws Exception if error.
	 */
	public void timePassing(double time) {
		Iterator<Transportable> i = transportItems.iterator();
		while (i.hasNext()) {
			Transportable transportItem = i.next();
			MarsClock currentTime = Simulation.instance().getMasterClock().getMarsClock();
			if (Transportable.PLANNED.equals(transportItem.getTransitState())) {
			    if (MarsClock.getTimeDiff(currentTime, transportItem.getLaunchDate()) >= 0D) {
			        // Transport item is launched.
			        transportItem.setTransitState(Transportable.IN_TRANSIT);
			        HistoricalEvent deliverEvent = new TransportEvent(transportItem, 
			        		TransportEvent.TRANSPORT_ITEM_LAUNCHED, "Transport item launched");
			        Simulation.instance().getEventManager().registerNewEvent(deliverEvent);
			        logger.info("Transport item launched: " + transportItem.toString());
			        continue;
			    }
			}
			else if (Transportable.IN_TRANSIT.equals(transportItem.getTransitState())) {
			    if (MarsClock.getTimeDiff(currentTime, transportItem.getArrivalDate()) >= 0D) {
                    // Transport item has arrived on Mars.
                    transportItem.setTransitState(Transportable.ARRIVED);
                    transportItem.performArrival();
                    HistoricalEvent arrivalEvent = new TransportEvent(transportItem, 
                    		TransportEvent.TRANSPORT_ITEM_ARRIVED, "Transport item arrived on Mars");
                    Simulation.instance().getEventManager().registerNewEvent(arrivalEvent);
                    logger.info("Transport item arrived: " + transportItem.toString());
                }
			}
		}
	}
	
	/**
	 * Prepare object for garbage collection.
	 */
    public void destroy() {
        Iterator<Transportable> i = transportItems.iterator();
        while (i.hasNext()) i.next().destroy();
        transportItems.clear();
        transportItems = null;
    }   
}