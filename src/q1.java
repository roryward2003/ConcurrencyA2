import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

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
            } catch (Exception e) {                        // Catch errors
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
                for(Cell c : cs) {
                    System.out.print(c.getValue());
                }
                System.out.println();
            }
            System.out.println();

            // // Create t threads using the wordSearchThread runnable
            WordSearchThread wordSearchThread = new WordSearchThread(grid, k, dict);
            Thread[] threads = new Thread[t];
            for(int i=0; i<t; i++)
                threads[i] = new Thread(wordSearchThread);
            for(Thread tr : threads)
                tr.start();
            Thread.sleep(1000);
            for(Thread tr : threads)
                tr.join();

        } catch (Exception e) {                        // Catch errors
            System.out.println("ERROR " +e);           // And print them to the console
            e.printStackTrace();                       // Also print the stack trace
        }
    }
}

class Cell {
    private int x;
    private int y;
    private char value;
    private List<String> wordList;

    public Cell (int x, int y, char value) {
        this.x = x;
        this.y = y;
        this.value = value;
        this.wordList = new ArrayList<String>();
    }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public char getValue() { return value; }
    public void setValue(char value) { this.value = value; }
    public boolean addWord(String s) {
        if(wordList.contains(s))
            return false;
        wordList.add(s);
        return true;
    }
}

class WordSearchThread implements Runnable {

    private Cell[][] grid;
    private int numCells;
    private ThreadLocalRandom rng;
    private TreeSet<String> dict;

    public WordSearchThread(Cell[][] grid, int numCells, TreeSet<String> dict) {
        this.grid = grid;
        this.numCells = numCells;
        this.rng = ThreadLocalRandom.current();
        this.dict = dict;
    }

    @Override
    public void run() {
        int numMoves, lastMove;
        List<Integer> sequence;
        List<Integer> possibleMoves;
        for(int i=0; i<numCells; i++) {
            sequence = new ArrayList<Integer>();
            possibleMoves = new ArrayList<Integer>();
            lastMove = rng.nextInt(grid.length*grid.length);
            sequence.add(lastMove);
            numMoves = 1;
            while(numMoves <= 7) {
                possibleMoves = getSurroundingIndices(lastMove, grid.length);
                possibleMoves.removeAll(sequence);
                if(possibleMoves.isEmpty())
                    break;
                lastMove = possibleMoves.get(rng.nextInt(possibleMoves.size()));
                sequence.add(lastMove);
                numMoves++;
            }

            String potentialWord = "";
            String subWord = "";

            // Acquire lock for all cells in sequence, in ASCENDING order

            for(int j : sequence) {
                potentialWord += grid[j/grid.length][j%grid.length].getValue();
            }
            for(int j=3; j<sequence.size(); j++) {
                subWord = potentialWord.substring(0, j);
                if(dict.contains(subWord.toLowerCase())) {
                    // Legit word found
                    System.out.println(subWord);
                }
            }
            // if so, add the word to the wordList for all i cells
            // (assuming its not already in the list)

            // sleep for 20ms
            try {
                Thread.sleep(20);
            } catch (Exception e) {                        // Catch errors
                System.out.println("ERROR " +e);           // And print them to the console
                e.printStackTrace();                       // Also print the stack trace
            }
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