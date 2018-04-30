/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
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
package net.runelite.client.ui;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Point;
import net.runelite.api.events.ConfigChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ExpandResizeType;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.events.ClientUILoaded;
import net.runelite.client.events.PluginToolbarButtonAdded;
import net.runelite.client.events.PluginToolbarButtonRemoved;
import net.runelite.client.events.TitleToolbarButtonAdded;
import net.runelite.client.events.TitleToolbarButtonRemoved;
import net.runelite.client.input.KeyManager;
import net.runelite.client.util.OSType;
import net.runelite.client.util.OSXUtil;
import net.runelite.client.util.SwingUtil;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;
import org.pushingpixels.substance.internal.utils.SubstanceCoreUtilities;
import org.pushingpixels.substance.internal.utils.SubstanceTitlePaneUtilities;

/**
 * Client UI.
 */
@Slf4j
@Singleton
public class ClientUI
{
	private static final String CONFIG_GROUP = "runelite";
	private static final String CONFIG_CLIENT_BOUNDS = "clientBounds";
	private static final String CONFIG_CLIENT_MAXIMIZED = "clientMaximized";
	private static final int CLIENT_WELL_HIDDEN_MARGIN = 160;
	private static final int CLIENT_WELL_HIDDEN_MARGIN_TOP = 10;
	private static final int SCREEN_EDGE_CLOSE_DISTANCE = 40;
	public static final BufferedImage ICON;
	private static final BufferedImage SIDEBAR_OPEN;
	private static final BufferedImage SIDEBAR_CLOSE;

