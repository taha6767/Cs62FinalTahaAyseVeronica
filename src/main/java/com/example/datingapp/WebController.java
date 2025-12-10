package com.example.datingapp;

import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * this acts as the middleman between the website and our java code
 * @author Taha
 */

@RestController
@CrossOrigin(origins = "*") 
public class WebController {

    private static PeopleHashTable database;

    public WebController() {
        if (database == null) {
            database = new PeopleHashTable();
            try {
                // Load existing data on startup
                database.loadPeopleFromCSV("userTest.csv");
                database.loadRelationships("relationshipsTest.csv");
                System.out.println("Database loaded successfully!");
            } catch (Exception e) {
                System.out.println("CSV files not found, starting empty.");
            }
        }
    }
 
    /**
     * gets the whole list of people so we can show them in the table
     * @return a list of people objects ready for display
     */
    @GetMapping("/api/table")
    public List<PeopleDto> getTable() {
        List<PeopleDto> displayList = new ArrayList<>();
        // Convert every person in the DB to a data transfer object for display
        for (People p : database.getAllPeople()) {
            displayList.add(new PeopleDto(p));
        }
        return displayList;
    }

    /**
     * checks if the user exists so they can log in to the app
     * @param request map containing the email the user typed
     * @return a response saying success or error
     */
    @PostMapping("/api/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        People user = database.get(email);
        Map<String, Object> response = new HashMap<>();
        
        if (user != null) {
            response.put("status", "success");
            response.put("message", "Login successful");
            response.put("user", new PeopleDto(user)); 
        } else {
            response.put("status", "error");
            response.put("message", "Email not found");
        }
        return response;
    }

    /**
     * adds a new user to the system from the registration form
     * @param request the details of the new user like name and mbti
     * @return success message
     */
    @PostMapping("/api/register")
    public Map<String, Object> registerUser(@RequestBody RegistrationRequest request) {
        People newPerson = new People(request.name, request.email);
        newPerson.setMbtiSelfType(request.mbti);
        database.insert(newPerson);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "User created: " + request.name);
        return response;
    }

    /**
     * handles when a user likes or friends someone
     * @param request object containing who did it and who they targeted
     * @return message saying if it worked
     */
    @PostMapping("/api/interact")
    public Map<String, Object> interact(@RequestBody InteractionRequest request) {
        People source = database.get(request.sourceEmail);
        People target = database.get(request.targetEmail);

        Map<String, Object> response = new HashMap<>();

        if (source == null || target == null) {
            response.put("status", "error");
            response.put("message", "One or both emails not found.");
            return response;
        }

        if (request.type.equalsIgnoreCase("like")) {
            source.addLikedEmail(request.targetEmail);
            source.updateMbtiStats(target.getMbtiRaw());
            response.put("message", "You LIKED " + target.getName());
        } else {
            source.addFriendEmail(request.targetEmail);
            response.put("message", "You became FRIENDS with " + target.getName());
        }

        response.put("status", "success");
        return response;
    }

    /**
     * finds a compatible match for the user using our algorithm
     * @param email the email of the user looking for love
     * @return a person dto of the match or null if none
     */
    @GetMapping("/api/match")
    public PeopleDto findMatch(@RequestParam String email) {
        People match = database.findMatch(email);
        if (match != null) {
            return new PeopleDto(match);
        }
        return null;
    }
    /**
     * helper class to make the people object look nice for json
     */
    static class PeopleDto {
        public String name;
        public String email;
        public String mbti;
        public List<Integer> stats; 
        public List<String> likedEmails;   
        public List<String> friendEmails; 

        /**
         * constructor that copies data from the real person object to this simple display object
         * @param p the person object we want to show on the website
         */
        public PeopleDto(People p) {
            this.name = p.getName();
            this.email = p.getEmail();
            this.mbti = p.getMbtiRaw();
            this.stats = p.getMbtiStats(); 
            
            // Map the arrays from People.java to the JSON response
            this.likedEmails = p.getLikedEmails();
            this.friendEmails = p.getFriendEmails();
        }
    }

    /**
     * simple object to hold registration data
     */
    static class RegistrationRequest {
        public String name;
        public String email;
        public String mbti;
    }

    /**
     * simple object to hold interaction data
     */
    static class InteractionRequest {
        public String sourceEmail;
        public String targetEmail;
        public String type; 
    }
}