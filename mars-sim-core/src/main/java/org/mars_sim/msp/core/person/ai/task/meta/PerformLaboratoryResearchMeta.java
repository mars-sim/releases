/**
 * Mars Simulation Project
 * PerformLaboratoryResearchMeta.java
 * @version 3.07 2014-10-12
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.util.Iterator;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Lab;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.task.PerformLaboratoryExperiment;
import org.mars_sim.msp.core.person.ai.task.PerformLaboratoryResearch;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.science.ScientificStudyManager;

/**
 * Meta task for the PerformLaboratoryResearch task.
 */
public class PerformLaboratoryResearchMeta implements MetaTask {
    
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.performLaboratoryResearch"); //$NON-NLS-1$
    
    /** default logger. */
    private static Logger logger = Logger.getLogger(PerformLaboratoryResearchMeta.class.getName());
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new PerformLaboratoryResearch(person);
    }

    @Override
    public double getProbability(Person person) {
        
        double result = 0D;
        
        // Add probability for researcher's primary study (if any).
        ScientificStudyManager studyManager = Simulation.instance().getScientificStudyManager();
        ScientificStudy primaryStudy = studyManager.getOngoingPrimaryStudy(person);
        if ((primaryStudy != null) && ScientificStudy.RESEARCH_PHASE.equals(primaryStudy.getPhase())) {
            if (!primaryStudy.isPrimaryResearchCompleted()) {
                try {
                    Lab lab = PerformLaboratoryResearch.getLocalLab(person, primaryStudy.getScience());
                    if (lab != null) {
                        double primaryResult = 50D;
                    
                        // Get lab building crowding modifier.
                        primaryResult *= PerformLaboratoryResearch.getLabCrowdingModifier(person, lab);
                    
                        // If researcher's current job isn't related to study science, divide by two.
                        Job job = person.getMind().getJob();
                        if (job != null) {
                            ScienceType jobScience = ScienceType.getJobScience(job);
                            if (!primaryStudy.getScience().equals(jobScience)) {
                                primaryResult /= 2D;
                            }
                        }
                    
                        result += primaryResult;
                    }
                }
                catch (Exception e) {
                    logger.severe("getProbability(): " + e.getMessage());
                }
            }
        }
        
        // Add probability for each study researcher is collaborating on.
        Iterator<ScientificStudy> i = studyManager.getOngoingCollaborativeStudies(person).iterator();
        while (i.hasNext()) {
            ScientificStudy collabStudy = i.next();
            if (ScientificStudy.RESEARCH_PHASE.equals(collabStudy.getPhase())) {
                if (!collabStudy.isCollaborativeResearchCompleted(person)) {
                    try {
                        ScienceType collabScience = collabStudy.getCollaborativeResearchers().get(person);
                    
                        Lab lab = PerformLaboratoryResearch.getLocalLab(person, collabScience);
                        if (lab != null) {
                            double collabResult = 25D;
                        
                            // Get lab building crowding modifier.
                            collabResult *= PerformLaboratoryResearch.getLabCrowdingModifier(person, lab);
                        
                            // If researcher's current job isn't related to study science, divide by two.
                            Job job = person.getMind().getJob();
                            if (job != null) {
                                ScienceType jobScience = ScienceType.getJobScience(job);
                                if (!collabScience.equals(jobScience)) {
                                    collabResult /= 2D;
                                }
                            }
                        
                            result += collabResult;
                        }
                    }
                    catch (Exception e) {
                        logger.severe("getProbability(): " + e.getMessage());
                    }
                }
            }
        }
        
        // Check if person is in a moving rover.
        if (PerformLaboratoryExperiment.inMovingRover(person)) {
            result = 0D;
        }
        
        // Effort-driven task modifier.
        result *= person.getPerformanceRating();
        
        // Job modifier.
        Job job = person.getMind().getJob();
        if (job != null) {
            result *= job.getStartTaskProbabilityModifier(PerformLaboratoryResearch.class);
        }
        
        return result;
    }
}