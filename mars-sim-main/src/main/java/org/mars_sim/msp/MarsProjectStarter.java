/**
 * Mars Simulation Project
 * MarsProjectStarter.java
 * @version 3.03 2012-06-14
 * @author Scott Davis
 */

package org.mars_sim.msp;

import java.io.File;
import java.io.IOException;

/**
 * MarsProjectStarter is the default main class for the main executable JAR.
 * It creates a new virtual machine with 512MB memory and logging properties.
 * It isn't used in the webstart release.
 */
public class MarsProjectStarter {

	/**
	 * @param args
	 */
    public static void main(String[] args) {

        StringBuilder command = new StringBuilder();

        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            if (javaHome.contains(" ")) javaHome = "\"" + javaHome;
            command.append(javaHome).append(File.separator).append("bin").append(File.separator).append("java");
            if (javaHome.contains(" ")) command.append("\"");
        }
        else command.append("java");

        command.append(" -Xms256m");
        command.append(" -Xmx512m");
        command.append(" -Djava.util.logging.config.file=logging.properties");
        command.append(" -cp .").append(File.pathSeparator);
        command.append("*").append(File.pathSeparator);
        command.append("jars").append(File.separator).append("*");
        command.append(" org.mars_sim.msp.MarsProject");

        String commandStr = command.toString();
        System.out.println("Command: " + commandStr);
        try {
            Process process = Runtime.getRuntime().exec(commandStr);

            // Creating stream consumers for processes.
            StreamConsumer errorConsumer = new StreamConsumer(process.getErrorStream(), "OUTPUT");            
            StreamConsumer outputConsumer = new StreamConsumer(process.getInputStream(), "OUTPUT");

            // Starting the stream consumers.
            errorConsumer.start();
            outputConsumer.start();

            process.waitFor();
            
            // Close stream consumers.
            errorConsumer.join();
            outputConsumer.join();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}