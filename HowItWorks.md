# How it works #

## Initial settings ##
The plugin will turn off automatic building.
It will set circular dependencies as an error.
It will set incomplete build path problems to warning only.
It will set the name of the default jre to 'JRE'. (some JST tool uses direct references to named installed JREs, so we used this name in our projects)
<br>
It will also set a path variable called <code>workspace</code>, which is set to the workspace root.<br>
<br>
<h2>Importing projects</h2>
This plugin can import projects into the workspace, so it can initialize a directory as a workspace. It is very useful in case of CI. It will checkout the projects, this plugin will import them into the workspace. It will mark <code>.svn</code> directories as team private to avoid copying .svn directories to the output directory.<br>
<br>If a project is already imported (e.g. running this plugin on a regular base), the <b>project will be refreshed</b>.<br>
<br>
<h2>Correcting build order</h2>
After importing, it will correct the build order. It is necessary because dynamic project references can be missing due to improper importing order (wich order is OS and file system dependent). This is done using "touch" on a selected set of resources (.project, .classpath, etc.).<br>
<br>
<h2>Building</h2>
It is now nearly ready for the build. But what about external tool builders? Ant needs the gui to be initialized. So a workbench must be initialized (in invisible mode). Another problem with ant is the console. I haven't achieved to catch output of the Console. So I change the external tool builders' launch configuration to log output to a file, and I poll this file instead. (At the end, the launch configs will be reverted)<br>
<br>
So, it is now possible to call builds (clean, and workspace build). The workspace build will start a <b>full build</b> (aka After a Clean) <b>after a pure import or after clean</b>, <b>otherwise</b> it will start <b>incremental</b> (aka Manual Build) build. It is good to remember this if you wish to use external tool builders.<br>
<br>
<h2>Exporting dynamic web projects</h2>
If <code>exportwars</code> parameter was given, all dynamic web projects will be exported to <code>&lt;project_name&gt;.war</code> file in the workspace directory. This will call the same code as the popup menu on a dynamic web project.<br>
<br>
<h2>Exporting enterprise application projects</h2>
If <code>exportears</code> parameter was given, all enterprise application projects will be exported to <code>&lt;project_name&gt;.ear</code> file in the workspace directory. This will call the same code as the popup menu on an enterprise application project.<br>
<br>
<h2>Exporting PDE plugin projects</h2>
If <code>exportplugins</code> parameter was given, all plugin projects will be exported to the <code>exportedplugins.zip</code> file in the workspace root directory. This is the same as exporting a plugin from the UI, and using an archive file as a destination. Sources will not be exported.<br>
<br>
<h2>Exporting PDE feature projects</h2>
If <code>exportfeatures</code> parameter was given, all feature projects and all connected plugin projects will be exported to the <code>exportedfeatures.zip</code> file in the workspace root directory. This is the same as exporting a feature from the UI, and using an archive file as a destination. Sources will not be exported.<br>
<br>
<h2>Exporting RCP applications (products)</h2>
If <code>exportproducts</code> parameter was given, then the plugin will search for <code>*.product</code> files, and it will load it and use it to export the product. The product will be exported to a zip file in the workspace root directory, with the same basename as the <code>*.product</code> file. It will not synchronize the product with the defining plugin.<br>
<br>
<h2>Exporting jar files</h2>
If <code>exportjars</code> parameter was given, then the plugin will search for all <a href='JarDesc.md'>*.jardesc</a> files in all project root directories. The plugin will load the description, changes the destination file path to only the last part of the path (just the file name). This will cause that the file will be generated to the workspace root. Then sets the following: will not save the description, will not save the generated manifest (if it is generated), it will not cause to start a build process (as assumed that the workspace is already built). Then calls the normal export, as if one right-clicks on this file, and clicks on "Create JAR".<br>
<br>
<h2>Dumping resolved classpaths of java projects</h2>
If <code>dumpclasspath</code> parameter was given, then the resolved classpath of all java projects will be written to a file called <code>&lt;project_name&gt;.classpath</code>. This file will contain only directories and jar files separated with OS specific path separator character. Resolved means that containers and referenced java projects (with exported libraries, etc). will be resolved to absolute directories or absolute jar file locations. (e.g. the JRE classpath container will be resolved to a set of jar files, including the rt.jar, etc...)<br>
<br>
<h2>Creating a server instance</h2>
It will search the server config of the given name, creates a server runtime associated with the server, and then creates a server. Then it can associate projects with this server, and start it... but it does not work... the start method hangs...