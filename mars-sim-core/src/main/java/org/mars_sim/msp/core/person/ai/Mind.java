/**
 * Mars Simulation Project
 * Mind.java
 * @version 2.88 2009-12-21
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.*;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.*;
import org.mars_sim.msp.core.person.ai.mission.*;
import org.mars_sim.msp.core.person.ai.task.*;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.vehicle.Vehicle;

/** The Mind class represents a person's mind.
 *  It keeps track of missions and tasks which
 *  the person is involved.
 */
public class Mind implements Serializable {
    
    private static String CLASS_NAME = 
        "org.mars_sim.msp.simulation.person.ai.Mind";
	
	private static Logger logger = Logger.getLogger(CLASS_NAME);

	// Unit events
	public static final String JOB_EVENT = "job event";	
	public static final String MISSION_EVENT = "mission event";	
	
    // Data members
    private Person person; // The person owning this mind.
    private TaskManager taskManager; // The person's task manager.
    private Mission mission; // The person's current mission (if any).
    private Job job; // The person's job.
    private boolean jobLock; // Is the job locked so another can't be chosen?
    private PersonalityType personality; // The person's personality.
    private SkillManager skillManager; // The person's skill manager.

    /** 
     * Constructor
     * @param person the person owning this mind
     * @throws Exception if mind could not be created.
     */
    public Mind(Person person) throws Exception {

        // Initialize data members
        this.person = person;
        mission = null;
        job = null;
        jobLock = false;
        
        // Set the MBTI personality type.
        personality = new PersonalityType(person);

        // Construct a task manager
        taskManager = new TaskManager(this);
        
        // Construct a skill manager.
        skillManager = new SkillManager(person);
    }
    
	/**
	 * Time passing 
	 * @param time the time passing (millisols)
	 * @throws Exception if error.
	 */
	public void timePassing(double time) throws Exception {
		
	    // Check if this person needs to get a new job or change jobs.
        if (!jobLock) setJob(JobManager.getNewJob(person), false);
	    
		// Take action as necessary.
		takeAction(time);
		
		// Update stress based on personality.
		personality.updateStress(time);
		
		// Update relationships.
		Simulation.instance().getRelationshipManager().timePassing(person, time);
	}

    /** 
     * Take appropriate action for a given amount of time.
     * @param time time in millisols
     * @throws Exception if error during action.
     */
    public void takeAction(double time) throws Exception {
    	
        if ((mission != null) && mission.isDone()) mission = null;
        boolean activeMission = (mission != null);
        
        // Check if mission creation at settlement (if any) is overridden.
        boolean overrideMission = false;
        if (person.getLocationSituation().equals(Person.INSETTLEMENT))
        	overrideMission = person.getSettlement().getMissionCreationOverride();
        
        // Perform a task if the person has one, or determine a new task/mission.
        try {
        	if (taskManager.hasActiveTask()) {
            	double remainingTime = taskManager.performTask(time, person.getPerformanceRating());
            	if (remainingTime > 0D) takeAction(remainingTime);
        	}
        	else {
            	if (activeMission) {
            		try {
            			mission.performMission(person);
            		}
            		catch (MissionException e) {
                        logger.log(Level.SEVERE, "Error performing mission.", e);
            			mission.endMission(e.getMessage());
            		}
            	}
            	
            	if (!taskManager.hasActiveTask()) 
            		getNewAction(true, (!activeMission && !overrideMission));
            	if (taskManager.hasActiveTask() || hasActiveMission()) takeAction(time);
        	}
        }
        catch (Exception e) {
        	e.printStackTrace(System.err);
        	throw new Exception("Mind.takeAction(): " + e.getMessage());
        }
    }

    /** Returns the person owning this mind.
     *  @return person
     */
    public Person getPerson() {
        return person;
    }

    /** Returns the person's task manager
     *  @return task manager
     */
    public TaskManager getTaskManager() {
        return taskManager;
    }

    /** Returns the person's current mission.
     *  Returns null if there is no current mission.
     *  @return current mission
     */
    public Mission getMission() {
        return mission;
    }
    
    /**
     * Gets the person's job
     * @return job or null if none.
     */
    public Job getJob() {
    	return job;
    }
    
    /**
     * Checks if the person's job is locked and can't be changed.
     * @return true if job lock.
     */
    public boolean getJobLock() {
    	return jobLock;
    }
    
