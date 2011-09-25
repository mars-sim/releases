/**
 * Mars Simulation Project
 * TravelMission.java
 * @version 3.00 2010-08-10
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.mission;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.time.MarsClock;

import java.util.ArrayList;
import java.util.List;

/**
 * A mission that involves traveling along a series of navpoints.
 */
public abstract class TravelMission extends Mission {

	// Mission event types
	public static final String TRAVEL_STATUS_EVENT = "travel status";
	public static final String NAVPOINTS_EVENT = "navpoints";
	public static final String DISTANCE_EVENT = "distance";
	
	// Travel Mission status
	public final static String AT_NAVPOINT = "At a navpoint";
	public final static String TRAVEL_TO_NAVPOINT = "Traveling to navpoint";
	
	// Data members
	private List<NavPoint> navPoints = new ArrayList<NavPoint>();  // List of navpoints for the mission.
	private int navIndex = 0; // The current navpoint index.
	private String travelStatus; // The current traveling status of the mission. 
	private NavPoint lastStopNavpoint; // The last navpoint the mission stopped at.
	private MarsClock legStartingTime;  // The time the last leg of the mission started at. 

	/**
	 * Constructor
	 * (note: The constructor handles setting the initial nav point.)
	 * @param name the name of the mission.
	 * @param startingPerson the person starting the mission.
	 * @param minPeople the minimum number of people required for mission.
	 * @throws MissionException if error constructing mission.
	 */
	protected TravelMission(String name, Person startingPerson, int minPeople) {
		// Use Mission constructor.
		super(name, startingPerson, minPeople);
		
		NavPoint startingNavPoint = null;
//		try {
			if (startingPerson.getSettlement() != null) 
				startingNavPoint = new NavPoint(getCurrentMissionLocation(), startingPerson.getSettlement(), 
						startingPerson.getSettlement().getName());
			else startingNavPoint = new NavPoint(getCurrentMissionLocation(), "starting location");
			addNavpoint(startingNavPoint);
			lastStopNavpoint = startingNavPoint;
//		}
//		catch (Exception e) {
//			throw new MissionException(getPhase(), e);
//		}
		
		setTravelStatus(AT_NAVPOINT);
	}
	
	/**
	 * Adds a navpoint to the mission.
	 * @param navPoint the new nav point location to be added.
	 * @throws IllegalArgumentException if location is null.
	 */
	public final void addNavpoint(NavPoint navPoint) {
		if (navPoint != null) {
			navPoints.add(navPoint);
			fireMissionUpdate(NAVPOINTS_EVENT);
		}
		else throw new IllegalArgumentException("navPoint is null");
	}
	
	/**
	 * Sets a nav point for the mission.
	 * @param index the index in the list of nav points.
	 * @param navPoint the new navpoint
	 * @throws IllegalArgumentException if location is null or index < 0.
	 */
	protected final void setNavpoint(int index, NavPoint navPoint) {
		if ((navPoint != null) && (index >= 0)) {
			navPoints.set(index, navPoint);
			fireMissionUpdate(NAVPOINTS_EVENT);
		}
		else throw new IllegalArgumentException("navPoint is null");
	}
	
	/**
	 * Clears out any unreached nav points.
	 */
	public final void clearRemainingNavpoints() {
		int index = getNextNavpointIndex();
		int numNavpoints = getNumberOfNavpoints();
		for (int x = index; x < numNavpoints; x++) {
			navPoints.remove(index);
			fireMissionUpdate(NAVPOINTS_EVENT);
		}
	}
	
	/**
	 * Gets the last navpoint reached.
	 * @return navpoint
	 */
	public final NavPoint getPreviousNavpoint() {
		return lastStopNavpoint;
	}
	
	/**
	 * Gets the mission's next navpoint.
	 * @return navpoint or null if no more navpoints.
	 */
	public final NavPoint getNextNavpoint() {
		if (navIndex < navPoints.size()) return navPoints.get(navIndex);
		else return null;
	}
	
	/**
	 * Gets the mission's next navpoint index.
	 * @return navpoint index or -1 if none.
	 */
	public final int getNextNavpointIndex() {
		if (navIndex < navPoints.size()) return navIndex;
		else return -1;
	}
	
	/**
	 * Set the next navpoint index.
	 * @param newNavIndex the next navpoint index.
	 * @throws MissionException if the new navpoint is out of range.
	 */
	public final void setNextNavpointIndex(int newNavIndex) {
		if (newNavIndex < getNumberOfNavpoints()) {
			navIndex = newNavIndex;
		}
		else throw new IllegalStateException(getPhase() + " : newNavIndex: " + newNavIndex + " is outOfBounds.");
	}
	
	/**
	 * Gets the navpoint at an index value.
	 * @param index the index value
	 * @return navpoint
	 * @throws IllegaArgumentException if no navpoint at that index.
	 */
	public final NavPoint getNavpoint(int index) {
		if ((index >= 0) && (index < getNumberOfNavpoints())) return navPoints.get(index);
		else throw new IllegalArgumentException("index: " + index + " out of bounds.");
	}
	
	/**
	 * Gets the index of a navpoint.
	 * @param navpoint the navpoint
	 * @return index or -1 if navpoint isn't in the trip.
	 */
	public final int getNavpointIndex(NavPoint navpoint) {
		if (navpoint == null) throw new IllegalArgumentException("navpoint is null");
		if (navPoints.contains(navpoint)) return navPoints.indexOf(navpoint);
		else return -1;
	}
	
