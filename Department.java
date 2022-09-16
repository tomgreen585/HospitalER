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

/**
 * A treatment Department (Surgery, X-ray room,  ER, Ultrasound, etc)
 * Each department will need
 * - A name,
 * - A maximum number of patients that can be treated at the same time
 * - A Set of Patients that are currently being treated
 * - A Queue of Patients waiting to be treated.
 *    (ordinary queue, or priority queue, depending on argument to constructor)
 */

public class Department{

    private String name;
    private int maxPatients;
    private boolean usePriorityQueue;
    private Queue<Patient>waitingRoom = new ArrayDeque<Patient>();
    private Set<Patient>treatmentRoom = new HashSet<Patient>();

    public Department(String departmentName, int maxPat, boolean usePriQueue){
        this.name = departmentName;
        this.maxPatients = maxPat;
        this.usePriorityQueue = usePriQueue;
        if(usePriorityQueue == true){
            waitingRoom = new PriorityQueue<Patient>();
        } else {
            waitingRoom = new ArrayDeque<Patient>();
        }

    }
    
    /**
     * Draw the department: the patients being treated and the patients waiting
     * You may need to change the names if your fields had different names
     */
    public void redraw(double y){
        UI.setFontSize(14);
        UI.drawString(name, 0, y-35);
        double x = 10;
        UI.drawRect(x-5, y-30, maxPatients*10, 30);  // box to show max number of patients
        for(Patient p : treatmentRoom){
            p.redraw(x, y);
            x += 10;
        }
        x = 200;
        for(Patient p : waitingRoom){
            p.redraw(x, y);
            x += 10;
        }
    }
    
    //return all variables
    //get the name field
    public String getName(){
        return name; 
    }
    //get treatment room
    public Set<Patient> getTreatmentRoom(){
        return treatmentRoom; 
    }
    //get max patients
    public int getMaxPatients(){
        return maxPatients; 
    }
    //get waiting room
    public Queue<Patient>getWaitingRoom(){
        return waitingRoom; 
    }
}
