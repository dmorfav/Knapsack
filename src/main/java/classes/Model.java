/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classes;

import java.util.List;

/**
 *
 * @author dmorfav
 */
public class Model {
    private List<Item> items;
    private final Knapsack kp;

    public Model(List<Item> items, Knapsack kp) {
        this.items = items;
        this.kp = kp;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
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
    
    public boolean evaluateRestriction(int[] assigment){
        float weigthsum = 0;
        for(int i=0; i<items.size(); i++){
            if(assigment[i] == 1){
                weigthsum += items.get(i).getWeight();
            }
        }        
        return (kp.getCapacity() > weigthsum);
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
