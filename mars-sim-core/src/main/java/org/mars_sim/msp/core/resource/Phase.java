/**
 * Mars Simulation Project
 * Phase.java
 * @version 2.79 2005-11-20
 * @author Scott Davis
 */

package org.mars_sim.msp.core.resource;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A phase of an amount resource.
 */
public final class Phase implements Serializable {
	
	// List of all phases.
	private final static Set<Phase> phases = new HashSet<Phase>(3);
	
	// The possible phases.
	public final static Phase GAS = new Phase("gas");
	public final static Phase LIQUID = new Phase("liquid");
	public final static Phase SOLID = new Phase("solid");
	
	// The name of the phase.
	private String name;

	/**
	 * Private constructor
	 * @param name the name of the phase.
	 */
	private Phase(String name) {
		this.name = name;
		phases.add(this);
	}
	
	/**
	 * Gets the name of the phase.
	 * @return name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Finds a phase by name.
	 * @param name the name of the phase.
	 * @return phase
	 * @throws Exception if phase could not be found.
	 */
	public static final Phase findPhase(String name) throws Exception {
		Phase result = null;
		Iterator<Phase> i = phases.iterator();
		while (i.hasNext()) {
			Phase phase = i.next();
			if (phase.getName().equals(name.toLowerCase())) result = phase;
		}
		if (result != null) return result;
		else throw new Exception("Phase: " + name + " could not be found.");
	}
	
	/**
	 * Gets a ummutable set of all the phases.
	 * @return set of phases.
	 */
	public static final Set<Phase> getPhases() {
		return Collections.unmodifiableSet(phases);
	}
	
	/**
	 * Checks if an object is equal to this object.
	 * @return true if equal
	 */
	public boolean equals(Object object) {
		if (object instanceof Phase) {
			Phase otherObject = (Phase) object;
			if (name.equals(otherObject.getName())) return true;
		}
		return false;
	}
	
	/**
	 * Gets the hash code value.
	 */
	public int hashCode() {
		return name.hashCode();
	}
	
	/**
	 * Gets the string value for the object.
	 */
	public String toString() {
		return name;
	}
}