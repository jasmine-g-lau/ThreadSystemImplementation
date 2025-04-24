package nachos.threads;

import nachos.threads.*;
import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
    private Lock lock;
    private Condition2 speakerWaiting;
    private Condition2 listenerWaiting;
    private boolean messageReady;
    private int message;
    private int numWaitingListeners;
    private int numWaitingSpeakers;

	/**
	 * Allocate a new communicator.
	 */
	public Communicator() { //default values
        lock = new Lock();
        speakerWaiting = new Condition2(lock);
        listenerWaiting = new Condition2(lock);
        messageReady = false;
        numWaitingListeners = 0;
        numWaitingSpeakers = 0;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
        lock.acquire();                                     // Current speaker acquires lock so that no other thread can access resources
        System.out.println("speaker acquired lock");
        numWaitingSpeakers++;                               // Adds new current speaker to wait to speak

        while (messageReady || numWaitingListeners == 0) {  // Make speaker wait until there is a listener ready to consume the message 
            System.out.println("speaker waiting");
            speakerWaiting.sleep();                         // or until the previous listener is finished listening
        }

        // Transfer the message
        numWaitingSpeakers--;                               // There is no longer a speaker waiting
        message = word;                                     // Accepting the message
        messageReady = true;                                // Flagging that there is now a message to be listened to
        System.out.println("speaker spoke");

        // Wake up a listener to receive the message
        listenerWaiting.wake();
        while(messageReady){
            speakerWaiting.sleep();
        }
        System.out.println("listener woke");
        lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
        lock.acquire();
        System.out.println("Listener acquired lock");
        numWaitingListeners++;

        while (!messageReady) {
            System.out.println("listener waiting");
            speakerWaiting.wake();
            listenerWaiting.sleep();
        }
        
        numWaitingListeners--;
        int temp = message;
        messageReady = false;
        System.out.println("listener listened");

        speakerWaiting.wakeAll();
        System.out.println("speaker woke");
        lock.release();
        return temp;
	}
}