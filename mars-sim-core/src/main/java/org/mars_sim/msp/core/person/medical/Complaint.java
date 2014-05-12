/**
 * Mars Simulation Project
 * Complaint.java
 * @version 3.06 2014-01-29
 * @author Barry Evans
 */

package org.mars_sim.msp.core.person.medical;

import java.io.Serializable;

/**
 * This class represents the definition of a specific Medical Complaint that can
 * effect a Person. The Complaint once effecting a Person can either
 * result in the Person entering a recovery period or developing a more serious
 * complaint or possibly death.
 *
 * I
 */
public class Complaint implements Serializable {

    /**
     * The maximum probability rating. This allows the complaint to be specifed
     * to 1/10th of a percentage.
     */
    public final static double MAXPROBABILITY = 10000D;


    private String name;                    // Identifying name
    private int seriousness;                // Seriousness of this illness
    private double degradePeriod;           // Time before complaint degrades
    private double recoveryPeriod;          // Time before Person recovers
    private double probability;             // Probability of occuring
    private double performanceFactor;       // Factor effecting Person performance
    private Treatment recoveryTreatment;    // Treatment needed for recovery
    Complaint nextPhase;                    // Next phase of this illness
    String nextPhaseStr;					// Temporary next phase complaint name.

    /**
     * Create a Medical Complaint instance.
     *
     * @param name Name of complaint.
     * @param seriousness How serious is this complaint.
     * @param degrade The time it takes before this complaint advances, if this
     * value is zero, then the Person can shelf heel themselves. This value is
     * in sols.
     * @param recovery The time is takes for a Person to recover. If this value
     * is zero it means the complaint results in death unless treated. This
     * value is in sols.
     * @param probability The probability of this illness occuring, this can be
     * between 0 and MAXPROBABILITY.
     * @param performance The percentage that a Persons performance is decreased.
     * @param recoveryTreatment Any treatment that is needed for recovery.
     * @param next The complaint that this degrades into unless checked.
     */
    Complaint(String name, int seriousness,
                             double degrade, double recovery,
                             double probability,
                             double performance,
                             Treatment recoveryTreatment, Complaint next) {
        this.name = name;
        this.seriousness = seriousness;
        this.degradePeriod = degrade;
        this.recoveryPeriod = recovery;
        this.performanceFactor = (performance / 100D);
        this.nextPhase = next;
        this.nextPhaseStr = "";
        if (next != null) this.nextPhaseStr = next.name;
        this.probability = probability;
        this.recoveryTreatment = recoveryTreatment;
    }
    
    /**
     * Constructor using string to store next complaint.
     * @param name name of complaint
     * @param seriousness seriousness of complaint
     * @param degrade degrade time until next complaint
     * @param recovery recovery time
     * @param probability probability of complaint
     * @param recoveryTreatment treatment for recovery
     * @param nextStr next complaint name
     * @param performance performance factor
     */
    Complaint(String name, int seriousness, double degrade, double recovery, 
    		double probability, Treatment recoveryTreatment, 
    		String nextStr, double performance) {
    	this.name = name;
		this.seriousness = seriousness;
		this.degradePeriod = degrade;
		this.recoveryPeriod = recovery;
		this.performanceFactor = (performance / 100D);
		this.probability = probability;
		this.recoveryTreatment = recoveryTreatment;
		this.nextPhaseStr = nextStr; 
    }

	/**
	 * Sets the next complaint this complaint degrades to.
	 * @param nextComplaint the next complaint
	 */
	void setNextComplaint(Complaint nextComplaint) {
		this.nextPhase = nextComplaint;
	}

    /**
     * Get the degrade period.
     * @return Double value representing a duration.
     */
    public double getDegradePeriod() {
        return degradePeriod;
    }

    /**
     * Get the name of complaint.
     * @return Complaint name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the next complaint that this complaint developers into.
     * @return The next complaint, if null then death results.
     */
    public Complaint getNextPhase() {
        return nextPhase;
    }
    
    /**
     * Gets the next complaint's string name.
     * @return complaint name
     */
    String getNextPhaseStr() {
    	return nextPhaseStr;
    }

    /**
     * Get the performance factor that effect Person with the complaint.
     * @return The value is between 0 -> 1.
     */
    public double getPerformanceFactor() {
        return performanceFactor;
    }

    /**
     * Get the probabity of this complaint.
     * @return Probabity from 0 to 100.
     */
    public double getProbability() {
        return probability;
    }

    /**
     * Get the treatment required for recovery to start.
     * @return recovery treatment.
     */
    public Treatment getRecoveryTreatment() {
        return recoveryTreatment;
    }

    /**
     * Get the recover period.
     * @return Double value representing a duration.
     */
    public double getRecoveryPeriod() {
        return recoveryPeriod;
    }

    /**
     * Get the seriousness of this complaint.
     * @return Seriousness rating.
     */
    public int getSeriousness() {
        return seriousness;
    }

    /**
     * Get a string respresentation.
     * @return The name of the complaint.
     */
    public String toString() {
        return name;
    }
}
