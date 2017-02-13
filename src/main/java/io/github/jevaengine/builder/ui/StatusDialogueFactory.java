/* 
 * Copyright (C) 2015 Jeremy Wildsmith.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package io.github.jevaengine.builder.ui;

import io.github.jevaengine.IDisposable;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.Label;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.ValueGuage;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.WindowManager;
import java.net.URI;

public class StatusDialogueFactory
{
	private static final URI WINDOW_LAYOUT = URI.create("local:///ui/windows/status.jwl");
	
	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;
	
	public StatusDialogueFactory(WindowManager windowManager, IWindowFactory windowFactory)
	{
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}
	
	public StatusDialogue create() throws WindowConstructionException
	{

		Window window = m_windowFactory.create(WINDOW_LAYOUT);	
		m_windowManager.addWindow(window);
		window.center();
	
		try
		{
			ValueGuage statusGauge = window.getControl(ValueGuage.class, "progress");
			Label statusLabel = window.getControl(Label.class, "lblStatus");
			return new StatusDialogue(window, statusGauge, statusLabel);
		} catch (NoSuchControlException e)
		{
			window.dispose();
			throw new WindowConstructionException(WINDOW_LAYOUT, e);
		}
	}
	
	public static class StatusDialogue implements IDisposable
	{
		private final Window m_window;
		private final ValueGuage m_statusGuage;
		private final Label m_statusLabel;
		
		private StatusDialogue(Window window, ValueGuage statusGuage, Label statusLabel)
		{
			m_window = window;
			m_statusGuage = statusGuage;
			m_statusLabel = statusLabel;
		}
		
		@Override
		public void dispose()
		{
			m_window.dispose();
		}
		
		public void setVisible(boolean isVisible)
		{
			m_window.setVisible(isVisible);
		}
		
		public void setLocation(Vector2D location)
		{
			m_window.setLocation(location);
		}
		
		public void center()
		{
			m_window.center();
		}
		
		public void setStatus(String status, float progress)
		{
			m_statusGuage.setValue(progress);
			m_statusLabel.setText(status);
		}
	}
}
