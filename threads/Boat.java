package nachos.threads;

import nachos.ag.BoatGrader;

public class Boat {
    static BoatGrader bg;

    // new implementation
    private static int adultsOahu;
    private static int childrenOahu;
    private static int adultsMolokai = 0;
    private static int childrenMolokai = 0;
    private static boolean boatOnOahu = true;
    private static final Object lock = new Object();
    // end

    public static void selfTest() {
        BoatGrader b = new BoatGrader();

        System.out.println("\n ***Testing Boats with only 2 children***");
        begin(0, 2, b);

        // System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
        // begin(1, 2, b);

        // System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
        // begin(3, 3, b);
    }

    public static void begin(int adults, int children, BoatGrader b) {
        // Store the externally generated autograder in a class
        // variable to be accessible by children.
        bg = b;

        // Instantiate global variables here

        // Create threads here. See section 3.4 of the Nachos for Java
        // Walkthrough linked from the projects page.

        // New implementation
        adultsOahu = adults;
        childrenOahu = children;

        for (int i = 0; i < adults; i++) {
            KThread adultThread = new KThread(new Runnable() {
                public void run() {
                    Boat.AdultItinerary();
                }
            });

            adultThread.setName("Adult Thread " + i);
            adultThread.fork();
        }

        for (int i = 0; i < children; i++) {
            KThread childThread = new KThread(new Runnable() {
                public void run() {
                    Boat.ChildItinerary();
                }
            });

            childThread.setName("Child Thread " + i);
            childThread.fork();
        }
        // end

        Runnable r = new Runnable() {
            public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();

    }

    static void AdultItinerary() {
        /*
         * This is where you should put your solutions. Make calls to the
         * BoatGrader to show that it is synchronized. For example:
         * bg.AdultRowToMolokai();
         * indicates that an adult has rowed the boat across to Molokai
         */

        // new implementation

        while (true) {
            synchronized (lock) {
                while (!boatOnOahu || childrenOahu > 0) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                boatOnOahu = false;
                adultsOahu--;
                adultsMolokai++;
                bg.AdultRowToMolokai();
                lock.notifyAll();
            }

            synchronized (lock) {
                while (boatOnOahu) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                boatOnOahu = true;
                adultsMolokai--;
                adultsOahu++;
                bg.AdultRowToOahu();
                lock.notifyAll();
            }
        }

        // end

    }

    static void ChildItinerary() {

        // new implementation
        while (true) {
            synchronized (lock) {
                if (boatOnOahu) {
                    if (childrenOahu >= 2) {
                        childrenOahu -= 2;
                        childrenMolokai += 2;
                        boatOnOahu = false;
                        bg.ChildRowToMolokai();
                        bg.ChildRideToMolokai();
                        lock.notifyAll();
                    } else if (childrenOahu >= 1) {
                        if (adultsOahu == 0 && childrenOahu == 1 && adultsMolokai == 0) {
                            childrenOahu--;
                            childrenMolokai++;
                            boatOnOahu = false;
                            bg.ChildRowToMolokai();
                            lock.notifyAll();
                        } else {
                            childrenOahu--;
                            childrenMolokai++;
                            boatOnOahu = false;
                            bg.ChildRowToMolokai();
                            lock.notifyAll();
                        }
                    }
                } else {
                    childrenMolokai--;
                    childrenOahu++;
                    boatOnOahu = true;
                    bg.ChildRowToOahu();
                    lock.notifyAll();
                }

                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        // end
    }

    static void SampleItinerary() {
        // Please note that this isn't a valid solution (you can't fit
        // all of them on the boat). Please also note that you may not
        // have a single thread calculate a solution and then just play
        // it back at the autograder -- you will be caught.
        System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
        bg.AdultRowToMolokai();
        bg.ChildRideToMolokai();
        bg.AdultRideToMolokai();
        bg.ChildRideToMolokai();
    }

}
