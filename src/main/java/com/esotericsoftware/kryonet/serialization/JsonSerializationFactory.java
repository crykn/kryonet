package com.esotericsoftware.kryonet.serialization;

import static com.esotericsoftware.minlog.Log.INFO;
import static com.esotericsoftware.minlog.Log.info;

import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;

import com.esotericsoftware.jsonbeans.Json;
import com.esotericsoftware.jsonbeans.JsonException;
import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.ByteBufferOutputStream;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage.DiscoverHost;
import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;
import com.esotericsoftware.kryonet.FrameworkMessage.Ping;
import com.esotericsoftware.kryonet.FrameworkMessage.RegisterTCP;
import com.esotericsoftware.kryonet.FrameworkMessage.RegisterUDP;

public class JsonSerializationFactory implements SerializationFactory {

	private final Serialization SERIALIZATION_INSTANCE;

	@Override
	public Serialization newInstance(Connection connection) {
		return SERIALIZATION_INSTANCE;
	}

	public JsonSerializationFactory() {
		this.SERIALIZATION_INSTANCE = new JsonSerialization();
	}

	public class JsonSerialization implements Serialization {
		private final Json json = new Json();
		private final ByteBufferInputStream byteBufferInputStream = new ByteBufferInputStream();
		private final ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream();
		private final OutputStreamWriter writer = new OutputStreamWriter(
				byteBufferOutputStream);
		private boolean logging = true, prettyPrint = true;
		private byte[] logBuffer = {};

		public JsonSerialization() {
			json.addClassTag("RegisterTCP", RegisterTCP.class);
			json.addClassTag("RegisterUDP", RegisterUDP.class);
			json.addClassTag("KeepAlive", KeepAlive.class);
			json.addClassTag("DiscoverHost", DiscoverHost.class);
			json.addClassTag("Ping", Ping.class);

			json.setWriter(writer);
		}

		public void setLogging(boolean logging, boolean prettyPrint) {
			this.logging = logging;
			this.prettyPrint = prettyPrint;
		}

		@Override
		public synchronized void write(ByteBuffer buffer, Object object) {
			byteBufferOutputStream.setByteBuffer(buffer);
			int start = buffer.position();
			try {
				json.writeValue(object, Object.class, null);
				writer.flush();
			} catch (Exception ex) {
				throw new JsonException("Error writing object: " + object, ex);
			}
			if (INFO && logging) {
				int end = buffer.position();
				buffer.position(start);
				buffer.limit(end);
				int length = end - start;
				if (logBuffer.length < length)
					logBuffer = new byte[length];
				buffer.get(logBuffer, 0, length);
				buffer.position(end);
				buffer.limit(buffer.capacity());
				String message = new String(logBuffer, 0, length);
				if (prettyPrint)
					message = json.prettyPrint(message);
				info("Wrote: " + message);
			}
		}

		@Override
		public synchronized Object read(ByteBuffer buffer) {
			byteBufferInputStream.setByteBuffer(buffer);
			return json.fromJson(Object.class, byteBufferInputStream);
		}

		@Override
		public void writeLength(ByteBuffer buffer, int length) {
			buffer.putInt(length);
		}

		@Override
		public int readLength(ByteBuffer buffer) {
			return buffer.getInt();
		}

		@Override
		public int getLengthLength() {
			return 4;
		}
	}

}
