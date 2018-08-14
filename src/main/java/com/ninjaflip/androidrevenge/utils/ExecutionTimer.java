/**
 * 
 */
package com.ninjaflip.androidrevenge.utils;

/**
 * @author Utilisateur
 * 
 */
public final class ExecutionTimer {

	private long start;
	private long end;

	public ExecutionTimer() {
		reset();
	}

	public void start() {
		start = System.currentTimeMillis();
	}

	public void end() {
		end = System.currentTimeMillis();
	}

	public double durationInSeconds() {
		return (end - start)/1000.0;
	}

	public void reset() {
		start = 0;
		end = 0;
	}

	public static String getTimeString(double totalSecs) {
		int hours = (int)(totalSecs / 3600);
		int minutes = (int)((totalSecs % 3600) / 60);
		int seconds = (int)(totalSecs % 60);

		if (hours == 0 && minutes == 0)
			return String.format("%d seconds", seconds);
		else if (hours == 0)
			return String.format("%d minute %d seconds", minutes, seconds);
		else
			return String.format("%d hour %d minute %d seconds", hours, minutes, seconds);
	}

	public static void main(String s[]) {
		// simple example
		ExecutionTimer t = new ExecutionTimer();
		t.start();
		for (int i = 0; i < 80; i++) {
			System.out.print(".");
		}
		t.end();
		System.out.println("\n" + t.durationInSeconds() + " seconds");
	}
}