	static
	{
		BufferedImage icon;
		BufferedImage sidebarOpen;
		BufferedImage sidebarClose;

		try
		{
			synchronized (ImageIO.class)
			{
				icon = ImageIO.read(ClientUI.class.getResourceAsStream("/runelite.png"));
				sidebarOpen = ImageIO.read(ClientUI.class.getResourceAsStream("open.png"));
				sidebarClose = ImageIO.read(ClientUI.class.getResourceAsStream("close.png"));
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		ICON = icon;
		SIDEBAR_OPEN = sidebarOpen;
		SIDEBAR_CLOSE = sidebarClose;
	}

	@Getter
	private TrayIcon trayIcon;

	private final RuneLite runelite;
	private final RuneLiteProperties properties;
	private final RuneLiteConfig config;
	private final EventBus eventBus;
	private final KeyManager keyManager;
	private Applet client;
	private ContainableFrame frame;
	private JPanel navContainer;
	private PluginPanel pluginPanel;
	private ClientPluginToolbar pluginToolbar;
	private ClientTitleToolbar titleToolbar;
	private JButton currentButton;
	private NavigationButton currentNavButton;
	private boolean sidebarOpen;
	private JPanel container;
	private PluginPanel lastPluginPanel;
	private NavigationButton sidebarNavigationButton;
	private JButton sidebarNavigationJButton;
	private boolean expandedClientOppositeDirection;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientUI(
		RuneLite runelite,
		RuneLiteProperties properties,
		RuneLiteConfig config,
		EventBus eventBus,
		KeyManager keyManager)
	{
		this.runelite = runelite;
		this.properties = properties;
		this.config = config;
		this.eventBus = eventBus;
		this.keyManager = keyManager;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("runelite"))
		{
			return;
		}

		SwingUtilities.invokeLater(() ->
		{
			if (event.getKey().equals("gameAlwaysOnTop"))
			{
				if (frame.isAlwaysOnTopSupported())
				{
					frame.setAlwaysOnTop(config.gameAlwaysOnTop());
				}
			}

			if (event.getKey().equals("lockWindowSize"))
			{
				frame.setResizable(!config.lockWindowSize());
			}

			if (event.getKey().equals("containInScreen") ||
				event.getKey().equals("uiEnableCustomChrome"))
			{
				frame.setContainedInScreen(config.containInScreen() && config.enableCustomChrome());
			}

			if (event.getKey().equals("rememberScreenBounds") && event.getNewValue().equals("false"))
			{
				configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_CLIENT_MAXIMIZED);
				configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_CLIENT_BOUNDS);
			}

			if (!event.getKey().equals("gameSize"))
			{
				return;
			}

			if (client == null)
			{
				return;
			}

			int width = config.gameSize().width;
			int height = config.gameSize().height;

			// The upper bounds are defined by the applet's max size
			// The lower bounds are taken care of by ClientPanel's setMinimumSize

			if (width > 7680)
			{
				width = 7680;
			}

			if (height > 2160)
			{
				height = 2160;
			}

			final Dimension size = new Dimension(width, height);

			client.setSize(size);
			client.setPreferredSize(size);
			client.getParent().setPreferredSize(size);
			client.getParent().setSize(size);

			if (frame.isVisible())
			{
				frame.pack();
			}
		});
	}

	@Subscribe
	public void onPluginToolbarButtonAdded(final PluginToolbarButtonAdded event)
	{
		SwingUtilities.invokeLater(() ->
		{
			final JButton button = SwingUtil.createSwingButton(event.getButton(), 0, (navButton, jButton) ->
			{
				final PluginPanel panel = navButton.getPanel();

				if (panel == null)
				{
					return;
				}

				boolean doClose = currentButton != null && currentButton == jButton && currentButton.isSelected();

				if (doClose)
				{
					contract();
					currentButton.setSelected(false);
					currentNavButton.setSelected(false);
					currentButton = null;
					currentNavButton = null;
				}
				else
				{
					if (currentButton != null)
					{
						currentButton.setSelected(false);
					}

					if (currentNavButton != null)
					{
						currentNavButton.setSelected(false);
					}

					currentButton = jButton;
					currentNavButton = navButton;
					currentButton.setSelected(true);
					currentNavButton.setSelected(true);
					expand(panel);
				}
			});

			pluginToolbar.addComponent(event.getIndex(), event.getButton(), button);
		});
	}

	@Subscribe
	public void onPluginToolbarButtonRemoved(final PluginToolbarButtonRemoved event)
	{
		SwingUtilities.invokeLater(() -> pluginToolbar.removeComponent(event.getButton()));
	}

	@Subscribe
	public void onTitleToolbarButtonAdded(final TitleToolbarButtonAdded event)
	{
		SwingUtilities.invokeLater(() ->
		{
			final int iconSize = ClientTitleToolbar.TITLEBAR_SIZE - 6;
			final JButton button = SwingUtil.createSwingButton(event.getButton(), iconSize, null);

			if (config.enableCustomChrome() || SwingUtil.isCustomTitlePanePresent(frame))
			{
				titleToolbar.addComponent(event.getButton(), button);
				return;
			}

			pluginToolbar.addComponent(-1, event.getButton(), button);
		});
	}

	@Subscribe
	public void onTitleToolbarButtonRemoved(final TitleToolbarButtonRemoved event)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (config.enableCustomChrome() || SwingUtil.isCustomTitlePanePresent(frame))
			{
				titleToolbar.removeComponent(event.getButton());
				return;
			}

			pluginToolbar.removeComponent(event.getButton());
		});
	}

	/**
	 * Initialize UI.
	 *
	 * @param client the client
	 * @throws Exception exception that can occur during creation of the UI
	 */
	public void init(@Nullable final Applet client) throws Exception
	{
		this.client = client;

		SwingUtilities.invokeAndWait(() ->
		{
			// Set some sensible swing defaults
			SwingUtil.setupDefaults();

			// Use substance look and feel
			SwingUtil.setTheme(new SubstanceGraphiteLookAndFeel());

			// Use custom UI font
			SwingUtil.setFont(FontManager.getRunescapeFont());

			// Create main window
			frame = new ContainableFrame();

			// Try to enable fullscreen on OSX
			OSXUtil.tryEnableFullscreen(frame);

			frame.setTitle(properties.getTitle());
			frame.setIconImage(ICON);
			frame.getLayeredPane().setCursor(Cursor.getDefaultCursor()); // Prevent substance from using a resize cursor for pointing
			frame.setLocationRelativeTo(frame.getOwner());
			frame.setResizable(true);

			SwingUtil.addGracefulExitCallback(frame,
				() ->
				{
					// Update window bounds to save position with
					if (sidebarOpen)
					{
						toggleSidebar();
					}
					contract();

					saveClientBoundsConfig();
					runelite.shutdown();
				},
				() -> client != null
					&& client instanceof Client
					&& ((Client) client).getGameState() != GameState.LOGIN_SCREEN);

			container = new JPanel();
			container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
			container.add(new ClientPanel(client));

			navContainer = new JPanel();
			navContainer.setLayout(new BorderLayout(0, 0));
			navContainer.setMinimumSize(new Dimension(0, 0));
			navContainer.setMaximumSize(new Dimension(0, Integer.MAX_VALUE));
			container.add(navContainer);

			pluginToolbar = new ClientPluginToolbar();
			titleToolbar = new ClientTitleToolbar();
			frame.add(container);

			// Add key listener
			final UiKeyListener uiKeyListener = new UiKeyListener(this);
			frame.addKeyListener(uiKeyListener);
			keyManager.registerKeyListener(uiKeyListener);
		});
	}

	/**
	 * Show client UI after everything else is done.
	 *
	 * @throws Exception exception that can occur during modification of the UI
	 */
	public void show() throws Exception
	{
		final boolean withTitleBar = config.enableCustomChrome();

		SwingUtilities.invokeAndWait(() ->
		{
			frame.setUndecorated(withTitleBar);

			if (withTitleBar)
			{
				frame.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);

				final JComponent titleBar = SubstanceCoreUtilities.getTitlePaneComponent(frame);
				titleToolbar.putClientProperty(SubstanceTitlePaneUtilities.EXTRA_COMPONENT_KIND, SubstanceTitlePaneUtilities.ExtraComponentKind.TRAILING);
				titleBar.add(titleToolbar);

				// Substance's default layout manager for the title bar only lays out substance's components
				// This wraps the default manager and lays out the TitleToolbar as well.
				LayoutManager delegate = titleBar.getLayout();
				titleBar.setLayout(new LayoutManager()
				{
					@Override
					public void addLayoutComponent(String name, Component comp)
					{
						delegate.addLayoutComponent(name, comp);
					}

					@Override
					public void removeLayoutComponent(Component comp)
					{
						delegate.removeLayoutComponent(comp);
					}

					@Override
					public Dimension preferredLayoutSize(Container parent)
					{
						return delegate.preferredLayoutSize(parent);
					}

					@Override
					public Dimension minimumLayoutSize(Container parent)
					{
						return delegate.minimumLayoutSize(parent);
					}

					@Override
					public void layoutContainer(Container parent)
					{
						delegate.layoutContainer(parent);
						final int width = titleToolbar.getPreferredSize().width;
						titleToolbar.setBounds(titleBar.getWidth() - 75 - width, 0, width, titleBar.getHeight());
					}
				});
			}

			// Show frame
			frame.pack();
			revalidateMinimumSize();
			if (config.rememberScreenBounds())
			{
				try
				{
					Rectangle clientBounds = configManager.getConfiguration(
						CONFIG_GROUP, CONFIG_CLIENT_BOUNDS, Rectangle.class);
					if (clientBounds != null)
					{
						frame.setBounds(clientBounds);
					}
					else
					{
						frame.setLocationRelativeTo(frame.getOwner());
					}

					if (configManager.getConfiguration(CONFIG_GROUP, CONFIG_CLIENT_MAXIMIZED) != null)
					{
						frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
					}
				}
				catch (Exception ex)
				{
					log.warn("Failed to set window bounds", ex);
					frame.setLocationRelativeTo(frame.getOwner());
				}
			}
			else
			{
				frame.setLocationRelativeTo(frame.getOwner());
			}
			frame.setVisible(true);
			frame.toFront();
			requestFocus();
			giveClientFocus();

			// If the frame is well hidden (e.g. unplugged 2nd screen),
			// we want to move it back to default position as it can be
			// hard for the user to reposition it themselves otherwise.
			Rectangle clientBounds = frame.getBounds();
			Rectangle screenBounds = frame.getGraphicsConfiguration().getBounds();
			if (clientBounds.x + clientBounds.width - CLIENT_WELL_HIDDEN_MARGIN < screenBounds.getX() ||
				clientBounds.x + CLIENT_WELL_HIDDEN_MARGIN > screenBounds.getX() + screenBounds.getWidth() ||
				clientBounds.y + CLIENT_WELL_HIDDEN_MARGIN_TOP < screenBounds.getY() ||
				clientBounds.y + CLIENT_WELL_HIDDEN_MARGIN > screenBounds.getY() + screenBounds.getHeight())
			{
				frame.setLocationRelativeTo(frame.getOwner());
			}

			trayIcon = SwingUtil.createTrayIcon(ICON, properties.getTitle(), frame);

			// Create hide sidebar button
			sidebarNavigationButton = NavigationButton
				.builder()
				.icon(SIDEBAR_CLOSE)
				.onClick(this::toggleSidebar)
				.build();

			sidebarNavigationJButton = SwingUtil.createSwingButton(
				sidebarNavigationButton,
				0,
				null);

			titleToolbar.addComponent(sidebarNavigationButton, sidebarNavigationJButton);
			toggleSidebar();
		});

		eventBus.post(new ClientUILoaded());
	}

	/**
	 * Paint this component to target graphics
	 *
	 * @param graphics the graphics
	 */
	public void paint(final Graphics graphics)
	{
		frame.paint(graphics);
	}

	/**
	 * Gets component width.
	 *
	 * @return the width
	 */
	public int getWidth()
	{
		return frame.getWidth();
	}

	/**
	 * Gets component height.
	 *
	 * @return the height
	 */
	public int getHeight()
	{
		return frame.getHeight();
	}

	/**
	 * Returns true if this component has focus.
	 *
	 * @return true if component has focus
	 */
	public boolean isFocused()
	{
		return frame.isFocused();
	}

	/**
	 * Request focus on this component and then on client component
	 */
	public void requestFocus()
	{
		if (OSType.getOSType() == OSType.MacOS)
		{
			OSXUtil.requestFocus();
		}

		frame.requestFocus();
		giveClientFocus();
	}

	/**
	 * Get offset of game canvas in game window
	 * @return game canvas offset
	 */
	public Point getCanvasOffset()
	{
		if (client instanceof Client)
		{
			final java.awt.Point point = SwingUtilities.convertPoint(((Client) client).getCanvas(), 0, 0, frame);
			return new Point(point.x, point.y);
		}

		return new Point(0, 0);
	}

	void toggleSidebar()
	{
		// Toggle sidebar open
		boolean isSidebarOpen = sidebarOpen;
		sidebarOpen = !sidebarOpen;

		// Select/deselect buttons
		if (currentButton != null)
		{
			currentButton.setSelected(sidebarOpen);
		}

		if (currentNavButton != null)
		{
			currentNavButton.setSelected(sidebarOpen);
		}

		if (isSidebarOpen)
		{
			sidebarNavigationJButton.setIcon(new ImageIcon(SIDEBAR_OPEN));
			sidebarNavigationJButton.setToolTipText("Open SideBar");

			// Save last panel and close current one
			lastPluginPanel = pluginPanel;
			contract();

			// Remove plugin toolbar
			container.remove(pluginToolbar);
		}
		else
		{
			sidebarNavigationJButton.setIcon(new ImageIcon(SIDEBAR_CLOSE));
			sidebarNavigationJButton.setToolTipText("Close SideBar");

			// Try to restore last panel
			expand(lastPluginPanel);

			// Add plugin toolbar back
			container.add(pluginToolbar);
		}

		// Revalidate sizes of affected Swing components
		container.revalidate();
		container.repaint();
		giveClientFocus();

		if (sidebarOpen)
		{
			expandFrameBy(pluginToolbar.getWidth());
		}
		else
		{
			contractFrameBy(pluginToolbar.getWidth());
		}
	}

	private void expand(@Nullable PluginPanel panel)
	{
		if (panel == null)
		{
			return;
		}

		if (!sidebarOpen)
		{
			toggleSidebar();
		}

		if (pluginPanel != null)
		{
			navContainer.remove(0);
		}

		pluginPanel = panel;
		navContainer.setMinimumSize(new Dimension(pluginPanel.getWrappedPanel().getPreferredSize().width, 0));
		navContainer.setMaximumSize(new Dimension(pluginPanel.getWrappedPanel().getPreferredSize().width, Integer.MAX_VALUE));

		final JPanel wrappedPanel = panel.getWrappedPanel();
		navContainer.add(wrappedPanel);
		navContainer.revalidate();

		// panel.onActivate has to go after giveClientFocus so it can get focus if it needs.
		giveClientFocus();
		panel.onActivate();
		wrappedPanel.repaint();
		expandFrameBy(pluginPanel.getWrappedPanel().getPreferredSize().width);
	}

	private void contract()
	{
		if (pluginPanel == null)
		{
			return;
		}

		pluginPanel.onDeactivate();
		navContainer.remove(0);
		navContainer.setMinimumSize(new Dimension(0, 0));
		navContainer.setMaximumSize(new Dimension(0, 0));
		navContainer.revalidate();
		giveClientFocus();
		contractFrameBy(pluginPanel.getWrappedPanel().getPreferredSize().width);
		pluginPanel = null;
	}

	private void expandFrameBy(int value)
	{
		if (isFullScreen())
		{
			return;
		}

		boolean forcedWidthIncrease = false;
		if (config.automaticResizeType() == ExpandResizeType.KEEP_WINDOW_SIZE)
		{
			int minimumWidth = frame.getLayout().minimumLayoutSize(frame).width;
			int currentWidth = frame.getWidth();
			if (minimumWidth > currentWidth)
			{
				forcedWidthIncrease = true;
				value = minimumWidth - currentWidth;
			}
		}

		if (forcedWidthIncrease || config.automaticResizeType() == ExpandResizeType.KEEP_GAME_SIZE)
		{
			int newWindowWidth = frame.getWidth() + value;
			int newWindowX = frame.getX();
			Rectangle screenBounds = frame.getGraphicsConfiguration().getBounds();
			boolean isCloseToEdge = Math.abs((frame.getX() + frame.getWidth()) -
				(screenBounds.getX() + screenBounds.getWidth())) <= SCREEN_EDGE_CLOSE_DISTANCE;
			boolean wouldExpandThroughEdge = frame.getX() + newWindowWidth >
				screenBounds.getX() + screenBounds.getWidth();
			if (!isCloseToEdge && wouldExpandThroughEdge)
			{
				// Move the window to the edge
				newWindowX = (int)(screenBounds.getX() + screenBounds.getWidth()) - frame.getWidth();
			}
			if (wouldExpandThroughEdge)
			{
				// Expand the window to the left as the user probably don't want the
				// window to go through the screen
				newWindowX -= value;

				expandedClientOppositeDirection = true;
			}
			frame.setBounds(newWindowX, frame.getY(), newWindowWidth, frame.getHeight());
		}

		revalidateMinimumSize();
	}

	private void contractFrameBy(int value)
	{
		if (isFullScreen())
		{
			return;
		}

		revalidateMinimumSize();

		boolean forcedWidthDecrease = false;
		if (config.automaticResizeType() == ExpandResizeType.KEEP_WINDOW_SIZE)
		{
			int minimumWidth = frame.getMinimumSize().width;
			int currentWidth = frame.getWidth();
			if (currentWidth - value <= minimumWidth)
			{
				forcedWidthDecrease = true;
				value = currentWidth - minimumWidth;
			}
		}

		if (forcedWidthDecrease || config.automaticResizeType() == ExpandResizeType.KEEP_GAME_SIZE)
		{
			int newWindowWidth = frame.getWidth() - value;
			int newWindowX = frame.getX();
			Rectangle screenBounds = frame.getGraphicsConfiguration().getBounds();
			boolean wasCloseToRightEdge = Math.abs((frame.getX() + frame.getWidth()) -
				(screenBounds.getX() + screenBounds.getWidth())) <= SCREEN_EDGE_CLOSE_DISTANCE;
			boolean wasCloseToLeftEdge = Math.abs(frame.getX() - screenBounds.getX()) <= SCREEN_EDGE_CLOSE_DISTANCE;
			if (wasCloseToRightEdge && (expandedClientOppositeDirection || !wasCloseToLeftEdge))
			{
				// Keep the distance to the right edge
				newWindowX += value;
			}

			frame.setBounds(newWindowX, frame.getY(), newWindowWidth, frame.getHeight());
		}

		expandedClientOppositeDirection = false;
	}

	private void revalidateMinimumSize()
	{
		frame.setMinimumSize(frame.getLayout().minimumLayoutSize(frame));
	}

	private boolean isFullScreen()
	{
		return (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
	}

	private void giveClientFocus()
	{
		if (client instanceof Client)
		{
			final Canvas c = ((Client) client).getCanvas();
			c.requestFocusInWindow();
		}
		else if (client != null)
		{
			client.requestFocusInWindow();
		}
	}

	private void saveClientBoundsConfig()
	{
		if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0)
		{
			configManager.setConfiguration(CONFIG_GROUP, CONFIG_CLIENT_MAXIMIZED, true);
		}
		else
		{
			configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_CLIENT_MAXIMIZED);
			configManager.setConfiguration(CONFIG_GROUP, CONFIG_CLIENT_BOUNDS, frame.getBounds());
		}
	}
}
