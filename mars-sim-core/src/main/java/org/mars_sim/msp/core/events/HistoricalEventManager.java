/**
 * Mars Simulation Project
 * HistoricalEventManager.java
 * @version 3.07 2014-10-31
 * @author Barry Evans
 */

package org.mars_sim.msp.core.events;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.MasterClock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class provides a manager that maintains a model of the events that
 * have occurred during the current simulation run. It provides support for a
 * listener pattern so the external objects can be notified when new events have
 * been registered.
 * The manager maintains an ordered list in terms of decreasing time, i.e.
 * most recent event first.
 * It should be noted that the throughput of new events of the manager can be
 * in the order of 100 event per simulation tick.
 */
public class HistoricalEventManager {

	/**
	 * This defines the maximum number of events that are stored.
	 * It should be a standard property.
	 */
	private final static int TRANSIENT_EVENTS = 1000;

	private List<HistoricalEventListener> listeners = new ArrayList<HistoricalEventListener>();
	private List<HistoricalEvent> events = new LinkedList<HistoricalEvent>();
	private MarsClock mainClock;
	//private static int count;
	/**
	 * Create a new EventManager that represents a particular simulation.
	 */
	public HistoricalEventManager() {
		// The main clock is not initialized until the simulation start
		this.mainClock = null;
		//count++;
		//System.out.println("HistoricalEventManager.java : constructor : count is " + count);	
	}

	/**
	 * Add a historical event listener
	 * @param newListener listener to add.
	 */
	public void addListener(HistoricalEventListener newListener) {
		if (!listeners.contains(newListener)) listeners.add(newListener);
	}

	/**
	 * Removes a historical event listener.
	 * @param oldListener listener to remove.
	 */
	public void removeListener(HistoricalEventListener oldListener) {
		if (listeners.contains(oldListener)) listeners.remove(oldListener);
	}

	/**
	 * Get the event at a specified index.
	 * @param index Index of event to retrieve.
	 * @return Historical event.
	 */
	public HistoricalEvent getEvent(int index) {
		return events.get(index);
	}

	/**
	 * An new event needs registering with the manager. The event will be
	 * time stamped with the current clock time and inserted at position zero.
	 * @param newEvent The event to register.
	 */
	// include any kind of events
	public void registerNewEvent(HistoricalEvent newEvent) {

		MarsClock timestamp;

		//System.out.println("HistoricalEventManager.java : calling registerNewEvent() : newEvent is " + newEvent); 
		// check if event is MALFUNCTION or MEDICAL, save it for notification box display	
		
		// Make space for the new event.
		if (events.size() >= TRANSIENT_EVENTS) {
			int excess = events.size() - (TRANSIENT_EVENTS - 1);
			removeEvents(events.size() - excess, excess);
		}

		MasterClock masterClock = Simulation.instance().getMasterClock();
		if (masterClock != null) {
			if (mainClock == null) mainClock = masterClock.getMarsClock();
			 timestamp = (MarsClock) mainClock.clone();
			newEvent.setTimestamp(timestamp);
		}

				
		events.add(0, newEvent);

		Iterator<HistoricalEventListener> iter = listeners.iterator();
		while (iter.hasNext()) iter.next().eventAdded(0, newEvent);
		
	}

	/**
	 * An event is removed from the list.
	 * @param index Index of the event to be removed.
	 * @param number Number to remove.
	 */
	private void removeEvents(int index, int number) {

		// Remove the rows
		for(int i = index; i < (index + number); i++) {
			events.remove(i);
		}

		Iterator<HistoricalEventListener> iter = listeners.iterator();
		while(iter.hasNext()) iter.next().eventsRemoved(index, index + number);
	}

	/**
	 * Get the number of events in the manager.
	 * @return Stored event count.
	 */
	public int size() {
		return events.size();
	}

	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		listeners.clear();
		listeners = null;
		events.clear();
		events = null;
		mainClock = null;
	}
}