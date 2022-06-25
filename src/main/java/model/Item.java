/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

/**
 *
 * @author dmorfav
 */
public class Item {
    private float profit;
    private float weight;

    public Item() {
    }

    public Item(float profit, float weight) {
        this.profit = profit;
        this.weight = weight;
    }

    public float getProfit() {
        return profit;
    }

    public void setProfit(float profit) {
        this.profit = profit;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Float.floatToIntBits(this.profit);
        hash = 29 * hash + Float.floatToIntBits(this.weight);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Item other = (Item) obj;
        if (Float.floatToIntBits(this.profit) != Float.floatToIntBits(other.profit) || Float.floatToIntBits(this.weight) != Float.floatToIntBits(other.weight)) {
            return false;
        }
        return true;
    }
    
}
