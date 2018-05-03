/*
 * Copyright (c) 2017, Kronos <https://github.com/KronosDesign>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.devtools;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginToolbar;
import net.runelite.client.ui.overlay.Overlay;

@PluginDescriptor(
	name = "Developer Tools",
	developerPlugin = true
)
@Slf4j
public class DevToolsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private PluginToolbar pluginToolbar;

	@Inject
	private DevToolsOverlay overlay;

	@Inject
	private LocationOverlay locationOverlay;

	@Inject
	private SceneOverlay sceneOverlay;

	@Inject
	private EventBus eventBus;

	private boolean togglePlayers;
	private boolean toggleNpcs;
	private boolean toggleGroundItems;
	private boolean toggleGroundObjects;
	private boolean toggleGameObjects;
	private boolean toggleWalls;
	private boolean toggleDecor;
	private boolean toggleInventory;
	private boolean toggleProjectiles;
	private boolean toggleLocation;
	private boolean toggleChunkBorders;
	private boolean toggleMapSquares;
	private boolean toggleValidMovement;
	private boolean toggleLineOfSight;
	private boolean toggleGraphicsObjects;

	Widget currentWidget;
	int itemIndex = -1;

	private Font font;
	private NavigationButton navButton;

	@Provides
	DevToolsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DevToolsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		final DevToolsPanel panel = injector.getInstance(DevToolsPanel.class);

		BufferedImage icon;
		synchronized (ImageIO.class)
		{
			icon = ImageIO.read(getClass().getResourceAsStream("devtools_icon.png"));
		}

		navButton = NavigationButton.builder()
			.name("Developer Tools")
			.icon(icon)
			.panel(panel)
			.build();

		pluginToolbar.addNavigation(navButton);

		font = FontManager.getRunescapeFont()
			.deriveFont(Font.BOLD, 16);
	}

	@Override
	protected void shutDown() throws Exception
	{
		pluginToolbar.removeNavigation(navButton);
	}

	@Override
	public Collection<Overlay> getOverlays()
	{
		return Arrays.asList(overlay, locationOverlay, sceneOverlay);
	}

	@Subscribe
	public void onCommand(CommandExecuted commandExecuted)
	{
		String[] args = commandExecuted.getArguments();

		switch (commandExecuted.getCommand())
		{
			case "getvar":
			{
				int varbit = Integer.parseInt(args[0]);
				int value = client.getVarbitValue(varbit);
				client.addChatMessage(ChatMessageType.SERVER, "", "Varbit " + varbit + ": " + value, null);
				break;
			}
			case "setvar":
			{
				int varbit = Integer.parseInt(args[0]);
				int value = Integer.parseInt(args[1]);
				client.setVarbitValue(varbit, value);
				client.addChatMessage(ChatMessageType.SERVER, "", "Set varbit " + varbit + " to " + value, null);
				eventBus.post(new VarbitChanged()); // fake event
				break;
			}
		}
	}

	Font getFont()
	{
		return font;
	}

	void togglePlayers()
	{
		togglePlayers = !togglePlayers;
	}

	void toggleNpcs()
	{
		toggleNpcs = !toggleNpcs;
	}

	void toggleGroundItems()
	{
		toggleGroundItems = !toggleGroundItems;
	}

	void toggleGroundObjects()
	{
		toggleGroundObjects = !toggleGroundObjects;
	}

	void toggleGameObjects()
	{
		toggleGameObjects = !toggleGameObjects;
	}

	void toggleWalls()
	{
		toggleWalls = !toggleWalls;
	}

	void toggleDecor()
	{
		toggleDecor = !toggleDecor;
	}

	void toggleInventory()
	{
		toggleInventory = !toggleInventory;
	}

	void toggleProjectiles()
	{
		toggleProjectiles = !toggleProjectiles;
	}

	void toggleLocation()
	{
		toggleLocation = !toggleLocation;
	}

	void toggleChunkBorders()
	{
		toggleChunkBorders = !toggleChunkBorders;
	}

	void toggleMapSquares()
	{
		toggleMapSquares = !toggleMapSquares;
	}

	void toggleValidMovement()
	{
		toggleValidMovement = !toggleValidMovement;
	}

	void toggleLineOfSight()
	{
		toggleLineOfSight = !toggleLineOfSight;
	}

	void toggleGraphicsObjects()
	{
		toggleGraphicsObjects = !toggleGraphicsObjects;
	}

	boolean isTogglePlayers()
	{
		return togglePlayers;
	}

	boolean isToggleNpcs()
	{
		return toggleNpcs;
	}

	boolean isToggleGroundItems()
	{
		return toggleGroundItems;
	}

	boolean isToggleGroundObjects()
	{
		return toggleGroundObjects;
	}

	boolean isToggleGameObjects()
	{
		return toggleGameObjects;
	}

	boolean isToggleWalls()
	{
		return toggleWalls;
	}

	boolean isToggleDecor()
	{
		return toggleDecor;
	}

	boolean isToggleInventory()
	{
		return toggleInventory;
	}

	boolean isToggleProjectiles()
	{
		return toggleProjectiles;
	}

	boolean isToggleLocation()
	{
		return toggleLocation;
	}

	boolean isToggleChunkBorders()
	{
		return toggleChunkBorders;
	}

	boolean isToggleMapSquares()
	{
		return toggleMapSquares;
	}

	boolean isToggleValidMovement()
	{
		return toggleValidMovement;
	}

	boolean isToggleLineOfSight()
	{
		return toggleLineOfSight;
	}

	boolean isToggleGraphicsObjects()
	{
		return toggleGraphicsObjects;
	}
}
