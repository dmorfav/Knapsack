package algorithms;

import classes.Model;

/**
 *
 * @author geo
 */
public class HillClimbingFirstAscent{
    /*************** Instance ******************/
    //Instance size
    private final int NVAR;   
    //Problem instance
    private final Model model;
    //Solution vector
    private final int[] solution;
    //Best global vector solution
    private int[] global;    
    //Best value of OF
    private float globalBest;   

    public HillClimbingFirstAscent(int NVAR, Model model) {
        this.NVAR = NVAR;
        this.model = model;
        solution = new int[NVAR];   
    }
    
    private void generateInitialSolution(){
        for(int i=0; i<NVAR; i++){
            solution[i] = (int)(Math.random()* 2);
        }
    }
    
    private void generateChildSolution(){
        int position = (int)(Math.random()* NVAR);
        if(solution[position] == 1){
            solution[position] = 0;            
        }else{
            solution[position] = 1; 
        }
    }    
    
    public int[] climbTheHill(){        
        globalBest = 0;
        boolean band = false;
        while(!band){
           generateInitialSolution();
           if(model.evaluateRestriction(solution)){
               band = true;
           }
        }        
        int[] best = new int[NVAR];
        global = new int[NVAR];
        System.arraycopy(solution, 0, best, 0, NVAR);
        System.arraycopy(solution, 0, global, 0, NVAR);
        float bestProfit = model.evaluateOF(solution); 
        globalBest = bestProfit;
        int iteration = 0;
        while(iteration < 20000){
            generateChildSolution();
            if(model.evaluateRestriction(solution)){
                float current = model.evaluateOF(solution);
                if(bestProfit <= current){
                    bestProfit = current;
                    System.arraycopy(solution, 0, best, 0, NVAR);
                    if(globalBest <= bestProfit){
                        globalBest = bestProfit;
                        System.arraycopy(best, 0, global, 0, NVAR);
                    }
                }
            }
            iteration++;
        }
        return global;
    }

    public int[] getGlobal() {
        return global;
    }

    public void setGlobal(int[] global) {
        this.global = global;
    }

    public float getGlobalBest() {
        return globalBest;
    }

    public void setGlobalBest(float globalBest) {
        this.globalBest = globalBest;
    }     
    
}
