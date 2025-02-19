import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public class q2 {
    
    private static int k;
    private static int q;

    public static void main(String[] args) {
        try {
            // Parse and verify command line params
            k = Integer.parseInt(args[0]);
            q = Integer.parseInt(args[1]);
            if(k<=3)
                throw new IllegalArgumentException("There must be at least 4 TAs");
            if(q<=0)
                throw new IllegalArgumentException("There must be at least a 1% chance for a TA to ask a question");
            if(q>= 100)
                throw new IllegalArgumentException("There must be less than a 100% chance for a TA to ask a question");

            // Create our 3 monitors, which will be passed into the various thread
            // constructors as necessary, to allow these threads to share monitors.
            TASession tas = new TASession();
            TAQueue taq = new TAQueue(tas);
            GradStudentList gsl = new GradStudentList(tas);

            // Create a thread array with 1 Professor, k TAs and 5 Grad Students
            Thread[] threads = new Thread[k+6];
            threads[0] = new Thread(new Professor(taq, gsl));
            for(int i=1; i<=k; i++)
                threads[i] = new Thread(new TA(taq, q), ""+i);
            for(int i=k+1; i<threads.length; i++)
                threads[i] = new Thread(new GradStudent(gsl));

            // Start the simulation
            for(Thread t : threads)
                t.start();
        } catch (Exception e) {                        // Catch exceptions
            System.out.println("ERROR " +e);           // And print them to the console
            e.printStackTrace();                       // Also print the stack trace
        }
    }
}

// This class is a runnable class for handling all actions taken by the
// Professor. It will be uesd in main to create the Professor thread.
class Professor implements Runnable {
    // Monitor references and local variables
    private TAQueue taq;
    private GradStudentList gsl;

    // Basic constructor
    public Professor(TAQueue taq, GradStudentList gsl) {
        this.taq = taq;
        this.gsl = gsl;
    }

    // This method defines the behaviour of the Prof's Thread.start() method in main
    @Override
    public void run() {
        try {
            while(true) {
                System.out.println("P goes to sleep");
                if(taq.waitForQsThenAnswer()) {        // Returns true if TA session was interrupted
                    System.out.println("P wakes their grad students");
                    gsl.wakeupGrads();                 // Wake up all the Grad Students
                    break;
                }
            }
        } catch (Exception e) {                        // Catch exceptions
            System.out.println("ERROR " +e);           // And print them to the console
            e.printStackTrace();                       // Also print the stack trace
        }
    }
}

// This class is a runnable class for handling all actions taken by the
// TAs. It will be uesd in main to create the TA threads.
class TA implements Runnable {
    // Monitor references and local variables
    private TAQueue taq;
    private int q;
    private ThreadLocalRandom rng;

    // Basic constructor
    public TA(TAQueue taq, int q) {
        this.taq = taq;
        this.q = q;
        this.rng = ThreadLocalRandom.current();
    }

    // This method defines the behaviour of the TAs Thread.start() method in main
    @Override
    public void run() {
        try {
            while(true) {                              // q% chance of coming up with a question
                if(rng.nextInt(100) < q) {
                    System.out.println("TA "+Thread.currentThread().getName()+" has come up with a question");
                    taq.question();                    // Handles question behaviour
                }
                Thread.sleep(1000);             // Wait one second between each potential question
            }
        } catch (Exception e) {                        // Catch exceptions
            System.out.println("ERROR " +e);           // And print them to the console
            e.printStackTrace();                       // Also print the stack trace
        }
    }
}

// This class is a runnable class for handling all actions taken by the
// Grad Students. It will be uesd in main to create the Grad Student threads.
class GradStudent implements Runnable {
    // Monitor references and local variables
    private GradStudentList gsl;
    private ThreadLocalRandom rng;

    // Basic constructor
    public GradStudent(GradStudentList gsl) {
        this.gsl = gsl;
        this.rng = ThreadLocalRandom.current();
    }

    // This method defines the behaviour of the Grad Students .start() method in main
    @Override
    public void run() {
        try {
            int arrivalTime = rng.nextInt(10, 61);
            Thread.sleep(arrivalTime*1000);            // Arrive some time in the next 10-60s
            gsl.arrive();                              // Handles arrival behaviour
        } catch (Exception e) {                        // Catch exceptions
            System.out.println("ERROR " +e);           // And print them to the console
            e.printStackTrace();                       // Also print the stack trace
        }
    }
}

// TAQueue is a monitor for the TAs' interaction with each other and with the Professor.
// This class directly handles TAs waiting to be seen, and indirectly handles other TA
// and Professor interactions through a TASession monitor.
class TAQueue {
    // Monitor references and local variables
    private Queue<Thread> q;
    private TASession tas;

