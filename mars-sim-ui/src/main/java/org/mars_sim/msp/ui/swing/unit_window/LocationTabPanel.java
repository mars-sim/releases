/**
 * Mars Simulation Project
 * LocationTabPanel.java
 * @version 3.07 2015-01-16
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_window;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;

/** 
 * The LocationTabPanel is a tab panel for location information.
 */
public class LocationTabPanel
extends TabPanel
implements ActionListener {

	/** default serial id. */
	private static final long serialVersionUID = 12L;

	 /** default logger.   */
	//private static Logger logger = Logger.getLogger(LocationTabPanel.class.getName());

	// 2014-11-11 Added new panels and labels
	private JPanel tpPanel =  new JPanel();
	private JPanel outsideReadingPanel = new JPanel();
	private JPanel containerPanel = new JPanel();
	private JLabel temperatureLabel;
	private JLabel airPressureLabel;
	private JLabel locationLabel;
	//private Color THEME_COLOR = Color.ORANGE;
	private double airPressureCache;
	private double temperatureCache;
	private Unit containerCache;
	
	private JPanel locationCoordsPanel;
	private JLabel latitudeLabel;
	private JLabel longitudeLabel;
	private JPanel locationLabelPanel;
	private JButton locationButton;
	
	private Coordinates locationCache;
	private JButton centerMapButton;

	DecimalFormat fmt = new DecimalFormat("###"); 
    /**
     * Constructor.
     * @param unit the unit to display.
     * @param desktop the main desktop.
     */
    public LocationTabPanel(Unit unit, MainDesktopPane desktop) { 
        // Use the TabPanel constructor
        super("Location", null, "Location", unit, desktop);
        
        // Initialize locationLabel.
        locationLabel = new JLabel("");
        
        // Create location panel
        JPanel locationPanel = new JPanel(new BorderLayout(0,0));
        locationPanel.setBorder(new MarsPanelBorder());
        locationPanel.setBorder(new EmptyBorder(5, 5, 5, 5) );
        
        topContentPanel.add(locationPanel);
        //topContentPanel.setBackground(THEME_COLOR);
        //locationPanel.setBackground(THEME_COLOR);
        
        // Create location label panel
        locationLabelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        //locationLabelPanel.setBackground(THEME_COLOR);
        locationPanel.add(locationLabelPanel, BorderLayout.NORTH);  
        
        // Prepare location coordinates panel
        locationCoordsPanel = new JPanel();
        //locationCoordsPanel.setBackground(THEME_COLOR);
        locationLabelPanel.add(locationCoordsPanel);
        //locationLabelPanel.setBackground(THEME_COLOR);
        locationCoordsPanel.setBorder(new EmptyBorder(5, 5, 5, 5) );
        locationCoordsPanel.setLayout(new BorderLayout(0, 0));
                
        // Create center map button
        centerMapButton = new JButton(ImageLoader.getIcon("CenterMap"));
        centerMapButton.setMargin(new Insets(1, 1, 1, 1));
        centerMapButton.addActionListener(this);
        centerMapButton.setOpaque(false);
        //centerMapButton.setBackground(THEME_COLOR);
        centerMapButton.setToolTipText("Locate in Mars Navigator (center map on location)");
        locationLabelPanel.add(centerMapButton);
         
        // Create location button
        locationButton = new JButton();
        locationButton.setOpaque(false);
        //locationButton.setBackground(THEME_COLOR);
        locationButton.addActionListener(this);

        containerPanel.add(locationButton);

        // Initialize location cache
        locationCache = new Coordinates(unit.getCoordinates());

        // Prepare latitude label
        latitudeLabel = new JLabel(getLatitudeString());
        latitudeLabel.setOpaque(false);
        latitudeLabel.setFont(new Font("Serif", Font.PLAIN, 13));
        latitudeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        locationCoordsPanel.add(latitudeLabel, BorderLayout.NORTH);
        
        // Prepare longitude label
        longitudeLabel = new JLabel(getLongitudeString());
        longitudeLabel.setOpaque(false);
        longitudeLabel.setFont(new Font("Serif", Font.PLAIN, 13));
        longitudeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        locationCoordsPanel.add(longitudeLabel, BorderLayout.CENTER);
        
        // 2014-11-11 Set up tpPanel for outside temperature and pressure
        tpPanel.setOpaque(false);
        BorderLayout tpLayout = new BorderLayout(0, 0);
        tpPanel.setLayout(tpLayout);
        //tpPanel.setBackground(THEME_COLOR);
        
        //TitledBorder tpTitle;
        //tpTitle = BorderFactory.createTitledBorder("Outside");
        //tpTitle.setTitleFont(new Font("Serif", Font.ITALIC, 9));
        //tpPanel.setBorder(tpTitle);
        //tpPanel.setBorder(BorderFactory.createEtchedBorder());
        
        // Prepare air pressure label
        airPressureLabel = new JLabel(getAirPressureString());
        airPressureLabel.setOpaque(false);
        airPressureLabel.setFont(new Font("Serif", Font.PLAIN, 13));
        airPressureLabel.setHorizontalAlignment(SwingConstants.CENTER);
        tpPanel.add(airPressureLabel, BorderLayout.CENTER); 
        
        // Prepare temperature label
        temperatureLabel = new JLabel(getTemperatureString());
        temperatureLabel.setOpaque(false);
        temperatureLabel.setFont(new Font("Serif", Font.PLAIN, 13));
        temperatureLabel.setHorizontalAlignment(SwingConstants.CENTER);
        tpPanel.add(temperatureLabel, BorderLayout.NORTH);
     
        locationLabelPanel.add(outsideReadingPanel);
        
        // Add the location button or outsideReadingPanel depending on the situation.
        Unit container = unit.getContainerUnit();
        if (container != null) {
            locationButton.setText(container.getName());
            addContainerPanel();
        }
        else {   
         	// 2014-10-22 Called new method checkOutsideReading()
        	checkOutsideReading();
        	addOutsideReadingPanel();
        }
        
        // initialize containerCache
        containerCache = unit.getContainerUnit();
    }
    
    // 2014-11-11 Modified temperature and pressure panel
    public String getTemperatureString() {
    	String result;
    	// 2015-01-16 Used Msg.getString for the degree sign
    	result = fmt.format(getTemperature()) + Msg.getString("temperature.sign.degreeCelsius"); //$NON-NLS-1$
    	// 2014-11-20 Changed from " °C" to " �C" for English Locale
		//return fmt.format(getTemperature()) + " °C"; //2 b localized?
    	return result;
    }
    public double getTemperature() {
    	double outsideTemp = Simulation.instance().getMars().getWeather()
    			.getTemperature(unit.getCoordinates());
		return outsideTemp;
    }
    
    // 2014-11-07 Added temperature and pressure panel    
    public String getAirPressureString() {
    	return fmt.format(getAirPressure()) + " kPa"; //2 b localized?
    }
    public double getAirPressure() {
    	double outsideAirPressure = Simulation.instance().getMars().getWeather()
	            .getAirPressure(unit.getCoordinates());
    	return outsideAirPressure;
    }
	
	private String getLatitudeString() {
		return locationCache.getFormattedLatitudeString();
	}

	private String getLongitudeString() {
		return locationCache.getFormattedLongitudeString();
	}
	

    /** 
     * Check the type of unit and its location 
     * Obtain temperature and pressure reading if the unit is outside the settlement
     * 
     * @param calling setText to update the locationTextLabel
     */
    // 2014-11-11 Overhauled checkOutsideReading()
	public void checkOutsideReading() {   
		
	       // 2014-11-11 Added temperature and pressure panel
        if (airPressureCache != getAirPressure()) {
        	airPressureCache = getAirPressure();
            airPressureLabel.setText(getAirPressureString());
        }
        
        if (temperatureCache != getTemperature()) {
        	temperatureCache = getTemperature();
        	temperatureLabel.setText(getTemperatureString());
        }
        
		//outsideReadingPanel.add(tpPanel);
		
		if (unit instanceof Settlement) {
			Settlement settlement = (Settlement) unit;
			outsideReadingPanel.remove(tpPanel);
			outsideReadingPanel.add(tpPanel);	
		}	
	    if (unit instanceof Person) {

	        Person person = (Person) unit;   
	        if (person.getLocationSituation() == LocationSituation.OUTSIDE) {
				outsideReadingPanel.remove(tpPanel);
	        	outsideReadingPanel.add(tpPanel);
	        }
	        else if (person.getLocationSituation() == LocationSituation.BURIED) {
				outsideReadingPanel.remove(tpPanel);
				// 2014-11-29 Commented out remove(locationLabel) to avoid Exception
				//outsideReadingPanel.remove(locationLabel);
				outsideReadingPanel.add(locationLabel);
				// 2014-11-17 Fixed NullPointer Exception. setText after adding locationLabel;
				locationLabel.setText("Buried Outside");
			}
	        else { // the person is inside a settlement/vehicle
	        	outsideReadingPanel.remove(tpPanel);
	        	outsideReadingPanel.add(locationLabel);
	        }
	    }
	    if (unit instanceof Vehicle) {
        	Vehicle vehicle = (Vehicle) unit;
        	if (vehicle.getStatus() == "Moving" ||
        		vehicle.getStatus() == "Towed" ||
   	      		// TODO: what if vehicle is malfunction in the settlement, NOT during an excursion
        		vehicle.getStatus() == "Malfunction") {    
    			outsideReadingPanel.remove(tpPanel);
				outsideReadingPanel.add(tpPanel);
        	}
        	else { // the vehicle should be in the settlement
            	outsideReadingPanel.remove(tpPanel);
	        	outsideReadingPanel.add(locationLabel);
        	}
	    }
	}
	
	
	
        	
    /** 
     * Action event occurs.
     *
     * @param event the action event
     */
    public void actionPerformed(ActionEvent event) {
        JComponent source = (JComponent) event.getSource();
        
        // If the center map button was pressed, update navigator tool.
        if (source == centerMapButton)
            desktop.centerMapGlobe(unit.getCoordinates());
            
        // If the location button was pressed, open the unit window.
        if (source == locationButton) 
            desktop.openUnitWindow(unit.getContainerUnit(), false);
    }
    
    /**
     * Updates the info on this panel.
     */
    // 2014-11-11 Overhauled update()
    public void update() {
        
        // If unit's location has changed, update location display.
    	// TODO: if a person goes outside the settlement for servicing an equipment
    	// does the coordinate (down to how many decimal) change?
        if (!locationCache.equals(unit.getCoordinates())) {
            locationCache.setCoords(unit.getCoordinates());
            latitudeLabel.setText(getLatitudeString());
            longitudeLabel.setText(getLongitudeString());
        }
 
        // Update location button or location text label as necessary.
        Unit container = unit.getContainerUnit();
        //if (!containerCache.equals(container)) {
        if (containerCache != container) {
        	containerCache = container;
        	// the unit's container has changed
            if (container != null) {
            	locationButton.setText(container.getName());
            	addContainerPanel();
            }                  
            else {
         	// 2014-10-22 mkung: Called new method checkOutsideReading()
        	checkOutsideReading();
        	addOutsideReadingPanel();
            }
        }
        else; // the unit's container has NOT changed
    }
    
    /**
     * Adds the location button to the location label panel if it isn't already on
     * there and removes the location text label if it's there.
     */
    // 2014-11-11 Modified addContainerPanel()
    private void addContainerPanel() {
        try {
            Component lastComponent = locationLabelPanel.getComponent(3);
            if (lastComponent == outsideReadingPanel) {
                locationLabelPanel.remove(outsideReadingPanel);
                locationLabelPanel.add(containerPanel);
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            locationLabelPanel.remove(outsideReadingPanel);
            locationLabelPanel.add(containerPanel);
        }
    }
    
    /**
     * Adds the outsideReadingPanel if it isn't already on
     * there and removes the location button if it's there.
     */
    // 2014-11-11 Modified addOutsideReadingPanel()
    private void addOutsideReadingPanel() {
        try {
            Component lastComponent = locationLabelPanel.getComponent(3); 
            if (lastComponent == locationButton) {
                locationLabelPanel.remove(containerPanel);
                locationLabelPanel.add(outsideReadingPanel);
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            locationLabelPanel.remove(containerPanel);
            locationLabelPanel.add(outsideReadingPanel);
        }
    }
}   
