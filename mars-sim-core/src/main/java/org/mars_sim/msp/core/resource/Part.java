/**
 * Mars Simulation Project
 * Part.java
 * @version 2.82 2007-10-08
 * @author Scott Davis
 */

package org.mars_sim.msp.core.resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The Part class represents a type of unit resource that is used for maintenance and repairs.
 */
public class Part extends ItemResource {

	// Domain members
	private List<MaintenanceEntity> maintenanceEntities;
	
	/**
	 * Constructor.
	 * @param name the name of the part.
	 * @param mass the mass of the part (kg)
	 */
	public Part(String name, double mass) {
		// Use ItemResource constructor.
		super(name, mass);
		
		maintenanceEntities = new ArrayList<MaintenanceEntity>();
	}
	
	/**
	 * Adds a maintenance entity for the part.
	 * @param name the name of the entity.
	 * @param probability the probability of the part being needed for maintenance.
	 * @param maxNumber the maximum number of parts needed for maintenance.
	 */
	void addMaintenanceEntity(String name, int probability, int maxNumber) {
		maintenanceEntities.add(new MaintenanceEntity(name, probability, maxNumber));
	}
	
	/**
	 * Checks if the part has a maintenance entity of a given name.
	 * @param entityName the name of the entity.
	 * @return true if part has the maintenance entity.
	 */
	public boolean hasMaintenanceEntity(String entityName) {
		if (entityName == null) throw new IllegalArgumentException("name is null");
		boolean result = false;
		Iterator<MaintenanceEntity> i = maintenanceEntities.iterator();
		while (i.hasNext()) {
			if (i.next().name.equalsIgnoreCase(entityName)) result = true;
		}
		return result;
	}
	
	/**
	 * Gets the percentage probability of a part being needed by an maintenance entity.
	 * @param entityName the name of the entity.
	 * @return percentage probability (0 - 100)
	 */
	public int getMaintenanceProbability(String entityName) {
		if (entityName == null) throw new IllegalArgumentException("name is null");
		int result = 0;
		Iterator<MaintenanceEntity> i = maintenanceEntities.iterator();
		while (i.hasNext()) {
			MaintenanceEntity entity = i.next();
			if (entity.name.equalsIgnoreCase(entityName)) result = entity.probability;
		}
		return result;
	}
	
	/**
	 * Gets the maximum number of this part needed by a maintenance entity.
	 * @param entityName the name of the entity.
	 * @return maximum number of parts.
	 */
	public int getMaintenanceMaximumNumber(String entityName) {
		if (entityName == null) throw new IllegalArgumentException("name is null");
		int result = 0;
		Iterator<MaintenanceEntity> i = maintenanceEntities.iterator();
		while (i.hasNext()) {
			MaintenanceEntity entity = i.next();
			if (entity.name.equalsIgnoreCase(entityName)) result = entity.maxNumber;
		}
		return result;
	}
	
	/**
	 * Gets a set of all parts.
	 * @return set of parts.
	 */
	public static final Set<Part> getParts() {
		Set<Part> result = new HashSet<Part>();
		
		Iterator<ItemResource> i = ItemResource.getItemResources().iterator();
		while(i.hasNext()) {
			ItemResource resource = i.next();
			if (resource instanceof Part) result.add((Part) resource);
		}
		
		return result;
	}
	
	/**
	 * A private inner class for holding maintenance entity information.
	 */
	private class MaintenanceEntity implements Serializable {
		
		// Domain members
		private String name;
		private int probability;
		private int maxNumber;
		
		/**
		 * Constructor
		 * @param name name of the entity.
		 * @param probability the probability of this part being needed for maintenance.
		 * @param maxNumber the maximum number of this part needed for maintenance.
		 */
		private MaintenanceEntity(String name, int probability, int maxNumber) {
			if (name == null) throw new IllegalArgumentException("name is null");
			this.name = name;
			this.probability = probability;
			this.maxNumber = maxNumber;
		}
	}
}