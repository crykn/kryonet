package com.esotericsoftware.kryonet.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.BiConsumer;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener.TypeListener;
import org.junit.jupiter.api.Test;

public class TypeListenerTest {

	private String i = null;
	private Integer j = 0;

	@Test
	public void test() {
		TypeListener listener = new TypeListener();

		// Basic stuff
		assertEquals(0, listener.size());

		// One consumer
		BiConsumer<Connection, String> cons1 = (t, u) -> i = u;
		listener.addTypeHandler(String.class, cons1);

		listener.received(null, "test");
		assertEquals("test", i);
		assertEquals(1, listener.size());

		// Add a consumer twice
		listener.addTypeHandler(String.class, cons1);
		assertEquals(1, listener.size());

		// Multiple Consumers
		listener.addTypeHandler(Integer.class, (t, u) -> j = u);

		listener.received(null, 2);
		assertEquals((Integer) 2, j);
		assertEquals(2, listener.size());

		// Remove & Clear
		listener.removeTypeHandler(String.class);
		assertEquals(1, listener.size());

		listener.clear();
		assertEquals(0, listener.size());

		// Numbers unchanged
		listener.received(null, 4);
		assertEquals((Integer) 2, j);
	}

}
