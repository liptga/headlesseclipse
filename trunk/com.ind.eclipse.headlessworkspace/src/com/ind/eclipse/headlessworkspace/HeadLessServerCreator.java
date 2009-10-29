package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.server.generic.core.internal.CorePlugin;
import org.eclipse.jst.server.generic.servertype.definition.Property;
import org.eclipse.jst.server.generic.servertype.definition.ServerRuntime;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IPublishListener;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.RuntimeWorkingCopy;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;

@SuppressWarnings("restriction")
public class HeadLessServerCreator
{
	private static HeadLessServerCreator instance;
	
	private IServer server;
	
	public static HeadLessServerCreator getInstance()
	{
		if (instance == null)
		{
			instance = new HeadLessServerCreator();
		}
		return instance;
	}
	
	private HeadLessServerCreator()
	{
	}
	
	@SuppressWarnings("unchecked")
	void createServer(String serverName, String propertiesFile, IProgressMonitor monitor) throws Exception
	{
       	String generatedName = serverName + " GENERATED";
       	
       	server = ServerCore.findServer(generatedName);
       	if (server == null)
       	{
        	System.out.println("Creating a server instance of: " + serverName);
        	System.out.println("Loading properties from: " + propertiesFile);
        	Properties pf = new Properties();
        	pf.load(new FileInputStream(propertiesFile));
        	System.out.println("Loaded properties: " + pf);
        	
    		
    		IServerType[] servertypes = ServerCore.getServerTypes();
    		IServerType serverType = null;
    		for (int i = 0; i < servertypes.length; i++)
    		{
    			if (servertypes[i].getName().equals(serverName))
    			{
    				serverType = servertypes[i];
    				break;
    			}
    		}
    		if (serverType == null)
    		{
    			System.out.println("Server not found!");
    			return;
    		}
    		System.out.println("Server type found, ID is: " + serverType.getId());
    		
    		IRuntimeType runtimeType = serverType.getRuntimeType();
    		
    		System.out.println("Associated runtime type: " + runtimeType.getName());
    		System.out.println("Runtime type ID is: " + runtimeType.getId());
    		
    		ServerRuntime serverRuntime = CorePlugin.getDefault().getServerTypeDefinitionManager().getServerRuntimeDefinition(serverType.getId(), runtimeType.getId(), null);
    		System.out.println("ServerRuntime found: " + serverRuntime.getName());
    		
    		List serverRuntimeProperties = serverRuntime.getProperty();
    		HashMap runtimeProperties = new HashMap();
    		HashMap serverProperties = new HashMap();
    		for (int i = 0; i < serverRuntimeProperties.size(); i++)
    		{
    			Property property = (Property) serverRuntimeProperties.get(i);
    			Map values = null;
    			if (property.getContext().equals(Property.CONTEXT_RUNTIME))
    			{
    				System.out.print("\tServer runtime property: ");
    				values = runtimeProperties;
    			}
    			else
    			{
    				System.out.print("\tServer property: ");
    				values = serverProperties;
    			}
    			System.out.println(property.getId() + ", default value is: " + property.getDefault());
    			String svalue = pf.getProperty(property.getId());
    			if (svalue == null)
    			{
    				System.out.println("ERROR: No value associated for " + property.getId() + " in " + propertiesFile);
    				return;
    			}
    			Object value = null;
    			if (property.getType().equals(Property.TYPE_DIRECTORY))
    			{
    				File f = new File(svalue);
    				if (!f.exists() || !f.isDirectory())
    				{
    					System.out.println("ERROR: directory property " + property.getId() + " value does not exist or not a directory: " + svalue);
    				}
    				value = svalue;
    			}
    			else if (property.getType().equals(Property.TYPE_FILE))
    			{
    				File f = new File(svalue);
    				if (!f.exists() || !f.isFile())
    				{
    					System.out.println("ERROR: file property " + property.getId() + " value does not exist or not a regular file: " + svalue);
    				}
    				value = svalue;
    			}
    			else
    			{
    				value = svalue;
    			}
    			values.put(property.getId(), value);
    		}
    		
    		System.out.println("Creating server runtime...");
    		
    		RuntimeWorkingCopy rwc = (RuntimeWorkingCopy) runtimeType.createRuntime(runtimeType.getName() + " GENERATED", monitor);
    		if (!pf.containsKey("location"))
    		{
    			System.out.println("ERROR: no location property in properties file: " + propertiesFile);
    			return;
    		}
    		rwc.setLocation(new Path(pf.getProperty("location")));
    		rwc.setName(runtimeType.getName() + " GENERATED");
    		rwc.setStub(false);
    		Iterator it = runtimeProperties.entrySet().iterator();
    		while (it.hasNext())
    		{
    			Map.Entry e = (Entry) it.next();
    			rwc.setAttribute((String)e.getKey(), (String)e.getValue());
    		}
    		Map m = rwc.getAttribute("generic_server_instance_properties", (Map)null);
    		m.put("key", "generic_server_instance_properties");
    		m.putAll(runtimeProperties);
    		rwc.setAttribute("generic_server_instance_properties", m);
    		rwc.setAttribute("runtime-type-id", runtimeType.getId());
    		rwc.setAttribute("server_definition_id", serverRuntime.getId());
    		IRuntime runtime = rwc.save(true, monitor);
    		
    		System.out.println("Server runtime created: " + runtime.getName());
    		
    		System.out.println("Creating server instance...");
    		
    		ServerWorkingCopy swc = (ServerWorkingCopy) serverType.createServer(null, null, runtime, monitor);
    		swc.setName(serverName + " GENERATED");
    		swc.setAttribute("server-type", serverType.getId());
    		m = swc.getAttribute("generic_server_instance_properties", (Map)null);
    		m.put("key", "generic_server_instance_properties");
    		m.putAll(serverProperties);
    		swc.setAttribute("generic_server_instance_properties", m);
    		
    		String modules = pf.getProperty("modules");
    		IModule[] add = new IModule[0];
    		if (modules != null)
    		{
    			String[] strmodules = modules.split(" ,;");
    			add = new IModule[strmodules.length];
    			for (int i = 0; i < strmodules.length; i++)
    			{
    				add[i] = ServerUtil.getModule(ResourcesPlugin.getWorkspace().getRoot().getProject(strmodules[i]));
    			}
    		}
    	
    		System.out.println("Modules to assign to server: " + Arrays.asList(add));
    		swc.modifyModules(add, new IModule[0], monitor);
    		server = swc.save(true, monitor);
    		
    		System.out.println("Server instance created: " + server.getName());
       	}
       	else
       	{
       		System.out.println("Server found: " + server.getName());
       	}
	}

/*	@SuppressWarnings("unchecked")
	private void listServer(Field f, IProgressMonitor monitor) throws Exception
	{
		IServer[] servers = ServerCore.getServers();
		for (int i = 0; i < servers.length; i++)
		{
			Server s = (Server) servers[i];
			Map m = (Map) f.get(s);
			System.out.println("Server: " + s.getName() + "; ID: " + s.getId());
			System.out.println("Server map: " + m);
			IModule[] modules = s.getModules();
			for (int j = 0; j < modules.length; j++)
			{
				System.out.println("\tModule: " + modules[j].getName() + "; " + modules[j].getProject() + "; " + modules[j].getModuleType().getName());
			}
		}
		
		IRuntime[] runtimes = ServerCore.getRuntimes();
		for (int i = 0; i < runtimes.length; i++)
		{
			Runtime s = (Runtime) runtimes[i];
			Map m = (Map) f.get(s);
			System.out.println("Runtime: " + s.getName() + "; ID: " + s.getId());
			System.out.println("Runtime map: " + m);
		}
	}
*/
	
