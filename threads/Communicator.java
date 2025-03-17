package nachos.threads;

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
    private Condition speakerWaiting;
    private Condition listenerWaiting;
    private boolean messageReady;
    private int message;
    private int waitingListeners;
    private int waitingSpeakers;

	/**
	 * Allocate a new communicator.
	 */
	public Communicator() { //default values
        lock = new Lock();
        speakerWaiting = new Condition(lock);
        listenerWaiting = new Condition(lock);
        messageReady = false;
        waitingListeners = 0;
        waitingSpeakers = 0;
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
        lock.acquire();                                 // Current speaker acquires lock so that no other thread can access resources
        waitingSpeakers++;                              // Adds new current speaker to wait to speak

        while (messageReady || waitingListeners == 0) { // Make speaker wait until there is a listener ready to consume the message 
            speakerWaiting.sleep();                     // or until the previous listener is finished listening
        }

        // Transfer the message
        message = word;                                 // Accepting the message
        messageReady = true;                            // Flagging that there is now a message to be listened to
        waitingSpeakers--;                              // There is no longer a speaker waiting

        // Wake up a listener to receive the message
        listenerWaiting.wake();

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
        waitingListeners++;

        while (!messageReady) {
            speakerWaiting.wake();
            listenerWaiting.sleep();
        }
        int temp = message;
        messageReady = false;
        waitingListeners--;

        speakerWaiting.wake();

        lock.release();
        return temp;
	}

    public static void testMultipleSpeakersBeforeListeners() {
        System.out.println("TESTING COMMUNICATOR");
        Communicator communicator = new Communicator();
    
        KThread speaker1 = new KThread(() -> communicator.speak(1));
        System.out.println("speaker1 spoke");
        KThread speaker2 = new KThread(() -> communicator.speak(2));
        System.out.println("speaker2 spoke");
        KThread speaker3 = new KThread(() -> communicator.speak(3));
        System.out.println("speaker3 spoke");
    
        KThread listener1 = new KThread(() -> {
            int received = communicator.listen();
            System.out.println("Listener 1 received: " + received);
        });
    
        KThread listener2 = new KThread(() -> {
            int received = communicator.listen();
            System.out.println("Listener 2 received: " + received);
        });
    
        KThread listener3 = new KThread(() -> {
            int received = communicator.listen();
            System.out.println("Listener 3 received: " + received);
        });
    
        speaker1.fork();
        speaker2.fork();
        speaker3.fork();
    
        ThreadedKernel.alarm.waitUntil(2000); // Simulate delay for listeners
    
        listener1.fork();
        listener2.fork();
        listener3.fork();
    
        speaker1.join();
        speaker2.join();
        speaker3.join();
        listener1.join();
        listener2.join();
        listener3.join();
    }
}