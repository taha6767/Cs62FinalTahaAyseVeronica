package com.example.datingapp;
import java.util.ArrayList;

/**
 * simple class to represent a person in our dating app holds their info and mbti stats
 * @author Taha
 */
public class People {
    private String name;
    private String email;
    private String mbtiRaw; // Stores "ENFP", "ISTJ", etc. needed for reference
    private int validLikes = 0;

    // Tracks compatibility: What this person *likes* in others
    // E(-1)/I(+1), N(-1)/S(+1), T(-1)/F(+1), P(-1)/J(+1)
    private ArrayList<Integer> mbtiStats; 
    
    // Tracks identity: What this person *is*
    // Logic: E(+1)/I(-1), S(+1)/N(-1), F(+1)/T(-1), J(+1)/P(-1)
    private ArrayList<Integer> mbtiSelfType; 


    //tracks the people the person has liked/friended
    private ArrayList<String> likedEmails;
    private ArrayList<String> friendEmails;

    //tracks the matches made by mutual likes
    private ArrayList<String> likeMatches;
    private ArrayList<String> friendMatches;


    /**
     * creates a new person with just their name and email starts with empty stats
     * @param name the users full name
     * @param email their unique email address
     */
    public People(String name, String email) {
        this.name = name;
        this.email = email;
        this.mbtiRaw = "NA";
        
        // Initialize compatibility stats with [0, 0, 0, 0]
        this.mbtiStats = new ArrayList<>();
        this.mbtiSelfType = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            this.mbtiStats.add(0);
            this.mbtiSelfType.add(0);
        }
        
        this.likedEmails = new ArrayList<>();
        this.friendEmails = new ArrayList<>();

        this.likeMatches = new ArrayList<>();
        this.friendMatches = new ArrayList<>();
    }

    /**
     * takes the string like ENTJ and turns it into numbers so we can do math on it
     * logic for self E S F J is +1 and I N T P is -1
     * @param mbtiType the 4 letter string
     */
    public void setMbtiSelfType(String mbtiType) {
        if (mbtiType == null || mbtiType.length() < 4) return;
        
        this.mbtiRaw = mbtiType.toUpperCase();
        
        // Index 0: E vs I
        mbtiSelfType.set(0, (mbtiRaw.charAt(0) == 'E') ? 1 : -1);
        // Index 1: S vs N
        mbtiSelfType.set(1, (mbtiRaw.charAt(1) == 'S') ? 1 : -1);
        // Index 2: F vs T
        mbtiSelfType.set(2, (mbtiRaw.charAt(2) == 'F') ? 1 : -1);
        // Index 3: J vs P
        mbtiSelfType.set(3, (mbtiRaw.charAt(3) == 'J') ? 1 : -1);
    }

    /**
     * when you like someone we look at their type and update your preferences to match what you like
     * logic for preference E S F J is +1 and I N T P is -1
     * @param targetMbti the mbti of the person you just liked
     */
    public void updateMbtiStats(String targetMbti) {
        if (targetMbti == null || targetMbti.equals("NA") || targetMbti.length() < 4) return;

        targetMbti = targetMbti.toUpperCase();
        this.validLikes++;

        // 1. E vs I
        int currentE = mbtiStats.get(0);
        mbtiStats.set(0, (targetMbti.charAt(0) == 'E') ? currentE + 1 : currentE - 1);

        // 2. S vs N
        int currentS = mbtiStats.get(1);
        mbtiStats.set(1, (targetMbti.charAt(1) == 'S') ? currentS + 1 : currentS - 1);

        // 3. F vs T
        int currentF = mbtiStats.get(2);
        mbtiStats.set(2, (targetMbti.charAt(2) == 'F') ? currentF + 1 : currentF - 1);

        // 4. J vs P
        int currentJ = mbtiStats.get(3);
        mbtiStats.set(3, (targetMbti.charAt(3) == 'J') ? currentJ + 1 : currentJ - 1);
    }

    /**
     * adds an email to the list of people you like
     * @param email the email to add
     */
    public void addLikedEmail(String email) { this.likedEmails.add(email); }
    /**
     * adds an email to the friend crush list
     * @param email the email to add
     */
    public void addFriendEmail(String email) { this.friendEmails.add(email); }

     /**
     * adds an email to the list of people you matched via liking
     * @param email the email to add
     */
     public void addLikedEmailMatch(String email) { this.likeMatches.add(email); }
     /**
      * adds an email to the list of people you matched as friends
      * @param email the email to add
      */
     public void addFriendEmailMatch(String email) { this.friendMatches.add(email); }

    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getMbtiRaw() { return mbtiRaw; } // Necessary for relationship processing
    public ArrayList<Integer> getMbtiStats() { return mbtiStats; }
    public ArrayList<Integer> getMbtiSelfType() { return mbtiSelfType; }
    public int getValidLikes() {return validLikes;}
    
    /**
     * accessor for the list of people this person likes
     * @return the arraylist of emails
     */
     public ArrayList<String> getLikedEmails() {
        return likedEmails;
    }
    
    /**
     * accessor for the list of friends this person has
     * @return the arraylist of friend emails
     */
    public ArrayList<String> getFriendEmailsMatch() {
        return friendMatches;
    }

     /**
     * accessor for the list of people this person likes
     * @return the arraylist of emails
     */
     public ArrayList<String> getLikedEmailsMatch() {
        return likeMatches;
    }
    
    /**
     * accessor for the list of friends this person has
     * @return the arraylist of friend emails
     */
    public ArrayList<String> getFriendEmails() {
        return friendEmails;
    }


    @Override
    public String toString() {
        return name + " (" + mbtiRaw + ") | SelfScore: " + mbtiSelfType + " | PrefScore: " + mbtiStats;
    }
}