/**
 * Mars Simulation Project
 * ClockListener.java
 * @version 3.03 2012-09-30
 * @author Scott Davis
 */
package org.mars_sim.msp.core.time;

/**
 * A listener for clock time changes.
 */
public interface ClockListener {

	/**
	 * Change in time.
	 * param time the amount of time changed. (millisols)
	 */
	public void clockPulse(double time);
	
	/**
	 * Change the pause state of the clock.
	 * @param isPaused true if clock is paused.
	 */
	public void pauseChange(boolean isPaused);
}