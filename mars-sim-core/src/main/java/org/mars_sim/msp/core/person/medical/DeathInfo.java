/**
 * Mars Simulation Project
 * DeathInfo.java
 * @version 3.07 2014-09-22
 * @author Barry Evans
 */

package org.mars_sim.msp.core.person.medical;

import java.io.Serializable;
import java.util.Iterator;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.malfunction.Malfunction;
import org.mars_sim.msp.core.malfunction.MalfunctionFactory;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonGender;
import org.mars_sim.msp.core.person.ai.Mind;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.task.TaskManager;
import org.mars_sim.msp.core.person.ai.task.TaskPhase;

/**
 * This class represents the status of a Person when death occurs. It records
 * the Complaint that caused the death to occur, the time of death and
 * the Location.<br/>
 * The Location is recorded as a dead body may be moved from the place of death.
 * This class is immutable since once Death occurs it is final.
 */
public class DeathInfo
implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    // Data members
    private String timeOfDeath;
    /** Medical cause of death. */
    private String illness;
    private String placeOfDeath;
    /** Container unit at time of death. */
    private Unit containerUnit;
    private Coordinates locationOfDeath;
    /** the person's job at time of death. */
    private Job job;
    /** Name of mission at time of death. */
    private String mission;
    /** Phase of mission at time of death. */
    private String missionPhase;
    /** Name of task at time of death. */
    private String task;
    /** Phase of task at time of death. */
    private String taskPhase;
    /** Name of the most serious local emergency malfunction. */
    private String malfunction; 
    /** gender at time of death. */
    private PersonGender gender;

    /**
     * The construct creates an instance of a DeathInfo class.
     * @param person the dead person
     */
    public DeathInfo(Person person) {

        // Initialize data members
        timeOfDeath = Simulation.instance().getMasterClock().getMarsClock().getTimeStamp();

        Complaint serious = person.getPhysicalCondition().getMostSerious();
        if (serious != null) illness = serious.getName();

        if (person.getLocationSituation() == LocationSituation.OUTSIDE) placeOfDeath = "Outside";
        else {
            containerUnit = person.getContainerUnit();  
            placeOfDeath = containerUnit.getName();
        }

        locationOfDeath = person.getCoordinates();

        Mind mind = person.getMind();

        job = mind.getJob();

        if (mind.getMission() != null) {
            mission = mind.getMission().getName();
            missionPhase = mind.getMission().getPhaseDescription();
        }

        TaskManager taskMgr = mind.getTaskManager();
        if (taskMgr.hasTask()) {
            task = taskMgr.getTaskName();
            TaskPhase phase = taskMgr.getPhase();
            if (phase != null) {
                taskPhase = phase.getName();
            }
            else {
                taskPhase = "";
            }
        }

        Iterator<Malfunctionable> i = MalfunctionFactory.getMalfunctionables(person).iterator();
        Malfunction mostSerious = null;
        int severity = 0;
        while (i.hasNext()) {
            Malfunctionable entity = i.next();
            MalfunctionManager malfunctionMgr = entity.getMalfunctionManager();
            if (malfunctionMgr.hasEmergencyMalfunction()) {
                Malfunction m = malfunctionMgr.getMostSeriousEmergencyMalfunction();
                if (m.getSeverity() > severity) {
                    mostSerious = m;
                    severity = m.getSeverity();
                }
            }
        }
        if (mostSerious != null) malfunction = mostSerious.getName();
        this.gender = person.getGender();
    }

    /**
     * Get the time death happened.
     * @return formatted time.
     */
    public String getTimeOfDeath() {
        if (timeOfDeath != null) return timeOfDeath;
        else return "";
    }

    /**
     * Gets the place the death happened.  
     * Either the name of the unit the person was in, or 'outside' if
     * the person died on an EVA.
     * @return place of death.
     */
    public String getPlaceOfDeath() {
        if (placeOfDeath != null) return placeOfDeath;
        else return "";
    }

    /** 
     * Gets the container unit at the time of death.
     * Returns null if none.
     * @return container unit
     */
    public Unit getContainerUnit() {
        return containerUnit;
    }

    /**
     * Get the name of the illness that caused the death.
     * @return name of the illness.
     */
    public String getIllness() {
        if (illness != null) return illness;
        else return "";
    }

    /**
     * Gets the location of death.
     * @return coordinates
     */
    public Coordinates getLocationOfDeath() {
        return locationOfDeath;
    }

    /**
     * Gets the person's job at the time of death.
     * @return job
     */
    public String getJob() {
        if (job != null) return job.getName(gender);
        else return "";
    }

    /**
     * Gets the mission the person was on at time of death.
     * @return mission name
     */
    public String getMission() {
        if (mission != null) return mission;
        else return "";
    }

    /**
     * Gets the mission phase at time of death.
     * @return mission phase
     */
    public String getMissionPhase() {
        if (missionPhase != null) return missionPhase;
        else return "";
    }

    /**
     * Gets the task the person was doing at time of death.
     * @return task name
     */
    public String getTask() {
        if (task != null) return task;
        else return "";
    }

    /**
     * Gets the task phase at time of death.
     * @return task phase
     */
    public String getTaskPhase() {
        if (taskPhase != null) return taskPhase;
        else return "";
    }

    /**
     * Gets the most serious emergency malfunction
     * local to the person at time of death.
     * @return malfunction name
     */
    public String getMalfunction() {
        if (malfunction != null) return malfunction;
        else return "";
    }
}
