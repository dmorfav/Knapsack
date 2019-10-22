package algorithms;

import classes.Model;

/**
 *
 * @author dmorfav
 */
public class DiscreteBinaryHarmonySearch {
    /******* Harmony Memory ******/
    private final int[][] HM;  
    //Harmony Memory Size
    private final int HMS;
    
    /**** Improvisation Control Parameters *****/
    //Harmony Consideration Rate
    private final double HMCR;
    //Pitch Adjustment Rate
    private final double PAR;
    //Pitch Adjustment BandWitdh
    private final double BW;
    
    /********** Bounds parameters ***************/
    //Maximum number of iterations
    private final int maxIter; 
    
    /*************** Instance ******************/
    //Instance size
    private final int NVAR;
    //Problem instance
    private final Model model;  

    public DiscreteBinaryHarmonySearch(int instSize, Model m) {
        //Setting parameters        
        HMS = 30;
        HMCR = 0.8;
        PAR = 0.4;
        BW = 0.08;
        maxIter = 20000;
        NVAR = instSize;
        //Just 2 decision variables with 0|1 min and max values
        HM = new int[HMS][NVAR];
        
        model = m; 
    }
    
    public int rand(){
        return (int)(Math.random()* 2);
    }
    
    public double probabilityRand(){
        return (double)(Math.random()* 1);
    }   
    
    public int randNeighbour(){
        return (int)(Math.random()* NVAR);
    }
    
    public int randMemory(){
        return (int)(Math.random()* HMS);
    }    
    
    public int randVal(int low, int high){
        return (int)(Math.floor(probabilityRand()*(high-low)+low+probabilityRand()));
    }    
    
    public void sortHM(){
        for(int i=0; i<HMS-1; i++){
            if(model.evaluateOF(HM[i]) < model.evaluateOF(HM[i+1])){
                //exchange elements if next is better than current
                int[] tmp = new int[NVAR];
                System.arraycopy(HM[i], 0, tmp, 0, NVAR-1);
                System.arraycopy(HM[i+1], 0, HM[i], 0, NVAR-1);
                System.arraycopy(tmp, 0, HM[i+1], 0, NVAR-1);           
            }            
        }
    }
    
    private void initializeHM(){
        //HM is a matrix of HMS (HM size) x N (dimension of the problem)
        int i = 0;
        while(i<HMS){
            int harmony[] = new int[NVAR];
            for(int j=0; j<NVAR; j++){                
                harmony[j] = rand();
            }            
            //if the harmony is tuned then assign it to Harmony Memory
            if(model.evaluateRestriction(harmony)){
                System.arraycopy(harmony, 0, HM[i], 0, NVAR-1);
            }else{
                //repair the vector
                int j = 0;
                while (!model.evaluateRestriction(harmony)) {
                    int rand = (int)(Math.random()* NVAR);
                    if(harmony[rand] == 1){
                       harmony[rand] = 0; 
                    }
                    
                }
                System.arraycopy(harmony, 0, HM[i], 0, NVAR-1);
            }
            i++;
        }
        //Sorting HM descending (maximazing profit)
        sortHM();       
    }    
    
    private void memoryUpdate(int bestHarmony[]){
        //Worst Harmony is always at the end so...
        if(model.evaluateOF(bestHarmony) > model.evaluateOF(HM[HMS-1])){
            //Exclude worst, include new best and sort again
            System.arraycopy(bestHarmony, 0, HM[HMS-1], 0, NVAR);
            sortHM();
        }
    }         
    
    private int[] createNewHarmony(){
        //Create harmony with HMCR, PAR and BW
        int[] result = new int[NVAR+1];
        for(int i=0; i<NVAR; i++){
            double rand = probabilityRand();
            if(rand <= HMCR){
                //Solution from Harmony Memory random
                //Select the bit on position i (column), at any HM position
                int position = randMemory();
                result[i] = HM[position][i];
                if(rand<= PAR){
                    result[i] = HM[0][i];
                }
            }else{
                if(rand<=0.5){
                    result[i] = 0;
                }else{
                    result[i] = 1;
                }
            }
        }          
        return result;
    }
    
    public int[] HarmonySearchAlgorithm(){                        
        //Initialize HM
        initializeHM();
        int i = 0;
        //long startTimeHC = System.currentTimeMillis(); 
        while(i < maxIter){
            //create new harmony with parameters
            int[] newHarmony = createNewHarmony();
            //evaluate created harmony
            if(model.evaluateRestriction(newHarmony)){
                memoryUpdate(newHarmony);           
            }
            i++;          
        }
        return HM[0];
    }    
    
}
