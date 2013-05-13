/**
 * Mars Simulation Project
 * ScientificStudyListener.java
 * @version 3.01 2011-07-16
 * @author Scott Davis
 */
package org.mars_sim.msp.core.science;

/**
 * Interface for a scientific study event listener.
 */
public interface ScientificStudyListener {

    /**
     * Catch scientific study event.
     * @param event the scientific study event.
     */
    public void scientificStudyUpdate(ScientificStudyEvent event);
}