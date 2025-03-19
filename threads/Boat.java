package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Machine;

public class Boat {

    static BoatGrader boatGrader;

    public static void selfTest() {
        BoatGrader grader = new BoatGrader();

        System.out.println("\n ***Testing Boats with only 2 children***");
        begin(1, 100, grader);


    }

    static int boatOccupancy = 0;
    static int adultsOnOahu = 0;
    static int childrenOnOahu = 0;

    static boolean boatOnOahu = true;
    static boolean journeyCompleted = false;

    static Lock boatLock;

    static Condition2 childrenPassenger;
    static Condition2 childrenWaitingOnBoat;
    static Condition2 childrenWaitingOnMolokai;
    static Condition2 adultWaitingOnOahu;
    static Condition2 mainThreadCondition;


    enum Location {
        OAHU,
        MOLOKAI
    }

    static Location boatLocation = Location.OAHU;


    public static void begin(int adults, int children, BoatGrader grader) {
        boatGrader = grader;
        boatLock = new Lock();
        childrenPassenger = new Condition2(boatLock);
        childrenWaitingOnBoat = new Condition2(boatLock);
        childrenWaitingOnMolokai = new Condition2(boatLock);
        adultWaitingOnOahu = new Condition2(boatLock);
        mainThreadCondition = new Condition2(boatLock);

        adultsOnOahu = adults;
        childrenOnOahu = children;

        Runnable adultRunnable = new Runnable() {
            public void run() {
                adultJourney();
            }
        };

        Runnable childRunnable = new Runnable() {
            public void run() {
                childJourney();
            }
        };

        boolean interruptStatus = Machine.interrupt().disable();

        for (int i = 0; i < adults; ++i) {
            KThread t = new KThread(adultRunnable);
            t.setName("Adult thread " + (i + 1));
            t.fork();
        }

        for (int i = 0; i < children; ++i) {
            KThread t = new KThread(childRunnable);
            t.setName("Child thread " + (i + 1));
            t.fork();
        }

        Machine.interrupt().restore(interruptStatus);

        boatLock.acquire();
        while (!journeyCompleted) {
            mainThreadCondition.sleep();
        }
        boatLock.release();

        System.out.println();
        System.out.println("All adults and children have successfully crossed to Molokai!");
        System.out.println("Final State:");
        System.out.println("Adults on Oahu: " + adultsOnOahu);
        System.out.println("Children on Oahu: " + childrenOnOahu);
    }

    static void adultJourney() {
        Location island = Location.OAHU;
        boatLock.acquire();
        while (!journeyCompleted) {

            while (!boatOnOahu || childrenOnOahu >= 2 || boatOccupancy > 0) {
                childrenWaitingOnMolokai.wakeAll();
                adultWaitingOnOahu.sleep();
            }

            boatGrader.AdultRowToMolokai();
            island = Location.MOLOKAI;
            boatOnOahu = false;
            adultsOnOahu--;

            if (adultsOnOahu + childrenOnOahu == 0) {
                journeyCompleted = true;
                mainThreadCondition.wake();
            }

            childrenWaitingOnMolokai.wake();
            boatLock.release();
            return;

        }
        boatLock.release();

    }


    static void childJourney() {
        Location island = Location.OAHU;

        while (!journeyCompleted) {
            boatLock.acquire();
            if (boatOnOahu) {
                if (island == Location.MOLOKAI) {
                    childrenWaitingOnMolokai.sleep();
                } else {
                    if (boatOccupancy == 0) {
                        childrenWaitingOnBoat.wakeAll();

                        boatOccupancy++;
                        childrenOnOahu--;


                        int lastSeenChildren = childrenOnOahu;
                        int lastSeenAdults = adultsOnOahu;

                        childrenPassenger.sleep();

                        boatGrader.ChildRideToMolokai();
                        island = Location.MOLOKAI;

                        if (lastSeenAdults == 0 && lastSeenChildren - 1 == 0) {
                            journeyCompleted = true;
                            mainThreadCondition.wake();
                        }

                        boatOnOahu = false;
                        boatOccupancy = 0;

                        childrenWaitingOnMolokai.wake();
                        childrenWaitingOnMolokai.sleep();

                    } else if (boatOccupancy == 1) {
                        boatOccupancy++;
                        childrenOnOahu--;

                        boatGrader.ChildRowToMolokai();
                        island = Location.MOLOKAI;

                        childrenPassenger.wakeAll();
                        childrenWaitingOnMolokai.sleep();
                    } else {
                        childrenWaitingOnBoat.sleep();
                    }
                }

            } else {
                if (island == Location.OAHU) {
                    childrenWaitingOnBoat.sleep();
                } else {
                    if (boatOccupancy == 0) {
                        boatGrader.ChildRowToOahu();
                        island = Location.OAHU;
                        boatOnOahu = true;
                        childrenOnOahu++;

                        childrenWaitingOnBoat.wakeAll();
                        adultWaitingOnOahu.wakeAll();

                        childrenWaitingOnBoat.sleep();
                    } else {
                        childrenWaitingOnMolokai.sleep();
                    }
                }
            }
            boatLock.release();
        }
    }

}
