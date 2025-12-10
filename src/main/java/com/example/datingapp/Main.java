package com.example.datingapp;
/**
 * this class is just for testing the backend logic without running the whole website
 * @author Taha
 */
public class Main {
    /**
     * standard main method to run the manual test
     * @param args arguments passed to the program
     */
    public static void main(String[] args) {
        PeopleHashTable database = new PeopleHashTable();
        
        // Load your data
        database.loadPeopleFromCSV("userTest.csv");
        database.loadRelationships("relationshipsTest.csv"); // This populates the stats

        // Test Matching for Dakota (who is ENFJ)
        // Dakota liked 'example100113' (ENTJ). 
        // If Dakota has strong preferences based on history, this will find a compatible match.
        String seekerEmail = "example100007@hmc.edu";
        
        System.out.println("Finding a match for: " + seekerEmail);
        People match = database.findMatch(seekerEmail);

        if (match != null) {
            System.out.println("MATCH FOUND!");
            System.out.println("Name: " + match.getName());
            System.out.println("Email: " + match.getEmail());
            System.out.println("Type: " + match.getMbtiRaw());
        } else {
            System.out.println("No mutual match found in the current database.");
        }
    }
}