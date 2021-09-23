/*
 * Description: Library for creating a single plot file
 * License: Apache-2.0
 */
package plotter;

import static core.Global.logger;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import utils.NumberFormatting;

/**
 * Date: 2021-09-21
 * Place: Zwingenberg, Germany
 * @author Max Brito
 */
public class Plotter {

    private File ssdPath; // only used when it is specified
    private File plotterUtilityFile;
    private File folderPlot; // where we store the plots
    private boolean lowPriorityCheck = true;
    private int cpusToPlot = 2;
    private final String numericId;
    private long plotFileSize;

    private static final long ONE_GIB = 1073741824L;
    private static final long BYTES_OF_A_NONCE = 262144L;
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    /**
     * Constructor for the plotter
     *
     * @param numericId The specific ID of the account
     */
    public Plotter(String numericId) {
        this.numericId = numericId;
    }

    private boolean plotting;
    private AtomicReference<Long> noncesPlotted = new AtomicReference<>();
    private long totalToPlot;
    private final ArrayList<File> pathList = new ArrayList<>();
    private ArrayList<File> newPlotFiles = new ArrayList<>();
    private ArrayList<File> resumePlotFiles = new ArrayList<>();
    private Process plotterProcess;

    // get only the files related to this numeric Id
    private final FileFilter PLOT_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isFile()
                    && pathname.getName().startsWith(numericId)
                    && pathname.getName().split("_").length == 3;
        }
    };

    public String formatSpace(double bytes) {
        bytes /= ONE_GIB;
        if (bytes < 500) {
            return NumberFormatting.SIGNA_2.format(bytes) + " GiB";
        }
        bytes /= 1024;
        return NumberFormatting.SIGNA_2.format(bytes) + " TiB";
    }

    public long getPercentPlotted() {
        return (100 * BYTES_OF_A_NONCE * noncesPlotted.get()) / totalToPlot;
    }

    /**
     * Calculates an initial nonce using random values
     *
     * @param folder
     * @return
     */
    public long getNonceInitial(File folder) {
        byte[] entropy = new byte[Short.BYTES];
        new SecureRandom().nextBytes(entropy);
        ByteBuffer bb = ByteBuffer.wrap(entropy);
        return (bb.getShort() & 0x0FFF) * 100000000000000L;
    }

    /**
     * Provides the highest nonce inside a given folder, compared to a given
     * value.
     * @param nonceStart the initial value as starting point. Use zero if none.
     * @param folder the folder where we find plot files
     * @return the highest number available or the same that was provided in
     * case no other was found
     */
    public long getNonceHighestInsideFolder(long nonceStart, File folder) {
        File[] plotFiles = folder.listFiles(PLOT_FILE_FILTER);
        for (File plot : plotFiles) {
            String pieces[] = plot.getName().split("_");
            long start = Long.parseUnsignedLong(pieces[1]);
            long nonces = Long.parseUnsignedLong(pieces[2]);
            nonceStart = Math.max(nonceStart, start + nonces);
        }
        return nonceStart;
    }
    
    /**
     * Provides the highest nonce inside a given folder, compared to a given
     * value.
     * @param nonceStart the initial value as starting point. Use zero if none.
     * @param file the file that we compare
     * @return the highest number available or the same that was provided in
     * case no other was found
     */
     public BigInteger getNonceHighestFromFile(BigInteger nonceStart, File file) {
            String pieces[] = file.getName().split("_");
            //long start = Long.parseUnsignedLong(pieces[1]);
            BigInteger start = new BigInteger(pieces[1]);
            //long nonces = Long.parseUnsignedLong(pieces[2]);
            BigInteger nonces = new BigInteger(pieces[2]);
            start.add(nonces);
            
            if(start.compareTo(nonceStart) == 1){
                return start;
            }else{
                return nonceStart;
            }
            //nonceStart = Math.max(nonceStart, start);
    }

    /**
     * Create the plot file
     * @param plotFileSize capacity of bytes that will be written
     * @param folderPlot where we store the plots that are generated
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public void startPlotting(long plotFileSize, File folderPlot) {
        // assign the variables
        this.plotFileSize = plotFileSize;
        this.folderPlot = folderPlot;
        
        if (plotting) {
            logger.error("Plotter is already running");
            return;
        }

        logger.info("Started plotting");
        noncesPlotted.set(0L);
        plotting = true;

        // Start nonce is random and we leave still enough bits for many PiB of unique nonces.
        // This way the user can disconnect disks and plot later or can use it on multiple machines.
        byte[] entropy = new byte[Short.BYTES];
        new SecureRandom().nextBytes(entropy);
        ByteBuffer bb = ByteBuffer.wrap(entropy);
        long startNonce = (bb.getShort() & 0x0FFF) * 100000000000000L;

//        for (File path : pathList) 
//            
//            if (path == null) {
//                continue;
//            }
//
//            File[] plotFiles = path.listFiles(PLOT_FILE_FILTER);
//            for (File plot : plotFiles) {
//                String pieces[] = plot.getName().split("_");
//                long start = Long.parseUnsignedLong(pieces[1]);
//                long nonces = Long.parseUnsignedLong(pieces[2]);
//                startNonce = Math.max(startNonce, start + nonces);
//        }
//        logger.info("Start nonce is: " + startNonce);
//        newPlotFiles.clear();
//        for (int i = 0; i < pathList.size(); i++) {
//            File path = pathList.get(i);
//            if (path == null) {
//                continue;
//            }
//
//            long freeSpace = path.getUsableSpace();
//
//            //long diskSpaceToUseWithPlots = freeSpace / 100 * fractionToPlotSliders.get(i).getValue();
//
//            long noncesToAdd = plotFileSize / BYTES_OF_A_NONCE;
//            if (noncesToAdd == 0) {
//                continue;
//            }
//
//            String newPlot = numericId + "_" + startNonce + "_" + noncesToAdd;
//            newPlotFiles.add(new File(path, newPlot));
//            logger.info("Added file to plot: " + newPlot);
//
//            startNonce += noncesToAdd + 1;
//        }
//
//        if (newPlotFiles.isEmpty() && resumePlotFiles.isEmpty()) {
//            plotting = false;
//            return;
//        }
//
//        String plotterName = "signum-plotter";
//        if (OSValidator.IS_WINDOWS) {
//            plotterName += ".exe";
//        } else if (OSValidator.IS_MAC) {
//            plotterName += ".app";
//        }
//
//        plotterUtilityFile = new File(TMP_DIR, plotterName);
//        if (!plotterUtilityFile.exists() || plotterUtilityFile.length() == 0) {
//            InputStream link = (getClass().getResourceAsStream("/plotter/" + plotterName));
//            try {
//                logger.info("Copying plotter to: " + plotterUtilityFile.getAbsolutePath());
//                Files.copy(link, plotterUtilityFile.getAbsoluteFile().toPath());
//                if (!OSValidator.IS_WINDOWS) {
//                    plotterUtilityFile.setExecutable(true);
//                }
//            } catch (IOException ex) {
//                ex.printStackTrace();
//                plotting = false;
//                return;
//            }
//        }
//
//        PlotThread plotThread = new PlotThread();
//        plotThread.start();
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void startPlottingOld() {
//        
    }

    /**
     * Are we plotting right now?
     *
     * @return true when a plotting operation is in progress, false otherwise
     */
    public boolean isPlotting() {
        return plotting;
    }

    class PlotThread extends Thread {

        @Override
        @SuppressWarnings({"CallToPrintStackTrace", "SleepWhileInLoop"})
        public void run() {
            long noncesFinished = 0;
            logger.info("Plotting started for a total of " + formatSpace(totalToPlot) + ", this can be a long process...");

            // Cache will use 45% of the free space, so we can have 2 (one moving and one plotting) and do not get a disk full
            long noncesCache = 0;
            if (ssdPath != null) {
                // clear possibly forgotten or killed plots on the cache folder
                File[] lostCacheFiles = ssdPath.listFiles(PLOT_FILE_FILTER);
                if (lostCacheFiles != null) {
                    for (File plot : lostCacheFiles) {
                        logger.info("Deleting plot on cache '" + plot.getName() + "'");
                        plot.delete();
                    }
                }

                noncesCache = ssdPath.getUsableSpace() * 45 / (100 * BYTES_OF_A_NONCE);
            }

            ArrayList<File> filesToPlot = new ArrayList<>();
            filesToPlot.addAll(resumePlotFiles);
            filesToPlot.addAll(newPlotFiles);

            for (File plot : filesToPlot) {
                if (resumePlotFiles.contains(plot)) {
                    logger.info("Resuming plot file '" + plot.getName() + "'");
                }
                String[] sections = plot.getName().split("_");

                long noncesInThisPlot = Long.parseLong(sections[2]);
                long nonceStart = Long.parseLong(sections[1]);

                long noncesAlreadyPlotted = 0;
                long noncesBeingPlot = noncesInThisPlot;

                File fileBeingPlot = plot;

                if (ssdPath != null && !resumePlotFiles.contains(plot)) {
                    noncesBeingPlot = Math.min(noncesCache, noncesInThisPlot);
                }

                while (noncesAlreadyPlotted < noncesInThisPlot) {
                    noncesBeingPlot = Math.min(noncesInThisPlot - noncesAlreadyPlotted, noncesBeingPlot);
                    if (ssdPath != null && !resumePlotFiles.contains(plot)) {
                        fileBeingPlot = new File(ssdPath, sections[0] + "_" + nonceStart + "_" + noncesBeingPlot);

                        long freeCacheSpaceNow = ssdPath.getUsableSpace() / BYTES_OF_A_NONCE;
                        while (freeCacheSpaceNow < noncesCache) {
                            logger.info("Waiting for enough space on your cache disk...");
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                plotting = false;
                            }
                            if (!plotting) {
                                logger.info("Stopped");
                                return;
                            }
                            freeCacheSpaceNow = ssdPath.getUsableSpace() / BYTES_OF_A_NONCE;
                        }
                    }

                    String cmd = plotterUtilityFile.getAbsolutePath() + " -i " + sections[0];
                    cmd += " -s " + nonceStart;
                    cmd += " -n " + noncesBeingPlot;
                    cmd += " -c " + cpusToPlot;
                    cmd += " -q";
                    cmd += " -d"; // FIXME: enable back direct io, but we need to find out the sector size then and adjust no of nonces
                    if (lowPriorityCheck) {
                        cmd += " -l";
                    }

                    logger.info("Plotting file '" + fileBeingPlot.getAbsolutePath() + "'");

                    try {
                        plotterProcess = Runtime.getRuntime().exec(cmd, null, fileBeingPlot.getParentFile());

                        long counter = 0;
                        while (plotterProcess.isAlive()) {
                            if (!plotting) {
                                logger.info("Stopped");
                                plotterProcess.destroyForcibly();
                                if (getPlotProgress(fileBeingPlot) < 0) {
                                    // delete the file, because we will not be able to resume it
                                    fileBeingPlot.delete();
                                }
                                break;
                            }
                            counter++;
                            Thread.sleep(100);
                            if (counter % 300 == 0) {
                                int partial = getPlotProgress(fileBeingPlot);
                                noncesPlotted.set(noncesFinished + partial);
                            }
                        }
                        // TODO: apparently for some systems this returns garbage, visit again later
//						if(plotting && plotterProcess.exitValue()!=0) {
//							addToConsole(PLOT_APP, "Error, plotter exit code: " + plotterProcess.exitValue());
//							plotting = false;
//							break;
//						}
                        if (!plotting) {
                            break;
                        }
                        nonceStart += noncesBeingPlot;
                        noncesAlreadyPlotted += noncesBeingPlot;
                        noncesFinished += noncesBeingPlot;

                        if (ssdPath != null) {
                            logger.info("Moving '" + fileBeingPlot.getName() + "' to '" + plot.getParent() + "'");
                            moveFile(fileBeingPlot.toPath(), new File(plot.getParent(), fileBeingPlot.getName()).toPath());
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (plotting) {
                logger.info("Plotting successfully finished! Be sure to stop and start the miner.");
            }
            if (ssdPath != null) {
                logger.info("But your system might still be moving files from cache");
            }

            plotting = false;
            plotterUtilityFile.delete();
            resumePlotFiles.clear();

            // Finished, so we reset all sliders
//            SwingUtilities.invokeLater(() -> {
//                for (int i = 0; i < fractionToPlotSliders.size(); i++) {
//                    fractionToPlotSliders.get(i).setValue(0);
//                }
//                //update();
//            });
        }
    ;

    };
	
	private void moveFile(Path source, Path target) {
        Thread copyThread = new Thread(() -> {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        });
        copyThread.start();
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private int getPlotProgress(File plot) {
        int progress = -1;
        try {
            RandomAccessFile raf = new RandomAccessFile(plot, "r");

            if (raf.length() > 8) {
                // Seek to the end of file
                raf.seek(raf.length() - 8);
                byte[] array = new byte[8];
                raf.read(array, 0, 8);

                // Check for the magic bytes at the end of the file
                if (array[4] == -81 && array[5] == -2 && array[6] == -81 && array[7] == -2) {
                    ByteBuffer buff = ByteBuffer.wrap(array, 0, 4);
                    buff.order(ByteOrder.LITTLE_ENDIAN);
                    progress = buff.getInt();
                }
            }
            raf.close();
        } catch (IOException e) {
            progress = -1;
            e.printStackTrace();
            logger.info(e.getMessage());
        }

        return progress;
    }

}
