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
import java.awt.Color;

/**
 * Simulation of a Hospital ER
 * 
 * The hospital has a collection of Departments, including the ER department, each of which has
 *  and a treatment room.
 * 
 * When patients arrive at the hospital, they are immediately assessed by the
 *  triage team who determine the priority of the patient and (unrealistically) a sequence of treatments 
 *  that the patient will need.
 *
 * The simulation should move patients through the departments for each of the required treatments,
 * finally discharging patients when they have completed their final treatment.
 *
 *  READ THE ASSIGNMENT PAGE!
 */

public class HospitalERCompl{
    // Copy the code from HospitalERCore and then modify/extend to handle multiple departments

    // Fields for recording the patients waiting in the waiting room and being treated in the treatment room
    private Queue<Patient> waitingRoomER = new ArrayDeque<Patient>();
    private static final int MAX_PATIENTS_ER = 8;   // max number of patients currently being treated
    private Set<Patient> treatmentRoomER = new HashSet<Patient>();
    private Map<String, Department> department = new HashMap<String ,Department>(); //Map of departments, key = name of department, value = department object.
    
    // fields for the statistics
    private static int totalNumberOfPatientTreat = 0; //number of patients who have finished treatment
    private static double totalWaitingTime = 0; //total waiting time of everyone who finshed treatment

    private static int totalNumberOfPriority1Treat = 0; //number of priority 1 patients who have finished treatment
    private static double totalWaitingTimeForPriority1 = 0; //total waiting time of priority 1 who finshed treatment

    // Fields for the simulation
    private boolean running = false;
    boolean advancedStats = false;
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
        HospitalERCompl er = new HospitalERCompl();
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
        UI.addButton("Challenge (Press first before press P & R)", () -> {advancedStats = !advancedStats;});
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

        // reset the waiting room, the treatment room.
        //Patient Variables
        totalNumberOfPatientTreat = 0;
        totalWaitingTime = 0;
        //Priority Patient Variables
        totalNumberOfPriority1Treat = 0;
        totalWaitingTimeForPriority1 =0;
        
        Department er = new Department("ER", 8, usePriorityQueue);
        Department mri = new Department("MRI", 1, usePriorityQueue);
        Department surgery = new Department("Surgery", 2, usePriorityQueue);
        Department xRay = new Department("X-Ray", 2, usePriorityQueue);
        Department ultraSound = new Department("Ultrasound", 3, usePriorityQueue);
        
