import java.io.*;

public class Shell {
    public static void main(String[] args) {
        FileSystem fs;
        String userInput;
        BufferedReader reader;

        try {
            fs = new FileSystem();
            reader = new BufferedReader(new FileReader("input.txt"));
            System.setOut(new PrintStream(new File("output.txt")));

            userInput = reader.readLine();
            while (userInput != null) {
                userInput = userInput.trim();
                String[] input = userInput.split(" ");

                if (userInput.isEmpty())
                    System.out.println();
                else if (input[0].equals("in"))
                    fs.init();
                else if (input[0].equals("cr")) {
                    String name = input[1];
                    fs.create(name);
                }
                else if (input[0].equals("de")) {
                    String name = input[1];
                    fs.destroy(name);
                }
                else if (input[0].equals("op")) {
                    String name = input[1];
                    fs.open(name);
                }
                else if (input[0].equals("cl")) {
                    int index = Integer.parseInt(input[1]);
                    fs.close(index);
                }
                else if (input[0].equals("sk")) {
                    int i = Integer.parseInt(input[1]);
                    int p = Integer.parseInt(input[2]);
                    fs.seek(i, p);
                }
                else if (input[0].equals("dr"))
                    fs.directory();
                else if (input[0].equals("rd")) {
                    int i = Integer.parseInt(input[1]);
                    int m = Integer.parseInt(input[2]);
                    int n = Integer.parseInt(input[3]);
                    fs.read(i, m, n);
                }
                else if (input[0].equals("wr")) {
                    int i = Integer.parseInt(input[1]);
                    int m = Integer.parseInt(input[2]);
                    int n = Integer.parseInt(input[3]);
                    fs.write(i, m, n);
                }
                else if (input[0].equals("rm")) {
                    int m = Integer.parseInt(input[1]);
                    int n = Integer.parseInt(input[2]);
                    fs.readMemory(m, n);
                }
                else if (input[0].equals("wm")) {
                    int m = Integer.parseInt(input[1]);
                    String s = input[2];
                    fs.writeMemory(m, s);
                }

                userInput = reader.readLine();
            }

            reader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
