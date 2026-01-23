package algorithms;

import kp_hs.Constants;
import model.Item;
import model.Knapsack;

import java.util.ArrayList;
import java.util.List;

/**
 * Ant Colony Optimization algorithm for the Knapsack problem.
 * 
 * This algorithm is inspired by the foraging behavior of ants.
 * Ants deposit pheromones on paths they traverse, and other ants
 * tend to follow paths with higher pheromone concentrations.
 * 
 * @author dmorfav
 */
public class AntColonyOptimization {
    // Colony parameters
    private final int numAnts;              // Number of ants (30-50 recommended)
    private final int maxIterations;        // Maximum iterations

    // Pheromone matrix
    private final double[] pheromoneMatrix;  // pheromoneMatrix[i] = pheromone for item i

    // Control parameters
    private final double alpha;             // Pheromone influence (1.0-2.0)
    private final double beta;              // Heuristic influence (2.0-5.0)
    private final double rho;               // Evaporation rate (0.1-0.3)
    private final double Q;                 // Pheromone deposit constant (100.0)
    private final double tau0;              // Initial pheromone (0.1)

    // Global best solution
    private int[] globalBestSolution;
    private double globalBestValue;

    // Problem instance
    private final Knapsack model;
    private final int NVAR;                 // Number of items

    /**
     * Constructor for Ant Colony Optimization.
     * 
     * @param m The Knapsack problem instance
     */
    public AntColonyOptimization(Knapsack m) {
        // Standard ACO parameters
        this.numAnts = 30;
        this.maxIterations = Constants.Iterations;
        this.alpha = 1.0;
        this.beta = 3.0;
        this.rho = 0.1;
        this.Q = 100.0;
        this.tau0 = 0.1;
        
        // Initialize model
        this.model = m;
        this.NVAR = m.instanceSize();
        
        // Initialize structures
        this.pheromoneMatrix = new double[NVAR];
        this.globalBestSolution = new int[NVAR];
        this.globalBestValue = 0.0;
        
        // Initialize pheromones
        initializePheromones();
    }

    /**
     * Initialize all pheromones with the initial value tau0.
     */
    private void initializePheromones() {
        for (int i = 0; i < NVAR; i++) {
            pheromoneMatrix[i] = tau0;
        }
    }

    /**
     * Construct a solution by having an ant probabilistically select items.
     * 
     * @return A binary array where solution[i]=1 indicates item i is selected
     */
    private int[] constructSolution() {
        int[] solution = new int[NVAR];
        float currentWeight = 0.0f;
        
        // Get items sorted by heuristic (value/weight ratio)
        List<Integer> availableItems = getItemsSortedByHeuristic();
        
        // Build solution item by item
        for (Integer itemIndex : availableItems) {
            // Calculate probability of selecting this item
            double probability = calculateSelectionProbability(
                itemIndex, 
                solution, 
                currentWeight
            );
            
            // Probabilistic decision
            if (Math.random() < probability) {
                // Check capacity constraint
                Item item = model.getItem(itemIndex);
                if (currentWeight + item.getWeight() <= model.getCapacity()) {
                    solution[itemIndex] = 1;
                    currentWeight += item.getWeight();
                }
            }
        }
        
        return solution;
    }

