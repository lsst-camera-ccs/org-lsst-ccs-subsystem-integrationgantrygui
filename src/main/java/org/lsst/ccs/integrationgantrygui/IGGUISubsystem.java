package org.lsst.ccs.integrationgantrygui;

import java.io.IOException;
import java.time.Duration;
import java.util.logging.Logger;
import org.lsst.ccs.Subsystem;
import org.lsst.ccs.bus.data.AgentInfo;
import org.lsst.ccs.bus.data.KeyValueData;
import org.lsst.ccs.commons.annotations.LookupField;
import org.lsst.ccs.framework.AgentPeriodicTask;
import org.lsst.ccs.framework.HasLifecycle;
import org.lsst.ccs.services.AgentPeriodicTaskService;

/**
 * A main "subsystem" for running integration gantry GUI and sending trending
 * into to the database. Note that currently this couples the GUI and subsystem
 * which means that trending is only generated while the GUI is running. This
 * should probably be fixed.
 *
 * @author tonyj
 */
public class IGGUISubsystem extends Subsystem implements HasLifecycle {

    private static final Logger LOG = Logger.getLogger(IGGUISubsystem.class.getName());

    @LookupField(strategy = LookupField.Strategy.TOP)
    private Subsystem subsys;

    @LookupField(strategy = LookupField.Strategy.TREE)
    private AgentPeriodicTaskService pts;

    private final Main main = new Main();
    private final long[] lastUpdateTime = new long[4];

    public IGGUISubsystem() {
        super("ig-gui", AgentInfo.AgentType.WORKER);
    }

    @Override
    public void postStart() {
        try {
            // Start the main routine without the GUI
            main.start(false);
        } catch (IOException ex) {
            throw new RuntimeException("Error calling start", ex);
        }
    }

    @Override
    public void build() {
        pts.scheduleAgentPeriodicTask(new AgentPeriodicTask("publish-trending", () -> {
            for (int i = 0; i < Main.NCAMERAS; i++) {
                KeyValueData data = main.getTrendingForCamera(i);
                if (data != null && data.getCCSTimeStamp().getUTCInstant().toEpochMilli() > lastUpdateTime[i]) {
                    subsys.publishSubsystemDataOnStatusBus(data);
                    lastUpdateTime[i] = data.getCCSTimeStamp().getUTCInstant().toEpochMilli();
                }
            }

        }).withPeriod(Duration.ofSeconds(15)));

    }

}
