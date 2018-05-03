package net.runelite.rs.api;

import net.runelite.api.GraphicsObject;
import net.runelite.mapping.Import;

public interface RSGraphicsObject extends GraphicsObject
{
	@Import("id")
	@Override
	int getId();

	@Import("x")
	@Override
	int getX();

	@Import("y")
	@Override
	int getY();

	@Import("startCycle")
	@Override
	int getStartCycle();

	@Import("level")
	@Override
	int getLevel();

	@Import("height")
	@Override
	int getHeight();
}
