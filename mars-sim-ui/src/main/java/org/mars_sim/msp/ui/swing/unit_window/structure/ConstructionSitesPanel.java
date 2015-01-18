/**
 * Mars Simulation Project
 * ConstructionSitesPanel.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure;

import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.structure.construction.*;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

/**
 * A panel displaying a list of construction sites at a settlement.
 */
public class ConstructionSitesPanel extends JPanel {

    // Data members
    private ConstructionManager manager;
    private List<ConstructionSite> sitesCache;
    private JPanel sitesListPane;
    private JScrollPane sitesScrollPane;
    
    /**
     * Constructor
     * @param manager the settlement construction manager.
     */
    public ConstructionSitesPanel(ConstructionManager manager) {
        // Use JPanel constructor.
        super();
        
        this.manager = manager;
        
        setLayout(new BorderLayout());
        setBorder(new MarsPanelBorder());
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        add(titlePanel, BorderLayout.NORTH);
        
        JLabel titleLabel = new JLabel("Construction Sites");
        titlePanel.add(titleLabel);
        
        // Create scroll panel for sites list pane.
        sitesScrollPane = new JScrollPane();
        sitesScrollPane.setPreferredSize(new Dimension(200, 75));
        sitesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(sitesScrollPane, BorderLayout.CENTER);  
        
        // Prepare sites outer list pane.
        JPanel sitesOuterListPane = new JPanel(new BorderLayout(0, 0));
        sitesScrollPane.setViewportView(sitesOuterListPane);
        
        // Prepare sites list pane.
        sitesListPane = new JPanel();
        sitesListPane.setLayout(new BoxLayout(sitesListPane, BoxLayout.Y_AXIS));
        sitesOuterListPane.add(sitesListPane, BorderLayout.NORTH);
        
        // Create the site panels.
        sitesCache = manager.getConstructionSites();
        Iterator<ConstructionSite> i = sitesCache.iterator();
        while (i.hasNext()) sitesListPane.add(new ConstructionSitePanel(i.next()));
    }
    
    /**
     * Update the information on this panel.
     */
    public void update() {
        // Update sites is necessary.
        List<ConstructionSite> sites = manager.getConstructionSites();
        if (!sitesCache.equals(sites)) {
            
            // Add site panels for new sites.
            Iterator<ConstructionSite> i = sites.iterator();
            while (i.hasNext()) {
                ConstructionSite site = i.next();
                if (!sitesCache.contains(site)) 
                    sitesListPane.add(new ConstructionSitePanel(site));
            }
            
            // Remove site panels for old sites.
            Iterator<ConstructionSite> j = sitesCache.iterator();
            while (j.hasNext()) {
                ConstructionSite site = j.next();
                if (!sites.contains(site)) {
                    ConstructionSitePanel panel = getConstructionSitePanel(site);
                    if (panel != null) sitesListPane.remove(panel);
                }
            }
            
            sitesScrollPane.validate();
            
            // Update sitesCache
            sitesCache.clear();
            sitesCache.addAll(sites);
        }
        
        // Update all site panels.
        Iterator<ConstructionSite> i = sites.iterator();
        while (i.hasNext()) {
            ConstructionSitePanel panel = getConstructionSitePanel(i.next());
            if (panel != null) panel.update();
        }
    }
    
    /**
     * Gets a construction site panel for a particular construction site.
     * @param site the construction site.
     * @return construction site panel or null if none found.
     */
    private ConstructionSitePanel getConstructionSitePanel(ConstructionSite site) {
        ConstructionSitePanel result = null;
        
        for (int x = 0; x < sitesListPane.getComponentCount(); x++) {
            Component component = sitesListPane.getComponent(x);
            if (component instanceof ConstructionSitePanel) {
                ConstructionSitePanel panel = (ConstructionSitePanel) component;
                if (panel.getConstructionSite().equals(site)) result = panel;
            }
        }
        
        return result;
    }
    
    /**
     * A panel displaying information about a particular construction site.
     */
    private static class ConstructionSitePanel extends JPanel {
        
        // Data members
        private ConstructionSite site;
        private JLabel statusLabel;
        private BoundedRangeModel workBarModel;
        
        /**
         * Constructor.
         * @param site the construction site.
         */
        private ConstructionSitePanel(ConstructionSite site) {
            // Use JPanel constructor
            super();
            
            // Initialize data members.
            this.site = site;
            
            // Set the layout.
            setLayout(new BorderLayout(5, 5));
            
            // Set border
            setBorder(new MarsPanelBorder());
            
            // Create the status panel.
            statusLabel = new JLabel("Status: ", JLabel.LEFT);
            add(statusLabel, BorderLayout.NORTH);
            
            // Create the progress bar panel.
            JPanel progressBarPanel = new JPanel();
            add(progressBarPanel, BorderLayout.CENTER);
            
            // Prepare work progress bar.
            JProgressBar workBar = new JProgressBar();
            workBarModel = workBar.getModel();
            workBar.setStringPainted(true);
            progressBarPanel.add(workBar);
            
            // Update progress bar.
            update();
            
            // Add tooltip.
            setToolTipText(getToolTipString());
        }
        
