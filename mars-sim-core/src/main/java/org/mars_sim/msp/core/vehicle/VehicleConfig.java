/**
 * Mars Simulation Project
 * VehicleConfig.java
 * @version 3.07 2015-01-04
 * @author Scott Davis
 */
package org.mars_sim.msp.core.vehicle;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.science.ScienceType;

/**
 * Provides configuration information about vehicle units.
 * Uses a DOM document to get the information. 
 */
public class VehicleConfig
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 2L;

	// Element names
	private static final String VEHICLE = "vehicle";
	private static final String TYPE = "type";
	private static final String WIDTH = "width";
	private static final String LENGTH = "length";
	private static final String DESCRIPTION = "description";
	private static final String FUEL_EFFICIENCY = "fuel-efficiency";
	private static final String BASE_SPEED = "base-speed";
	private static final String EMPTY_MASS = "empty-mass";
	private static final String CREW_SIZE = "crew-size";
	private static final String CARGO = "cargo";
	private static final String TOTAL_CAPACITY = "total-capacity";
	private static final String CAPACITY = "capacity";
	private static final String RESOURCE = "resource";
	private static final String VALUE = "value";
	private static final String SICKBAY = "sickbay";
	private static final String LAB = "lab";
	private static final String TECH_LEVEL = "tech-level";
	private static final String BEDS = "beds";
	private static final String TECH_SPECIALTY = "tech-specialty";
	private static final String PART_ATTACHMENT = "part-attachment";
	private static final String NUMBER_SLOTS = "number-slots";
	private static final String PART = "part";
	private static final String NAME = "name";
	private static final String AIRLOCK = "airlock";
	private static final String X_LOCATION = "xloc";
    private static final String Y_LOCATION = "yloc";
    private static final String INTERIOR_X_LOCATION = "interior-xloc";
    private static final String INTERIOR_Y_LOCATION = "interior-yloc";
    private static final String EXTERIOR_X_LOCATION = "exterior-xloc";
    private static final String EXTERIOR_Y_LOCATION = "exterior-yloc";
	private static final String ROVER_NAME_LIST = "rover-name-list";
	private static final String ROVER_NAME = "rover-name";
	private static final String ACTIVITY = "activity";
	private static final String ACTIVITY_SPOT = "activity-spot";
	private static final String OPERATOR_TYPE = "operator";
	private static final String PASSENGER_TYPE = "passenger";
	private static final String SICKBAY_TYPE = "sickbay";
	private static final String LAB_TYPE = "lab";

	private Document vehicleDoc;
	private List<String> roverNames;
	private Map<String,VehicleDescription> map = null;

	/**
	 * Constructor.
	 * @param vehicleDoc {@link Document} DOM document with vehicle configuration.
	 */
	public VehicleConfig(Document vehicleDoc) {
		this.vehicleDoc = vehicleDoc;
	}

	/**
	 * parse only once, store resulting config data for later use.
	 */
	@SuppressWarnings("unchecked")
	private void parseIfNeccessary() {
		// only parse when necessary (i.e. when not yet parsed)
		if (map == null) {
			map = new HashMap<String,VehicleDescription>();
			Element root = vehicleDoc.getRootElement();
			List<Element> vehicleNodes = root.getChildren(VEHICLE);
			for (Element vehicleElement : vehicleNodes) {
				String type = vehicleElement.getAttributeValue(TYPE).toLowerCase();

				// vehicle description
				VehicleDescription v = new VehicleDescription();
				v.width = Double.parseDouble(vehicleElement.getAttributeValue(WIDTH));
				v.length = Double.parseDouble(vehicleElement.getAttributeValue(LENGTH));
				v.description = "no description available.";
				if (vehicleElement.getChildren(DESCRIPTION).size() > 0) {
					v.description = vehicleElement.getChildText(DESCRIPTION);
				}
				v.fuelEff = Double.parseDouble(vehicleElement.getChild(FUEL_EFFICIENCY).getAttributeValue(VALUE));
				v.baseSpeed = Double.parseDouble(vehicleElement.getChild(BASE_SPEED).getAttributeValue(VALUE));
				v.emptyMass = Double.parseDouble(vehicleElement.getChild(EMPTY_MASS).getAttributeValue(VALUE));
				v.crewSize = Integer.parseInt(vehicleElement.getChild(CREW_SIZE).getAttributeValue(VALUE));

				// cargo capacities
				Element cargoElement = vehicleElement.getChild(CARGO);
				v.cargoCapacity = new HashMap<String,Double>();
				if (cargoElement != null) {
					double resourceCapacity = 0D;
					List<Element> capacityList = cargoElement.getChildren(CAPACITY);
					for (Element capacityElement : capacityList) {
						resourceCapacity = Double.parseDouble(capacityElement.getAttributeValue(VALUE));
						v.cargoCapacity.put(
							capacityElement.getAttributeValue(RESOURCE).toLowerCase(),
							resourceCapacity
						);
					}
					v.totalCapacity = Double.parseDouble(cargoElement.getAttributeValue(TOTAL_CAPACITY));
				} else v.totalCapacity = 0d;

				// sickbay
				v.sickbayBeds = 0;
				v.sickbayTechLevel = -1;
				v.hasSickbay = (vehicleElement.getChildren(SICKBAY).size() > 0);
				if (v.hasSickbay) {
					Element sickbayElement = vehicleElement.getChild(SICKBAY);
					if (sickbayElement != null) {
						v.sickbayTechLevel = Integer.parseInt(sickbayElement.getAttributeValue(TECH_LEVEL));
						v.sickbayBeds = Integer.parseInt(sickbayElement.getAttributeValue(BEDS));
					}
				};

				// labs
				v.labTechLevel = -1;
				v.labTechSpecialties = new ArrayList<ScienceType>();
				v.hasLab = (vehicleElement.getChildren(LAB).size() > 0);
				if (v.hasLab) {
					Element labElement = vehicleElement.getChild(LAB);
					if (labElement != null) {
						v.labTechLevel = Integer.parseInt(labElement.getAttributeValue(TECH_LEVEL));
						for (Object tech : labElement.getChildren(TECH_SPECIALTY)) {
							v.labTechSpecialties.add(
								ScienceType.valueOf(
									(((Element) tech).getAttributeValue(VALUE))
									.toUpperCase() // make sure the value from xml config conforms with enum values
									.replace(" ","_")
								)
							);
						}
					}
				}

				// attachments
				v.attachableParts = new ArrayList<Part>();
				v.attachmentSlots = 0;
				v.hasPartAttachments = (vehicleElement.getChildren(PART_ATTACHMENT).size() > 0);
				if (v.hasPartAttachments) {
					Element attachmentElement = vehicleElement.getChild(PART_ATTACHMENT);
					v.attachmentSlots = Integer.parseInt(attachmentElement.getAttributeValue(NUMBER_SLOTS));
					for (Object part : attachmentElement.getChildren(PART)) {
						v.attachableParts.add(
							(Part) Part.findItemResource(
								(((Element) part).getAttributeValue(NAME)).toLowerCase()
							)
						);
					}
				}
				
				// airlock locations (optional).
				Element airlockElement = vehicleElement.getChild(AIRLOCK);
				if (airlockElement != null) {
				    v.airlockXLoc = Double.parseDouble(airlockElement.getAttributeValue(X_LOCATION));
				    v.airlockYLoc = Double.parseDouble(airlockElement.getAttributeValue(Y_LOCATION));
				    v.airlockInteriorXLoc = Double.parseDouble(airlockElement.getAttributeValue(INTERIOR_X_LOCATION));
				    v.airlockInteriorYLoc = Double.parseDouble(airlockElement.getAttributeValue(INTERIOR_Y_LOCATION));
				    v.airlockExteriorXLoc = Double.parseDouble(airlockElement.getAttributeValue(EXTERIOR_X_LOCATION));
                    v.airlockExteriorYLoc = Double.parseDouble(airlockElement.getAttributeValue(EXTERIOR_Y_LOCATION));
				}
				
				// Activity spots.
				Element activityElement = vehicleElement.getChild(ACTIVITY);
				if (activityElement != null) {
				    
				    // Initialize activity spot lists.
				    v.operatorActivitySpots = new ArrayList<Point2D>();
				    v.passengerActivitySpots = new ArrayList<Point2D>();
				    v.sickBayActivitySpots = new ArrayList<Point2D>();
				    v.labActivitySpots = new ArrayList<Point2D>();
				    
				    for (Object activitySpot : activityElement.getChildren(ACTIVITY_SPOT)) {
				        Element activitySpotElement = (Element) activitySpot;
				        double xLoc = Double.parseDouble(activitySpotElement.getAttributeValue(X_LOCATION));
				        double yLoc = Double.parseDouble(activitySpotElement.getAttributeValue(Y_LOCATION));
				        Point2D spot = new Point2D.Double(xLoc, yLoc);
				        String activitySpotType = activitySpotElement.getAttributeValue(TYPE);
				        if (OPERATOR_TYPE.equals(activitySpotType)) {
				            v.operatorActivitySpots.add(spot);
				        }
				        else if (PASSENGER_TYPE.equals(activitySpotType)) {
                            v.passengerActivitySpots.add(spot);
                        }
				        else if (SICKBAY_TYPE.equals(activitySpotType)) {
                            v.sickBayActivitySpots.add(spot);
                        }
				        else if (LAB_TYPE.equals(activitySpotType)) {
                            v.labActivitySpots.add(spot);
                        }
				    }
				}

				// and keep results for later use
				map.put(type,v);
			}
		}
	}

	/**
	 * Returns a set of all vehicle types.
	 * @return set of vehicle types as strings.
	 * @throws Exception if error retrieving vehicle types.
	 */
	public Set<String> getVehicleTypes() {
		parseIfNeccessary();
		return map.keySet();
	}

	/**
	 * Gets the vehicle's width.
	 * @param vehicleType the vehicle type.
	 * @return width (meters).
	 */
	public double getWidth(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).width;
	}

	/**
	 * Gets the vehicle's length.
	 * @param vehicleType the vehicle type.
	 * @return length (meters).
	 */
	public double getLength(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).length;
	}

	/**
	 * Gets the vehicle's fuel efficiency.
	 * @param vehicleType the vehicle type
	 * @return fuel efficiency in km/kg.
	 * @throws Exception if vehicle type could not be found or XML parsing error.
	 */
	public double getFuelEfficiency(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).fuelEff;
	}

	/**
	 * Gets the vehicle's base speed.
	 * @param vehicleType the vehicle type
	 * @return base speed in km/hr.
	 */
	public double getBaseSpeed(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).baseSpeed;
	}

	/**
	 * Gets the vehicle's mass when empty.
	 * @param vehicleType the vehicle type
	 * @return empty mass in kg.
	 */
	public double getEmptyMass(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).emptyMass;
	}

	/**
	 * Gets the vehicle's maximum crew size.
	 * @param vehicleType the vehicle type
	 * @return crew size
	 */
	public int getCrewSize(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).crewSize;
	}

	/**
	 * Gets the vehicle's total cargo capacity.
	 * @param vehicleType the vehicle type
	 * @return total cargo capacity
	 */
	public double getTotalCapacity(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).totalCapacity;
	}

	/**
	 * Gets the vehicle's capacity for a resource.
	 * @param vehicleType the vehicle type
	 * @param resource the resource
	 * @return vehicle capacity for resource might be <code>null</code>
	 */
	public Double getCargoCapacity(String vehicleType, String resource) {
		parseIfNeccessary();
		//2015-01-05 Changed to resource.toLowerCase()
		Double value = map.get(vehicleType.toLowerCase()).cargoCapacity.get(resource.toLowerCase());
		if (value == null) return 0d;
		return value;
	}

	/**
	 * Checks if the vehicle has a sickbay.
	 * @param vehicleType the vehicle type
	 * @return true if sickbay
	 */
	public boolean hasSickbay(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).hasSickbay;
	}

	/**
	 * Gets the vehicle's sickbay tech level.
	 * @param vehicleType the vehicle type
	 * @return tech level or -1 if no sickbay.
	 */
	public int getSickbayTechLevel(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).sickbayTechLevel;
	}

	/**
	 * Gets the vehicle's sickbay bed number.
	 * @param vehicleType the vehicle type
	 * @return number of sickbay beds or -1 if no sickbay.
	 */
	public int getSickbayBeds(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).sickbayBeds;
	}

	/**
	 * Checks if the vehicle has a lab.
	 * @param vehicleType the vehicle type
	 * @return true if lab
	 */
	public boolean hasLab(String vehicleType) {	
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).hasLab;
	}	

	/**
	 * Gets the vehicle's lab tech level.
	 * @param vehicleType the vehicle type
	 * @return lab tech level or -1 if no lab.
	 */
	public int getLabTechLevel(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).labTechLevel;
	}

	/**
	 * Gets a list of the vehicle's lab tech specialties.
	 * @param vehicleType the vehicle type
	 * @return list of lab tech specialty strings.
	 */
	public List<ScienceType> getLabTechSpecialties(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).labTechSpecialties;
	}

	/**
	 * Checks if a vehicle type has the ability to attach parts.
	 * @param vehicleType the vehicle type
	 * @return true if can attach parts.
	 */
	public boolean hasPartAttachments(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).hasPartAttachments;
	}

	/**
	 * Gets the number of part attachment slots for a vehicle.
	 * @param vehicleType the vehicle type.
	 * @return number of part attachment slots.
	 */
	public int getPartAttachmentSlotNumber(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).attachmentSlots;
	}

	/**
	 * Gets all of the parts that can be attached to a vehicle.
	 * @param vehicleType the vehicle type
	 * @return collection of parts that are attachable.
	 */
	public Collection<Part> getAttachableParts(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).attachableParts;
	}

	public String getDescription(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase()).description;
	}

	public VehicleDescription getVehicleDescription(String vehicleType) {
		parseIfNeccessary();
		return map.get(vehicleType.toLowerCase());
	}
	
	/**
	 * Gets the vehicle airlock's relative X location.
	 * @param vehicleType the vehicle type.
	 * @return relative X location (meters from vehicle center).
	 */
	public double getAirlockXLocation(String vehicleType) {
	    parseIfNeccessary();
        return map.get(vehicleType.toLowerCase()).getAirlockXLoc();
	}
	
	/**
     * Gets the vehicle airlock's relative Y location.
     * @param vehicleType the vehicle type.
     * @return relative Y location (meters from vehicle center).
     */
    public double getAirlockYLocation(String vehicleType) {
        parseIfNeccessary();
        return map.get(vehicleType.toLowerCase()).getAirlockYLoc();
    }
    
    /**
     * Gets the relative X location of the interior side of the vehicle airlock..
     * @param vehicleType the vehicle type.
     * @return relative X location (meters from vehicle center).
     */
    public double getAirlockInteriorXLocation(String vehicleType) {
        parseIfNeccessary();
        return map.get(vehicleType.toLowerCase()).getAirlockInteriorXLoc();
    }
    
    /**
     * Gets the relative Y location of the interior side of the vehicle airlock..
     * @param vehicleType the vehicle type.
     * @return relative Y location (meters from vehicle center).
     */
    public double getAirlockInteriorYLocation(String vehicleType) {
        parseIfNeccessary();
        return map.get(vehicleType.toLowerCase()).getAirlockInteriorYLoc();
    }
    
    /**
     * Gets the relative X location of the exterior side of the vehicle airlock..
     * @param vehicleType the vehicle type.
     * @return relative X location (meters from vehicle center).
     */
    public double getAirlockExteriorXLocation(String vehicleType) {
        parseIfNeccessary();
        return map.get(vehicleType.toLowerCase()).getAirlockExteriorXLoc();
    }
    
    /**
     * Gets the relative Y location of the exterior side of the vehicle airlock..
     * @param vehicleType the vehicle type.
     * @return relative Y location (meters from vehicle center).
     */
    public double getAirlockExteriorYLocation(String vehicleType) {
        parseIfNeccessary();
        return map.get(vehicleType.toLowerCase()).getAirlockExteriorYLoc();
    }
    
    /**
     * Gets a list of operator activity spots for the vehicle.
     * @param vehicleType the vehicle type.
     * @return list of activity spots and Point2D objects.
     */
    public List<Point2D> getOperatorActivitySpots(String vehicleType) {
        parseIfNeccessary();
        VehicleDescription vehicle = map.get(vehicleType.toLowerCase());
        List<Point2D> result = vehicle.getOperatorActivitySpots();
        if (result == null) {
            result = new ArrayList<Point2D>(0);
        }
        
        return result;
    }
    
    /**
     * Gets a list of passenger activity spots for the vehicle.
     * @param vehicleType the vehicle type.
     * @return list of activity spots and Point2D objects.
     */
    public List<Point2D> getPassengerActivitySpots(String vehicleType) {
        parseIfNeccessary();
        VehicleDescription vehicle = map.get(vehicleType.toLowerCase());
        List<Point2D> result = vehicle.getPassengerActivitySpots();
        if (result == null) {
            result = new ArrayList<Point2D>(0);
        }
        
        return result;
    }
    
    /**
     * Gets a list of sick bay activity spots for the vehicle.
     * @param vehicleType the vehicle type.
     * @return list of activity spots and Point2D objects.
     */
    public List<Point2D> getSickBayActivitySpots(String vehicleType) {
        parseIfNeccessary();
        VehicleDescription vehicle = map.get(vehicleType.toLowerCase());
        List<Point2D> result = vehicle.getSickBayActivitySpots();
        if (result == null) {
            result = new ArrayList<Point2D>(0);
        }
        
        return result;
    }
    
    /**
     * Gets a list of lab activity spots for the vehicle.
     * @param vehicleType the vehicle type.
     * @return list of activity spots and Point2D objects.
     */
    public List<Point2D> getLabActivitySpots(String vehicleType) {
        parseIfNeccessary();
        VehicleDescription vehicle = map.get(vehicleType.toLowerCase());
        List<Point2D> result = vehicle.getLabActivitySpots();
        if (result == null) {
            result = new ArrayList<Point2D>(0);
        }
        
        return result;
    }

	/**
	 * Gets a list of rover names.
	 * @return list of rover names as strings.
	 * @throws Exception if XML parsing error.
	 */
    @SuppressWarnings("unchecked")
	public List<String> getRoverNameList() {
		if (roverNames == null) {
			roverNames = new ArrayList<String>();
			
			Element root = vehicleDoc.getRootElement();
			Element vehicleNameListElement = root.getChild(ROVER_NAME_LIST);
			List<Element> vehicleNameNodes = vehicleNameListElement.getChildren(ROVER_NAME);
			
			for (Element vehicleNameElement : vehicleNameNodes) {
				roverNames.add(vehicleNameElement.getAttributeValue(VALUE));
			}
		}
		return roverNames;
	}

    /**
     * Prepare object for garbage collection. or simulation reboot.
     */
    public void destroy() {
        vehicleDoc = null;
    	if (map != null) {
    		map.clear();
    		map = null;
    	}
        if(roverNames != null){
            roverNames.clear();
            roverNames = null;
        }
    }

	/** used to reduce access to the raw xml config. */
	public class VehicleDescription implements Serializable {

		/** default serial id. */
		private static final long serialVersionUID = 12L;

		private String description;
		private double width,length;
		private double fuelEff,baseSpeed,emptyMass;
		private int crewSize;
		private double totalCapacity;
		private Map<String,Double> cargoCapacity;
		private boolean hasSickbay,hasLab,hasPartAttachments;
		private int sickbayTechLevel,sickbayBeds;
		private int labTechLevel,attachmentSlots;
		private List<ScienceType> labTechSpecialties;
		private List<Part> attachableParts;
		private double airlockXLoc;
		private double airlockYLoc;
		private double airlockInteriorXLoc;
		private double airlockInteriorYLoc;
		private double airlockExteriorXLoc;
		private double airlockExteriorYLoc;
		private List<Point2D> operatorActivitySpots;
		private List<Point2D> passengerActivitySpots;
		private List<Point2D> sickBayActivitySpots;
		private List<Point2D> labActivitySpots;

		/**
		 * get <code>0.0d</code> or capacity for given cargo.
		 * @param cargo {@link String}
		 * @return {@link Double}
		 */
		public final double getCargoCapacity(String cargo) {
			Double capacity = cargoCapacity.get(cargo);
			return (capacity == null) ? 0d : capacity;
		}
		///////////////////////////////////////
		// generated getters
		///////////////////////////////////////
		/** @return the description */
		public final String getDescription() {
			return description;
		}
		/** @return the width */
		public final double getWidth() {
			return width;
		}
		/** @return the length */
		public final double getLength() {
			return length;
		}
		/** @return the fuelEff */
		public final double getFuelEff() {
			return fuelEff;
		}
		/** @return the baseSpeed */
		public final double getBaseSpeed() {
			return baseSpeed;
		}
		/** @return the emptyMass */
		public final double getEmptyMass() {
			return emptyMass;
		}
		/** @return the crewSize */
		public final int getCrewSize() {
			return crewSize;
		}
		/** @return the totalCapacity */
		public final double getTotalCapacity() {
			return totalCapacity;
		}
		/** @return the cargoCapacity */
		public final Map<String, Double> getCargoCapacity() {
			return cargoCapacity;
		}
		/** @return the hasSickbay */
		public final boolean hasSickbay() {
			return hasSickbay;
		}
		/** @return the hasLab */
		public final boolean hasLab() {
			return hasLab;
		}
		/** @return the hasPartAttachments */
		public final boolean hasPartAttachments() {
			return hasPartAttachments;
		}
		/** @return the sickbayTechLevel */
		public final int getSickbayTechLevel() {
			return sickbayTechLevel;
		}
		/** @return the sickbayBeds */
		public final int getSickbayBeds() {
			return sickbayBeds;
		}
		/** @return the labTechLevel */
		public final int getLabTechLevel() {
			return labTechLevel;
		}
		/** @return the attachmentSlots */
		public final int getAttachmentSlots() {
			return attachmentSlots;
		}
		/** @return the labTechSpecialties */
		public final List<ScienceType> getLabTechSpecialties() {
			return labTechSpecialties;
		}
		/** @return the attachableParts */
		public final List<Part> getAttachableParts() {
			return attachableParts;
		}
		/** @return the airlockXLoc */
		public final double getAirlockXLoc() {
		    return airlockXLoc;
		}
		/** @return the airlockYLoc */
        public final double getAirlockYLoc() {
            return airlockYLoc;
        }
        /** @return the airlockInteriorXLoc */
        public final double getAirlockInteriorXLoc() {
            return airlockInteriorXLoc;
        }
        /** @return the airlockInteriorYLoc */
        public final double getAirlockInteriorYLoc() {
            return airlockInteriorYLoc;
        }
        /** @return the airlockExteriorXLoc */
        public final double getAirlockExteriorXLoc() {
            return airlockExteriorXLoc;
        }
        /** @return the airlockExteriorYLoc */
        public final double getAirlockExteriorYLoc() {
            return airlockExteriorYLoc;
        }
        /** @return the operator activity spots. */
        public final List<Point2D> getOperatorActivitySpots() {
            return operatorActivitySpots;
        }
        /** @return the passenger activity spots. */
        public final List<Point2D> getPassengerActivitySpots() {
            return passengerActivitySpots;
        }
        /** @return the sick bay activity spots. */
        public final List<Point2D> getSickBayActivitySpots() {
            return sickBayActivitySpots;
        }
        /** @return the lab activity spots. */
        public final List<Point2D> getLabActivitySpots() {
            return labActivitySpots;
        }
	}
}