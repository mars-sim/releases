/**
 * Mars Simulation Project
 * RepairEmergencyMalfunctionEVA.java
 * @version 3.07 2015-01-14
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Airlock;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.LocalBoundedObject;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.malfunction.Malfunction;
import org.mars_sim.msp.core.malfunction.MalfunctionFactory;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.person.EventType;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * A task to repair an emergency malfunction requiring an EVA.
 */
public class RepairEmergencyMalfunctionEVA extends EVAOperation implements
        Repair, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default logger. */
    private static Logger logger = Logger.getLogger(RepairEVAMalfunction.class.getName());

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.repairEmergencyMalfunctionEVA"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase REPAIRING = new TaskPhase(Msg.getString(
            "Task.phase.repairing")); //$NON-NLS-1$

    /** The malfunctionable entity being repaired. */
    private Malfunctionable entity;
    
    /** Problem being fixed. */
    private Malfunction malfunction;
    
    /**
     * Constructor
     * @param person the person to perform the task
     */
    public RepairEmergencyMalfunctionEVA(Person person) {
        super(NAME, person, false, 0D);
        
        // Get the malfunctioning entity.
        claimMalfunction();
        if (entity == null) {
            endTask();
            return;
        }
        
        // Create starting task event if needed.
        if (getCreateEvents() && !isDone()) {
            TaskEvent startingEvent = new TaskEvent(person, this, EventType.TASK_START, "");
            Simulation.instance().getEventManager().registerNewEvent(startingEvent);
        }

        // Determine location for repairing malfunction.
        Point2D malfunctionLoc = determineMalfunctionLocation();
        setOutsideSiteLocation(malfunctionLoc.getX(), malfunctionLoc.getY());

        // Initialize phase
        addPhase(REPAIRING);

        logger.fine(person.getName() + " has started the RepairEmergencyMalfunctionEVA task.");
    }
    
    /**
     * Checks if the emergency repair requires an EVA.
     * @param person the person to perform the repair.
     * @return true if repair requires EVA.
     */
    public static boolean requiresEVARepair(Person person) {
        
        boolean result = false;
        
        Malfunction malfunction = null;
        Malfunctionable entity = null;
        Iterator<Malfunctionable> i = MalfunctionFactory.getMalfunctionables(person).iterator();
        while (i.hasNext() && (malfunction == null)) {
            Malfunctionable e = i.next();
            MalfunctionManager manager = e.getMalfunctionManager();
            if (manager.hasEmergencyMalfunction()) {
                malfunction = manager.getMostSeriousEmergencyMalfunction();
                entity = e;
            }
        }
        
        if (entity != null) {
            if (entity instanceof Vehicle) {
                // Perform EVA emergency repair on outside vehicles that the person isn't inside.
                Vehicle vehicle = (Vehicle) entity;
                boolean outsideVehicle = BuildingManager.getBuilding(vehicle) == null;
                boolean personNotInVehicle = !vehicle.getInventory().containsUnit(person);
                if (outsideVehicle && personNotInVehicle) {
                    result = true;
                }
            }
            else if (entity instanceof Building) {
                // Perform EVA emergency repair on uninhabitable buildings.
                Building building = (Building) entity;
                if (!building.hasFunction(BuildingFunction.LIFE_SUPPORT)) {
                    result = true;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Checks if a person can perform an EVA for the emergency repair.
     * @param person the person.
     * @return true if person can perform the EVA.
     */
    public static boolean canPerformEVA(Person person) {
        
        boolean result = true;
        
        // Check if an airlock is available
        Airlock airlock = EVAOperation.getWalkableAvailableAirlock(person);
        if (airlock == null) {
            result = false;
        }

        // Check if it is night time.
        SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();
        if (surface.getSurfaceSunlight(person.getCoordinates()) == 0) {
            if (!surface.inDarkPolarRegion(person.getCoordinates())) {
                result = false;
            }
        } 
        
        // Check if person is outside.
        if (person.getLocationSituation().equals(LocationSituation.OUTSIDE)) {
            result = false;
        }

        // Check if EVA suit is available.
        if ((airlock != null) && !ExitAirlock.goodEVASuitAvailable(airlock.getEntityInventory())) {
            result = false;
        }

        // Check if person is incapacitated.
        if (person.getPerformanceRating() == 0D) {
            result = false;
        }
        
        return result;
    }
    
    /**
     * Gets a local emergency malfunction.
     */
    private void claimMalfunction() {
        malfunction = null;
        Iterator<Malfunctionable> i = MalfunctionFactory.getMalfunctionables(person).iterator();
        while (i.hasNext() && (malfunction == null)) {
            Malfunctionable e = i.next();
            MalfunctionManager manager = e.getMalfunctionManager();
            if (manager.hasEmergencyMalfunction()) {
                malfunction = manager.getMostSeriousEmergencyMalfunction();
                entity = e;
                setDescription(Msg.getString("Task.description.repairEmergencyMalfunctionEVA.detail", 
                        malfunction.getName(), entity.getName())); //$NON-NLS-1$
            }
        }
    }
    
    /**
     * Determine location to repair malfunction.
     * @return location.
     */
    private Point2D determineMalfunctionLocation() {

        Point2D.Double newLocation = new Point2D.Double(0D, 0D);

        if (entity instanceof LocalBoundedObject) {
            LocalBoundedObject bounds = (LocalBoundedObject) entity;
            boolean goodLocation = false;
            for (int x = 0; (x < 50) && !goodLocation; x++) {
                Point2D.Double boundedLocalPoint = LocalAreaUtil.getRandomExteriorLocation(
                        bounds, 1D);
                newLocation = LocalAreaUtil.getLocalRelativeLocation(boundedLocalPoint.getX(), 
                        boundedLocalPoint.getY(), bounds);
                goodLocation = LocalAreaUtil.checkLocationCollision(newLocation.getX(), 
                        newLocation.getY(), person.getCoordinates());
            }
        }

        return newLocation;
    }
    
    @Override
    protected double performMappedPhase(double time) {

        time = super.performMappedPhase(time);

        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (REPAIRING.equals(getPhase())) {
            return repairMalfunctionPhase(time);
        }
        else {
            return time;
        }
    }
    
    /**
     * Perform the repair malfunction phase of the task.
     * @param time the time to perform this phase (in millisols)
     * @return the time remaining after performing this phase (in millisols)
     */
    private double repairMalfunctionPhase(double time) {

        if (shouldEndEVAOperation() || addTimeOnSite(time)) {
            setPhase(WALK_BACK_INSIDE);
            return time;
        }
        
        // Check if there emergency malfunction work is fixed.
        double workTimeLeft = malfunction.getEmergencyWorkTime() -
                malfunction.getCompletedEmergencyWorkTime();
        if (workTimeLeft == 0) {
            setPhase(WALK_BACK_INSIDE);
            return time;
        }

        // Determine effective work time based on "Mechanic" skill.
        double workTime = time;
        int mechanicSkill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.MECHANICS);
        if (mechanicSkill == 0) {
            workTime /= 2;
        }
        if (mechanicSkill > 1) {
            workTime += workTime * (.2D * mechanicSkill);
        }

        // Add work to emergency malfunction.
        double remainingWorkTime = malfunction.addEmergencyWorkTime(workTime);

        // Add experience points
        addExperience(time);

        // Check if an accident happens during maintenance.
        checkForAccident(time);

        return remainingWorkTime;
    }
    
    @Override
    protected TaskPhase getOutsideSitePhase() {
        return REPAIRING;
    }

    @Override
    public int getEffectiveSkillLevel() {
        SkillManager manager = person.getMind().getSkillManager();
        int EVAOperationsSkill = manager.getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
        int mechanicsSkill = manager.getEffectiveSkillLevel(SkillType.MECHANICS);
        return (int) Math.round((double)(EVAOperationsSkill + mechanicsSkill) / 2D); 
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(2);
        results.add(SkillType.EVA_OPERATIONS);
        results.add(SkillType.MECHANICS);
        return results;
    }

    @Override
    protected void addExperience(double time) {

        // Add experience to "EVA Operations" skill.
        // (1 base experience point per 100 millisols of time spent)
        double evaExperience = time / 100D;

        // Experience points adjusted by person's "Experience Aptitude" attribute.
        NaturalAttributeManager nManager = person.getNaturalAttributeManager();
        int experienceAptitude = nManager.getAttribute(NaturalAttribute.EXPERIENCE_APTITUDE);
        double experienceAptitudeModifier = (((double) experienceAptitude) - 50D) / 100D;
        evaExperience += evaExperience * experienceAptitudeModifier;
        evaExperience *= getTeachingExperienceModifier();
        person.getMind().getSkillManager().addExperience(SkillType.EVA_OPERATIONS, evaExperience);

        // If phase is repair malfunction, add experience to mechanics skill.
        if (REPAIRING.equals(getPhase())) {
            // 1 base experience point per 20 millisols of collection time spent.
            // Experience points adjusted by person's "Experience Aptitude" attribute.
            double mechanicsExperience = time / 20D;
            mechanicsExperience += mechanicsExperience * experienceAptitudeModifier;
            person.getMind().getSkillManager().addExperience(SkillType.MECHANICS, mechanicsExperience);
        }
    }

    @Override
    public Malfunctionable getEntity() {
        return entity;
    }
    
    @Override
    public void destroy() {
        super.destroy();

        entity = null;
        malfunction = null;
    }
}