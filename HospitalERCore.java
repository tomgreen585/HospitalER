// This program is copyright VUW.
// You are granted permission to use it to construct your answer to a COMP103 assignment.
// You may not distribute it in any other way without permission.

/* Code for COMP103 - 2022T2, Assignment 3
 * Name: Thomas Green
 * Username: greenthom
 * ID: 300536064
 */

import ecs100.*;
import java.util.*;
import java.io.*;

/**
 * Simple Simulation of a Hospital ER
 * 
 * The Emergency room has a waiting room and a treatment room that has a fixed
 *  set of beds for examining and treating patients.
 * 
 * When a patient arrives at the emergency room, they are immediately assessed by the
 *  triage team who determines the priority of the patient.
 *
 * They then wait in the waiting room until a bed becomes free, at which point
 * they go from the waiting room to the treatment room.
 *
 * When a patient has finished their treatment, they leave the treatment room and are discharged,
 *  at which point information about the patient is added to the statistics. 
 *
 *  READ THE ASSIGNMENT PAGE!
 */

public class HospitalERCore{

    // Fields for recording the patients waiting in the waiting room and being treated in the treatment room
    private Queue<Patient> waitingRoom = new ArrayDeque<Patient>();
    private static final int MAX_PATIENTS = 5;   // max number of patients currently being treated
    private Set<Patient> treatmentRoom = new HashSet<Patient>();
    //private Queue<Patient> priorityWaitingRoom = new PriorityQueue<Patient>(); //added boo
    
    // fields for the statistics
    private int totalNumberOfPatientTreat = 0; //number patients that have been discharged
    private int totalWaitingTime = 0; //waiting time of patients that have been discharged
    
    private static int totalNumberOfPriority1Treat = 0; //priority 1 patients that have been discharged
    private static double totalWaitingTimeForPriority1 = 0; //waiting time of priority 1 that have been discharged

    
    // Fields for the simulation
    private boolean running = false;
    private int time = 0; // The simulated time - the current "tick"
    private int delay = 300;  // milliseconds of real time for each tick

    // fields controlling the probabilities.
    private int arrivalInterval = 5;   // new patient every 5 ticks, on average
    private double probPri1 = 0.1; // 10% priority 1 patients
    private double probPri2 = 0.2; // 20% priority 2 patients
    private Random random = new Random();  // The random number generator.

    /**
     * Construct a new HospitalERCore object, setting up the GUI, and resetting
     */
    public static void main(String[] arguments){
        HospitalERCore er = new HospitalERCore();
        er.setupGUI();
        er.reset(false);   // initialise with an ordinary queue.
    }        

    /**
     * Set up the GUI: buttons to control simulation and sliders for setting parameters
     */
    public void setupGUI(){
        UI.addButton("Reset (Queue)", () -> {this.reset(false); });
        UI.addButton("Reset (Pri Queue)", () -> {this.reset(true);});
        UI.addButton("Start", ()->{if (!running){ run(); }});   //don't start if already running!
        UI.addButton("Pause & Report", ()->{running=false;});
        UI.addSlider("Speed", 1, 400, (401-delay),
            (double val)-> {delay = (int)(401-val);});
        UI.addSlider("Av arrival interval", 1, 50, arrivalInterval,
            (double val)-> {arrivalInterval = (int)val;});
        UI.addSlider("Prob of Pri 1", 1, 100, probPri1*100,
            (double val)-> {probPri1 = val/100;});
        UI.addSlider("Prob of Pri 2", 1, 100, probPri2*100,
            (double val)-> {probPri2 = Math.min(val/100,1-probPri1);});
        UI.addButton("Quit", UI::quit);
        UI.setWindowSize(1000,600);
        UI.setDivider(0.5);
    }

    /**
     * Reset the simulation:
     *  stop any running simulation,
     *  reset the waiting and treatment rooms
     *  reset the statistics.
     */
    public void reset(boolean usePriorityQueue){
        running=false;
        UI.sleep(2*delay);  // to make sure that any running simulation has stopped
        time = 0;           // set the "tick" to zero.

        // reset the waiting room, the treatment room, and the statistics.
        waitingRoom.clear();
        treatmentRoom.clear();
        //patient variables
        totalNumberOfPatientTreat = 0;
        totalWaitingTime = 0;
        
        if(usePriorityQueue == true){
            waitingRoom = new PriorityQueue<Patient>();
        } else {
            waitingRoom = new ArrayDeque<Patient>();
        }
        
        UI.clearGraphics();
        UI.clearText();
    }

