/*
 * Copyright (c) 2018, Woox <https://github.com/wooxsolo>
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

import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.Duration;
import java.time.Instant;
import javax.swing.JFrame;
import lombok.Getter;

public class ContainableFrame extends JFrame
{
	private static final int LARGE_WIDTH_THRESHOLD = 1600;
	private static final int NEAR_EDGE_DISTANCE = 50;
	private static final int PANEL_EXPANDED_WIDTH = PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH;

	@Getter
	private boolean containedInScreen;

	private boolean expanded;
	private Rectangle oldBounds;
	private Instant ignoreNextBoundsNullUntil;

	public ContainableFrame()
	{
		this.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentMoved(ComponentEvent e)
			{
				super.componentMoved(e);

				if (ignoreNextBoundsNullUntil != null && Instant.now().compareTo(ignoreNextBoundsNullUntil) > 0)
				{
					oldBounds = null;
				}
			}

			@Override
			public void componentResized(ComponentEvent e)
			{
				super.componentResized(e);

				if (ignoreNextBoundsNullUntil != null && Instant.now().compareTo(ignoreNextBoundsNullUntil) > 0)
				{
					oldBounds = null;
				}
			}
		});
	}

	public void expand()
	{
		this.oldBounds = this.getBounds();
		this.ignoreNextBoundsNullUntil = Instant.now().plus(Duration.ofMillis(300));

		if ((this.getExtendedState() & MAXIMIZED_HORIZ) != 0)
		{
			// If the user is using a maximized window, they probably
			// don't want it to expand through the edge
		}
		else if (this.getWidth() >= LARGE_WIDTH_THRESHOLD)
		{
			// If the user is using a big window, they probably don't
			// mind if it gets a little smaller when expanding
			// one of the panels
		}
		else
		{
			int newX = this.getX();
			int newWidth = this.getWidth() + PANEL_EXPANDED_WIDTH;

			Rectangle bounds = this.getGraphicsConfiguration().getBounds();
			if (newX + newWidth > bounds.getX() + bounds.getWidth())
			{
				newX = Math.max((int)bounds.getX(),
					(int)(bounds.getX() + bounds.getWidth()) -
						this.getWidth() - PANEL_EXPANDED_WIDTH);
			}

			this.setBounds(newX, this.getY(), newWidth, this.getHeight());
		}

		this.expanded = true;
	}

	public void contract()
	{
		this.setBounds(getContractedBounds());
		this.expanded = false;
	}

	public Rectangle getContractedBounds()
	{
		if (oldBounds != null)
		{
			return oldBounds;
		}
		if (expanded &&
			(this.getExtendedState() & MAXIMIZED_HORIZ) == 0 &&
			this.getWidth() < LARGE_WIDTH_THRESHOLD)
		{
			// If the user accidentally moved the window, oldBounds gets nulled,
			// but we still want to shrink the window size for them

			Rectangle screenBounds = this.getGraphicsConfiguration().getBounds();
			if (Math.abs(this.getX() - screenBounds.getX()) > NEAR_EDGE_DISTANCE &&
				Math.abs(this.getX() + this.getWidth() - screenBounds.getX() - screenBounds.getWidth()) <= NEAR_EDGE_DISTANCE)
			{
				// If the user has the window near the right edge, they probably
				// want it to stay there instead of having it move to the left.
				return new Rectangle(this.getX() + PANEL_EXPANDED_WIDTH, this.getY(),
					this.getWidth() - PANEL_EXPANDED_WIDTH, this.getHeight());
			}
			else
			{
				return new Rectangle(this.getX(), this.getY(),
					this.getWidth() - PANEL_EXPANDED_WIDTH, this.getHeight());
			}
		}
		return this.getBounds();
	}

	public void setContainedInScreen(boolean value)
	{
		this.containedInScreen = value;
		if (value)
		{
			// Reposition the frame if it is intersecting with the bounds
			this.setLocation(this.getX(), this.getY());
			this.setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight());
		}
	}

	@Override
	public void setLocation(int x, int y)
	{
		if (containedInScreen)
		{
			Rectangle bounds = this.getGraphicsConfiguration().getBounds();
			x = Math.max(x, (int)bounds.getX());
			x = Math.min(x, (int)(bounds.getX() + bounds.getWidth() - this.getWidth()));
			y = Math.max(y, (int)bounds.getY());
			y = Math.min(y, (int)(bounds.getY() + bounds.getHeight() - this.getHeight()));
		}
		super.setLocation(x, y);
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		if (containedInScreen)
		{
			Rectangle bounds = this.getGraphicsConfiguration().getBounds();
			width = Math.min(width, width - (int)bounds.getX() + x);
			x = Math.max(x, (int)bounds.getX());
			height = Math.min(height, height - (int)bounds.getY() + y);
			y = Math.max(y, (int)bounds.getY());
			width = Math.min(width, (int)(bounds.getX() + bounds.getWidth()) - x);
			height = Math.min(height, (int)(bounds.getY() + bounds.getHeight()) - y);
		}
		super.setBounds(x, y, width, height);
	}
}
