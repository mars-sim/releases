/**
 * Mars Simulation Project
 * SVGMapUtil.java
 * @version 3.04 2013-04-23
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.settlement;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.batik.gvt.GraphicsNode;
import org.mars_sim.msp.ui.swing.SVGLoader;

/**
 * Static utility class for mapping settlement map structures, such
 * as buildings and construction sites, with their SVG image views 
 * on the settlement map.
 */
public final class SVGMapUtil {

    // Static members.
    private static Properties svgMapProperties;
    
    /**
     * Private constructor for utility class.
     */
    private SVGMapUtil() {}
    
    /**
     * Loads the SVG image mapping properties file.
     */
    private static void loadSVGImageMappingPropertiesFile() {
        
        svgMapProperties = new Properties();

        String fileName = SVGLoader.SVG_DIR + "svg_image_mapping.properties";
        URL resource = SVGLoader.class.getResource(fileName);
        InputStream inputStream = null;
        try {
            inputStream = resource.openStream();
            svgMapProperties.load(inputStream);
        }
        catch (IOException e) {
            e.printStackTrace(System.err);
        }
        finally {
            try {
                inputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
    }
    
    /**
     * Gets an SVG graphics node for a given map item name.
     * @param prefix the property key prefix (ex: building)
     * @param name the name of the map item.
     * @return the SVG graphics node.
     */
    private static GraphicsNode getSVGGraphicsNode(String prefix, String name) {
        
        GraphicsNode result = null;
        
        // Load svgMapProperties from file if necessary.
        if (svgMapProperties == null) {
            loadSVGImageMappingPropertiesFile();
        }
        
        // Append property prefix.
        StringBuffer propertyNameBuff = new StringBuffer("");
        if (prefix != null) {
            propertyNameBuff.append(prefix);
            propertyNameBuff.append(".");
        }
        
        // Append name.
        if (name != null) {
            String prepName = name.trim().toLowerCase();
            propertyNameBuff.append(prepName);
        }
        
        String propertyName = propertyNameBuff.toString();
        
        String svgFileName = svgMapProperties.getProperty(propertyName);
        if (svgFileName != null) {
            result = SVGLoader.getSVGImage(svgFileName);
        }
        
        return result;
    }
    
    /**
     * Gets a SVG node for a building.
     * @param buildingName the building's name.
     * @return SVG node or null if none found.
     */
    public static GraphicsNode getBuildingSVG(String buildingName) {
        
        return getSVGGraphicsNode("building", buildingName);
    }
    
    /**
     * Gets a SVG node for a construction site.
     * @param constructionSiteStageName the construction site's current stage name.
     * @return SVG node or null if none found.
     */
    public static GraphicsNode getConstructionSiteSVG(String constructionSiteStageName) {
        
        return getSVGGraphicsNode("construction_stage", constructionSiteStageName);
    }
    
    /**
     * Gets a SVG node for a vehicle.
     * @param vehicleType the vehicle type.
     * @return SVG node or null if none found.
     */
    public static GraphicsNode getVehicleSVG(String vehicleType) {
        
        return getSVGGraphicsNode("vehicle", vehicleType);
    }
    
    /**
     * Gets a SVG node for a vehicle maintenance/repair overlay.
     * @param vehicleType the vehicle type.
     * @return SVG node of null if none found.
     */
    public static GraphicsNode getMaintenanceOverlaySVG(String vehicleType) {
        
        return getSVGGraphicsNode("vehicle.maintenance", vehicleType);
    }
    
    /**
     * Gets a SVG node for a vehicle loading/unloading overlay.
     * @param vehicleType the vehicle type.
     * @return SVG node of null if none found.
     */
    public static GraphicsNode getLoadingOverlaySVG(String vehicleType) {
        
        return getSVGGraphicsNode("vehicle.loading", vehicleType);
    }
    
    /**
     * Gets a SVG node for an attachment part.
     * @param partType the part type.
     * @return SVG node or null if none found.
     */
    public static GraphicsNode getAttachmentPartSVG(String partType) {
        
        return getSVGGraphicsNode("vehicle.attachment_part", partType);
    }
}