    /**
     * Main loop of the simulation
     */
    public void run(){
        if (running) { return; } // don't start simulation if already running one!
        running = true;
        while (running){         // each time step, check whether the simulation should pause.
            time++;
            // Hint: if you are stepping through a set, you can't remove
            // items from the set inside the loop!
            // If you need to remove items, you can add the items to a 
            // temporary list, and after the loop is done, remove all
            // the items on the temporary list from the set
            //If patient completed treatment move patient out of treatment room
            List<Patient> storage = new ArrayList<>(); //tempset
            for (Patient p : treatmentRoom){
                if (p.completedCurrentTreatment() == (true)){
                    storage.add(p); // add patients that have completed current treatment into tempset
                    UI.println(time+ ": Discharge: " + p); //debug
                } else{
                    p.advanceTreatmentByTick(); // advance time for patients still in treatment room
                }
            }
            for(Patient p : storage){ //removes patients on the tempset
                treatmentRoom.remove(p); // calulate total waiting time as well as number of patients that have been discharged
                totalNumberOfPatientTreat++;
                totalWaitingTime = p.getWaitingTime() + totalWaitingTime;
                if(p.getPriority() == 1){ // calculate total waiting time as well as number of priority 1 patients that have been discharged
                    totalNumberOfPriority1Treat++;
                    totalWaitingTimeForPriority1 = p.getWaitingTime() + totalWaitingTimeForPriority1;
                }
            }
            for(Patient p : waitingRoom){
                p.waitForATick();
            }
            for(int i = 0; i < MAX_PATIENTS; i++){
                if(treatmentRoom.size() != MAX_PATIENTS){
                    if(waitingRoom.peek() != null){ //waiting room not empty
                        Patient move = waitingRoom.remove();
                        treatmentRoom.add(move);
                    }
                }
            }
            // Get any new patient that has arrived and add them to the waiting room
            if (time==1 || Math.random()<1.0/arrivalInterval){
                Patient newPatient = new Patient(time, randomPriority());
                UI.println(time+ ": Arrived: "+newPatient);
                waitingRoom.offer(newPatient);
            }
            redraw();
            UI.sleep(delay);
        }
        // paused, so report current statistics
        reportStatistics();
    }
    
    /**
     * Report summary statistics about all the patients that have been discharged.
     * (Doesn't include information about the patients currently waiting or being treated)
     * The run method should have been recording various statistics during the simulation.
     */
    public void reportStatistics(){
        //average waiting time for patients
        double averageWaitTime = totalWaitingTime/totalNumberOfPatientTreat;
        double averagePriorityWaitTime = totalWaitingTimeForPriority1/totalNumberOfPriority1Treat;
        UI.printf("Processed " + totalNumberOfPatientTreat + " patients with average waiting time of %.1f minutes.\n", averageWaitTime);
        UI.printf("Processed " + totalNumberOfPriority1Treat + " priority one patients with average waiting time of %.1f minutes.\n", averagePriorityWaitTime);
    }
    // HELPER METHODS FOR THE SIMULATION AND VISUALISATION
    /**
     * Redraws all the departments
     */
    public void redraw(){
        UI.clearGraphics();
        UI.setFontSize(14);
        UI.drawString("Treating Patients", 5, 15);
        UI.drawString("Waiting Queues", 200, 15);
        UI.drawLine(0,32,400, 32);
        // Draw the treatment room and the waiting room:
        double y = 80;
        UI.setFontSize(14);
        UI.drawString("ER", 0, y-35);
        double x = 10;
        UI.drawRect(x-5, y-30, MAX_PATIENTS*10, 30);  // box to show max number of patients
        for(Patient p : treatmentRoom){
            p.redraw(x, y);
            x += 10;
        }
        x = 200;
        for(Patient p : waitingRoom){
            p.redraw(x, y);
            x += 10;
        }
        UI.drawLine(0,y+2,400, y+2);
    }

    /** 
     * Returns a random priority 1 - 3
     * Probability of a priority 1 patient should be probPri1
     * Probability of a priority 2 patient should be probPri2
     * Probability of a priority 3 patient should be (1-probPri1-probPri2)
     */
    private int randomPriority(){
        double rnd = random.nextDouble();
        if (rnd < probPri1) {return 1;}
        if (rnd < (probPri1 + probPri2) ) {return 2;}
        return 3;
    }
}
