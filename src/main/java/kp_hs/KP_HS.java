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
			ArrayList<Item> items =  Reader.readKPInstance("src//main//resources//test_instance.json");
			Knapsack kp = new Knapsack(100, items);
			int[] result = new int[kp.instanceSize()];
			long startTime = 0;
			long finishTime = 0;
			int algorithm = 1;
			String algorithmName = null;

			switch(algorithm){
				case 1: {
					HarmonySearch hs = new HarmonySearch(kp);
					algorithmName = hs.toString();
					startTime = System.currentTimeMillis();
					result = hs.HarmonySearchAlgorithm();
					finishTime = System.currentTimeMillis();      
				}
			}
			Summary.print(algorithmName, result, kp, startTime, finishTime);
		}catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

}
