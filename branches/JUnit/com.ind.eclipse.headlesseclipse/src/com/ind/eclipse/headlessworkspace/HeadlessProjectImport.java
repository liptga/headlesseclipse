package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;


/***********************************************************************************
 * Imports new projects from external location to the workspace.
 *
 * @author $Author$
 * @version $Revision$
 *
 * Copyright (c)2009 by Prueftechnik Condition Monitoring GmbH
 * All Rights reserved.
 */
public class HeadlessProjectImport implements IOverwriteQuery
{
	private final String[] args;
	private final IProgressMonitor monitor;
	private final static boolean COPYFILES = true;

	/**
	 * @param args program arguments particularly external source paths
	 * @param monitor the monitor to report to  
	 */
	public HeadlessProjectImport(final String[] args, final IProgressMonitor monitor)
	{
		this.args = args;
		this.monitor = monitor;
	}

	/**
	 * @see org.eclipse.ui.dialogs.IOverwriteQuery#queryOverwrite(java.lang.String)
	 */
	public String queryOverwrite(String pathString) 
	{
		return IOverwriteQuery.ALL;
	}

	/**
	 * Imports projects from specified external location(s).   
	 */
	@SuppressWarnings( { "unchecked" })
	public void importProjects()
	{
		try
		{
			if (args.length == 0)
			{
				SysOutProgressMonitor.out.println();
				SysOutProgressMonitor.out.println("Possible parameters: <sourcepath1> [<sourcepath2>, ...]");
				SysOutProgressMonitor.out.println();
				return;
			}
			else
			{
				final IWorkspaceDescription wsd = ResourcesPlugin.getWorkspace().getDescription();
				wsd.setAutoBuilding(false);
				ResourcesPlugin.getWorkspace().setDescription(wsd);
				ResourcesPlugin.getWorkspace().save(true, null);
				SysOutProgressMonitor.out.println();
				SysOutProgressMonitor.out.println("Auto building is set to false");
				SysOutProgressMonitor.out.println();

				monitor.beginTask("Searching for new projects", 100);
				final List<String> list = Arrays.asList(args);
				Collection files = new ArrayList();
				ProjectRecord[] projects = new ProjectRecord[0];
				monitor.worked(10);
				boolean extDirSpecified = false;
						
				for (String path : list) 
				{
					File directory = new File(path);
					
					if (directory.isDirectory())
					{
						extDirSpecified = true;
						if (!collectProjectFilesFromDirectory(files, directory, null, monitor)) return;
						
						Iterator filesIterator = files.iterator();
						projects = new ProjectRecord[files.size()];
						int index = 0;
						monitor.worked(50);
						monitor.subTask("Processing results");
						while (filesIterator.hasNext()) 
						{
							File file = (File) filesIterator.next();
							projects[index] = new ProjectRecord(file);
							index++;
						}
						
						monitor.worked(70);
						projects = getValidProjects(projects);				
					}
				}

				monitor.done();
				
				if (projects.length == 0)
				{
					if (extDirSpecified)
					{
						SysOutProgressMonitor.out.println("Projects already exist in workspace");
					}
					else
					{
						SysOutProgressMonitor.out.println("External location not specified");
					}
				}
				else
				{
					try
					{
						monitor.beginTask("Import new projects", projects.length);
						
						for (int i = 0; i < projects.length; i++) 
						{
							createExistingProject(projects[i], new SubProgressMonitor(monitor, 1));
						}
					} 
					finally 
					{
						monitor.done();
					}
				}
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace(SysOutProgressMonitor.out);
		}
	}
	
	/**
	 * Create the project described in record. If it is successful return true.
	 * 
	 * @param record
	 * @return boolean <code>true</code> if successful
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unchecked")
	private boolean createExistingProject(final ProjectRecord record, IProgressMonitor monitor) 
	throws InvocationTargetException, InterruptedException 
	{
		String projectName = record.getProjectName();
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProject project = workspace.getRoot().getProject(projectName);
		
		if (record.description == null) 
		{
			// error case
			record.description = workspace.newProjectDescription(projectName);
			IPath locationPath = new Path(record.projectSystemFile.getAbsolutePath());

			// If it is under the root use the default location
			if (Platform.getLocation().isPrefixOf(locationPath)) 
			{
				record.description.setLocation(null);
			} else 
			{
				record.description.setLocation(locationPath);
			}
		} 
		else 
		{
			record.description.setName(projectName);
		}
		
		// import from file system
		File importSource = null;
		
		if (COPYFILES) 
		{
			// import project from location copying files - use default project
			// location for this workspace
			URI locationURI = record.description.getLocationURI();
			// if location is null, project already exists in this location or
			// some error condition occured.
			if (locationURI != null) 
			{
				importSource = new File(locationURI);
				IProjectDescription desc = workspace
						.newProjectDescription(projectName);
				desc.setBuildSpec(record.description.getBuildSpec());
				desc.setComment(record.description.getComment());
				desc.setDynamicReferences(record.description
						.getDynamicReferences());
				desc.setNatureIds(record.description.getNatureIds());
				desc.setReferencedProjects(record.description
						.getReferencedProjects());
				record.description = desc;
			}
		}

		try 
		{
			monitor.beginTask("Creating Projects", 100);
			project.create(record.description, new SubProgressMonitor(monitor, 30));
			project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 70));
		} 
		catch (CoreException e) 
		{
			throw new InvocationTargetException(e);
		} 
		finally 
		{
			monitor.done();
		}

		// import operation to import project files if copy checkbox is selected
		if (COPYFILES && importSource != null) 
		{
			List filesToImport = FileSystemStructureProvider.INSTANCE.getChildren(importSource);
			ImportOperation operation = new ImportOperation(project.getFullPath(), importSource,
					FileSystemStructureProvider.INSTANCE, this, filesToImport);
			operation.setOverwriteResources(true); // need to overwrite .project, .classpath files
			operation.setCreateContainerStructure(false);
			operation.run(monitor);
		}

		return true;
	}
	
	/**
	 * Collect the list of .project files that are under directory into files.
	 * 
	 * @param files
	 * @param directory
	 * @param directoriesVisited
	 * 		Set of canonical paths of directories, used as recursion guard
	 * @param monitor
	 * 		The monitor to report to
	 * @return boolean <code>true</code> if the operation was completed.
	 */
	@SuppressWarnings("unchecked")
	private boolean collectProjectFilesFromDirectory(Collection files,
			File directory, Set directoriesVisited, IProgressMonitor monitor) {

		if (monitor.isCanceled()) {
			return false;
		}
		monitor.subTask(NLS.bind("Checking: {0}", directory.getPath()));
		File[] contents = directory.listFiles();
		if (contents == null)
			return false;

		// Initialize recursion guard for recursive symbolic links
		if (directoriesVisited == null) {
			directoriesVisited = new HashSet();
			try {
				directoriesVisited.add(directory.getCanonicalPath());
			} 
			catch (IOException exception) 
			{
				SysOutProgressMonitor.out.print(exception.getLocalizedMessage());
			}
		}

		// first look for project description files
		final String dotProject = IProjectDescription.DESCRIPTION_FILE_NAME;
		for (int i = 0; i < contents.length; i++) {
			File file = contents[i];
			if (file.isFile() && file.getName().equals(dotProject)) {
				files.add(file);
				// don't search sub-directories since we can't have nested
				// projects
				return true;
			}
		}
		// no project description found, so recurse into sub-directories
		for (int i = 0; i < contents.length; i++) {
			if (contents[i].isDirectory()) {
				if (!contents[i].getName().equals(".metadata") &&
						!contents[i].getName().equals(".svn") &&
						!contents[i].getName().startsWith("target-platform")) 
				{
					try 
					{
						String canonicalPath = contents[i].getCanonicalPath();
						if (!directoriesVisited.add(canonicalPath)) {
							// already been here --> do not recurse
							continue;
						}
					} 
					catch (IOException exception) 
					{
						SysOutProgressMonitor.out.print(exception.getLocalizedMessage());					
					}
					collectProjectFilesFromDirectory(files, contents[i],
							directoriesVisited, monitor);
				}
			}
		}
		return true;
	}
	
	/**
	 * Get the array of valid project records that can be imported from the
	 * source workspace or archive, selected by the user. If a project with the
	 * same name exists in both the source workspace and the current workspace,
	 * it will not appear in the list of projects to import and thus cannot be
	 * selected for import.
	 * 
	 * Method declared public for test suite.
	 * 
	 * @return ProjectRecord[] array of projects that can be imported into the
	 * 	workspace
	 */
	private ProjectRecord[] getValidProjects(ProjectRecord[] projects) {
		List<ProjectRecord> validProjects = new ArrayList<ProjectRecord>();
		for (int i = 0; i < projects.length; i++) {
			if (!isProjectInWorkspace(projects[i].getProjectName())) {
				validProjects.add(projects[i]);
			}
		}
		return (ProjectRecord[]) validProjects.toArray(new ProjectRecord[validProjects.size()]);
	}

	/**
	 * Determine if the project with the given name is in the current workspace.
	 * 
	 * @param projectName
	 * 		String the project name to check
	 * @return boolean true if the project with the given name is in this
	 * 	workspace
	 */
	private boolean isProjectInWorkspace(String projectName) {
		if (projectName == null) {
			return false;
		}
		IProject[] workspaceProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < workspaceProjects.length; i++) {
			if (projectName.equals(workspaceProjects[i].getName())) {
				return true;
			}
		}
		return false;
	}	
	
