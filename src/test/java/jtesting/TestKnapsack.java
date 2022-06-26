package jtesting;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.Item;
import model.Knapsack;

class TestKnapsack {

	Knapsack knapsack = new Knapsack();
	ArrayList<Item> items = new ArrayList<>();
	int allow[] = {1,1,1,0};

	@BeforeEach
	void setUp() throws Exception {
		items.add(new Item(10, 10));
		items.add(new Item(20, 20));
		items.add(new Item(30, 30));
		items.add(new Item(40, 40));
	}

	@Test
	void testCreateKnapSack() {
		Knapsack kp = new Knapsack(100, items);
		assertEquals(100, kp.getCapacity());
		assertEquals(60, kp.totalWeight(this.allow));
	}
	
	@Test
	void testKnapSackSize() {
		Knapsack kp = new Knapsack(100, items);
		assertEquals(4, kp.instanceSize());
	}
	
	@Test
	void testAccessData() {
		Knapsack kp = new Knapsack(100, items);
		assertEquals(100, kp.getCapacity());
		assertEquals(60, kp.totalWeight(this.allow));
		assertEquals(60, kp.evaluateOF(this.allow));
	}
	
	@Test
	void testEvaluateRestrinctionSuccess() {
		Knapsack kp = new Knapsack(100, items);
		assertTrue(kp.evaluateRestriction(this.allow));
	}
	
	@Test
	void testEvaluateRestrinctionFail() {
		Knapsack kp = new Knapsack(50, items);
		assertFalse(kp.evaluateRestriction(new int[]{1,1,1,1}));
	}

}
