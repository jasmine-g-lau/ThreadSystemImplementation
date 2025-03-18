package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Machine;

public class Boat {
    static BoatGrader bg;

    public static void selfTest() {
        BoatGrader b = new BoatGrader();

        System.out.println("\n ***Testing Boats with only 2 children***");
        begin(0, 2, b);

        // System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
        // begin(1, 2, b);

        // System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
        // begin(3, 3, b);
    }


    private static Lock lockBoat; //new
    private static Lock lockPopulation; //new

    private static Condition populationControl; //new
    private static Condition boatControl; //new

    private static int adultsOnOahu; //new
    private static int childrenOnOahu; //new
    private static int boatCap; //new


    private static boolean boatOnOahu; //new
    private static boolean done; //new
    private static boolean temp; //new


    public static void begin(int adults, int children, BoatGrader b) {
        // Store the externally generated autograder in a class
        // variable to be accessible by children.
        bg = b;
        
        // Instantiate global variables here

        // Create threads here. See section 3.4 of the Nachos for Java
        // Walkthrough linked from the projects page.

        lockPopulation = new Lock();
		lockBoat = new Lock();
        populationControl = new Condition(lockPopulation);
        boatControl = new Condition(lockBoat);
        adultsOnOahu = adults;
        childrenOnOahu = children;
        boatOnOahu = true;

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
			KThread threadA = new KThread(AdultThread);
			threadA.fork();
		}

		for (int i = 0; i < children; ++i) {
			KThread threadC = new KThread(ChildThread);
			threadC.fork();
		}

        Machine.interrupt().restore(intStatus);

		while (!done){
			KThread.yield();
        }
    }



    private static void AdultItinerary() {
        /*
         * This is where you should put your solutions. Make calls to the
         * BoatGrader to show that it is synchronized. For example:
         * bg.AdultRowToMolokai();
         * indicates that an adult has rowed the boat across to Molokai
         */
        boatOnOahu = true;
        temp = true;

        lockPopulation.acquire();
        adultsOnOahu++;
        populationControl.sleep();
        lockPopulation.release();

        KThread.yield();


        while(!done){
            lockBoat.acquire();

            if (boatOnOahu && temp && boatCap == 0 && childrenOnOahu == 0 && !done){
                bg.AdultRowToMolokai();

                lockPopulation.acquire();
                adultsOnOahu--;
                lockPopulation.release();

                temp = false;
                boatOnOahu = false;

                if ((adultsOnOahu + childrenOnOahu) == 0){
                    done = true;
                    boatControl.wakeAll();
                }
            }

            else if (!temp && !done){
                boatControl.sleep();
            }

            lockBoat.release();
        }
        
    }





    private static void ChildItinerary() {
        temp = true;

        lockPopulation.acquire();
        childrenOnOahu++;
        lockPopulation.release();

        while(!done){
            lockBoat.acquire();

            if (temp && boatOnOahu && boatCap < 2 && !done){
                boatCap++;
                
                lockPopulation.acquire();
                childrenOnOahu--;
                lockPopulation.release();

                temp = false;

                if ((adultsOnOahu + childrenOnOahu) == 0) {
                    bg.ChildRowToMolokai();
                    if (boatCap == 2) {
                        bg.ChildRideToMolokai();
                    }
                    boatCap = 0;
                    boatOnOahu = false;
                    done = true;
                    boatControl.wakeAll();
                }

                else if (boatCap == 2) {
                    bg.ChildRowToMolokai();
                    bg.ChildRideToMolokai();
                    boatCap = 0;
                    boatOnOahu = false;
                }

                else if (childrenOnOahu == 0) {
                    boatCap--;
                    lockPopulation.acquire();
                    childrenOnOahu++;
                    lockPopulation.release();
                    temp = true;
                }

                else if (!boatOnOahu && !temp && boatCap < 2 && !done) {
                    bg.ChildRowToOahu();
                    temp = true;
                    lockPopulation.acquire();
                    childrenOnOahu++;
                    lockPopulation.release();
                    boatOnOahu = true;
                }

                lockBoat.release();
                if(!done){
                    KThread.yield();
                }

            }
        }
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
