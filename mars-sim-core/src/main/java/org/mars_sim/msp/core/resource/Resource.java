/**
 * Mars Simulation Project
 * AmountResource.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 */

package org.mars_sim.msp.core.resource;

/**
 * A resource used in the simulation.
 */
public interface Resource
extends Comparable<Resource> {

	/**
	 * Gets the resource's name.
	 * @return name
	 */
	public String getName();

	/**
	 * Gets the resource's description.
	 * @return {@link String}
	 */
	public String getDescription();
}