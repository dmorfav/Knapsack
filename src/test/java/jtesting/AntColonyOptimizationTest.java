package jtesting;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import algorithms.AntColonyOptimization;
import model.Item;
import model.Knapsack;

class AntColonyOptimizationTest {

	Knapsack knapsack;
	ArrayList<Item> items = new ArrayList<>();

	@BeforeEach
	void setUp() throws Exception {
		items.add(new Item(10, 10));
		items.add(new Item(20, 20));
		items.add(new Item(30, 30));
		items.add(new Item(40, 40));
		knapsack = new Knapsack(100, items);
	}

	@Test
	void testACOConstructor() {
		// Verify correct initialization
		AntColonyOptimization aco = new AntColonyOptimization(knapsack);
		assertNotNull(aco);
		assertEquals("AntColonyOptimization", aco.toString());
	}

	@Test
	void testACOAlgorithmProducesValidSolution() {
		// Run the algorithm
		AntColonyOptimization aco = new AntColonyOptimization(knapsack);
		int[] solution = aco.antColonyOptimizationAlgorithm();
		
		// Verify solution is valid (meets capacity constraint)
		assertTrue(knapsack.evaluateRestriction(solution));
		
		// Verify solution has correct length
		assertEquals(knapsack.instanceSize(), solution.length);
	}

	@Test
	void testACOAlgorithmProducesNonZeroSolution() {
		// Run the algorithm
		AntColonyOptimization aco = new AntColonyOptimization(knapsack);
		int[] solution = aco.antColonyOptimizationAlgorithm();
		
		// Verify solution has some items selected (not all zeros)
		boolean hasItems = false;
		for (int i = 0; i < solution.length; i++) {
			if (solution[i] == 1) {
				hasItems = true;
				break;
			}
		}
		assertTrue(hasItems, "Solution should have at least one item selected");
		
		// Verify objective function value is positive
		assertTrue(knapsack.evaluateOF(solution) > 0);
	}

	@Test
	void testACOWithSmallCapacity() {
		// Create a knapsack with small capacity
		Knapsack smallKp = new Knapsack(15, items);
		AntColonyOptimization aco = new AntColonyOptimization(smallKp);
		int[] solution = aco.antColonyOptimizationAlgorithm();
		
		// Solution should still be valid
		assertTrue(smallKp.evaluateRestriction(solution));
		assertTrue(smallKp.totalWeight(solution) <= 15);
	}

	@Test
	void testACOWithLargeCapacity() {
		// Create a knapsack with large capacity to fit all items
		Knapsack largeKp = new Knapsack(200, items);
		AntColonyOptimization aco = new AntColonyOptimization(largeKp);
		int[] solution = aco.antColonyOptimizationAlgorithm();
		
		// Solution should be valid
		assertTrue(largeKp.evaluateRestriction(solution));
		
		// With large capacity, should be able to fit many items
		float profit = largeKp.evaluateOF(solution);
		assertTrue(profit > 0);
	}
}
