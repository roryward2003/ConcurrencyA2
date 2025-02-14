import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Random;
import java.util.TreeMap;

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

            // Create and initialise n*n grid of seeded random characters, by frequency
            Random rng = new Random(s);
            Cell[][] grid = new Cell[n][n];
            for(int i=0; i<n; i++)
                for(int j=0; j<n; j++) // rng(1, 100001) will return a random int from 1-100000 inclusive
                    grid[i][j] = new Cell(i,j,freqs.get(freqs.ceilingKey(rng.nextInt(1, freqSum+1))));

            // Print out the grid for debugging purposese
            for(Cell[] cs : grid) {
                for(Cell c : cs) {
                    System.out.print(c.getValue()+" ");
                }
                System.out.println();
            }

            // // Create t threads using the wordSearchThread runnable
            WordSearchThread wordSearchThread = new WordSearchThread(grid, k);
            Thread[] threads = new Thread[t];
            for(int i=0; i<t; i++)
                threads[i] = new Thread(wordSearchThread);

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

    public Cell (int x, int y, char value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public char getValue() { return value; }
    public void setValue(char value) { this.value = value; }
}

class WordSearchThread implements Runnable {

    private Cell[][] grid;
    private int numCells;

    public WordSearchThread(Cell[][] grid, int numCells) {
        this.grid = grid;
        this.numCells = numCells;
    }

    @Override
    public void run() {
        // TODO run
    }
}