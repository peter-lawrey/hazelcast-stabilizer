package com.hazelcast.stabilizer.coordinator;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.stabilizer.tests.Failure;

import java.io.File;
import java.util.List;

import static com.hazelcast.stabilizer.Utils.appendText;
import static com.hazelcast.stabilizer.Utils.sleepSeconds;

class FailureMonitorThread extends Thread {
    private final Coordinator coordinator;
    private final ILogger log = Logger.getLogger(FailureMonitorThread.class);
    private final File file;

    public FailureMonitorThread(Coordinator coordinator) {
        super("FailureMonitorThread");

        file = new File("failures-" + coordinator.testSuite.id + ".txt");

        if (coordinator == null) {
            throw new NullPointerException();
        }
        this.coordinator = coordinator;
        this.setDaemon(true);
    }

    public void run() {
        for (; ; ) {
            try {
                //todo: this delay should be configurable.
                sleepSeconds(1);
                scan();
            } catch (Throwable e) {
                log.severe(e);
            }
        }
    }

    private void scan() {
        List<Failure> failures = coordinator.agentsClient.getFailures();
        for (Failure failure : failures) {
            coordinator.failureList.add(failure);
            log.warning(buildMessage(failure));
            appendText(failure.toString() + "\n", file);
        }
    }

    private String buildMessage(Failure failure) {
        StringBuilder sb = new StringBuilder();
        sb.append("Failure #").append(coordinator.failureList.size()).append(" ");
        if (failure.workerAddress != null) {
            sb.append(' ');
            sb.append(failure.workerAddress);
            sb.append(' ');
        } else if (failure.agentAddress != null) {
            sb.append(' ');
            sb.append(failure.agentAddress);
            sb.append(' ');
        }
        sb.append(failure.type);

        if (failure.cause != null) {
            String[] lines = failure.cause.split("\n");
            if (lines.length > 0) {
                sb.append("[");
                sb.append(lines[0]);
                sb.append("]");
            }
        }

        return sb.toString();
    }
}