        /**
         * Gets the construction site for this panel.
         * @return construction site.
         */
        private ConstructionSite getConstructionSite() {
            return site;
        }
        
        /**
         * Updates the panel information.
         */
        private void update() {
            
            // Update status label.
            String statusString = getStatusString();
            
            // Make sure status label isn't too long.
            if (statusString.length() > 31) statusString = statusString.substring(0, 31) + "...";
            
            // Set the text in the status label.
            statusLabel.setText(statusString);
            
            // Update work progress bar.
            int workProgress = 0;
            ConstructionStage stage = site.getCurrentConstructionStage();
            if (stage != null) {
                double completedWork = stage.getCompletedWorkTime();
                double requiredWork = stage.getRequiredWorkTime();
                if (requiredWork > 0D) workProgress = (int) (100D * completedWork / requiredWork);
            }
            workBarModel.setValue(workProgress);
            
            // Update the tool tip string.
            setToolTipText(getToolTipString());
        }
        
        /**
         * Gets the status label string.
         * @return status string.
         */
        private String getStatusString() {
            String statusString = "";
            ConstructionStage stage = site.getCurrentConstructionStage();
            if (stage != null) {
                if (site.isUndergoingConstruction()) statusString = "Status: constructing " + 
                        stage.getInfo().getName();
                else if (site.isUndergoingSalvage()) statusString = "Status: salvaging " + 
                        stage.getInfo().getName();
                else if (site.hasUnfinishedStage()) {
                    if (stage.isSalvaging()) statusString = "Status: salvaging " + 
                            stage.getInfo().getName() + " unfinished";
                    else statusString = "Status: constructing " + 
                            stage.getInfo().getName() + " unfinished";
                }
                else statusString = "Status: " + stage.getInfo().getName() + " completed";
            }
            else statusString = "No construction";
            
            return statusString;
        }
        
        /**
         * Gets a tool tip string for the panel.
         */
        private String getToolTipString() {
            StringBuilder result = new StringBuilder("<html>");
            result.append(getStatusString()).append("<br>");
            
            ConstructionStage stage = site.getCurrentConstructionStage();
            if (stage != null) {
                ConstructionStageInfo info = stage.getInfo();
                result.append("Stage Type: ").append(info.getType()).append("<br>");
                if (stage.isSalvaging()) result.append("Work Type: salvage<br>");
                else result.append("Work Type: Construction<br>");
                DecimalFormat formatter = new DecimalFormat("0.0");
                String requiredWorkTime = formatter.format(stage.getRequiredWorkTime() / 1000D);
                result.append("Work Time Required: ").append(requiredWorkTime).append(" Sols<br>");
                String completedWorkTime = formatter.format(stage.getCompletedWorkTime() / 1000D);
                result.append("Work Time Completed: ").append(completedWorkTime).append(" Sols<br>");
                result.append("Architect Construction Skill Required: ").append(info.getArchitectConstructionSkill()).append("<br>");
                
                // Add construction resources.
                if ((info.getResources().size() > 0) && !stage.isSalvaging()) {
                    result.append("<br>Construction Resources:<br>");
                    Iterator<AmountResource> i = info.getResources().keySet().iterator();
                    while (i.hasNext()) {
                        AmountResource resource = i.next();
                        double amount = info.getResources().get(resource);
                        result.append("&nbsp;&nbsp;").append(resource.getName()).append(": ").append(amount).append(" kg<br>");
                    }
                }
                
                // Add construction parts.
                if (info.getParts().size() > 0) {
                    if (stage.isSalvaging()) result.append("<br>Salvagable Parts:<br>");
                    else result.append("<br>Construction Parts:<br>");
                    Iterator<Part> j = info.getParts().keySet().iterator();
                    while (j.hasNext()) {
                        Part part = j.next();
                        int number = info.getParts().get(part);
                        result.append("&nbsp;&nbsp;").append(part.getName()).append(": ").append(number).append("<br>");
                    }
                }
                
                // Add construction vehicles.
                if (info.getVehicles().size() > 0) {
                    if (stage.isSalvaging()) result.append("<br>Salvage Vehicles:<br>");
                    else result.append("<br>Construction Vehicles:<br>");
                    Iterator<ConstructionVehicleType> k = info.getVehicles().iterator();
                    while (k.hasNext()) {
                        ConstructionVehicleType vehicle = k.next();
                        result.append("&nbsp;&nbsp;Vehicle Type: ").append(vehicle.getVehicleType()).append("<br>");
                        result.append("&nbsp;&nbsp;Attachment Parts:<br>");
                        Iterator<Part> l = vehicle.getAttachmentParts().iterator();
                        while (l.hasNext()) {
                            result.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(l.next().getName()).append("<br>");
                        }
                    }
                }
            }
            
            result.append("</html>");
            
            return result.toString();
        }
    }   
}