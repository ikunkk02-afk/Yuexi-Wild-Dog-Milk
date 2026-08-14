package com.shouyun.wilddogmilk.time;

public enum SprintMode {
	NONE(0),
	NORMAL(1),
	DEEP_TIME(2);

	private final byte id;

	SprintMode(int id) {
		this.id = (byte) id;
	}

	public byte id() {
		return id;
	}

	public static SprintMode fromId(byte id) {
		return switch (id) {
			case 0 -> NONE;
			case 1 -> NORMAL;
			case 2 -> DEEP_TIME;
			default -> null;
		};
	}

	public static SprintMode requireValid(byte id) {
		SprintMode mode = fromId(id);
		if (mode == null) {
			throw new IllegalArgumentException("Invalid sprint mode id: " + id);
		}
		return mode;
	}
}
