/*
 * Description: Test the creation of plot files
 * License: Apache-2.0
 */
package basic;

import core.Actions;
import core.Global;
import java.io.File;
import java.math.BigInteger;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import plotter.Plotter;
import static utils.NumberFormatting.ONE_GIB;

/**
 * Date: 2021-09-21 Place: Zwingenberg, Germany
 *
 * @author Max Brito
 */
public class PlottingTest {

    private final Logger logger = LogManager.getLogger(Actions.class);

    private final String numericId = "5505480724951664437";
    
    public PlottingTest() {
        // read the config for the log4j class
        PropertyConfigurator.configure(getClass().getResourceAsStream("/log4j.properties"));
    }
    
    @Test
    public void getHigherNonce(){
        // test that we can get a nonce higher than what is expected
        Plotter plotter = new Plotter(numericId);
        long value = plotter.getNonceHighestInsideFolder(0, Global.getFolderPlotsMain());
        assertEquals(0, value);
        value = plotter.getNonceHighestInsideFolder(1500, Global.getFolderPlotsMain());
        assertEquals(1500, value);
        // create a fake file on disk
        File file = new File(Global.getFolderPlotsMain(), 
                numericId + "_15871238935771348992_38144");
        BigInteger value1 = plotter.getNonceHighestFromFile(new BigInteger("1500"), file);
        // test that we are getting the right higher value
        assertEquals("15871238935771348992", value1 + "");
    }
    

    @Test
    @SuppressWarnings("SleepWhileInLoop")
    public void createPlot() throws InterruptedException {
        long plotSize = ONE_GIB;

        // choose a folder for plotting
        File folderPlot = new File("plot");

        Plotter plotter = new Plotter(numericId);

        logger.info("Beginning to plot");
        plotter.startPlotting(plotSize, folderPlot);

//        while (plotter.isPlotting()) {
//            Thread.sleep(1000);
//        }
        logger.info("Completed plot test");
    }
}