    /**
     * Sets the person's job.
     * @param newJob the new job
     * @param locked is the job locked so another can't be chosen?
     */
    public void setJob(Job newJob, boolean locked) {
    	
    	jobLock = locked;
    	if (!newJob.equals(job)) {
    		job = newJob;
    		person.fireUnitUpdate(JOB_EVENT, newJob);
    	}
    }

    /** Returns true if person has an active mission.
     *  @return true for active mission
     */
    public boolean hasActiveMission() {
        if ((mission != null) && !mission.isDone()) return true;
        else return false;
    }

    /**
     * Set this mind as inactive. Needs move work on this; has to abort the Task
     * can not just close it. This abort action would then allow the Mission to
     * be also aborted.
     */
    public void setInactive() {
		taskManager.clearTask();
        if (hasActiveMission()) {
            mission.removePerson(person);
            mission = null;
        }
    }

    /** Sets the person's current mission.
     *  @param newMission the new mission
     */
    public void setMission(Mission newMission) {
    	if (newMission != mission) {
    		if (mission != null) mission.removePerson(person);
    		mission = newMission;
    		if (newMission != null) newMission.addPerson(person);
    		person.fireUnitUpdate(MISSION_EVENT, newMission);
    	}
    }

    /** 
     * Determines a new action for the person based on
     * available tasks, missions and active missions.
     * @param tasks can actions be tasks?
     * @param missions can actions be new missions?
     * @throws Exception if new action cannot be found.
     */
    public void getNewAction(boolean tasks, boolean missions) throws Exception {
		MissionManager missionManager = Simulation.instance().getMissionManager();
		
        // If this Person is too weak then they can not do Missions
        if (person.getPerformanceRating() < 0.5D) missions = false;
        
        // If for some reason person is in a rover at the settlement, and not on a mission,
        // have them enter the settlement.
        enterSettlementIfInRover();

        // Get probability weights from tasks, missions and active missions.
        double taskWeights = 0D;
        double missionWeights = 0D;

        // Determine sum of weights based on given parameters
        double weightSum = 0D;
        if (tasks) {
        	taskWeights = taskManager.getTotalTaskProbability(false);
        	weightSum += taskWeights;
        }
        if (missions) {
        	missionWeights = missionManager.getTotalMissionProbability(person);
        	weightSum += missionWeights;
        }
		if (weightSum <= 0D) throw new Exception("Mind.getNewAction(): weight sum: " + weightSum);

        // Select randomly across the total weight sum.
        double rand = RandomUtil.getRandomDouble(weightSum);

        // Determine which type of action was selected and set new action accordingly.
        if (tasks) {
            if (rand < taskWeights) {
                taskManager.addTask(taskManager.getNewTask());
                return;
            }
            else rand -= taskWeights;
        }
        if (missions) {
            if (rand < missionWeights) {
        	
                if(logger.isLoggable(Level.FINE)){
                    logger.fine(person.getName() + " starting a new mission.");
                }
                Mission newMission = missionManager.getNewMission(person);
                if (newMission != null) {
                	missionManager.addMission(newMission);
                	setMission(newMission);
                }
                return;
            }
            else rand -= missionWeights;
        }
        
        // If reached this point, no task or mission has been found.
        logger.severe(person.getName() + " couldn't determine new action - taskWeights: " + 
                taskWeights + ", missionWeights: " + missionWeights);
    }
    
    /**
     * If person is not on mission and is in a vehicle parked at a settlement,
     * have person leave vehicle and enter settlement.
     */
    private void enterSettlementIfInRover() {
    	if (getMission() == null) {
    		if (person.getLocationSituation().equals(Person.INVEHICLE)) {
    			Vehicle vehicle = person.getVehicle();
    			if (vehicle.getSettlement() != null) {
    				Settlement settlement = vehicle.getSettlement();
    				// Move person from vehicle to settlement.
    				try {
    	        		vehicle.getInventory().retrieveUnit(person);
    	        		settlement.getInventory().storeUnit(person);
    	        	}
    	        	catch (InventoryException e) {
    	        		e.printStackTrace(System.err);
    	        	}
    			}
    		}
    	}
    }
    
    /**
     * Gets the person's personality type.
     * @return personality type.
     */
    public PersonalityType getPersonalityType() {
    	return personality;
    }
    
    /** Returns a reference to the Person's skill manager
     *  @return the person's skill manager
     */
    public SkillManager getSkillManager() {
        return skillManager;
    }
}