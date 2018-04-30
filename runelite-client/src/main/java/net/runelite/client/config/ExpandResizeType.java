package net.runelite.client.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExpandResizeType
{
	KEEP_WINDOW_SIZE("Keep window size"),
	KEEP_GAME_SIZE("Keep game size");

	private final String type;

	@Override
	public String toString()
	{
		return type;
	}
}
