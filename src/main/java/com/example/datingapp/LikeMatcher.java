package com.example.datingapp;
import java.util.ArrayList;

/**
 * This class handles sending romantic and friendship likes, and checks for matches based on these likes.
 * @author Aysegul
 */

public class LikeMatcher {

    private People liker;
    private String myEmail;

    public LikeMatcher(People p){
        liker = p;
        myEmail = p.getEmail();
    }


    /**
     * when you like someone, updates your liked list, updates your mbti preferences
     * calls isMatch() to see if there is a match anytime you send a like
     * @param p the person you liked
     */
    public void RomanticLiker(People p){

        //update the mbti preference stats based on who you like
        liker.updateMbtiStats(p.getMbtiRaw());

        String likeeEmail = p.getEmail();

        //add the person you liked emails to the list
        liker.addLikedEmail(likeeEmail);

        //call isMatch with true, so match knows what to match romantically
        if(isMatch(p, true)){
            //add each other to matches list
            liker.addLikedEmailMatch(likeeEmail);
            p.addLikedEmailMatch(myEmail);

            //remove them from the liked list
            p.getLikedEmails().remove(myEmail);
            liker.getLikedEmails().remove(likeeEmail);
        }
        
    }

    /**
     * when you friend like someone, updates your friend list
     * calls isMatch() to see if there is a match anytime you send a friendlike
     * @param p the person you friend liked
     */
    public void FriendLiker(People p){
        String friendEmail = p.getEmail();

        liker.addFriendEmail(friendEmail);
        //call isMatch with true, so match knows what to match friends
        if(isMatch(p, false)){
            //add each other to matches list
            liker.addFriendEmailMatch(friendEmail);
            p.addFriendEmailMatch(myEmail);

            p.getFriendEmails().remove(myEmail);
            liker.getFriendEmails().remove(friendEmail);
        }
    }


    /**
     * checks to see if there is a match
     * @param p the person you liked 
     * @param b to decide for searching a romantic or a friendship match
     */
    private boolean isMatch(People p, boolean b){
        
        if(b){
            for(String email : p.getLikedEmails()){
                if(email.equals(myEmail)){
                    return true;
                }
            }     
        }
        else{
            for(String email : p.getFriendEmails()){
                if(email.equals(myEmail)){
                    return true;
                }
            }  
        }
        return false;
    }

    public static void main(String[] args) {
        People aysegul = new People("Aysegul", "akam2024@mymail.com");
        People kula = new People("kula", "xx@mymail.com");
        People taha = new People("taha", "taha email.com");
        People veronica = new People("Aysegul", "veronica@mymail.com");

        LikeMatcher ayM = new LikeMatcher(aysegul);
        LikeMatcher kulaM = new LikeMatcher(kula);
        LikeMatcher tahaM = new LikeMatcher(taha);
        LikeMatcher vM = new LikeMatcher(veronica);

        //aysegul likes veronica veronica friends aysegul
        vM.FriendLiker(aysegul);
        ayM.RomanticLiker(veronica);

        System.out.println("aysegul romantic and friend matches:");
        System.out.println(aysegul.getLikedEmailsMatch());
        System.out.println(aysegul.getFriendEmailsMatch());

        System.out.println("veronica romantic and friend matches:");
        System.out.println(veronica.getLikedEmailsMatch());
        System.out.println(veronica.getFriendEmailsMatch());

        System.out.println("aysegul likes and friend likes:");
        System.out.println(aysegul.getLikedEmails());
        System.out.println(aysegul.getFriendEmails());

        System.out.println("veronica likes and friend likes:");
        System.out.println(veronica.getLikedEmails());
        System.out.println(veronica.getFriendEmails());

        ayM.FriendLiker(veronica);
        System.out.println("After aysegul and veronica friendly match:");

        System.out.println("aysegul romantic and friend matches:");
        System.out.println(aysegul.getLikedEmailsMatch());
        System.out.println(aysegul.getFriendEmailsMatch());

        System.out.println("veronica romantic and friend matches:");
        System.out.println(veronica.getLikedEmailsMatch());
        System.out.println(veronica.getFriendEmailsMatch());

        System.out.println("aysegul likes and friend likes:");
        System.out.println(aysegul.getLikedEmails());
        System.out.println(aysegul.getFriendEmails());

        System.out.println("veronica likes and friend likes:");
        System.out.println(veronica.getLikedEmails());
        System.out.println(veronica.getFriendEmails());

        vM.RomanticLiker(aysegul);
        System.out.println("After aysegul and veronica romantically match:");

        System.out.println("aysegul romantic and friend matches:");
        System.out.println(aysegul.getLikedEmailsMatch());
        System.out.println(aysegul.getFriendEmailsMatch());

        System.out.println("veronica romantic and friend matches:");
        System.out.println(veronica.getLikedEmailsMatch());
        System.out.println(veronica.getFriendEmailsMatch());

        System.out.println("aysegul likes and friend likes:");
        System.out.println(aysegul.getLikedEmails());
        System.out.println(aysegul.getFriendEmails());

        System.out.println("veronica likes and friend likes:");
        System.out.println(veronica.getLikedEmails());
        System.out.println(veronica.getFriendEmails());

    }


    
}
