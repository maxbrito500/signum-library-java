/*
 * Description: Mining and plotting actions
 * License: Apache-2.0
 */
package core;

import java.io.File;
import org.apache.log4j.PropertyConfigurator;
import miner.Miner;

/**
 * Date: 2021-09-19
 * Place: Zwingenberg, Germany
 * @author Max Brito
 */
public class Actions {

    private final Miner miner;
    
    @SuppressWarnings("CallToThreadStartDuringObjectConstruction")
    public Actions() {
        // read the config for the log4j class
        PropertyConfigurator.configure(getClass().getResourceAsStream("/log4j.properties"));
        // get the miner started
        miner = new Miner();
        // add the path to the plots
        File path = new File("/home/brito/temp/plot-signa");
        miner.addMiningFolder(path);
    }

    public void start() {
        miner.startMining();
    }

}
