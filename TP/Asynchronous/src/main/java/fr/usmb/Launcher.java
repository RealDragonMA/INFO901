package fr.usmb;

import fr.usmb.process.Communicator;
import fr.usmb.process.Process;

import java.util.ArrayList;

public class Launcher {

    private static final int runningTime = 5000;

    public static void main(String[] args) {

        ArrayList<Process> processes = new ArrayList<Process>();

        for (int i = 0; i < Communicator.maxNbProcess; i++) {
            processes.add(new Process("P" + i));
        }

        // Get random process
        processes.get(processes.size() - 1).getCommunicator().initToken();

        try {
            Thread.sleep(runningTime);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < Communicator.maxNbProcess; i++) {
            processes.get(i).stop();
        }
    }
}
