/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dmorfav
 */
public class Knapsack {
    private final float capacity;
    private final List<Item> items;

    public Knapsack() {
    	this.capacity = 0;
		this.items = null;
    }

    public Knapsack(float capacity, ArrayList<Item> items) {
        this.capacity = capacity;
		this.items = items;
    }

    public float getCapacity() {
        return capacity;
    }
    
    public float evaluateOF(int[] assigment){
        float totalprofit = 0;
        for(int i=0; i<items.size(); i++){
            if(assigment[i] == 1){
                totalprofit += items.get(i).getProfit();
            }
        }  
        return totalprofit;
    } 
    
    public int instanceSize() {
		return items.size();
	}
    
    public boolean evaluateRestriction(int[] assigment){
        float weigthsum = 0;
        for(int i=0; i<items.size(); i++){
            if(assigment[i] == 1){
                weigthsum += items.get(i).getWeight();
            }
        }        
        return (weigthsum <= this.getCapacity());
    } 
    
    public float totalWeight(int[] assigment){
        float totalprofit = 0;
        for(int i=0; i<items.size(); i++){
            if(assigment[i] == 1){
                totalprofit += items.get(i).getWeight();
            }
        }  
        return totalprofit;
    }      

}
