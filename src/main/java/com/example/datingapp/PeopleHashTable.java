package com.example.datingapp;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * this class so far handles the hash table logic for storing people and finding matches based on compatibility
 * @author Taha
 */

/**
 * New Features:
 * - Token-based name matching for autocomplete and search
 * - Autocomplete suggestions ranked by popularity
 * - Final search results ranked by MBTI compatibility
 * 
 * This class now handles both autocomplete and search as well.
 * @author Veronica
 */
public class PeopleHashTable {

    // Internal class to handle Lazy Deletion
    private static class HashEntry {
        public People element;
        public boolean isActive; //false means deleted

        public HashEntry(People e) {
            this(e, true);
        }

        public HashEntry(People e, boolean i) {
            this.element = e;
            this.isActive = i;
        }
    }

    private HashEntry[] array; // The storage array
    private int currentSize;   // Number of active elements
    private static final int DEFAULT_TABLE_SIZE = 11; // Small prime to start as our %m

    public PeopleHashTable() {
        this(DEFAULT_TABLE_SIZE);
    }

    public PeopleHashTable(int size) {
        allocateArray(size);
        makeEmpty();
    }

    /**
     * clears out the whole table by setting everything to null
     */
    public void makeEmpty() {
        currentSize = 0;
        for (int i = 0; i < array.length; i++) {
            array[i] = null;
        }
    }

    /**
     * finds a person using their email which is unique
     * @param email the email of the person we want
     * @return the person object if found otherwise null
     */
    public People get(String email) {
        int currentPos = findPos(email);
        if (isActive(currentPos)) {
            return array[currentPos].element;
        }
        return null;
    }

    /**
     * checks if a person is in the table
     * @param email email to check
     * @return true if they exist false otherwise
     */
    public boolean contains(String email) {
        int currentPos = findPos(email);
        return isActive(currentPos);
    }

    /**
     * adds a new person to the hash table handling collisions
     * @param person the person object to add
     */
    public void insert(People person) {
        String emailKey = person.getEmail();
        int currentPos = findPos(emailKey);

        // If the slot is already active with the same email, it's a duplicate.
        // We do not override it as the original user is probably the real one
        if (isActive(currentPos)) {
            return; 
        }

        // Insert new entry (or overwrite a lazy-deleted one)
        array[currentPos] = new HashEntry(person, true);
        currentSize++;

        // Check Load Factor > 0.5
        if (currentSize > array.length / 2) {
            rehash();
        }
    }

    /**
     * removes a person but just marks them as inactive instead of deleting
     * @param email email of the person to remove
     */
    public void remove(String email) {
        int currentPos = findPos(email);
        if (isActive(currentPos)) {
            array[currentPos].isActive = false;
            currentSize--; // Reduce count of active items
        }
    }

    /**
     * finds the position for a key using quadratic probing
     * @param key the email key
     * @return the index where the key is or should be
     */
    private int findPos(String key) {
        int offset = 1;
        int currentPos = myHash(key);

        // Loop while slot is not null AND the key doesn't match
        while (array[currentPos] != null && 
               !array[currentPos].element.getEmail().equals(key)) {
            
            // Quadratic probing formula implementation:
            // More compute efficent than multiplication we learned in class still works
            currentPos += offset;  
            offset += 2;
            
            // wraparound
            if (currentPos >= array.length) {
                currentPos -= array.length;
            }
        }

        return currentPos;
    }

    /**
     * resizes the table when it gets too full
     */
    private void rehash() {
        HashEntry[] oldArray = array;

        // Create new array of double size (next prime)
        allocateArray(nextPrime(2 * oldArray.length));
        currentSize = 0;

        // Copy active elements (Lazy deleted items are discarded)
        for (HashEntry entry : oldArray) {
            if (entry != null && entry.isActive) {
                insert(entry.element);
            }
        }
    }

    /**
     * Hashing function for Strings.
     */
    private int myHash(String key) {
        int hashVal = 0;

        for (int i = 0; i < key.length(); i++) {
            hashVal = 27 * hashVal + key.charAt(i); // 27 method as we learned in class
        }

        hashVal %= array.length;
        if (hashVal < 0) {
            hashVal += array.length;
        }

        return hashVal;
    }

    /**
     * helper to create the array with a specific size
     * @param arraySize how big we want the array to be
     */
    private void allocateArray(int arraySize) {
        array = new HashEntry[arraySize];
    }

    /**
     * checks if the spot in the array has something and isn't deleted
     * @param currentPos the index we are checking
     * @return true if there is an active person there false if empty or deleted
     */
    private boolean isActive(int currentPos) {
        return array[currentPos] != null && array[currentPos].isActive;
    }

    // Helper function for finding the new prime hasher

    /**
     * finds the next prime number after n so we can resize the table properly
     * @param n the number we start looking from
     * @return the next prime number found
     */
    private static int nextPrime(int n) {
        // step 1 make sure n is odd
        if (n % 2 == 0) {
            n++;
        }

        // step 2 keep checking numbers until we find a prime
        while (!isPrime(n)) {
            n = n + 2; // add 2 to skip the next even number
        }

        return n;
    }
    
