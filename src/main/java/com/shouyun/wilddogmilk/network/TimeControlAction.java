package com.shouyun.wilddogmilk.network;

/**
 * Server-owned time operations. The client only requests one of these fixed
 * actions and never supplies a tick rate or sprint duration.
 */
public enum TimeControlAction {
	CYCLE(0),
	RESET(1),
	EXTREME(2),
	TOGGLE_FREEZE(3),
	SPRINT(4);

	private final byte id;

	TimeControlAction(int id) {
		this.id = (byte) id;
	}

	public byte id() {
		return id;
	}

	public static TimeControlAction fromId(byte id) {
		for (TimeControlAction action : values()) {
			if (action.id == id) {
				return action;
			}
		}
		return null;
	}
}
