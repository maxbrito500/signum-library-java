/*
 * Description: Provides optimal usage calculations for available CPU
 * License: Apache-2.0
 */
package utils;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

/**
 * Date: 2021-09-21 Place: Zwingenberg, Germany
 *
 * @author Max Brito
 */
public class CPUcalculator {

    public static int getNumberOfCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static double getSystemLoadCPU() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                OperatingSystemMXBean.class);
        // have to repeat twice
        osBean.getSystemCpuLoad();
        // What % load the overall system is at, from 0.0-1.0
        return osBean.getSystemCpuLoad();
    }

}