    /**
     * Calculate the probability of selecting an item based on pheromone and heuristic information.
     * 
     * @param itemIndex The index of the item to consider
     * @param currentSolution The current partial solution
     * @param currentWeight The current total weight
     * @return Probability of selecting the item (0.0 to 1.0)
     */
    private double calculateSelectionProbability(int itemIndex, int[] currentSolution, float currentWeight) {
        // Get item
        Item item = model.getItem(itemIndex);
        
        // If exceeds capacity, probability = 0
        if (currentWeight + item.getWeight() > model.getCapacity()) {
            return 0.0;
        }
        
        // If already selected, probability = 0
        if (currentSolution[itemIndex] == 1) {
            return 0.0;
        }
        
        // Guard against division by zero
        if (item.getWeight() == 0) {
            return 0.0;
        }
        
        // Calculate heuristic information (value/weight ratio)
        double eta = item.getProfit() / item.getWeight();
        
        // Get pheromone
        double tau = pheromoneMatrix[itemIndex];
        
        // Calculate numerator: tau^alpha * eta^beta
        double numerator = Math.pow(tau, alpha) * Math.pow(eta, beta);
        
        // Calculate denominator (sum over available items)
        double denominator = 0.0;
        for (int j = 0; j < NVAR; j++) {
            if (currentSolution[j] == 0) {
                Item otherItem = model.getItem(j);
                if (currentWeight + otherItem.getWeight() <= model.getCapacity() && otherItem.getWeight() > 0) {
                    double otherEta = otherItem.getProfit() / otherItem.getWeight();
                    double otherTau = pheromoneMatrix[j];
                    denominator += Math.pow(otherTau, alpha) * Math.pow(otherEta, beta);
                }
            }
        }
        
        // Avoid division by zero
        if (denominator == 0.0) {
            return 0.0;
        }
        
        return numerator / denominator;
    }

    /**
     * Get list of item indices sorted by heuristic (value/weight ratio) in descending order.
     * 
     * @return List of item indices sorted by profitability
     */
    private List<Integer> getItemsSortedByHeuristic() {
        // Create list of indices
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < NVAR; i++) {
            indices.add(i);
        }
        
        // Sort by value/weight ratio (descending)
        indices.sort((a, b) -> {
            Item itemA = model.getItem(a);
            Item itemB = model.getItem(b);
            // Guard against division by zero
            double ratioA = itemA.getWeight() > 0 ? itemA.getProfit() / itemA.getWeight() : 0.0;
            double ratioB = itemB.getWeight() > 0 ? itemB.getProfit() / itemB.getWeight() : 0.0;
            return Double.compare(ratioB, ratioA);
        });
        
