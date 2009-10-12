package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.ui.externaltools.internal.model.BuilderUtils;
import org.eclipse.ui.externaltools.internal.model.ExternalToolBuilder;

public class HeadLessBuilder
{
	private static HeadLessBuilder instance;
	
	public static HeadLessBuilder getInstance()
	{
		if (instance == null)
		{
			instance = new HeadLessBuilder();
		}
		return instance;
	}
	
	private int errorCount;
	private String[] strprojects;
	
	private HeadLessBuilder()
	{
		errorCount = 0;
		strprojects = null;
	}
	
	public int getErrorCount()
	{
		return errorCount;
	}
	
	void dumpProjectReferences(IProject[] projects) throws Exception
	{
		SysOutProgressMonitor.out.println("PROJECT DEPENDENCIES:");
		for (int i = 0; i < projects.length; i++)
		{
			SysOutProgressMonitor.out.println("PROJECT DEPENDENCIES FOR: " + projects[i]);
			IProjectDescription desc = projects[i].getDescription();
			IProject[] staticRefs = desc.getReferencedProjects();
			for (int j = 0; j < staticRefs.length; j++)
			{
				SysOutProgressMonitor.out.println("\tStatic reference: " + staticRefs[j]);
			}
			IProject[] dynaRefs = desc.getDynamicReferences();
			for (int j = 0; j < dynaRefs.length; j++)
			{
				SysOutProgressMonitor.out.println("\tDynamic reference: " + dynaRefs[j]);
			}
		}
	}
	
	void cleanProjects(final IProgressMonitor monitor) throws Exception
	{
		IProject[] projects = ResourcesPlugin.getWorkspace().computeProjectOrder(ResourcesPlugin.getWorkspace().getRoot().getProjects()).projects;
		
		SysOutProgressMonitor.out.println("CLEANING PROJECTS:");
		for (int i = projects.length - 1; i >= 0; i--)
		{
			if (projects[i].exists())
			{
				SysOutProgressMonitor.out.println("CLEANING PROJECT: " + projects[i].getName());
				projects[i].build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
			}
		}
		
		projects = ResourcesPlugin.getWorkspace().computeProjectOrder(projects).projects;
		dumpProjectReferences(projects);
	}

