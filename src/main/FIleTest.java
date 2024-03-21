package main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FIleTest {

    public static void main(String[] args) {
        try {
            BufferedWriter br = new BufferedWriter(new FileWriter("port.txt"));
            br.write("Hello World");
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
