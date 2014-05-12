package org.mars_sim.msp.core.structure.building.function;

import org.mars_sim.msp.core.Msg;

/**
 * @author stpa
 * 2014-03-14
 */
public enum BuildingFunction {

	ASTRONOMICAL_OBSERVATIONS	(Msg.getString("BuildingFunction.astronomicalObservations")), //$NON-NLS-1$
	BUILDING_CONNECTION			(Msg.getString("BuildingFunction.buildingConnection")), //$NON-NLS-1$
	COMMUNICATION				(Msg.getString("BuildingFunction.communication")), //$NON-NLS-1$
	COOKING						(Msg.getString("BuildingFunction.cooking")), //$NON-NLS-1$
	DINING						(Msg.getString("BuildingFunction.dining")), //$NON-NLS-1$
	EARTH_RETURN				(Msg.getString("BuildingFunction.earthReturn")), //$NON-NLS-1$
	EVA							(Msg.getString("BuildingFunction.eva")), //$NON-NLS-1$
	EXERCISE					(Msg.getString("BuildingFunction.exercise")), //$NON-NLS-1$
	FARMING						(Msg.getString("BuildingFunction.farming")), //$NON-NLS-1$
	GROUND_VEHICLE_MAINTENANCE	(Msg.getString("BuildingFunction.groundVehicleMaintenance")), //$NON-NLS-1$
	LIFE_SUPPORT				(Msg.getString("BuildingFunction.lifeSupport")), //$NON-NLS-1$
	LIVING_ACCOMODATIONS		(Msg.getString("BuildingFunction.livingAccomodations")), //$NON-NLS-1$
	MANAGEMENT					(Msg.getString("BuildingFunction.management")), //$NON-NLS-1$
	MANUFACTURE					(Msg.getString("BuildingFunction.manufacture")), //$NON-NLS-1$
	MEDICAL_CARE				(Msg.getString("BuildingFunction.medicalCare")), //$NON-NLS-1$
	POWER_GENERATION			(Msg.getString("BuildingFunction.powerGeneration")), //$NON-NLS-1$
	POWER_STORAGE				(Msg.getString("BuildingFunction.powerStorage")), //$NON-NLS-1$
	RECREATION					(Msg.getString("BuildingFunction.recreation")), //$NON-NLS-1$
	RESEARCH					(Msg.getString("BuildingFunction.research")), //$NON-NLS-1$
	RESOURCE_PROCESSING			(Msg.getString("BuildingFunction.resourceProcessing")), //$NON-NLS-1$
	STORAGE						(Msg.getString("BuildingFunction.storage")), //$NON-NLS-1$
	;

	private String name;

	/** hidden constructor. */
	private BuildingFunction(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
}
