package nachos.threads;

import nachos.machine.*;
import java.util.PriorityQueue;
import nachos.threads.KThread;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	PriorityQueue<waitingThread> waitQueue = new PriorityQueue<waitingThread>();

	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
				
			}
		});
	}

	private static class waitingThread implements Comparable<waitingThread>{
		long wakeUpTime;
		KThread sleeper;

		waitingThread(long wakeUpTime, KThread sleeper){
			this.wakeUpTime = wakeUpTime;
			this.sleeper = sleeper;
		}

		public int compareTo(waitingThread other){
			return Long.compare(this.wakeUpTime, other.wakeUpTime);
		}
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		boolean intStatus = Machine.interrupt().disable();
		long currentTime = Machine.timer().getTime();
		
		while (!waitQueue.isEmpty() && waitQueue.peek().wakeUpTime <= currentTime) {
			waitingThread sleeper = waitQueue.poll();
			KThread thread = sleeper.sleeper;
			thread.ready();
		}
		
		KThread.currentThread().yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		if (x <= 0){
			return;
		}
		long wakeUpTime = Machine.timer().getTime() + x;
		KThread currentThread = KThread.currentThread();
		boolean intStatus = Machine.interrupt().disable();

		waitingThread sleeper = new waitingThread(wakeUpTime, currentThread);

		waitQueue.add(sleeper);
		currentThread.sleep();

		Machine.interrupt().restore(intStatus);
	}

	// Add Alarm testing code to the Alarm class
	public static void alarmTest1() {
			int durations[] = {1000, 10*1000, 100*1000};
			long t0, t1;
			for (int d : durations) {
				t0 = Machine.timer().getTime();
				ThreadedKernel.alarm.waitUntil (d);
				t1 = Machine.timer().getTime();
				System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
			}
		}
		// Implement more test methods here ...
		// Invoke Alarm.selfTest() from ThreadedKernel.selfTest()
		public static void selfTest() {
			alarmTest1();
		// Invoke your other test methods here ...
		}
	
}