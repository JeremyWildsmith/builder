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
import io.github.jevaengine.builder.ui.MessageBoxFactory.IMessageBoxObserver;
import io.github.jevaengine.builder.ui.MessageBoxFactory.MessageBox;
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
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileInputQueryFactory
{
	private static final URI WINDOW_LAYOUT = URI.create("local:///ui/windows/fileInput.jwl");
	
	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;
	private final URI m_base;
	
	public FileInputQueryFactory(WindowManager windowManager, IWindowFactory windowFactory, URI base)
	{
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
		m_base = base;
	}
	
	public FileInputQuery create(FileInputQueryMode mode, String query, URI defaultValue) throws WindowConstructionException
	{
		Observers observers = new Observers();

		Window window = m_windowFactory.create(WINDOW_LAYOUT, new FileInputQueryBehaviourInjector(observers, query, defaultValue, mode));
		m_windowManager.addWindow(window);
			
		window.center();
		return new FileInputQuery(observers, window);
	}
	
	public static class FileInputQuery implements IDisposable
	{
		private final IObserverRegistry m_observers;
		
		private final Window m_window;
		
		private FileInputQuery(IObserverRegistry observers, Window window)
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

		public void setTopMost(boolean b) {
			m_window.setTopMost(b);
		}
	}
	
	private class FileInputQueryBehaviourInjector extends WindowBehaviourInjector
	{
		private final Logger m_logger = LoggerFactory.getLogger(FileInputQueryFactory.class);
		private final Observers m_observers;
		private final String m_query;
		
		private final URI m_defaultValue;
		private final FileInputQueryMode m_mode;

		public FileInputQueryBehaviourInjector(Observers observers, String query, URI defaultValue, FileInputQueryMode mode)
		{
			m_observers = observers;
			m_query = query;
			m_defaultValue = defaultValue;
			m_mode = mode;
		}
		
		private void displayMessage(String cause)
		{
			try
			{
				final MessageBox msgBox = new MessageBoxFactory(m_windowManager, m_windowFactory).create(cause);
				
				msgBox.getObservers().add(new IMessageBoxObserver() {
					@Override
					public void okay() {
						msgBox.dispose();
					}
				});
				
			} catch (WindowConstructionException e) {
				m_logger.error("Unable to notify use of validation failures", e);
			}
		}
		
		@Override
		protected void doInject() throws NoSuchControlException
		{
			final TextArea txtValue = getControl(TextArea.class, "txtValue");
			
			getControl(Label.class, "lblQuery").setText(m_query);
			txtValue.setText(m_base.relativize(m_defaultValue).toString());
			
			getControl(Button.class, "btnOkay").getObservers().add(new IButtonPressObserver() {
				@Override
			public void onPress() {
				
				try
				{
					URI path = new URI(txtValue.getText());
					
					if(m_base != null)
					{
						path = m_base.relativize(path);
						
						if(path.isAbsolute())
							displayMessage("Cannot relativize the specified path. Assure it is a child of this project's base directory.");
						else
							m_observers.raise(IFileInputQueryObserver.class).okay(URI.create("/").resolve(path));
					} else
						m_observers.raise(IFileInputQueryObserver.class).okay(path);
					} catch (URISyntaxException e)
					{
						displayMessage("Specified path is not a valid URI.");					
					}
				}
			});
			
			getControl(Button.class, "btnCancel").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_observers.raise(IFileInputQueryObserver.class).cancel();
				}
			});
			
			getControl(Button.class, "btnBrowse").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					
					try
					{
						SwingUtilities.invokeAndWait(new Runnable() {
	
							@Override
							public void run()
							{
								JFileChooser c = new JFileChooser(m_defaultValue.isAbsolute() ? new File(m_defaultValue) : new File(new File(m_base), m_defaultValue.toString()));
								
								int result = 0;
								
								switch(m_mode)
								{
								case SaveFile:
									result = c.showSaveDialog(null);
									break;
								case OpenDirectory:
									c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
									result = c.showOpenDialog(null);
								break;
								case OpenFile:
									result = c.showOpenDialog(null);
									break;
								default:
									throw new RuntimeException("Unrecognized mode");
								}
								
								if(result == JFileChooser.APPROVE_OPTION)
									txtValue.setText(URI.create("/").resolve(m_base.relativize(c.getSelectedFile().toURI())).toString());
							}
						});
					} catch (InvocationTargetException | InterruptedException e)
					{
						throw new RuntimeException(e);
					}
				}
			});
		}
	}
	
	public enum FileInputQueryMode
	{
		OpenDirectory,
		OpenFile,
		SaveFile,
	}
	
	public interface IFileInputQueryObserver
	{
		void okay(URI input);
		void cancel();
	}
}
