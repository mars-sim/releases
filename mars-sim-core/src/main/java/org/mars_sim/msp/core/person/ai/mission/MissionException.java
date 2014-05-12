/*
 * Mars Simulation Project
 * MissionException.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.mission;

/**
 * An exception that can be thrown by missions.
 */
public class MissionException extends Exception {

	// Domain members
	private String phase;
	
	/**
	 * Constructor
	 * @param phase the phase of the mission in which this exception is thrown (or null if none).
	 * @param message the exception message.
	 */
	public MissionException(String phase, String message) {
		// Use Exception constructor.
		super(message);
		this.phase = phase;
	}
	
	/**
	 * Constructor with existing exception
	 * @param phase the phase of the mission in which this exception is thrown (or null if none).
	 * @param exception the exception
	 */
	public MissionException(String phase, Exception exception) {
		// Use Exception constructor.
		super(exception);
		this.phase = phase;
	}
	
	/**
	 * Gets the mission phase that the exception happened.
	 * @return the phase
	 */
	public String getPhase() {
		return phase;
	}
}