	/**
	 * This class represents a project record based on a .project file. 
	 */
	private class ProjectRecord 
	{
		File projectSystemFile;

		String projectName;

		IProjectDescription description;

		/**
		 * Create a record for a project based on the info in the file.
		 * 
		 * @param file
		 */
		ProjectRecord(File file) {
			projectSystemFile = file;
			setProjectName();
		}

		/**
		 * Set the name of the project based on the projectFile.
		 */
		private void setProjectName() {
			try 
			{
				// If we don't have the project name try again
				if (projectName == null) {
					IPath path = new Path(projectSystemFile.getPath());
					// if the file is in the default location, use the directory
					// name as the project name
					if (isDefaultLocation(path)) 
					{
						projectName = path.segment(path.segmentCount() - 2);
						description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
					} 
					else 
					{
						description = ResourcesPlugin.getWorkspace().loadProjectDescription(path);
						projectName = description.getName();
					}

				}
			} catch (CoreException e) {
				// no good couldn't get the name
			}
		}

		/**
		 * Returns whether the given project description file path is in the
		 * default location for a project
		 * 
		 * @param path
		 * 		The path to examine
		 * @return Whether the given path is the default location for a project
		 */
		private boolean isDefaultLocation(IPath path) {
			// The project description file must at least be within the project,
			// which is within the workspace location
			if (path.segmentCount() < 2)
				return false;
			return path.removeLastSegments(2).toFile().equals(
					Platform.getLocation().toFile());
		}

		/**
		 * Get the name of the project
		 * 
		 * @return String
		 */
		public String getProjectName() {
			return projectName;
		}
	}

}