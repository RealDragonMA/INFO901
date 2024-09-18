package fr.usmb;

import fr.usmb.process.Process;

import java.util.ArrayList;

public class Launcher{

	private static final int runningTime = 5000;

	public static void main(String[] args) throws InterruptedException {

		ArrayList<Process> processes = new ArrayList<Process>();

		for(int i=0; i<Process.maxNbProcess; i++) {
			processes.add(new Process("P"+i));
		}

		// Get random process
		Process randomProcess = processes.get((int)(Math.random() * Process.maxNbProcess));


		try{
			Thread.sleep(runningTime);
		}catch(Exception e){
			e.printStackTrace();
		}

		for(int i=0; i<Process.maxNbProcess; i++) {
			processes.get(i).stop();
		}
	}
}
