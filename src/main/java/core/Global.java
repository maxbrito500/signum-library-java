/*
 * Description: Global objects reused across the code
 * License: Apache-2.0
 */
package core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Date: 2021-09-21 Place: Zwingenberg, Germany
 *
 * @author Max Brito
 */
public class Global {

    public static final Logger logger = LogManager.getLogger(Actions.class);

    private final static String pathNamePlots = "plots";
    
    static {
        // read the config for the log4j class
        PropertyConfigurator.configure(Global.class.getResourceAsStream("/log4j.properties"));
    }

    /**
     * Gets the pointer to the folder where plots are mainly stored.
     * Attention that this is only the default location, advanced users
     * will place their plots in multiple different locations.
     * @return null when something went wrong.
     */
    public static File getFolderPlotsMain() {
        Path path = Paths.get(pathNamePlots);
        File folder = path.toFile();
        if (folder.exists() == false) {
            try {
                Files.createDirectories(path);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(Global.class.getName()).log(Level.SEVERE, null, ex);
                logger.error("Failed to create a folder for the plots");
                return null;
            }
        }
        return folder;
    }

}
