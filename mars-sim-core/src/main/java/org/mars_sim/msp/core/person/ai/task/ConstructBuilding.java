/**
 * Mars Simulation Project
 * ConstructBuilding.java
 * @version 3.07 2014-09-22
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
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.BuildingConstructionMission;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionManager;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.construction.ConstructionSite;
import org.mars_sim.msp.core.structure.construction.ConstructionStage;
import org.mars_sim.msp.core.vehicle.GroundVehicle;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;

/**
 * Task for constructing a building construction site stage.
 */
public class ConstructBuilding
extends EVAOperation
implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default logger. */
    private static Logger logger = Logger.getLogger(ConstructBuilding.class.getName());

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.constructBuilding"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase CONSTRUCTION = new TaskPhase(Msg.getString(
            "Task.phase.construction")); //$NON-NLS-1$

    // The base chance of an accident while operating LUV per millisol.
    public static final double BASE_LUV_ACCIDENT_CHANCE = .001;

    // Data members.
    private ConstructionStage stage;
    private ConstructionSite site;
    private List<GroundVehicle> vehicles;
    private LightUtilityVehicle luv;
    private boolean operatingLUV;

    /**
     * Constructor.
     * @param person the person performing the task.
     */
    public ConstructBuilding(Person person) {
        // Use EVAOperation parent constructor.
        super(NAME, person, true, RandomUtil.getRandomDouble(50D) + 10D);

        BuildingConstructionMission mission = getMissionNeedingAssistance();
        if ((mission != null) && canConstruct(person, mission.getConstructionSite())) {

            // Initialize data members.
            this.stage = mission.getConstructionStage();
            this.site = mission.getConstructionSite();
            this.vehicles = mission.getConstructionVehicles();

            // Determine location for construction site.
            Point2D constructionSiteLoc = determineConstructionLocation();
            setOutsideSiteLocation(constructionSiteLoc.getX(), constructionSiteLoc.getY());

            // Add task phase
            addPhase(CONSTRUCTION);
        }
        else {
            endTask();
        }
    }

    /**
     * Constructor.
     * @param person the person performing the task.
     * @param stage the construction site stage.
     * @param vehicles the construction vehicles.
     * @throws Exception if error constructing task.
     */
    public ConstructBuilding(Person person, ConstructionStage stage, 
            ConstructionSite site, List<GroundVehicle> vehicles) {
        // Use EVAOperation parent constructor.
        super(NAME, person, true, RandomUtil.getRandomDouble(50D) + 10D);

        // Initialize data members.
        this.stage = stage;
        this.site = site;
        this.vehicles = vehicles;

        // Determine location for construction site.
        Point2D constructionSiteLoc = determineConstructionLocation();
        setOutsideSiteLocation(constructionSiteLoc.getX(), constructionSiteLoc.getY());

        // Add task phase
        addPhase(CONSTRUCTION);
    }

    /**
     * Checks if a given person can work on construction at this time.
     * @param person the person.
     * @return true if person can construct.
     */
    public static boolean canConstruct(Person person, ConstructionSite site) {

        // Check if person can exit the settlement airlock.
        boolean exitable = false;
        Airlock airlock = getClosestWalkableAvailableAirlock(person, site.getXLocation(), 
                site.getYLocation());
        if (airlock != null) exitable = ExitAirlock.canExitAirlock(person, airlock);

        SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();

        // Check if it is night time outside.
        boolean sunlight = surface.getSurfaceSunlight(person.getCoordinates()) > 0;

        // Check if in dark polar region.
        boolean darkRegion = surface.inDarkPolarRegion(person.getCoordinates());

        // Check if person's medical condition will not allow task.
        boolean medical = person.getPerformanceRating() < .5D;

        return (exitable && (sunlight || darkRegion) && !medical);
    }

    /**
     * Gets a random building construction mission that needs assistance.
     * @return construction mission or null if none found.
     */
    private BuildingConstructionMission getMissionNeedingAssistance() {

        BuildingConstructionMission result = null;

        List<BuildingConstructionMission> constructionMissions = getAllMissionsNeedingAssistance(
                person.getSettlement());

        if (constructionMissions.size() > 0) {
            int index = RandomUtil.getRandomInt(constructionMissions.size() - 1);
            result = (BuildingConstructionMission) constructionMissions.get(index);
        }

        return result;
    }

    /**
     * Gets a list of all building construction missions that need assistance at a settlement.
     * @param settlement the settlement.
     * @return list of building construction missions.
     */
    public static List<BuildingConstructionMission> getAllMissionsNeedingAssistance(
            Settlement settlement) {

        List<BuildingConstructionMission> result = new ArrayList<BuildingConstructionMission>();

        MissionManager manager = Simulation.instance().getMissionManager();
        Iterator<Mission> i = manager.getMissionsForSettlement(settlement).iterator();
        while (i.hasNext()) {
            Mission mission = (Mission) i.next();
            if (mission instanceof BuildingConstructionMission) {
                result.add((BuildingConstructionMission) mission);
            }
        }

        return result;
    }

    /**
     * Determine location to go to at construction site.
     * @return location.
     */
    private Point2D determineConstructionLocation() {

        Point2D.Double relativeLocSite = LocalAreaUtil.getRandomInteriorLocation(site, false);
        Point2D.Double settlementLocSite = LocalAreaUtil.getLocalRelativeLocation(relativeLocSite.getX(), 
                relativeLocSite.getY(), site);

        return settlementLocSite;
    }

    @Override
    protected TaskPhase getOutsideSitePhase() {
        return CONSTRUCTION;
    }

    @Override
    protected double performMappedPhase(double time) {

        time = super.performMappedPhase(time);

        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (CONSTRUCTION.equals(getPhase())) {
            return constructionPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Perform the construction phase of the task.
     * @param time amount (millisols) of time to perform the phase.
     * @return time (millisols) remaining after performing the phase.
     * @throws Exception
     */
    private double constructionPhase(double time) {

        // Check for an accident during the EVA operation.
        checkForAccident(time);

        // Check if site duration has ended or there is reason to cut the construction 
        // phase short and return to the rover.
        if (shouldEndEVAOperation() || addTimeOnSite(time) || stage.isComplete()) {

            // End operating light utility vehicle.
            if ((luv != null) && luv.getInventory().containsUnit(person)) {
                returnVehicle();
            }

            setPhase(WALK_BACK_INSIDE);
            return time;
        }

        // Operate light utility vehicle if no one else is operating it.
        if (!operatingLUV) {
            obtainVehicle();
        }

        // Determine effective work time based on "Construction" and "EVA Operations" skills.
        double workTime = time;
        int skill = getEffectiveSkillLevel();
        if (skill == 0) {
            workTime /= 2;
        }
        else if (skill > 1) {
            workTime += workTime * (.2D * skill);
        }

        // Work on construction.
        stage.addWorkTime(workTime);

        // Add experience points
        addExperience(time);

        // Check if an accident happens during construction.
        checkForAccident(time);

        return 0D;
    }

    /**
     * Obtains a construction vehicle from the settlement if possible.
     * @throws Exception if error obtaining construction vehicle.
     */
    private void obtainVehicle() {
        Iterator<GroundVehicle> i = vehicles.iterator();
        while (i.hasNext() && (luv == null)) {
            GroundVehicle vehicle = i.next();
            if (!vehicle.getMalfunctionManager().hasMalfunction()) {
                if (vehicle instanceof LightUtilityVehicle) {
                    LightUtilityVehicle tempLuv = (LightUtilityVehicle) vehicle;
                    if (tempLuv.getOperator() == null) {
                        tempLuv.getInventory().storeUnit(person);
                        tempLuv.setOperator(person);
                        luv = tempLuv;
                        operatingLUV = true;

                        // Place light utility vehicles at random location in construction site.
                        Point2D.Double relativeLocSite = LocalAreaUtil.getRandomInteriorLocation(site);
                        Point2D.Double settlementLocSite = LocalAreaUtil.getLocalRelativeLocation(relativeLocSite.getX(), 
                                relativeLocSite.getY(), site);
                        luv.setParkedLocation(settlementLocSite.getX(), settlementLocSite.getY(), RandomUtil.getRandomDouble(360D));

                        break;
                    }
                }
            }
        }
    }

    /**
     * Returns the construction vehicle used to the settlement.
     * @throws Exception if error returning construction vehicle.
     */
    private void returnVehicle() {
        luv.getInventory().retrieveUnit(person);
        luv.setOperator(null);
        operatingLUV = false;
    }

    @Override
    public int getEffectiveSkillLevel() {
        SkillManager manager = person.getMind().getSkillManager();
        int EVAOperationsSkill = manager.getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
        int constructionSkill = manager.getEffectiveSkillLevel(SkillType.CONSTRUCTION);
        return (int) Math.round((double)(EVAOperationsSkill + constructionSkill) / 2D); 
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(2);
        results.add(SkillType.EVA_OPERATIONS);
        results.add(SkillType.CONSTRUCTION);
        return results;
    }

    @Override
    protected void addExperience(double time) {
        SkillManager manager = person.getMind().getSkillManager();

        // Add experience to "EVA Operations" skill.
        // (1 base experience point per 100 millisols of time spent)
        double evaExperience = time / 100D;

        // Experience points adjusted by person's "Experience Aptitude" attribute.
        NaturalAttributeManager nManager = person.getNaturalAttributeManager();
        int experienceAptitude = nManager.getAttribute(NaturalAttribute.EXPERIENCE_APTITUDE);
        double experienceAptitudeModifier = (((double) experienceAptitude) - 50D) / 100D;
        evaExperience += evaExperience * experienceAptitudeModifier;
        evaExperience *= getTeachingExperienceModifier();
        manager.addExperience(SkillType.EVA_OPERATIONS, evaExperience);

        // If phase is construction, add experience to construction skill.
        if (CONSTRUCTION.equals(getPhase())) {
            // 1 base experience point per 10 millisols of construction time spent.
            // Experience points adjusted by person's "Experience Aptitude" attribute.
            double constructionExperience = time / 10D;
            constructionExperience += constructionExperience * experienceAptitudeModifier;
            manager.addExperience(SkillType.CONSTRUCTION, constructionExperience);

            // If person is driving the light utility vehicle, add experience to driving skill.
            // 1 base experience point per 10 millisols of mining time spent.
            // Experience points adjusted by person's "Experience Aptitude" attribute.
            if (operatingLUV) {
                double drivingExperience = time / 10D;
                drivingExperience += drivingExperience * experienceAptitudeModifier;
                manager.addExperience(SkillType.DRIVING, drivingExperience);
            }
        }
    }

    @Override
    protected void checkForAccident(double time) {
        super.checkForAccident(time);

        // Check for light utility vehicle accident if operating one.
        if (operatingLUV) {
            double chance = BASE_LUV_ACCIDENT_CHANCE;

            // Driving skill modification.
            int skill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
            if (skill <= 3) {
                chance *= (4 - skill);
            }
            else {
                chance /= (skill - 2);
            }

            // Modify based on the LUV's wear condition.
            chance *= luv.getMalfunctionManager().getWearConditionAccidentModifier();

            if (RandomUtil.lessThanRandPercent(chance * time)) {
                luv.getMalfunctionManager().accident();
            }
        }
    }

    @Override
    protected boolean shouldEndEVAOperation() {
        boolean result = super.shouldEndEVAOperation();

        // If operating LUV, check if LUV has malfunction.
        if (operatingLUV && luv.getMalfunctionManager().hasMalfunction()) {
            result = true;
        }

        return result;
    }

    /**
     * Gets the construction stage that is being worked on.
     * @return construction stage.
     */
    public ConstructionStage getConstructionStage() {
        return stage;
    }

    @Override
    public void destroy() {
        super.destroy();

        stage = null;
        if (vehicles != null) {
            vehicles.clear();
        }
        vehicles = null;
        luv = null;
    }
}