import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.Semaphore;

public class q1 {

    private static long s;
    private static int n;
    private static int t;
    private static int k;

    public static void main(String[] args) {
        try {
            // Initialise input parameters from the command line
            s = Long.parseLong(args[0]);
            n = Integer.parseInt(args[1]);
            t = Integer.parseInt(args[2]);
            k = Integer.parseInt(args[3]);

            // Do not accept input of n<=4
            if(n<=4) {
                System.out.println("ERROR grid size must be greater than 4x4");
                return;
            }

            // Instantiate some vars needed for parsing data from the freq.txt file
            BufferedReader br = new BufferedReader(new FileReader("../freq.txt"));
            TreeMap<Integer, Character> freqs = new TreeMap<Integer, Character>();
            String line, character;
            int freq, freqSum = 0;

            // Parse freq.txt into a TreeMap with cumulative frequency as key for each char
            while(br.ready()) {
                line = br.readLine();
                if(line.length()<=1)                   // If blank line encountered, stop parsing
                    break;
                character = line.split(" ")[0];
                freq = Integer.parseInt(line.split(" +")[1]);
                freqSum += freq;
                freqs.put(freqSum, character.charAt(0));
            }
            br.close();

            // Initialise dictionary for lookups later
            TreeSet<String> dict = new TreeSet<String>();
            try {
                br = new BufferedReader(new FileReader("../dict.txt"));
                while(br.ready()) {
                    line = br.readLine();
                    if(line == "")
                        break;
                    dict.add(line);
                }
                br.close();
            } catch (Exception e) {                        // Catch exceptions
                System.out.println("ERROR " +e);           // And print them to the console
                e.printStackTrace();                       // Also print the stack trace
            }

            // Create and initialise n*n grid of seeded random characters, by frequency
            Random rng = new Random(s);
            Cell[][] grid = new Cell[n][n];
            for(int i=0; i<n; i++)
                for(int j=0; j<n; j++) // rng(1, 100001) will return a random int from 1-100000 inclusive
                    grid[i][j] = new Cell(i,j,freqs.get(freqs.ceilingKey(rng.nextInt(1, freqSum+1))));

            // Print out the grid
            for(Cell[] cs : grid) {
                for(Cell c : cs)
                    System.out.print(c.getValue());
                System.out.println();
            }

            // Create t threads using the wordSearchThread runnable
            WordSearchThread wordSearchThread = new WordSearchThread(grid, k, dict);
            Thread[] threads = new Thread[t];
            for(int i=0; i<t; i++)
                threads[i] = new Thread(wordSearchThread);

            // Run all the threads
            for(Thread tr : threads)
                tr.start();
            for(Thread tr : threads)
                tr.join();

            // Print out the resulting wordLists of each cell
            for(Cell[] cs : grid) {
                for(Cell c : cs) {
                    System.out.print("("+c.getX()+","+c.getY()+")");
                    for(String s : c.getWordList())
                        System.out.print(" "+s);
                    System.out.println();
                }
            }

        } catch (Exception e) {                        // Catch exceptions
            System.out.println("ERROR " +e);           // And print them to the console
            e.printStackTrace();                       // Also print the stack trace
        }
    }
}

// Class for storing all necessary info relating to a cell,
// including the binary semaphore used to lock the cell.
class Cell {
    private int x;
    private int y;
    private char value;
    private List<String> wordList;
    private Semaphore s;

    // Basic constructor
    public Cell (int x, int y, char value) {
        this.x = x;
        this.y = y;
        this.value = value;
        this.wordList = new ArrayList<String>();
        this.s = new Semaphore(1);
    }

    // Getters and setters for all primitives
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public char getValue() { return value; }
    public void setValue(char value) { this.value = value; }
    
    // Get wordList
    public List<String> getWordList() { return wordList; }

    // Add word to wordList, if it's not already there. The boolean
    // return value isn't used, but it makes sense to include it
    public boolean addWord(String s) {
        if(wordList.contains(s))
            return false;
        wordList.add(s);
        return true;
    }

