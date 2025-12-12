package com.example.datingapp;
import java.util.ArrayList;

/**
 * Interface describing operations supported by the PeopleHashTable class
 * @author Veronica
 */

public interface MatchDatabase {

    // Hash table operations
    void insert(People person);
    void remove(String email);
    People get(String email);
    boolean contains(String email);
    ArrayList<People> getAllPeople();

    //Data loading
    void loadPeopleFromCSV(String filename);
    void loadRelationships(String filename);

    //Matching features
    People findMatch(String email);


    //Autocomplete feature
    ArrayList<People> autocompleteByPopularity(String prefix);

    // Search results ranked by MBTI compatibility & popularity
    ArrayList<People> searchByNameRankedByMbti(String nameQuery, String currentUserEmail);

    // Debugging & visualization
    void printTable();
    void printDetailedRelations();
}
