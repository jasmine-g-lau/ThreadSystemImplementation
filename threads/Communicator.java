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

    public static void testMultipleSpeakersBeforeListeners1() {
        System.out.println("TESTING COMMUNICATOR");
        Communicator communicator = new Communicator();
    
        // Creating six speakers with unique messages
        KThread speaker1 = new KThread(() -> communicator.speak(1));
        KThread speaker2 = new KThread(() -> communicator.speak(2));
        KThread speaker3 = new KThread(() -> communicator.speak(3));
        KThread speaker4 = new KThread(() -> communicator.speak(4));
        KThread speaker5 = new KThread(() -> communicator.speak(5));
        KThread speaker6 = new KThread(() -> communicator.speak(6));
    
        // Creating six listeners
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
    
        KThread listener4 = new KThread(() -> {
            int received = communicator.listen();
            System.out.println("Listener 4 received: " + received);
        });
    
        KThread listener5 = new KThread(() -> {
            int received = communicator.listen();
            System.out.println("Listener 5 received: " + received);
        });
    
        KThread listener6 = new KThread(() -> {
            int received = communicator.listen();
            System.out.println("Listener 6 received: " + received);
        });
    
        // Start all speakers
        speaker1.fork();
        System.out.println("speaker1 started");
        speaker2.fork();
        System.out.println("speaker2 started");
        speaker3.fork();
        System.out.println("speaker3 started");
        speaker4.fork();
        System.out.println("speaker4 started");
        speaker5.fork();
        System.out.println("speaker5 started");
        speaker6.fork();
        System.out.println("speaker6 started");
    
        // Delay to simulate speakers sending messages before listeners activate
        ThreadedKernel.alarm.waitUntil(2000);
    
        // Start all listeners
        listener1.fork();
        System.out.println("listener1 started");
        listener2.fork();
        System.out.println("listener2 started");
        listener3.fork();
        System.out.println("listener3 started");
        listener4.fork();
        System.out.println("listener4 started");
        listener5.fork();
        System.out.println("listener5 started");
        listener6.fork();
        System.out.println("listener6 started");
    
        // Join all threads to ensure test completion
        speaker1.join();
        speaker2.join();
        speaker3.join();
        speaker4.join();
        speaker5.join();
        speaker6.join();
        listener1.join();
        listener2.join();
        listener3.join();
        listener4.join();
        listener5.join();
        listener6.join();
    }
    
    public static void testMultipleSpeakersBeforeListeners() {
        System.out.println("TESTING COMMUNICATOR");
        Communicator communicator = new Communicator();
    
        KThread speaker1 = new KThread(() -> communicator.speak(1));
        KThread speaker2 = new KThread(() -> communicator.speak(2));
        KThread speaker3 = new KThread(() -> communicator.speak(3));
    
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

// class CommSelfTester {

// 	/**
// 	 * Test with 1 listener then 1 speaker.
// 	 */
// 	public static void selfTest1() {
//         System.out.println("SelfTest1");
// 		KThread listener1 = new KThread(listenRun);
// 		listener1.setName("listener1");
// 		listener1.fork();

// 		KThread speaker1 = new KThread(speakerRun);
// 		speaker1.setName("speaker1");
// 		speaker1.fork();

// 	} // selfTest1()

// 	/**
// 	 * Test with 1 speaker then 1 listener.
// 	 */
// 	public static void selfTest2() {
//         System.out.println("SelfTest2");
// 		KThread speaker1 = new KThread(speakerRun);
// 		speaker1.setName("speaker1");
// 		speaker1.fork();

// 		KThread listener1 = new KThread(listenRun);
// 		listener1.setName("listener1");
// 		listener1.fork();

// 	} // selfTest2()

// 	/**
// 	 * Test with 2 speakers and 2 listeners intermixed.
// 	 */
// 	public static void selfTest3() {
//         System.out.println("SelfTest3");
// 		KThread speaker1 = new KThread(speakerRun);
// 		speaker1.setName("speaker1");
// 		speaker1.fork();

// 		KThread listener1 = new KThread(listenRun);
// 		listener1.setName("listener1");
// 		listener1.fork();

// 		KThread speaker2 = new KThread(speakerRun);
// 		speaker2.setName("speaker2");
// 		speaker2.fork();

// 		KThread listener2 = new KThread(listenRun);
// 		listener2.setName("listener2");
// 		listener2.fork();

// 	} // selfTest3()

// 	/**
// 	 * Second test with 2 speakers and 2 listeners intermixed.
// 	 */
// 	public static void selfTest4() {
//         System.out.println("SelfTest4");
// 		KThread speaker1 = new KThread(speakerRun);
// 		speaker1.setName("speaker1");
// 		speaker1.fork();

// 		KThread speaker2 = new KThread(speakerRun);
// 		speaker2.setName("speaker2");
// 		speaker2.fork();

// 		KThread listener1 = new KThread(listenRun);
// 		listener1.setName("listener1");
// 		listener1.fork();

// 		KThread listener2 = new KThread(listenRun);
// 		listener2.setName("listener2");
// 		listener2.fork();

// 	} // selfTest4()

// 	/**
// 	 * Stress test with 100 speakers and 100 listeners intermixed.
// 	 */
// 	public static void selfTest5() {
//         System.out.println("SelfTest5");
// 		for (int i = 0; i < 100; i++) {
// 			new KThread(speakerRun).setName("Speaker " + Integer.toString(i)).fork();
// 			new KThread(listenRun).setName("Listen " + Integer.toString(i)).fork();
// 		}

// 	} // selfTest5()

// 	/**
// 	 * Function to run inside Runnable object listenRun. Uses the function listen on
// 	 * static object myComm inside this class, allowing the threads inside the
// 	 * respective selfTests above to call the runnable variables below and test
// 	 * functionality for listen. Needs to run with debug flags enabled. See NACHOS
// 	 * README for info on how to run in debug mode.
// 	 */
// 	static void listenFunction() {
// 		Lib.debug(dbgThread, "Thread " + KThread.currentThread().getName() + " is about to listen");
// 		Lib.debug(dbgThread, "Thread " + KThread.currentThread().getName() + " got value " + myComm.listen());

// 	} // listenFunction()

// 	/**
// 	 * Function to run inside Runnable object speakerRun. Uses the function listen
// 	 * on static object myComm inside this class, allowing the threads inside the
// 	 * respective selfTests above to call the runnable variables below and test
// 	 * functionality for speak. Needs to run with debug flags enabled. See NACHOS
// 	 * README for info on how to run in debug mode.
// 	 */
// 	static void speakFunction() {
// 		Lib.debug(dbgThread, "Thread " + KThread.currentThread().getName() + " is about to speak");
// 		myComm.speak(myWordCount++);
// 		Lib.debug(dbgThread, "Thread " + KThread.currentThread().getName() + " has spoken");
// 	} // speakFunction()

// 	/**
// 	 * Wraps listenFunction inside a Runnable object so threads can be generated for
// 	 * testing.
// 	 */
// 	private static Runnable listenRun = new Runnable() {
// 		public void run() {
// 			listenFunction();
// 		}
// 	}; // runnable listenRun

// 	/**
// 	 * Wraps speakFunction inside a Runnable object so threads can be generated for
// 	 * testing.
// 	 */
// 	private static Runnable speakerRun = new Runnable() {
// 		public void run() {
// 			speakFunction();
// 		}
// 	}; // Runnable speakerRun

// 	// dbgThread = 't' variable needed for debug output
// 	private static final char dbgThread = 't';
// 	// myComm is a shared object that tests Communicator functionality
// 	private static Communicator myComm = new Communicator();
// 	// myWordCount is used for selfTest5 when spawning listening/speaking threads
// 	private static int myWordCount = 0;

// } // CommSelfTester class