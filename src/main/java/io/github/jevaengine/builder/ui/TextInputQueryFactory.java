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
import io.github.jevaengine.ui.Button;
import io.github.jevaengine.ui.Button.IButtonPressObserver;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.Label;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.TextArea;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WindowManager;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;

import java.net.URI;

public final class TextInputQueryFactory
{
	private static final URI WINDOW_LAYOUT = URI.create("local:///ui/windows/textInput.jwl");
	
	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;
	
	public TextInputQueryFactory(WindowManager windowManager, IWindowFactory windowFactory)
	{
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}
	
	public TextInputQuery create(String query, String defaultValue) throws WindowConstructionException
	{
		Observers observers = new Observers();
			
		Window window = m_windowFactory.create(WINDOW_LAYOUT, new TextInputQueryBehaviourInjector(observers, query, defaultValue));
		m_windowManager.addWindow(window);
			
		window.center();
		return new TextInputQuery(observers, window);
	}
	
	public static class TextInputQuery implements IDisposable
	{
		private final IObserverRegistry m_observers;
		
		private final Window m_window;
		
		private TextInputQuery(IObserverRegistry observers, Window window)
		{
			m_observers = observers;
			m_window = window;
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
		
		public IObserverRegistry getObservers()
		{
			return m_observers;
		}
	}
	
	private class TextInputQueryBehaviourInjector extends WindowBehaviourInjector
	{
		private final Observers m_observers;
		private final String m_query;
		private final String m_defaultValue;

		public TextInputQueryBehaviourInjector(Observers observers, String query, String defaultValue)
		{
			m_observers = observers;
			m_query = query;
			m_defaultValue = defaultValue;
		}
		
		@Override
		protected void doInject() throws NoSuchControlException
		{
			final TextArea txtValue = getControl(TextArea.class, "txtValue");
			
			getControl(Label.class, "lblQuery").setText(m_query);
			txtValue.setText(m_defaultValue);
			
			getControl(Button.class, "btnOkay").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_observers.raise(ITextInputQueryObserver.class).okay(txtValue.getText());
				}
			});
			
			getControl(Button.class, "btnCancel").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_observers.raise(ITextInputQueryObserver.class).cancel();
				}
			});
		}
	}
	
	public interface ITextInputQueryObserver
	{
		void okay(String input);
		void cancel();
	}
}
