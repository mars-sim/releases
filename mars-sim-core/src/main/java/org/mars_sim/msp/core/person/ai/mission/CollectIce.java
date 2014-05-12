/**
 * Mars Simulation Project
 * CollectIce.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.mission;

import java.util.Collection;
import java.util.List;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.equipment.Bag;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.goods.GoodsManager;
import org.mars_sim.msp.core.structure.goods.GoodsUtil;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.vehicle.Rover;

/** 
 * The Exploration class is a mission to travel in a rover to several
 * random locations around a settlement and collect ice.
 */
public class CollectIce extends CollectResourcesMission {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

//	private static Logger logger = Logger.getLogger(CollectIce.class.getName());

	/** Default description. */
	public static final String DEFAULT_DESCRIPTION = "Ice Prospecting";

	/** Amount of ice to be gathered at a given site (kg). */
	private static final double SITE_GOAL = 1000D;

	/** Number of bags required for the mission. */
	public static final int REQUIRED_BAGS = 20;

	/** Collection rate of ice during EVA (kg/millisol). */
	private static final double COLLECTION_RATE = 1D;

	/** Number of collection sites. */
	private static final int NUM_SITES = 1;

	/** Minimum number of people to do mission. */
	private final static int MIN_PEOPLE = 2;

	/**
	 * Constructor
	 * @param startingPerson the person starting the mission.
	 * @throws MissionException if problem constructing mission.
	 */
	public CollectIce(Person startingPerson) {
		
		// Use CollectResourcesMission constructor.
		super(DEFAULT_DESCRIPTION, startingPerson, getIceResource(), SITE_GOAL, 
				COLLECTION_RATE, Bag.class, REQUIRED_BAGS, NUM_SITES, MIN_PEOPLE);
	}
	
    /**
     * Constructor with explicit data.
     * @param members collection of mission members.
     * @param startingSettlement the starting settlement.
     * @param iceCollectionSites the sites to collect ice.
     * @param rover the rover to use.
     * @param description the mission's description.
     * @throws MissionException if error constructing mission.
     */
    public CollectIce(Collection<Person> members, Settlement startingSettlement, 
    		List<Coordinates> iceCollectionSites, Rover rover, 
    		String description) {
    	
       	// Use CollectResourcesMission constructor.
    	super(description, members, startingSettlement, getIceResource(), SITE_GOAL, 
                COLLECTION_RATE, Bag.class, REQUIRED_BAGS, iceCollectionSites.size(), 
                1, rover, iceCollectionSites);
    }
	
	/** 
	 * Gets the weighted probability that a given person would start this mission.
	 * @param person the given person
	 * @return the weighted probability
	 */
	public static double getNewMissionProbability(Person person) {

		double result = CollectResourcesMission.getNewMissionProbability(person, Bag.class, 
				REQUIRED_BAGS, MIN_PEOPLE, CollectIce.class);
		
		if (result > 0D) {
		    
		    // Factor the value of ice at the settlement.
		    GoodsManager manager = person.getSettlement().getGoodsManager();
		    AmountResource iceResource = AmountResource.findAmountResource("ice");
		    double value = manager.getGoodValuePerItem(GoodsUtil.getResourceGood(iceResource));
		    result *= value;
		    if (result > 10D) {
		        result = 10D;
		    }

		    // Don't start mission until after first Sol of the simulation.
		    MarsClock startTime = Simulation.instance().getMasterClock().getInitialMarsTime();
		    MarsClock currentTime = Simulation.instance().getMasterClock().getMarsClock();
		    double totalTimeSols = MarsClock.getTimeDiff(currentTime, startTime) / 1000D;
		    if (totalTimeSols < 1D) {
		        result = 0;
		    }
			
			// Check if min number of EVA suits at settlement.
			if (Mission.getNumberAvailableEVASuitsAtSettlement(person.getSettlement()) < MIN_PEOPLE) {
			    result = 0D;
			}
		}
		
		return result;
	}
	
    /**
     * Gets the description of a collection site.
     * @param siteNum the number of the site.
     * @return description
     */
    protected String getCollectionSiteDescription(int siteNum) {
    	return "prospecting site";
    }
    
    /**
     * Gets the ice resource.
     * @return ice resource.
     * @throws MissionException if error getting ice resource.
     */
    private static AmountResource getIceResource() {
        return AmountResource.findAmountResource("ice");
    }
}