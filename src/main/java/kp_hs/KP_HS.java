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

	/**
	 * @param args the command line arguments
	 */
	
	public static void main(String[] args) {
		try{
			Reader.createRandomInstance("instance.json",20, 1, 100);
			ArrayList<Item> items =  Reader.readKPInstance("src//main//resources//instances//20_instance.json");
			Knapsack kp = new Knapsack(100, items);
			int[] result = new int[kp.instanceSize()];
			long startTime = 0;
			long finishTime = 0;
			int algorithm = -1;
			String algorithmName = null;

			switch(algorithm){
				case 1: {
					HarmonySearch hs = new HarmonySearch(kp);
					algorithmName = hs.toString();
					startTime = System.currentTimeMillis();
					result = hs.HarmonySearchAlgorithm();
					finishTime = System.currentTimeMillis();
					break;
				}
				case 2: {
					 ImprovedHarmonySearch ihs = new ImprovedHarmonySearch(kp);
					algorithmName = ihs.toString();
					startTime = System.currentTimeMillis();
					result = ihs.HarmonySearchAlgorithm();
					finishTime = System.currentTimeMillis();
					break;
				}
				case 3: {
					DiscreteBinaryHarmonySearch dbhs = new DiscreteBinaryHarmonySearch(kp);
					algorithmName = dbhs.toString();
					startTime = System.currentTimeMillis();
					result = dbhs.HarmonySearchAlgorithm();
					finishTime = System.currentTimeMillis();
					break;
				}
				case 4: {
					HillClimbingFirstAscent hcfa = new HillClimbingFirstAscent(kp);
					algorithmName = hcfa.toString();
					startTime = System.currentTimeMillis();
					result = hcfa.climbTheHill();
					finishTime = System.currentTimeMillis();
					break;
				}
				case 5: {
					 HillClimbingBestAscent hcba = new HillClimbingBestAscent(kp);
					algorithmName = hcba.toString();
					startTime = System.currentTimeMillis();
					result = hcba.climbTheHill();
					finishTime = System.currentTimeMillis();
					break;
				}
				default: {
					HarmonySearch hs = new HarmonySearch(kp);
					algorithmName = hs.toString();
					startTime = System.currentTimeMillis();
					result = hs.HarmonySearchAlgorithm();
					finishTime = System.currentTimeMillis();
					Summary.print(algorithmName, result, kp, startTime, finishTime);
					System.out.println();
				
					ImprovedHarmonySearch ihs = new ImprovedHarmonySearch(kp);
					algorithmName = ihs.toString();
					startTime = System.currentTimeMillis();
					result = ihs.HarmonySearchAlgorithm();
					finishTime = System.currentTimeMillis();
					Summary.print(algorithmName, result, kp, startTime, finishTime);
					System.out.println();
				
					DiscreteBinaryHarmonySearch dbhs = new DiscreteBinaryHarmonySearch(kp);
					algorithmName = dbhs.toString();
					startTime = System.currentTimeMillis();
					result = dbhs.HarmonySearchAlgorithm();
					finishTime = System.currentTimeMillis();
					Summary.print(algorithmName, result, kp, startTime, finishTime);
					System.out.println();
				
					HillClimbingFirstAscent hcfa = new HillClimbingFirstAscent(kp);
					algorithmName = hcfa.toString();
					startTime = System.currentTimeMillis();
					result = hcfa.climbTheHill();
					finishTime = System.currentTimeMillis();
					Summary.print(algorithmName, result, kp, startTime, finishTime);
					System.out.println();
				
					HillClimbingBestAscent hcbas = new HillClimbingBestAscent(kp);
					algorithmName = hcbas.toString();
					startTime = System.currentTimeMillis();
					result = hcbas.climbTheHill();
					finishTime = System.currentTimeMillis();
					Summary.print(algorithmName, result, kp, startTime, finishTime);
					System.out.println();
					break;
				}
			}
			
			if(algorithm != -1) {
				Summary.print(algorithmName, result, kp, startTime, finishTime);
			}
			
		}catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

}
