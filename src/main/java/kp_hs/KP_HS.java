/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kp_hs;

import java.util.ArrayList;

import algorithms.*;
import model.Item;
import model.Knapsack;

public class KP_HS {

    public static void executeHarmonySearch(int instanceSize, Knapsack m){
        //Reference
//        System.out.print("REFERENCE");
//        int[] tmp = {1,0,1,0,1,0,1,1,1,0,0,0,0,1,1};
//        System.out.println(m.evaluateOF(tmp)); 
        //**********************************************//*
        HarmonySearch hs = new HarmonySearch(instanceSize, m);
        long startTimeHS = System.currentTimeMillis();
        int[] result = hs.HarmonySearchAlgorithm();
        long currentTimeHS = System.currentTimeMillis(); 
        long elapsedTimehs = (currentTimeHS-startTimeHS);
        int counter = 0;
        for(int j=0; j<instanceSize; j++){
            if((int)result[j] == 1){
                counter++;
            } 
            System.out.print((int)result[j]);
        }
        System.out.println("");
        System.out.print("HARMONY SEARCH");
        System.out.println("");        
        System.out.print("Entraron "+counter+" elementos en la mochila");
        System.out.println("");
        System.out.print("Beneficio máximo: ");
        System.out.print(m.evaluateOF(result));
        System.out.println("");
        System.out.print("Peso máximo: ");
        System.out.print(m.totalWeight(result));
        System.out.println("");        
        System.out.print("Tiempo de ejecución: ");
        //System.out.print((elapsedTimehs/1000)*60+" segundos");
        System.out.print(elapsedTimehs+" milisegundos");
        System.out.println("");        
    }
    
    public static void executeImprovedHarmonySearch(int instanceSize, Knapsack m){
        //Reference
//        System.out.println("REFERENCE");
//        int[] tmp = {1,0,1,0,1,0,1,1,1,0,0,0,0,1,1};
//        System.out.println(m.evaluateOF(tmp)); 
        //**********************************************//*
        //Improved Harmony Search        
        ImprovedHarmonySearch ihs = new ImprovedHarmonySearch(instanceSize, m);
        long startTimeIHS = System.currentTimeMillis();
        int[] resultIHS = ihs.HarmonySearchAlgorithm();
        long currentTimeIHS = System.currentTimeMillis(); 
        long elapsedTimeIHS = (currentTimeIHS-startTimeIHS);         
        int counter2 = 0;
        for(int l=0; l<instanceSize; l++){
            if((int)resultIHS[l] == 1){
                counter2++;
            } 
            System.out.print(resultIHS[l]);
        }
        System.out.println("");
        System.out.print("IMPROVED HARMONY SEARCH");
        System.out.println("");        
        System.out.print("Entraron "+counter2+" elementos en la mochila");
        System.out.println("");
        System.out.print("Beneficio máximo: ");
        System.out.print(m.evaluateOF(resultIHS));
        System.out.println("");
        System.out.print("Peso máximo: ");
        System.out.print(m.totalWeight(resultIHS));
        System.out.println("");        
        System.out.print("Tiempo de ejecución: ");
        //System.out.print((elapsedTimehs/1000)*60+" segundos");
        System.out.print(elapsedTimeIHS+" milisegundos");
        System.out.println("");        
    }
    
