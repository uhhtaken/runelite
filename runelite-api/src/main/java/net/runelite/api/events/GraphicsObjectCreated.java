package net.runelite.api.events;

import lombok.Value;
import net.runelite.api.GraphicsObject;

@Value
public class GraphicsObjectCreated
{
	private final GraphicsObject graphicsObject;
}
