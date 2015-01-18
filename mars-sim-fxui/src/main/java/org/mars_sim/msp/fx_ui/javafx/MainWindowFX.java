/**
 * Mars Simulation Project
 * MainWindowFX.java
 * @version 3.07 2015-01-10
 * @author Lars Næsbye Christensen
 */

package org.mars_sim.msp.fx_ui.javafx;

import javafx.stage.Stage;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.scene.text.Font;

/**
 * The MainWindowFX class is the primary JavaFX frame for the project.
 * It replaces the MainWindow class from version 4.0 on.
 */
public class MainWindowFX {

	public static final String WINDOW_TITLE = "4.0";


	/**
	 * Constructor.
	 */
	public MainWindowFX() {
        Text text = new Text(10, 40, "Mars Simulation Project "+WINDOW_TITLE);
        text.setFont(new Font(40));
        Stage stage = new Stage();
        Scene scene = new Scene(new Group(text));

        stage.setTitle("JavaFX MSP testbed!"); 
        stage.setScene(scene); 
        stage.sizeToScene(); 
        stage.show(); 

	}
}