/*
 * Description: Functions for mining the plot files on disk
 * License: Apache-2.0
 */
package miner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import signumj.entity.SignumAddress;
import core.Actions;
import utils.OSValidator;

/**
 * Date: 2021-09-20
 * Place: Zwingenberg, Germany
 * @author Max Brito
 */
public class Miner {

    public static String pool = "https://pool.signumcoin.ro";
    int cpusToMine = 2;
    String[] poolList = {
        "https://pool.signumcoin.ro",
        "http://signa.voiplanparty.com:8124",
        "http://opensignumpool.ddns.net:8126",
        "http://signumpool.de:8080",
        "https://signumpool.com",
        "https://signapool.notallmine.net",
        "https://fomplopool.com",
        "https://signum.space",
        "http://signum.land",};

    private final Logger logger= LogManager.getLogger(Actions.class);
    private File minerFile;
    private final ArrayList<File> pathList = new ArrayList<>();

    private boolean mining;
    private Process minerProcess;
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String MINER_CONFIG_FILE = "btdex-miner.yaml";
    private final HashMap<String, String> poolMaxDeadlines = new HashMap<>();
    private static final OkHttpClient CLIENT = new OkHttpClient();
    private final LinkedHashMap<String, SignumAddress> poolAddresses = new LinkedHashMap<>();

    public Miner() {
        // update the information we have about our pools
        updatePoolInfo(poolList);
    }
    
    /**
     * Update the list of pools that are supported
     * @param urlList 
     */
    private void updatePoolInfo(String[] urlList) {
        logger.info("Updating the pool information");
        for (String poolURL : urlList) {
            String poolURLgetConfig = poolURL + "/api/getConfig";
            try {
                Request request = new Request.Builder().url(poolURLgetConfig).build();
                Response responses = CLIENT.newCall(request).execute();
                String jsonData = responses.body().string();
                JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();

                SignumAddress poolAddress = SignumAddress.fromEither(json.get("poolAccount").getAsString());
                poolAddresses.put(poolURL, poolAddress);
                poolMaxDeadlines.put(poolURL, "100000000");

                JsonElement jsonMaxDeadline = json.get("maxDeadline");
                if (jsonMaxDeadline != null) {
                    poolMaxDeadlines.put(poolURL, jsonMaxDeadline.getAsString());
                }
                logger.info("Pool added: " + poolURL);
            } catch (JsonSyntaxException | IOException e) {
                logger.debug("Pool incompatible or down: " + poolURL);
            }
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void startMining() {
        if (mining) {
            return;
        }

        logger.info("Started mining");
        mining = true;

        String minerName = "signum-miner";
        if (OSValidator.IS_WINDOWS) {
            minerName += ".exe";
        } else if (OSValidator.IS_MAC) {
            minerName += ".app";
        }

        minerFile = new File(TMP_DIR, minerName);

        InputStream minerStream = getClass().getResourceAsStream("/miner/" + minerName);
        InputStream minerConfigStream = (getClass().getResourceAsStream("/miner/config.yaml"));
        try {
            if (minerFile.exists() && minerFile.isFile()) {
                minerFile.delete();
            }

            logger.info("Copying miner to: " + minerFile.getAbsolutePath());
            Files.copy(minerStream, minerFile.getAbsoluteFile().toPath());
            if (!OSValidator.IS_WINDOWS) {
                minerFile.setExecutable(true);
            }

            File minerConfigFile = new File(minerFile.getParent(), MINER_CONFIG_FILE);
            FileWriter minerConfig = new FileWriter(minerConfigFile);
            minerConfig.append("plot_dirs:\n");
            for (File path : pathList) {
                if (path == null) {
                    continue;
                }
                minerConfig.append(" - '" + path.getAbsolutePath() + "'\n");
            }
            minerConfig.append("url: '" + pool + "'\n");

            String poolDeadline = poolMaxDeadlines.get(pool);
            minerConfig.append("target_deadline: " + poolDeadline + "\n");

            minerConfig.append("cpu_threads: " + (cpusToMine) + "\n");
            minerConfig.append("cpu_worker_task_count: " + (cpusToMine) + "\n");

            logger.info("Copying miner config to: " + minerConfigFile.getAbsolutePath());
            IOUtils.copy(minerConfigStream, minerConfig);
            minerConfig.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            //Toast.makeText((JFrame) SwingUtilities.getWindowAncestor(this), ex.getMessage(), Toast.Style.ERROR).display();
            mining = false;
            return;
        }
        MineThread mineThread = new MineThread();
        mineThread.start();
    }

    /**
     * Add a new folder where the mining plots are located
     * @param path 
     */
    public void addMiningFolder(File path) {
        pathList.add(path);
    }

    class MineThread extends Thread {

        @Override
        @SuppressWarnings("CallToPrintStackTrace")
        public void run() {
            try {
                String cmd = minerFile.getAbsolutePath() + " -c " + MINER_CONFIG_FILE;
                logger.info("Running miner with: " + cmd);
                minerProcess = Runtime.getRuntime().exec(cmd, null, minerFile.getParentFile());

                InputStream stdIn = minerProcess.getInputStream();
                InputStreamReader isr = new InputStreamReader(stdIn);
                BufferedReader br = new BufferedReader(isr);
                boolean isAlive = minerProcess.isAlive();
                while (isAlive) {
                    String line = br.readLine();
                    if (line != null) {
                        logger.info(line);
                        // addToConsole(MINER_APP, line);
                    }
                    isAlive = minerProcess.isAlive();
                    
                }
                mining = false;
                minerFile.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    
}