    /**
     * Helper to check if a number is prime
     * @param n the number to check
     * @return true if it is prime false if not
     */
    private static boolean isPrime(int n) {
        if (n == 2 || n == 3) return true;
        if (n == 1 || n % 2 == 0) return false;
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }
    
    // Helper for Csv File to Hash Table Making
    
    /**
     * reads the csv file with user data and adds them to our hash table
     * @param filename the path to the csv file
     */
    public void loadPeopleFromCSV(String filename) {
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if(data.length < 4) continue;

                String fullName = data[1].trim() + " " + data[2].trim();
                String email = data[3].trim();
                String mbti = (data.length > 4) ? data[4].trim() : "NA";

                People p = new People(fullName, email);
                
                //Calculate and store the person's own type vector
                p.setMbtiSelfType(mbti); 

                this.insert(p);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * loads who likes who or who is friends from another csv
     * @param filename name of the relationship file
     */
    public void loadRelationships(String filename) {
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if(data.length < 3) continue;

                String sourceEmail = data[0].trim();
                String type = data[1].trim().toLowerCase();
                String targetEmail = data[2].trim();

                //Find both people in the hash table
                People sourcePerson = this.get(sourceEmail);
                People targetPerson = this.get(targetEmail);

                //If either doesn't exist, skip this relationship
                if (sourcePerson == null || targetPerson == null) continue;

                // Update lists based on relationship type
                if (type.equals("friend")) {
                    sourcePerson.addFriendEmail(targetEmail);
                } 
                else if (type.equals("like")) {
                    sourcePerson.addLikedEmail(targetEmail);
                    
                    //If Source likes Target, update Source's stats based on Target's MBTI type.
                    String targetMbti = targetPerson.getMbtiRaw();
                    sourcePerson.updateMbtiStats(targetMbti);

                    // NEW: Target gets +1 popularity because someone liked them
                    targetPerson.incrementLikedByCount();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Visualization of the Hash Table
     * Shows
     * Index | Status | Email | Self Type (Identity) | Stats (Who they like)
     */
    public void printTable() {
        System.out.println("\n==================== HASH TABLE VISUALIZATION ====================");
        System.out.printf("Table Size: %d | Active Items: %d | Load Factor: %.2f%n", 
                          array.length, currentSize, (double)currentSize / array.length);
        System.out.println("------------------------------------------------------------------");
        System.out.printf("%-6s | %-10s | %-35s | %-15s | %-15s%n", 
                          "IDX", "STATUS", "EMAIL", "SELF (MBTI)", "PREFS (STATS)");
        System.out.println("------------------------------------------------------------------");

        for (int i = 0; i < array.length; i++) {
            if (array[i] != null && array[i].isActive) {
                People p = array[i].element;
                // Format: [1, -1, 1, -1]
                String self = p.getMbtiSelfType().toString(); 
                // Format: [2, 0, -1, 5]
                String stats = p.getMbtiStats().toString();   

                System.out.printf("%-6d | %-10s | %-35s | %-15s | %-15s%n", 
                                  i, "Active", p.getEmail(), self, stats);
            } 
            else if (array[i] != null && !array[i].isActive) {
                System.out.printf("%-6d | %-10s | %-35s | %-15s | %-15s%n", 
                                  i, "Deleted", array[i].element.getEmail(), "---", "---");
            } 
            else {
                System.out.printf("%-6d | %-10s | %-35s | %-15s | %-15s%n", 
                                  i, "Empty", "---", "---", "---");
            }
        }
        System.out.println("==================================================================\n");
    }
    /**
     * Prints the full list of 'Likes' and 'Friends' for every active user.
     */
    public void printDetailedRelations() {
        System.out.println("\n==================== RELATIONSHIP AUDIT ==========================");
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null && array[i].isActive) {
                People p = array[i].element;
                System.out.println("User: " + p.getName() + " (" + p.getEmail() + ")");
                System.out.println("   -> Friends: " + p.getFriendEmails()); 
                System.out.println("   -> Likes:   " + p.getLikedEmails());  
                System.out.println("------------------------------------------------------------------");
            }
        }
        System.out.println("==================================================================\n");
    }
    /**
     * the main matching algorithm that finds someone compatible for the user
     * @param email email of the person who needs a match
     * @return a person object that matches or null if nobody is found
     */
    public People findMatch(String email) {
        People seeker = get(email);
        if (seeker == null) {
            System.out.println("User not found: " + email);
            return null;
        }

        // start at a random index to vary the results
        int startIndex = (int) (Math.random() * array.length);

        // Loop through the entire table once
        for (int i = 0; i < array.length; i++) {
            //to wrap around the array
            int currentIndex = (startIndex + i) % array.length;
            HashEntry entry = array[currentIndex];

            // Skip empty slots, inactive slots, or the user themselves
            if (entry == null || !entry.isActive || entry.element == seeker) {
                continue;
            }

            People candidate = entry.element;

            // If they already mutually like each other, skip this candidate.
            boolean seekerLikesCandidate = seeker.getLikedEmails().contains(candidate.getEmail());
            boolean candidateLikesSeeker = candidate.getLikedEmails().contains(seeker.getEmail());
            
            if (seekerLikesCandidate && candidateLikesSeeker) {
                continue; // They are already a match, so don't suggest them again.
            }
            // ------------------------------

            //Statistical compatibility check (The 33% Rule)
            if (isCompatible(seeker, candidate) && isCompatible(candidate, seeker)) {
                return candidate; // Match found, you guys deserve love!
            }
        }

        return null; // No new match found
    }

    /**
     * math helper to see if two people fit each others preferences
     * @param judge the person who has preferences
     * @param subject the person being checked against those preferences
     * @return true if they match well enough false if not
     */
    private boolean isCompatible(People judge, People subject) {
        // Using validLikes so 'NA' people don't mess up the ratio
        int totalValid = judge.getValidLikes(); 
        
        // If no valid data points, assume they are okay with anyone
        if (totalValid == 0) return true;

        ArrayList<Integer> judgePrefs = judge.getMbtiStats();
        ArrayList<Integer> subjectType = subject.getMbtiSelfType();

        for (int k = 0; k < 4; k++) {
            double score = judgePrefs.get(k);
            
            // divide by the count of valid people only
            double ratio = score / totalValid;
            
            int subjectTrait = subjectType.get(k); 

            // for each personality type if they have a preference as strong as + or - 0.33 we consider that
            if (ratio > 0.33) {
                if (subjectTrait != 1) return false;
            }
            else if (ratio < -0.33) {
                if (subjectTrait != -1) return false;
            }
        }
        return true;
    }
    
    /**
     * gets a list of everyone currently active in the system
     * @return arraylist containing all the people objects
     */
    public ArrayList<People> getAllPeople() {
    ArrayList<People> activeList = new ArrayList<>();
    for (HashEntry entry : array) {
        if (entry != null && entry.isActive) {
            activeList.add(entry.element);
        }
    }
    return activeList;
    }

    /**
     * Helper Method for Autocomplete:
     * Returns true if the prefix matches any token in the name
     */

    private boolean matchesNameToken(String fullName, String prefix){
        String normPrefix = prefix.toLowerCase();
        String[] tokens = fullName.toLowerCase().split(" ");

        for(String token : tokens){
            if(token.startsWith(normPrefix)){
                return true;
            }
        }

        return false;
    }

    /**
     * Autocomplete suggestions:
     * Given a name prefix, return users whose names contain a token 
     * starting with that prefix, sorted by popularity (likedByCount, descending)
     * 
     * This is used while the user is still typing, before final search.
     */
    public ArrayList<People> autocompleteByPopularity(String prefix){
        ArrayList<People> result = new ArrayList<>();
        if(prefix == null) return result;

        String normalized = prefix.toLowerCase();

        //Collect all matched names
        ArrayList<People> candidates = new ArrayList<>();
        for(People p: getAllPeople()){
            String name = p.getName();
            if(name == null) continue;
            if(matchesNameToken(name, normalized)){
                candidates.add(p);
            }
        }

        // Sort by popularity: likedByCount descending
        candidates.sort((a,b) -> Integer.compare(b.getLikedByCount(), a.getLikedByCount()));

        result.addAll(candidates);
        return result;
    }

    /**
     * Full search:
     * Given the current user's email and a name query (which might still be partial)
     * find all matching users and rank them by MBTI compatibility score
     * (higher is better), then by popularity as a tiebreaker.
     * 
     * This is used after the user finishes typing and clicks "Search".
     */
    public ArrayList<People> searchByNameRankedByMbti(String nameQuery, String currentUserEmail){
        ArrayList<People> result = new ArrayList<>();
        People currentUser = get(currentUserEmail);

        if(currentUser == null || nameQuery == null){
            return result;
        }

        String normalized = nameQuery.toLowerCase();
        ArrayList<SearchCandidate> candidates = new ArrayList<>();

        for(People p: getAllPeople()){
            //Skip self
            if(p == currentUser) continue;

            String name = p.getName();
            if(name == null) continue;

            if(matchesNameToken(name, normalized)){
                double mbtiScore = computeMbtiMatchScorePlaceholder(currentUser, p);
                int popularity = p.getLikedByCount()candidates.add(new SearchCandidate(p, mbtiScore, popularity));

        
            }

            //Sort by MBTI score (desc), then popularity (desc)
            candidates.sort((a,b) -> {
                int cmp = Double.compare(b.mbtiScore, a.mbtiScore);
                if(cmp != 0) return cmp;
                return Integer.compare(b.popularity, a.popularity);
            });

            for(SearchCandidate c: candidates){
                result.add(c.person);
            }

            return result;
        }
    }

    private static class SearchCandidate{
        People person;
        double mbtiScore;
        int popularity;

        SearchCandidate(People person, double mbtiScore, int popularity){
            this.person = person;
            this.mbtiScore = mbtiScore;
            this.popularity = popularity;
        }
    }


}