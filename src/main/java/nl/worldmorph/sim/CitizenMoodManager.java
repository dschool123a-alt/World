package nl.worldmorph.sim;
import java.util.*;
public final class CitizenMoodManager {
 public enum Mood{CONTENT,HOPEFUL,UNEASY,ANGRY,REBELLIOUS}
 public record Factors(int food,int safety,int housing,int taxFairness,int trust){}
 public Mood evaluate(Factors f){int score=(f.food()+f.safety()+f.housing()+f.taxFairness()+f.trust())/5;if(score>=80)return Mood.CONTENT;if(score>=65)return Mood.HOPEFUL;if(score>=45)return Mood.UNEASY;if(score>=25)return Mood.ANGRY;return Mood.REBELLIOUS;}
 public int stabilityEffect(Mood m){return switch(m){case CONTENT->2;case HOPEFUL->1;case UNEASY->0;case ANGRY->-2;case REBELLIOUS->-4;};}
}