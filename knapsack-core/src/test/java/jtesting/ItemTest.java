package jtesting;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.Item;

class ItemTest {

	Item item = new Item();

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void testSetWeight() {
		this.item.setWeight(10);
		assertEquals(10, this.item.getWeight());
	}

	@Test
	void testSetProfit() {
		this.item.setProfit(10);
		assertEquals(10, this.item.getProfit());
	}

	@Test
	void testcreateItem() {
		Item newItem = new Item(20, 20);
		assertEquals(20, newItem.getWeight());
		assertEquals(20, newItem.getProfit());
	}


	@Test
	void testHashCode() {
		assertEquals(5887, this.item.hashCode());
	}

	@Test
	void testEqual() {
		assertTrue(this.item.equals(this.item));
	}
	
	@Test
	void testEqualNull() {
		assertFalse(this.item.equals(null));
	}
	
	@Test
	void testEqualOtherClass() {
		assertFalse(this.item.equals(new Object()));
	}
	
	@Test
	void testEqualOtherWeight() {
		assertFalse(this.item.equals(new Item(20, 20)));
	}

}
