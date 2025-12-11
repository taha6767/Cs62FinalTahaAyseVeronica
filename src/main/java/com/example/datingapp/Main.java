package com.example.datingapp;
/**
 * this class is just for testing the backend logic without running the whole website
 * @author Taha
 */

/**
 * New Features:
 * - Tested Autocomplete (popularity-ranked)
 * @author Veronica
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

        // Test Popularity tracking (likedByCount)
        System.out.println("\n--- Testing popularity tracking ---");
        for(People p:database.getAllPeople()){
            System.out.println(p.getName() + " | Email: " + p.getEmail()
                    + " | Liked By Count = " + p.getLikedByCount());
        }
        
        // Test substring-based name matching + autocomplete
        System.out.println("\n---Testing Autocomplete (popularity ranked)---");
        String[] testPrefixes = {"a", "li", "jo", "mi"};
        for(String prefix: testPrefixes){
            System.out.println("\nAutocomplete for prefix: \"" + prefix +"\"");

            for(People p : database.autocompleteByPopularity(prefix)){
                System.out.println(" -> " + p.getName() + " | Popularity =" + p.getLikedByCount());
            }
        }

    }
}