    // Basic constructor
    public TAQueue(TASession tas) {
        this.q = new LinkedList<Thread>();
        this.tas = tas;
    }

    // This function defines the behaviour of TAs who have come up with a question
    public synchronized void question() {
        try {
            // Add yourself to the queue
            q.add(Thread.currentThread());

            // If there are now 3 TAs waiting, wake the other 2 up, and the Professor
            if(q.size()==3)
                notifyAll();

            // Wait until my question has been answered
            while(q.contains(Thread.currentThread()))
                wait();
        } catch (Exception e) {                        // Catch exceptions
            System.out.println("ERROR " +e);           // And print them to the console
            e.printStackTrace();                       // Also print the stack trace
        }
    }

    // This function defines the behaviour of the Prof when sleeping and answering Qs
    // It returns true if the TASession was interrupted by a Grad Student.
    public synchronized boolean waitForQsThenAnswer() {
        try {
            // Wait until 3 TAs have questions
            if(q.size()<3)
                wait();

            System.out.println("P wakes");
            System.out.println("TA questions being answered");

            // Wait 500ms, return true immediately if interrupted during this time
            if(tas.answerQs()) {
                System.out.println("TA Session interrupted by grad student");
                return true;                           // Indicate that session was interrupted
            }

            // Inidicate which TAs have been answered
            System.out.println("TAs "+q.remove().getName()+" "+q.remove().getName()
                +" and "+q.remove().getName()+" have been answered");

            // Notify the TAs that their questions may have been answered
            notifyAll();
            return false;
        } catch (Exception e) {                        // Catch exceptions
            System.out.println("ERROR " +e);           // And print them to the console
            e.printStackTrace();                       // Also print the stack trace
            return false;
        }
    }
}

// GradStudentList is a monitor for the Grad Students' interaction with each other and
// with the Professor. This monitor directly handles Grad Student arrival and sleeping,
// and indirectly handles the TASession interruption through a reference to the same
// TASession monitor that the TAQueue uses.
class GradStudentList {
    // Monitor references and local variables
    private List<Thread> l;
    private TASession tas;

    // Basic constructor
    public GradStudentList(TASession tas) {
        this.l = new ArrayList<Thread>();
        this.tas = tas;
    }

    // This function defines the behaviour of newly arrived Grad Students
    public synchronized void arrive() {
        try {
            // Add yourself to the list of Grad Students that have arrived
            l.add(Thread.currentThread());
            System.out.println("Grad Student "+l.size()+" has arrived");

            // If you are not the last to arrive, then go to sleep
            if(l.size() <= 4)
                wait();
            else if (l.size() == 5)                    // Otherwise, interrupt the next
                tas.interruptNextSession();            // TA Session to get the Professor
            
            // Remove yourself from the list to indicate you are awake
            l.removeFirst();

            // If you are the last to wake up, indicate that all Grad Students are awake
            if(l.isEmpty())
                System.out.println("All grad students have been woken");
        } catch (Exception e) {                        // Catch exceptions
            System.out.println("ERROR " +e);           // And print them to the console
            e.printStackTrace();                       // Also print the stack trace
        }
    }

    // This function allows the Professor to wake up all the Grad Students
    public synchronized void wakeupGrads() {
        notifyAll();
    }
}

// GradStudentList is a monitor for the Grad Students' interaction with each other and
// with the Professor. This monitor directly handles Grad Student arrival and sleeping,
// and indirectly handles the TASession interruption through a reference to the same
// TASession monitor that the TAQueue uses.

// TASession is a monitor for interactions relating directly to the TA answering session.
// It allows the Professor to answer the TAs questions, and also handles the interruption
// of the session by a Grad Student, if this occurs.
class TASession {
    // Monitor references and local variables
    private boolean interrupted;

    // Basic constructor
    public TASession() {
        this.interrupted = false;
    }

    // This function defines the behaviour of the Professor when answering TA questions.
    // It returns true if the session is interrupted, or false otherwise
    public synchronized boolean answerQs() {
        try {
            if(interrupted)                            // If interrupted is already set
                return true;                           //   Grad Student interrupts immediately
            wait(500);                   // Otherwise wait to be interrupted for up
            return interrupted;                        // to 500ms to simulate answering questions

        } catch (Exception e) {                        // Catch exceptions
            System.out.println("ERROR " +e);           // And print them to the console
            e.printStackTrace();                       // Also print the stack trace
            return false;                              // Return false on exceptions
        }
    }

    // This function defines the behaviour of a Grad Student interrupting the next session
    public synchronized void interruptNextSession() {
        interrupted = true;                            // Set interrupted to true, and notify the
        notify();                                      // Prof immediately if he is currently
    }                                                  // answering TA questions
}