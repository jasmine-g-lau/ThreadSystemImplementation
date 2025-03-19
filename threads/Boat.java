package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Machine;

public class Boat {

    static BoatGrader boatGrader;

    public static void selfTest() {
        BoatGrader grader = new BoatGrader();
        System.out.println("\n ***Testing Boats with only 2 children***");
        startJourney(1, 6, grader);
    }

    
    static int boatOccupancy = 0;
    static int adultsOnOahu = 0;
    static int childrenOnOahu = 0;

    static boolean boatOnOahu = true;
    static boolean allowAdult = false;
    static boolean journeyCompleted = false;

    static Alarm syncAlarm;

    static Lock oahuLock;
    static Lock boatLock;

    static Condition spawnCondition;
    static Condition journeyCondition;

    public static void startJourney(int adults, int children, BoatGrader grader) {
        boatGrader = grader;
        oahuLock = new Lock();
        boatLock = new Lock();
        syncAlarm = new Alarm();
        spawnCondition = new Condition(oahuLock);
        journeyCondition = new Condition(boatLock);

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

        while (!journeyCompleted) {
            KThread.yield();
        }
        System.out.println();
        System.out.println("All adults and children have successfully crossed to Molokai!");
        System.out.println("Final State:");
        System.out.println("Adults on Oahu: " + adultsOnOahu);       // Output final counts
        System.out.println("Children on Oahu: " + childrenOnOahu);     // Output final counts
    }

    static void adultJourney() {
        boolean atOahu = true;
        oahuLock.acquire();
        adultsOnOahu++;
        spawnCondition.sleep();
        oahuLock.release();

        syncAlarm.waitUntil(100);
        KThread.yield();

        while (!journeyCompleted) {
            boatLock.acquire();
            if (canAdultRowToMolokai(atOahu)) {
                boatGrader.AdultRowToMolokai();
                oahuLock.acquire();
                adultsOnOahu--;
                oahuLock.release();

                if ((adultsOnOahu + childrenOnOahu) == 0) {
                    journeyCompleted = true;
                    journeyCondition.wakeAll();
                }

                atOahu = false;
                boatOnOahu = false;
                allowAdult = false;
            }
            if (!journeyCompleted && !atOahu) {
                journeyCondition.sleep();
            }
            boatLock.release();
        }
    }

    static void childJourney() {
        boolean atOahu = true;
        oahuLock.acquire();
        childrenOnOahu++;
        oahuLock.release();

        syncAlarm.waitUntil(100);

        while (!journeyCompleted) {
            boatLock.acquire();
            if (canChildRowToMolokai(atOahu)) {
                boardChildOntoBoat();
                atOahu = false;

                if ((adultsOnOahu + childrenOnOahu) == 0) {
                    journeyCompleted = true;
                    boatGrader.ChildRowToMolokai();
                    if (boatOccupancy == 2) {
                        boatGrader.ChildRideToMolokai();
                    }
                    resetBoatAtMolokai();
                } else if (boatOccupancy == 2) {
                    boatGrader.ChildRowToMolokai();
                    boatGrader.ChildRideToMolokai();
                    resetBoatAtMolokai();
                } else if (childrenOnOahu == 0) {
                    unboardChildFromBoat();
                }
            } else if (canChildRowToOahu(atOahu)) {
                boatGrader.ChildRowToOahu();
                atOahu = true;

                oahuLock.acquire();
                spawnCondition.wakeAll();
                childrenOnOahu++;
                oahuLock.release();

                boatOnOahu = true;
                allowAdult = true;
            }
            boatLock.release();
            if (!journeyCompleted) {
                KThread.yield();
            }
        }
    }

    private static boolean canAdultRowToMolokai(boolean atOahu) {
        return boatOnOahu && atOahu && boatOccupancy == 0 && (allowAdult || childrenOnOahu == 0) && !journeyCompleted && childrenOnOahu < 2;
    }

    private static boolean canChildRowToMolokai(boolean atOahu) {
        return boatOnOahu && atOahu && boatOccupancy < 2 && !journeyCompleted && (allowAdult || childrenOnOahu > 0);
    }

    private static boolean canChildRowToOahu(boolean atOahu) {
        return !boatOnOahu && !atOahu && boatOccupancy < 2 && !journeyCompleted;
    }

    private static void boardChildOntoBoat() {
        boatOccupancy++;
        oahuLock.acquire();
        childrenOnOahu--;
        oahuLock.release();
    }

    private static void unboardChildFromBoat() {
        boatOccupancy--;
        oahuLock.acquire();
        childrenOnOahu++;
        oahuLock.release();
    }

    private static void resetBoatAtMolokai() {
        boatOccupancy = 0;
        boatOnOahu = false;
        journeyCondition.wakeAll();
    }
}