	/**
	 * Gets the number of navpoints on the trip.
	 * @return number of navpoints
	 */
	public final int getNumberOfNavpoints() {
		return navPoints.size();
	}
	
	/**
	 * Gets the current navpoint the mission is stopped at.
	 * @return navpoint or null if mission is not stopped at a navpoint.
	 */
	public final NavPoint getCurrentNavpoint() {
		if (AT_NAVPOINT.equals(travelStatus)) {
			if (navIndex < navPoints.size()) return navPoints.get(navIndex);
			else return null;
		}
		else return null;
	}
	
	/**
	 * Gets the index of the current navpoint the mission is stopped at.
	 * @return index of current navpoint or -1 if mission is not stopped at a navpoint.
	 */
	public final int getCurrentNavpointIndex() {
		if (AT_NAVPOINT.equals(travelStatus)) return navIndex;
		else return -1;
	}
	
	/**
	 * Get the travel mission's current status.
	 * @return travel status as a String.
	 */
	public final String getTravelStatus() {
		return travelStatus;
	}
	
	/**
	 * Set the travel mission's current status.
	 * @param newTravelStatus the mission travel status.
	 */
	private void setTravelStatus(String newTravelStatus) {
		travelStatus = newTravelStatus;
		fireMissionUpdate(TRAVEL_STATUS_EVENT);
	}
	
	/**
	 * Starts travel to the next navpoint in the mission.
	 * @throws MissionException if no more navpoints.
	 */
	protected final void startTravelToNextNode() {
		setNextNavpointIndex(navIndex + 1);
		setTravelStatus(TRAVEL_TO_NAVPOINT);
		legStartingTime = (MarsClock) Simulation.instance().getMasterClock().getMarsClock().clone();
	}
	
	/**
	 * The mission has reached the next navpoint.
	 * @throws MisisonException if error determining mission location.
	 */
	protected final void reachedNextNode() {
		setTravelStatus(AT_NAVPOINT);
		lastStopNavpoint = getCurrentNavpoint();
	}
	
	/**
	 * Performs the travel phase of the mission.
	 * @param person the person currently performing the mission.
	 * @throws MissionException if error performing travel phase.
	 */
	protected abstract void performTravelPhase(Person person);
	
	/**
	 * Gets the starting time of the current leg of the mission.
	 * @return starting time
	 */
	protected final MarsClock getCurrentLegStartingTime() {
		if (legStartingTime != null) return (MarsClock) legStartingTime.clone();
		else return null;
	}
	
	/**
	 * Gets the distance of the current leg of the mission, or 0 if not in the travelling phase.
	 * @return distance (km) 
	 */
	public final double getCurrentLegDistance() {
		if (TRAVEL_TO_NAVPOINT.equals(travelStatus)) 
			return lastStopNavpoint.getLocation().getDistance(getNextNavpoint().getLocation());
		else return 0D;
	}
	
	/**
	 * Gets the remaining distance for the current leg of the mission.
	 * @return distance (km) or 0 if not in the travelling phase.
	 * @throws MissionException if error determining distance.
	 */
	public final double getCurrentLegRemainingDistance() {
		if (travelStatus.equals(TRAVEL_TO_NAVPOINT))
			return getCurrentMissionLocation().getDistance(getNextNavpoint().getLocation());
		else return 0D;
	}
	
	/**
	 * Gets the total distance of the trip.
	 * @return total distance (km)
	 */
	public final double getTotalDistance() {
		double result = 0D;
		if (navPoints.size() > 1) {
			for (int x = 1; x < navPoints.size(); x++) {
				NavPoint prevNav = navPoints.get(x - 1);
				NavPoint currNav = navPoints.get(x);
				double distance = currNav.getLocation().getDistance(prevNav.getLocation());
				result += distance;
			}
		}
		return result;
	}
	
	/**
	 * Gets the total remaining distance to travel in the mission.
	 * @return distance (km).
	 * @throws MissionException if error determining distance.
	 */
	public final double getTotalRemainingDistance() {
		double result = getCurrentLegRemainingDistance();
		
		int index = 0;
		if (AT_NAVPOINT.equals(travelStatus)) index = getCurrentNavpointIndex();
		else if (TRAVEL_TO_NAVPOINT.equals(travelStatus)) index = getNextNavpointIndex();
		
		for (int x = (index + 1); x < getNumberOfNavpoints(); x++) 
			result += getNavpoint(x - 1).getLocation().getDistance(getNavpoint(x).getLocation());
		
		return result;
	}
	
	/**
	 * Gets the total distance travelled during the mission so far.
	 * @return distance (km)
	 */
	public abstract double getTotalDistanceTravelled();
	
    /**
     * Gets the estimated time of arrival (ETA) for the current leg of the mission.
     * @return time (MarsClock) or null if not applicable.
     */
    public abstract MarsClock getLegETA();
    
    /**
     * Gets the estimated time remaining for the mission.
     * @param useBuffer use a time buffer in estimation if true.
     * @return time (millisols)
     * @throws MissionException
     */
    public abstract double getEstimatedRemainingMissionTime(boolean useBuffer) ;
    
    /**
     * Gets the estimated time for a trip.
     * @param useBuffer use time buffers in estimation if true.
     * @param distance the distance of the trip.
     * @return time (millisols)
     * @throws MissionException
     */
    public abstract double getEstimatedTripTime(boolean useBuffer, double distance) ;
    
    /**
     * Update mission to the next navpoint destination.
     */
    public abstract void updateTravelDestination();
}