	void startServer(IProgressMonitor monitor) throws Exception
	{
		if (server == null)
		{
			return;
		}
		server.addServerListener(new IServerListener(){

			public void serverChanged(ServerEvent event)
			{
				if ((event.getKind() | ServerEvent.SERVER_CHANGE) > 0)
				{
					if (event.getState() == IServer.STATE_STARTED)
					{
						System.out.println("Server started!");
					}
					if (event.getState() == IServer.STATE_STOPPED)
					{
						System.out.println("Server stopped!");
						synchronized (HeadLessServerCreator.this)
						{
							HeadLessServerCreator.this.notifyAll();
						}
					}
				}
			}});
		
		server.addPublishListener(new IPublishListener(){

			public void publishFinished(IServer server, IStatus status)
			{
				System.out.println("Publish finished!");
				synchronized (HeadLessServerCreator.this)
				{
					HeadLessServerCreator.this.notifyAll();
				}
			}

			public void publishStarted(IServer server)
			{
				System.out.println("Publish started!");
				new Exception().printStackTrace();
				System.out.println("Thread: " + Thread.currentThread());
			}});
		
		server.start("run", monitor);
		
		System.out.println("Waiting server to start...");
		synchronized (this)
		{
			wait(1000 * 60);
		}
		System.out.println("Exiting...");
	}
	
}