        return indices;
    }

    /**
     * Update pheromones using evaporation and deposition.
     * 
     * @param allSolutions Array of all ant solutions from current iteration
     * @param allValues Array of objective function values for all solutions
     */
    private void updatePheromones(int[][] allSolutions, double[] allValues) {
        // 1. Evaporation
        for (int i = 0; i < NVAR; i++) {
            pheromoneMatrix[i] *= (1.0 - rho);
        }
        
        // 2. Pheromone deposit
        for (int k = 0; k < numAnts; k++) {
            int[] solution = allSolutions[k];
            double value = allValues[k];
            
            // Amount of pheromone to deposit
            double deltaTau = Q / (1.0 + Math.abs(globalBestValue - value));
            
            // Deposit on selected items
            for (int i = 0; i < NVAR; i++) {
                if (solution[i] == 1) {
                    pheromoneMatrix[i] += deltaTau;
                }
            }
        }
        
        // 3. Reinforce global best solution (elitism)
        if (globalBestValue > 0) {
            double elitistBonus = Q / globalBestValue;
            for (int i = 0; i < NVAR; i++) {
                if (globalBestSolution[i] == 1) {
                    pheromoneMatrix[i] += elitistBonus * 2.0; // Elitist factor
                }
            }
        }
        
        // 4. Limit pheromones (avoid stagnation)
        for (int i = 0; i < NVAR; i++) {
            pheromoneMatrix[i] = Math.max(tau0 / 10.0, 
                                  Math.min(pheromoneMatrix[i], tau0 * 100.0));
        }
    }

    /**
     * Execute the Ant Colony Optimization algorithm to solve the knapsack problem.
     * 
     * The algorithm works in the following phases:
     * 1. Initialize pheromones
     * 2. For each iteration:
     *    a. Each ant constructs a solution probabilistically
     *    b. Evaluate and repair solutions if necessary
     *    c. Update global best solution
     *    d. Update pheromones (evaporation + deposit)
     * 3. Return the best solution found
     * 
     * @return Binary array where solution[i]=1 indicates item i is in the knapsack
     */
    public int[] antColonyOptimizationAlgorithm() {
        // Initialize best solution
        globalBestValue = 0.0;
        
        // Main iterations
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            // Store solutions from all ants
            int[][] solutions = new int[numAnts][NVAR];
            double[] values = new double[numAnts];
            
            // Each ant constructs a solution
            for (int k = 0; k < numAnts; k++) {
                solutions[k] = constructSolution();
                
                // Evaluate solution
                if (model.evaluateRestriction(solutions[k])) {
                    values[k] = model.evaluateOF(solutions[k]);
                    
                    // Update global best solution
                    if (values[k] > globalBestValue) {
                        globalBestValue = values[k];
                        System.arraycopy(solutions[k], 0, globalBestSolution, 0, NVAR);
                    }
                } else {
                    // Invalid solution, repair it
                    solutions[k] = repairSolution(solutions[k]);
                    values[k] = model.evaluateOF(solutions[k]);
                    
                    // Update global best if repaired solution is better
                    if (values[k] > globalBestValue) {
                        globalBestValue = values[k];
                        System.arraycopy(solutions[k], 0, globalBestSolution, 0, NVAR);
                    }
                }
            }
            
            // Update pheromones
            updatePheromones(solutions, values);
            
            // Optional: Apply local search to best solution every 10 iterations
            if (iteration % 10 == 0 && globalBestValue > 0) {
                globalBestSolution = localSearch(globalBestSolution);
                globalBestValue = model.evaluateOF(globalBestSolution);
            }
        }
        
        return globalBestSolution;
    }

    /**
     * Repair an invalid solution by removing items until constraint is satisfied.
     * 
     * @param solution The solution to repair
     * @return A valid solution
     */
    private int[] repairSolution(int[] solution) {
        // Remove items until constraint is met
        while (!model.evaluateRestriction(solution)) {
            // Find item with worst value/weight ratio that is selected
            int worstItem = -1;
            double worstRatio = Double.MAX_VALUE;
            
            for (int i = 0; i < NVAR; i++) {
                if (solution[i] == 1) {
                    Item item = model.getItem(i);
                    // Guard against division by zero
                    double ratio = item.getWeight() > 0 ? item.getProfit() / item.getWeight() : Double.MAX_VALUE;
                    if (ratio < worstRatio) {
                        worstRatio = ratio;
                        worstItem = i;
                    }
                }
            }
            
            // Remove worst item
            if (worstItem != -1) {
                solution[worstItem] = 0;
            } else {
                break; // Should not happen
            }
        }
        
        return solution;
    }

    /**
     * Apply local search to improve a solution using 1-1 swaps.
     * 
     * @param solution The solution to improve
     * @return An improved solution
     */
    private int[] localSearch(int[] solution) {
        boolean improved = true;
        int[] currentSolution = solution.clone();
        double currentValue = model.evaluateOF(currentSolution);
        
        while (improved) {
            improved = false;
            
            // Try 1-1 swaps
            for (int i = 0; i < NVAR; i++) {
                if (currentSolution[i] == 1) {
                    // Item included, try swapping with non-included item
                    for (int j = 0; j < NVAR; j++) {
                        if (currentSolution[j] == 0) {
                            // Create neighbor
                            int[] neighbor = currentSolution.clone();
                            neighbor[i] = 0;
                            neighbor[j] = 1;
                            
                            // Evaluate neighbor
                            if (model.evaluateRestriction(neighbor)) {
                                double neighborValue = model.evaluateOF(neighbor);
                                if (neighborValue > currentValue) {
                                    currentSolution = neighbor;
                                    currentValue = neighborValue;
                                    improved = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (improved) break;
                }
            }
        }
        
        return currentSolution;
    }

    @Override
    public String toString() {
        return "AntColonyOptimization";
    }
}