    // Acquires the semaphore lock for this cell. If the semaphore is
    // already locked (there are no permits available), the thread will
    // be blocked until the lock is released
    public void up() {
        try {
            s.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Releases the semaphore lock for this cell, and notifies all threads
    // that are waiting. Should only be called by the thread that currently
    // controls the lock
    public void down() {
        s.release();
    }
}

// Class to encapsulate all word searching behaviour done by a thread. This
// class extends runnable, and will be used to populate the .start() method
// of the threads created and run by the main thread.
class WordSearchThread implements Runnable {

    private Cell[][] grid;
    private int numCells;
    private ThreadLocalRandom rng;
    private TreeSet<String> dict;

    // Basic constructor
    public WordSearchThread(Cell[][] grid, int numCells, TreeSet<String> dict) {
        this.grid = grid;
        this.numCells = numCells;
        this.rng = ThreadLocalRandom.current();
        this.dict = dict;
    }

    // This method will replace the thread.start() method of the threads
    // which are created using an instance of this class in the constructor.
    // This is how my threads are created and run by the main thread.
    @Override
    public void run() {
        try {
            // Instantiating a few uninitialsed variables
            int lastMove;
            List<Integer> sequence;
            List<Integer> possibleMoves;

            // Select numCells random starting points, and search for words
            // based on a random move sequence from these starting positions
            for(int i=0; i<numCells; i++) {
                sequence = new ArrayList<Integer>();
                possibleMoves = new ArrayList<Integer>();

                // Select a random starting cell and add it to the new sequence
                lastMove = rng.nextInt(grid.length*grid.length);
                sequence.add(lastMove);

                // Randomly elect 7 valid moves from this position, adding
                // each selected index to the sequence.
                for(int n=1; n <= 7; n++) {
                    // Make a list of all valid moves and remove already used indices
                    possibleMoves = getSurroundingIndices(lastMove, grid.length);
                    possibleMoves.removeAll(sequence);
                    if(possibleMoves.isEmpty()) // Stop if no possible moves left
                        break;
                    lastMove = possibleMoves.get(rng.nextInt(possibleMoves.size()));
                    sequence.add(lastMove);
                }

                // Instantiate two new empty strings
                String potentialWord = "";
                String subWord = "";

                // Acquire lock for all cells in sequence, in ASCENDING order.
                // Acquiring the locks in ascending order is abolutely essential
                // for preventing deadlock, as explained in q1.txt
                TreeSet<Integer> sortedSeq = new TreeSet<Integer>(sequence);
                for(int index : sortedSeq)
                    grid[index/grid.length][index%grid.length].up();

                // Build a string from the cell values at each index in the sequence
                for(int j : sequence)
                    potentialWord += grid[j/grid.length][j%grid.length].getValue();

                // Search for valid substrings of this string, with length 3-8 inclusive
                for(int j=3; j<=sequence.size(); j++) {
                    subWord = potentialWord.substring(0, j);
                    
                    // If legit word, add it to the word list for all the involved cells
                    if(dict.contains(subWord.toLowerCase())) {
                        int currIndex;
                        for(int k=0; k<j; k++) {
                            currIndex = sequence.get(k);
                            grid[currIndex/grid.length][currIndex%grid.length].addWord(subWord);
                        }
                    }
                }
                
                // Release the locks and notify any sleeping threads
                for(int index : sortedSeq)
                    grid[index/grid.length][index%grid.length].down();

                // sleep for 20ms
                    Thread.sleep(20);
            }
        } catch (Exception e) {                        // Catch exceptions
            System.out.println("ERROR " +e);           // And print them to the console
            e.printStackTrace();                       // Also print the stack trace
        }
    }

    // Find all surrounding grid indices
    // Preventing moves that overlap is done by the caller
    public static List<Integer> getSurroundingIndices(int index, int n) {
        List<Integer> result = new ArrayList<Integer>();
        int x = index/n;
        int y = index%n;
        for(int i=x-1; i<=x+1; i++)
            for(int j=y-1; j<=y+1; j++)
                if((i!=x || j!=y) && i>=0 && i<n && j>=0 && j<n)
                    result.add((i*n)+j);

        return result;
    }
}