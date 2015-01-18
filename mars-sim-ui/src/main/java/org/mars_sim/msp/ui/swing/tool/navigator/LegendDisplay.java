/**
 * Mars Simulation Project
 * LegendDisplay.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 * @author Greg Whelan
 */
package org.mars_sim.msp.ui.swing.tool.navigator;

import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.ui.swing.ImageLoader;

/** 
 * The LegendDisplay class is a UI class that represents a map legend
 * in the `Mars Navigator' tool. It can either show a distance
 * legend, or a color chart indicating elevation for the
 * topographical map.
 */
public class LegendDisplay
extends JLabel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	/** Image icon. */
	private ImageIcon legend;
	private Image colorImg;
	private Image distanceImg;
	private Image usgsDistanceImg;
	private boolean useUSGSLegend;

	/** Constructs a LegendDisplay object */
	public LegendDisplay() {
		colorImg = ImageLoader.getImage(Msg.getString("img.mars.colorLegend")); //$NON-NLS-1$
		distanceImg = ImageLoader.getImage(Msg.getString("img.mars.mapLegend")); //$NON-NLS-1$
		usgsDistanceImg = ImageLoader.getImage(Msg.getString("img.mars.usgsMapLegend")); //$NON-NLS-1$
		legend = new ImageIcon(distanceImg);
		setIcon(legend);
		useUSGSLegend = false;
	}

	/** Change to topographical mode */
	public void showColor() {
		legend.setImage(colorImg);
		repaint();
	}

	/** Change to distance mode and refresh canvas */
	public void showMap() {
		if (useUSGSLegend) legend.setImage(usgsDistanceImg);
		else legend.setImage(distanceImg);
		repaint();
	}

	/** Set USGS map legend mode
	 *  @param useUSGSLegend true if using USGS map legend
	 */
	public void setUSGSMode(boolean useUSGSLegend) {
		this.useUSGSLegend = useUSGSLegend;
	}
}