        department.put("ER", er);
        department.put("MRI", mri);
        department.put("Surgery", surgery);
        department.put("X-ray", xRay);
        department.put("Ultrasound", ultraSound);

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
            time++; // advance the time by one tick, by adding one.
            // Hint: if you are stepping through a set, you can't remove
            //   items from the set inside the loop!
            //   If you need to remove items, you can add the items to a
            //   temporary list, and after the loop is done, remove all 
            //   the items on the temporary list from the set.
            for(Department d: department.values()){
                //If patient has completed treatment move them out of the treatment room.
                Set<Patient> toRemove = new HashSet<>(); // temp list.
                for(Patient p : d.getTreatmentRoom()){
                    if(p.completedCurrentTreatment() == true){
                        p.incrementTreatmentNumber();//moves patients to next treatment.
                        // adding all the patients who have completed their current treatment to the temp list.
                        toRemove.add(p);
                        if(p.noMoreTreatments() == true){
                            toRemove.add(p);
                            UI.println(time+ ": Discharge: " + p); //for debugging
                        } else if(p.noMoreTreatments() == false){
                            Department toChangeTo = department.get(p.getCurrentTreatment());
                            Queue<Patient> newWaitingRoom = toChangeTo.getWaitingRoom();
                            newWaitingRoom.offer(p);
                        }
                    } else{
                        p.advanceTreatmentByTick(); //advances the time for those still getting treated
                    }
                }
                for(Patient p : toRemove){ //removes all patients who are on the temp list
                    d.getTreatmentRoom().remove(p); 
                    // stop concurrent exception
                    //d.getTreatmentRoom().remove(p); 
                    //calculates the Wait time and number of patients who finshed treatment
                    totalNumberOfPatientTreat++;
                    totalWaitingTime = p.getWaitingTime() + totalWaitingTime;
                    //the same as above but just for Priority 1 patients.
                    if(p.getPriority() == 1){
                        totalNumberOfPriority1Treat++;
                        totalWaitingTimeForPriority1 = p.getWaitingTime() + totalWaitingTimeForPriority1;
                    }
                } 
                for(Patient p : d.getWaitingRoom()){
                    p.waitForATick(); //advances the time for those in the waiting room
                } 
                //Move patients from waiting room to the treatment room if ther is space.
                for(int i = 0; i < d.getMaxPatients(); i++){
                    if(d.getTreatmentRoom().size() != d.getMaxPatients()){
                        if(d.getWaitingRoom().peek() != null){ //if waiting room is not empty.
                            Patient move = d.getWaitingRoom().remove();
                            d.getTreatmentRoom().add(move);
                        }
                    }
                }
                
                // Get any new patient that has arrived and add them to the waiting room
                if (time==1 || Math.random()<1.0/arrivalInterval){
                    Patient newPatient = new Patient(time, randomPriority());
                    UI.println(time+ ": Arrived: "+newPatient);
                    department.get(newPatient.getCurrentTreatment()).getWaitingRoom().offer(newPatient);
                    //Department er = ;
                    //er.getWaitingRoom().offer(newPatient);
                }
                //UI.println(waitingRoom);
                redraw();
                UI.sleep(delay);
            }
        }   
        // paused, so report current statistics
            reportStatistics();
    }


    // Additional methods used by run() (You can define more of your own)
    /**
     * Report summary statistics about all the patients that have been discharged.
     * (Doesn't include information about the patients currently waiting or being treated)
     * The run method should have been recording various statistics during the simulation.
     */
    public void reportStatistics(){
        if(totalNumberOfPatientTreat > 0){ // avoid / 0 error
            UI.println("Total Patients Processed: " + totalNumberOfPatientTreat); //total patients
            UI.println("Average Waiting Time: " + totalWaitingTime/totalNumberOfPatientTreat); //average waiting time
        }                
        else{
            UI.println("No patients proccessed yet"); //no patients processed
        }
        if(totalNumberOfPriority1Treat > 0){ // Avoid  / 0 error
            UI.println("Total Priority one patients Processed: " + totalNumberOfPriority1Treat); //total pri1 patients
            UI.println("Average Waiting Time for priority one: " + totalWaitingTimeForPriority1/totalNumberOfPriority1Treat); // ave wait for pri1
        }
        else{
            UI.println("No priority one patients proccessed yet"); // no pri1 processed
        }
    
        if(advancedStats){
            UI.clearGraphics();
            Collection <Department> values = department.values();        
            int y = 1;                
            for(Department currentDepartment : values){ // redraw after clear so graphs look clean and dont have leftover text
                currentDepartment.redraw(y*60);             
                y++;
            }
            
            //Graph Methods
            UI.drawLine(10, 320, 10, 500); // y(random figures)
            UI.drawLine(10, 500, 1000, 500); // x(random figures)
            
            //Total Number of Patients/Pri1 Patients Treated ------ Graphs
            //Patients
            UI.setColor(Color.blue);
            UI.setFontSize(10);
            if(totalNumberOfPatientTreat < 200){
                UI.drawString("Total Amount of Patients Treated = " + totalNumberOfPatientTreat, 50, 500-totalNumberOfPatientTreat - 20);      
                UI.fillRect(10,500- totalNumberOfPatientTreat, 20,  totalNumberOfPatientTreat); // Total num
            }
            else{
                UI.drawString("Total Amount of Patients Treated > 200", 50, 320);      
                UI.fillRect(10,300, 20,200); // Total num
            }
            //Pri1
            UI.setColor(Color.red);
            if(totalNumberOfPriority1Treat < 200){
                UI.drawString("Total Amount of Priority One Patients Treated = " + totalNumberOfPriority1Treat, 290, 500-totalNumberOfPriority1Treat - 20);      
                UI.fillRect(250,500- totalNumberOfPriority1Treat, 20,  totalNumberOfPriority1Treat); // Total num
            }
            else{
                UI.drawString("Total Amount of Priority One Patients Treated > 200", 290, 320);      
                UI.fillRect(250,300, 20,200); // Total num
            }
            
            //Waiting Time of Patients/Pri1 Patients Treated ------ Graphs
            //Patients
            UI.setColor(Color.blue);
            if(totalNumberOfPatientTreat > 0){
                if(totalWaitingTime/totalNumberOfPatientTreat < 200){
                    UI.drawString("Average Waiting Time for Patients = " + totalWaitingTime/totalNumberOfPatientTreat, 520, 500-totalWaitingTime/totalNumberOfPatientTreat - 20);      
                    UI.fillRect(500,500- totalWaitingTime/totalNumberOfPatientTreat, 20,  totalWaitingTime/totalNumberOfPatientTreat); // Total num
                }
                else{
                    UI.drawString("Average Waiting Time for Patients > 200", 520, 320);      
                    UI.fillRect(500,300, 20,200); // Total num
                }
            }
            //Pri1
            UI.setColor(Color.red);
            if(totalNumberOfPriority1Treat > 0){
                if(totalWaitingTimeForPriority1/totalNumberOfPriority1Treat < 200){
                    UI.drawString("Average Waiting Time for Priority One Patients = " + totalWaitingTimeForPriority1/totalNumberOfPriority1Treat, 770, 500-totalWaitingTimeForPriority1/totalNumberOfPriority1Treat - 20);      
                    UI.fillRect(750,500- totalWaitingTimeForPriority1/totalNumberOfPriority1Treat, 20,  totalWaitingTimeForPriority1/totalNumberOfPriority1Treat); // Total num
                }
                else{
                    UI.drawString("Average Waiting Time for Priority One Patients > 200", 770, 320);  
                    UI.fillRect(750,300, 20,200); // Total num
                }
            }
        }
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

        double y = 80;
        for(Department d: department.values()){
            d.redraw(y);
            y+=60;
        }
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