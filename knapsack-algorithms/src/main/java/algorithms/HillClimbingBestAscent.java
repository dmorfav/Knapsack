package algorithms;

import kp_hs.Constants;
import model.Knapsack;

public class HillClimbingBestAscent {
    /*************** Instance ******************/
    //Instance size
    private final int NVAR;
    //Problem instance
    private final Knapsack model;
    //Solution vector
    private final int[] solution;
    //Quantity of iteration
    private final int iteration;
    //Best global vector solution
    private int[] global;
    //Best profit find
    private float bestProfit;

    public HillClimbingBestAscent(Knapsack model) {
        this.NVAR = model.instanceSize();
        this.model = model;
        this.solution = new int[NVAR];
        this.global = new int[NVAR];
        this.iteration = Constants.Iterations;
        this.bestProfit = 0f;
    }

    private void generateInitialSolution() {
        for (int i = 0; i < NVAR; i++) {
            //this.solution[i] = (int) (Math.random() * 2);
            this.solution[i] = 0;
        }
    }

    private boolean checkAllActiveState() {
        for (int i = 0; i < NVAR; i++) {
            this.solution[i] = 1;
        }
        if (model.evaluateRestriction(solution)) {
            System.arraycopy(solution, 0, this.global, 0, NVAR);
            return true;
        } else {
            return false;
        }
    }

    private void evaluateSolution(int[] solution) {
        if (model.evaluateRestriction(solution)) {
            float current = model.evaluateOF(solution);
            if (this.bestProfit <= current) {
                this.bestProfit = current;
                System.arraycopy(solution, 0, this.global, 0, NVAR);
            }
        }
    }

    private void SearchBestNeighborhoodSolution(int[] solution) {
        for (int i = 0; i < solution.length; i++) {
            solution[i] = (solution[i] == 0) ? 1 : 0;
            if (i > 0)
                solution[i - 1] = (solution[i - 1] == 0) ? 1 : 0;
            this.evaluateSolution(solution);
        }
    }

    public int[] climbTheHill() {

        if (this.checkAllActiveState())
            return this.global;

        boolean band = false;
        int i = 0;
        while (!band) {
            generateInitialSolution();
            if (model.evaluateRestriction(this.solution))
                band = true;
        }

        while (i < this.iteration) {
            this.evaluateSolution(solution);
            this.SearchBestNeighborhoodSolution(this.solution);
            i++;
        }
        return this.global;
    }
    
    @Override
    public String toString() {
    	return "HillClimbingBestAscent";
    }

}
