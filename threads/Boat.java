package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Machine;

public class Boat {
    static BoatGrader bg;

    public static void selfTest() {
        BoatGrader b = new BoatGrader();

        // System.out.println("\n ***Testing Boats with only 2 children***");
        // begin(0, 2, b);

        // System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
        // begin(1, 2, b);

        // System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
        // begin(3, 3, b);

        System.out.println("\n ***Testing Boats with 3 children, 100 adults***");
        begin(3, 99, b);
    }

    private static Lock lockBoat;
    private static Lock lockPopulation;

    private static Condition2 boatControl; //Not using Condition

    private static int adultsOnOahu;
    private static int childrenOnOahu;
    private static int boatCap;


    private static boolean boatOnOahu;
    private static boolean done;

    public static void begin(int adults, int children, BoatGrader b) {
        bg = b;

        lockPopulation = new Lock();
        lockBoat = new Lock();
        boatControl = new Condition2(lockBoat);
        adultsOnOahu = 0;
        childrenOnOahu = 0;
        boatOnOahu = true;
        done = false;
        boatCap = 0;

        Runnable AdultThread = new Runnable() {
            public void run() {
                AdultItinerary();
            }
        };
        Runnable ChildThread = new Runnable() {
            public void run() {
                ChildItinerary();
            }
        };

        boolean intStatus = Machine.interrupt().disable();

        for (int i = 0; i < adults; ++i) {
            KThread thread = new KThread(AdultThread);
            thread.setName("Adult thread " + (i + 1));
            thread.fork();
        }

        for (int i = 0; i < children; ++i) {
            KThread thread = new KThread(ChildThread);
            thread.setName("Child thread " + (i + 1));
            thread.fork();
        }

        Machine.interrupt().restore(intStatus);

        while (!done) {
            KThread.yield();
        }

        System.out.println("All adults and children have successfully crossed to Molokai!");
        System.out.println("Final State:");
        System.out.println("Adults on Oahu: " + adultsOnOahu);       // Output final counts
        System.out.println("Children on Oahu: " + childrenOnOahu);     // Output final counts
    }



    private static void AdultItinerary() {

        lockPopulation.acquire();
        adultsOnOahu++;
        lockPopulation.release();


        while (!done) {
            lockBoat.acquire();

            if (boatOnOahu && boatCap == 0 && childrenOnOahu < 2 && !done && adultsOnOahu != 0) {
                bg.AdultRowToMolokai();

                lockPopulation.acquire();
                adultsOnOahu--;
                lockPopulation.release();

                boatOnOahu = false;

                if (adultsOnOahu == 0 && childrenOnOahu == 0) {
                    done = true;
                    boatControl.wakeAll();
                }
            } else if (!done) {
                boatControl.sleep();
            }

            lockBoat.release();
        }

    }

    private static void ChildItinerary() {
        lockPopulation.acquire();
        childrenOnOahu++;
        lockPopulation.release();

        while (!done) {
            lockBoat.acquire();

            if (boatOnOahu && boatCap < 2 && childrenOnOahu > 0 && !done) {
                boatCap++;

                lockPopulation.acquire();
                childrenOnOahu--;
                lockPopulation.release();

                if ((adultsOnOahu == 0 && childrenOnOahu == 0) || boatCap == 2) {
                    bg.ChildRowToMolokai();
                    if (boatCap == 2) {
                        bg.ChildRideToMolokai();
                    }
                    boatCap = 0;
                    boatOnOahu = false;
                    if (adultsOnOahu == 0 && childrenOnOahu == 0) {
                        done = true;
                    }
                    boatControl.wakeAll();
                } else if (childrenOnOahu > 0 && boatCap == 1 && !boatOnOahu ) {
                    bg.ChildRowToOahu();
                    boatOnOahu = true;
					boatCap = 0;
                    lockPopulation.acquire();
                    childrenOnOahu++;
                    lockPopulation.release();
                }

            } else if (!boatOnOahu && childrenOnOahu > 0 && boatCap == 0 && !done){
                bg.ChildRowToOahu();
                boatOnOahu = true;
                lockPopulation.acquire();
                childrenOnOahu++;
                lockPopulation.release();
            }


            lockBoat.release();
            if (!done) {
                KThread.yield();
            }
        }
    }


    static void SampleItinerary() {
        System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
        bg.AdultRowToMolokai();
        bg.ChildRideToMolokai();
        bg.AdultRideToMolokai();
        bg.ChildRideToMolokai();
    }
}