	@SuppressWarnings("unchecked")
	Map modifyExternalToolBuilders(final IProgressMonitor monitor, File logFile) throws Exception
	{
		Map modifications = new HashMap();
		
		IProject[] projects = ResourcesPlugin.getWorkspace().computeProjectOrder(ResourcesPlugin.getWorkspace().getRoot().getProjects()).projects;
		SysOutProgressMonitor.out.println("MODIFYING EXTERNAL TOOL BUILDER OPTIONS TO BE ABLE TO CATCH OUTPUT:");
		for (int i = 0; i < projects.length; i++)
		{
			IProject p = projects[i];

			Map buildermap = new HashMap();
			modifications.put(p, buildermap);
			ICommand[] builders = p.getDescription().getBuildSpec();
			for (int j = 0; j < builders.length; j++)
			{
				if (ExternalToolBuilder.ID.equals(builders[j].getBuilderName()))
				{
					Map configChanges = new HashMap();
					buildermap.put(builders[j], configChanges);
					
					ILaunchConfiguration config = BuilderUtils.configFromBuildCommandArgs(p, builders[j].getArguments(), new String[1]);

					SysOutProgressMonitor.out.println("MODIFYING EXTERNAL TOOL BUILDER '" + config.getName() + "' ON PROJECT: " + p.getName());
					
					configChanges.put(IDebugUIConstants.ATTR_APPEND_TO_FILE, config.getAttribute(IDebugUIConstants.ATTR_APPEND_TO_FILE, false));
					configChanges.put(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, config.getAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, (String)null));
					
					ILaunchConfigurationWorkingCopy configwc = config.getWorkingCopy();
					configwc.setAttribute(IDebugUIConstants.ATTR_APPEND_TO_FILE, true);
					configwc.setAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, logFile.getAbsolutePath());
					configwc.doSave();
				}
			}
		}
		return modifications;
	}
		
	@SuppressWarnings("unchecked")
	void importProjects(final IProgressMonitor monitor) throws Exception
	{
		SysOutProgressMonitor.out.println();
		String circular = JavaCore.getOption(JavaCore.CORE_CIRCULAR_CLASSPATH);
		SysOutProgressMonitor.out.println("Circular build path problems: " + circular);
		Hashtable options = JavaCore.getOptions();
		options.put(JavaCore.CORE_CIRCULAR_CLASSPATH, JavaCore.ERROR);
		JavaCore.setOptions(options);
		circular = JavaCore.getOption(JavaCore.CORE_CIRCULAR_CLASSPATH);
		SysOutProgressMonitor.out.println("Circular build path problems set to: " + circular);
		SysOutProgressMonitor.out.println();
		
		String incomplete = JavaCore.getOption(JavaCore.CORE_INCOMPLETE_CLASSPATH);
		SysOutProgressMonitor.out.println("Incomplete build path problems: " + circular);
		options = JavaCore.getOptions();
		options.put(JavaCore.CORE_INCOMPLETE_CLASSPATH, JavaCore.WARNING);
		JavaCore.setOptions(options);
		incomplete = JavaCore.getOption(JavaCore.CORE_INCOMPLETE_CLASSPATH);
		SysOutProgressMonitor.out.println("Incomplete build path problems set to: " + incomplete);
		SysOutProgressMonitor.out.println();
		
		IVMInstall vminstall = JavaRuntime.getDefaultVMInstall();
		if (!vminstall.getName().equals("JRE"))
		{
			SysOutProgressMonitor.out.println("Setting the default JRE name to 'JRE' from '" + vminstall.getName() + "'");
			vminstall.setName("JRE");
			SysOutProgressMonitor.out.println();
		}
		
		IWorkspaceRoot wroot = ResourcesPlugin.getWorkspace().getRoot();
		File workspace = wroot.getLocation().toFile();
		File[] dirs = workspace.listFiles();

		SysOutProgressMonitor.out.println("IMPORTING PROJECTS...");
		for (int i = 0; i < dirs.length; i++)
		{
			if (dirs[i].isDirectory())
			{
				File dotProject = new File(dirs[i], ".project");
				if (dotProject.exists() && dotProject.isFile())
				{
					IProject p = wroot.getProject(dirs[i].getName());
					if (!p.exists())
					{
						SysOutProgressMonitor.out.println("CREATE PROJECT: " + p.getName());
						p.create(monitor);
					}
				}
			}
		}
		
		IProject[] projects = wroot.getProjects();
		for (int i = 0; i < projects.length; i++)
		{
			if (!projects[i].isOpen())
			{
				SysOutProgressMonitor.out.println("OPENING PROJECT: " + projects[i].getName());
				projects[i].open(monitor);
			}
			else
			{
				SysOutProgressMonitor.out.println("REFRESHING PROJECT: " + projects[i].getName());
				projects[i].refreshLocal(IResource.DEPTH_INFINITE, monitor);
			}
		}

		projects = ResourcesPlugin.getWorkspace().computeProjectOrder(projects).projects;
		dumpProjectReferences(projects);
		
		SysOutProgressMonitor.out.println("TOUCHING .classpath, .project AND org.eclipse.wst.common.component FILES TO RELOAD PROJECT DYNAMIC DEPENDENCIES:");
		wroot.accept(new IResourceVisitor(){

			public boolean visit(IResource res) throws CoreException
			{
				if (res.getType() == IResource.FILE)
				{
					if (res.getName().equals(".classpath") || res.getName().equals("org.eclipse.wst.common.component") || res.getName().equals(".project"))
					{
						res.touch(monitor);
					}
				}
				return true;
			}});

		projects = ResourcesPlugin.getWorkspace().computeProjectOrder(projects).projects;
		dumpProjectReferences(projects);
		
		SysOutProgressMonitor.out.println("SETTING .svn DIRECTORIES AS TEAM PRIVATE:");
		
		for (int i = 0; i < 3; i++)
		{
			wroot.accept(new IResourceVisitor(){

				public boolean visit(IResource res)
						throws CoreException
				{
					if (res.getName().equals(".svn"))
					{
						if (!res.isTeamPrivateMember())
						{
							SysOutProgressMonitor.out.println("SVN directory set to Team Private: " + res);
							res.setTeamPrivateMember(true);
						}
						return false;
					}
					return true;
				}});
		}
		
		projects = ResourcesPlugin.getWorkspace().computeProjectOrder(wroot.getProjects()).projects;
		
		dumpProjectReferences(projects);
	}

	@SuppressWarnings("unchecked")
	void revertExternalToolBuilders(Map changes) throws Exception
	{
		Iterator projects = changes.keySet().iterator();

		SysOutProgressMonitor.out.println("REVERTING EXTERNAL TOOL BUILDER OPTIONS TO BE ABLE TO CATCH OUTPUT:");
		while (projects.hasNext())
		{
			IProject p = (IProject) projects.next();
			Map buildermap = (Map) changes.get(p);
			Iterator builders = buildermap.keySet().iterator();
			while (builders.hasNext())
			{
				ICommand command = (ICommand) builders.next();
				ILaunchConfiguration config = BuilderUtils.configFromBuildCommandArgs(p, command.getArguments(), new String[1]);

				SysOutProgressMonitor.out.println("REVERTING EXTERNAL TOOL BUILDER '" + config.getName() + "' ON PROJECT: " + p.getName());
				
				Map attrs = (Map) buildermap.get(command);
				ILaunchConfigurationWorkingCopy configwc = config.getWorkingCopy();
				configwc.setAttribute(IDebugUIConstants.ATTR_APPEND_TO_FILE, ((Boolean)attrs.get(IDebugUIConstants.ATTR_APPEND_TO_FILE)).booleanValue());
				configwc.setAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, (String)attrs.get(IDebugUIConstants.ATTR_CAPTURE_IN_FILE));
				configwc.doSave();
			}
		}
	}
	
	void buildWorkspace(IProgressMonitor monitor) throws Exception
	{
		Thread.sleep(2000);
		ResourcesPlugin.getWorkspace().save(true, monitor);

		SysOutProgressMonitor.out.println("BUILDING WORKSPACE (incremental or full after a clean): ");
		ResourcesPlugin.getWorkspace().build(IResource.DEPTH_INFINITE, monitor);

		SysOutProgressMonitor.out.println();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		HashMap<String,List<IMarker>> errors = new HashMap<String,List<IMarker>>();
		for (int i = 0; i < projects.length; i++)
		{
			IMarker[] markers = projects[i].findMarkers(null, true, IResource.DEPTH_INFINITE);
			for (int m = 0; m < markers.length; m++)
			{
				int severity = markers[m].getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
				if (severity == IMarker.SEVERITY_ERROR)
				{
					List<IMarker> list = errors.get(projects[i].getName());
					if (list == null)
					{
						list = new ArrayList<IMarker>();
						errors.put(projects[i].getName(), list);
					}
					list.add(markers[m]);
					SysOutProgressMonitor.out.println("ERROR: " + markers[m].getResource() + ":" + markers[m].getAttribute(IMarker.LINE_NUMBER, 0) + "(" + markers[m].getAttribute(IMarker.CHAR_START, 0) + "-" + markers[m].getAttribute(IMarker.CHAR_END, 0) + "): " + markers[m].getAttribute(IMarker.MESSAGE));
					SysOutProgressMonitor.out.println("   ERROR TYPE: " + markers[m].getType());
					errorCount++;
				}
			}
		}

		SysOutProgressMonitor.out.println();
		SysOutProgressMonitor.out.println("Errors found: " + errorCount);
		Iterator<String> it = errors.keySet().iterator();
		while (it.hasNext())
		{
			String project = it.next();
			List<IMarker> list = errors.get(project);
			SysOutProgressMonitor.out.println("Project " + project + " has " + list.size() + " errors.");
		}
		SysOutProgressMonitor.out.println();
		
		if (errorCount > 0)
		{
			SysOutProgressMonitor.out.println("Build done with errors!");
		}
		else
		{
			SysOutProgressMonitor.out.println("Build done.");
		}
	}
	
	private IPath makeAbsolute(IPath path)
	{
		IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (res != null)
		{
			return res.getLocation();
		}
		return path;
	}

	void dumpClassPath() throws Exception
	{
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		projects = ResourcesPlugin.getWorkspace().computeProjectOrder(projects).projects;
		
		HashMap<String, LinkedHashSet<IPath>> classpaths = new HashMap<String, LinkedHashSet<IPath>>();
		
		for (int i = 0; i < projects.length; i++)
		{
			if (JavaProject.hasJavaNature(projects[i]))
			{
				SysOutProgressMonitor.out.println("Generating classpath for java project " + projects[i].getName() + ": " + ResourcesPlugin.getWorkspace().getRoot().getLocation().append(projects[i].getName() + ".classpath"));

				LinkedHashSet<IPath> entries = new LinkedHashSet<IPath>();
				classpaths.put(projects[i].getName(), entries);
				
				IJavaProject javaProject = JavaCore.create(projects[i]);
				IClasspathEntry[] classpath = javaProject.getResolvedClasspath(false);
				for (int j = 0; j < classpath.length; j++)
				{
					if (classpath[j].getEntryKind() == IClasspathEntry.CPE_PROJECT)
					{
						IJavaProject referencedProject = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(classpath[j].getPath().toString().substring(1)));
						IClasspathEntry[] referencedentries = referencedProject.getResolvedClasspath(false);
						for (int k = 0; k < referencedentries.length; k++)
						{
							//get project's output directories
							if (referencedentries[k].getEntryKind() == IClasspathEntry.CPE_SOURCE)
							{
								IPath out = referencedentries[k].getOutputLocation();
								if (out == null)
								{
									out = referencedProject.getOutputLocation();
								}
								entries.add(makeAbsolute(out));
							}
							else if (referencedentries[k].getEntryKind() == IClasspathEntry.CPE_LIBRARY && referencedentries[k].isExported())
							{
								entries.add(makeAbsolute(referencedentries[k].getPath()));
							}
							else if (referencedentries[k].getEntryKind() == IClasspathEntry.CPE_PROJECT && referencedentries[k].isExported())
							{
								entries.addAll(classpaths.get(referencedentries[k].getPath().toString().substring(1)));
							}
						}
					}
					else if (classpath[j].getEntryKind() == IClasspathEntry.CPE_SOURCE)
					{
						//get output
						IPath path = classpath[j].getOutputLocation();
						if (path == null)
						{
							path = javaProject.getOutputLocation();
						}
						entries.add(makeAbsolute(path));
					}
					else if (classpath[j].getEntryKind() == IClasspathEntry.CPE_LIBRARY)
					{
						entries.add(makeAbsolute(classpath[j].getPath()));
					}
					else
					{
						throw new Exception("Unknown classpath entry type: " + classpath[j].getEntryKind());
					}
				}
				FileOutputStream fos = new FileOutputStream(new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), projects[i].getName() + ".classpath"));
				PrintStream ps = new PrintStream(fos, true);
				for (IPath p: entries)
				{
					ps.print(p.toOSString());
					ps.print(System.getProperty("path.separator"));
				}
				ps.println();
				ps.close();
			}
		}
	}

}