    public static void executeDiscreteBinaryHarmonySearch(int instanceSize, Knapsack m){
        //Reference
//        System.out.print("REFERENCE: ");
//        int[] tmp = {1,0,1,0,1,0,1,1,1,0,0,0,0,1,1};
//        System.out.println(m.evaluateOF(tmp)); 
        /********************************************/
        DiscreteBinaryHarmonySearch dbhs = new DiscreteBinaryHarmonySearch(instanceSize, m);
        long startTimeHS = System.currentTimeMillis();
        int[] result = dbhs.HarmonySearchAlgorithm();
        long currentTimeHS = System.currentTimeMillis(); 
        long elapsedTimehs = (currentTimeHS-startTimeHS);
        int counter = 0;
        for(int j=0; j<instanceSize; j++){
            if((int)result[j] == 1){
                counter++;
            }  
            System.out.print(result[j]);
        }
        System.out.println("");
        System.out.print("DISCRETE BINARY HARMONY SEARCH");
        System.out.println("");        
        System.out.print("Entraron "+counter+" elementos en la mochila");
        System.out.println("");
        System.out.print("Beneficio máximo: ");
        System.out.print(m.evaluateOF(result));
        System.out.println("");
        System.out.print("Peso máximo: ");
        System.out.print(m.totalWeight(result));
        System.out.println("");        
        System.out.print("Tiempo de ejecución: ");
        //System.out.print((elapsedTimehs/1000)*60+" segundos");
        System.out.print(elapsedTimehs+" milisegundos");
        System.out.println("");        
    }
    
    public static void executeHillClimbingFAJI(int instanceSize, Knapsack m){
        //temporary
//        int[] tmp = {1,0,1,0,1,0,1,1,1,0,0,0,0,1,1};
//        System.out.println(m.evaluateOF(tmp));        
        //Hill Climbing
        HillClimbingFirstAscent hc = new HillClimbingFirstAscent(instanceSize, m);
        long startTimeHC = System.currentTimeMillis();
        int[] resultHC = hc.climbTheHill();
        long currentTimeHC = System.currentTimeMillis(); 
        long elapsedTimeHC = (currentTimeHC-startTimeHC);
        //int[] resultHC = new int[instanceSize];
        //System.arraycopy(hc.getGlobal(), 0, resultHC, 0, instanceSize);
        int counter3 = 0;
        for(int k=0; k<instanceSize; k++){
            if(resultHC[k] == 1){
                counter3++;
            }
            System.out.print(resultHC[k]);
        }
        System.out.println("");
        System.out.print("HILL CLIMBING FIRST ASCENT ACCEPTING EQUALS");
        System.out.println("");        
        System.out.print("Entraron "+counter3+" elementos en la mochila");
        System.out.println("");
        System.out.print("Beneficio máximo: ");
        System.out.print(m.evaluateOF(resultHC));
        System.out.println("");
        System.out.print("Peso máximo: ");
        System.out.print(m.totalWeight(resultHC));
        System.out.println("");        
        System.out.print("Tiempo de ejecución: ");
        //System.out.print((elapsedTimeHC/1000)*60+" segundos");
        System.out.print(elapsedTimeHC+" milisegundos");
        System.out.println("");
                
    }

    private static void executeHillClimbingBAJI(int instanceSize, Knapsack m, int QuantityIteration){
        HillClimbingBestAscent hc = new HillClimbingBestAscent(instanceSize, m, QuantityIteration);
        long startTimeHC = System.currentTimeMillis();
        int[] resultHC = hc.climbTheHill();
        long currentTimeHC = System.currentTimeMillis();
        int counter3 = 0;
        for(int k=0; k<instanceSize; k++){
            if(resultHC[k] == 1){
                counter3++;
            }
            System.out.print(resultHC[k]);
        }
        System.out.println("");
        System.out.print("HILL CLIMBING BEST ASCENT ACCEPTING EQUALS");
        System.out.println("");
        System.out.print("Entraron "+counter3+" elementos en la mochila");
        System.out.println("");
        System.out.print("Beneficio máximo: ");
        System.out.print(m.evaluateOF(resultHC));
        System.out.println("");
        System.out.print("Peso máximo: ");
        System.out.print(m.totalWeight(resultHC));
        System.out.println("");
        System.out.print("Tiempo de ejecución: ");
        System.out.print(((currentTimeHC-startTimeHC)/1000*60)+" milisegundos");
        System.out.println("");

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try{
            ArrayList<Item> items =  Reader.readKPInstance("src//main//resources//test_instance.json");
            Knapsack kp = new Knapsack(254, items);
          executeHarmonySearch(9, kp);
        }catch (Exception ex) {
           System.out.println(ex.getMessage());
        }
    }
    
}
