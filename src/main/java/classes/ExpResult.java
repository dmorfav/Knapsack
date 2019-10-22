/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classes;

/**
 *
 * @author dmorfav
 */
public class ExpResult {
    private int result;
    private int time;

    public ExpResult() {
        super();
    }

    public ExpResult(int result, int time) {
        this.result = result;
        this.time = time;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.result;
        hash = 67 * hash + this.time;
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
        final ExpResult other = (ExpResult) obj;
        if (this.result != other.result) {
            return false;
        }
        if (this.time != other.time) {
            return false;
        }
        return true;
    }
    
    
}
