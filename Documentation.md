# Introduction #

We have faced the problem of building our application on our continuous integration server. It was a pain to write separate ant build scripts, because all information is already present in the eclipse environment, so nobody interested in synchronizing hundreds of eclipse projects build path settings to build scripts.

I've googled the web for such a plugin, but all have lack of functionality.

# Requirements #

See plugin.xml for detailed requirements. Short version is you have to add this plugin to plugins directory of Eclipse, which should be an edition of 3.4 or 3.5 version for Java Enterprise Edition (JEE). Get it at [the "Eclipse IDE for Java EE Developers" link of Eclipse homepage](http://eclipse.org/downloads).

# Usage #
This plugin is an application plugin. You should invoke like this:

```
eclipsec -nosplash -data <workspace_dir> -application com.ind.eclipse.headlessworkspace.Application [parameters]
```

On Unix like systems, you need to start a fake X server, like Xvfb:

```
Xvfb :7 &
export DISPLAY=:7
eclipse -nosplash -data <workspace_dir> -application com.ind.eclipse.headlessworkspace.Application [parameters]
```

So, it is not a true headless application, because it will initialize a workbench. But in invisible mode. It is necessary, because Ant launching is not separated form the gui. Please see this bug report: https://bugs.eclipse.org/bugs/show_bug.cgi?id=264338

Possible parameters:
#### import ####
Imports all directory containing .project file to the workspace. If a project is already imported, then it will be opened (if closed) and **refreshed**.
The .svn directories will be marked as 'team private', as the workspace assumed to be a checkout from subversion.
#### clean ####
Calls the clean build on every project, causing output folders to be purged.
#### build ####
Builds all projects using **incremental** ('Manual Build') building.
After a clean, or for the first time, a **full build ('After a "Clean"')** will be run.
_It will change the external tools builder to log output to a file. At the end, it will restore the original state._
#### exportwars ####
This will export all dynamic web projects to a war file in the workspace root directory as `<project_name>.war`
#### exportears ####
This will export all enterprise application projects to an ear file in the workspace root directory as `<project_name>.ear`
#### exportjars ####
This will actually search for [\*.jardesc](JarDesc.md) files in each project directory, and runs it. The jardesc contains a path to the destination file. Only the file name (last segment of the path) will be used, and the file will be created in the workspace root directory.
#### exportplugins ####
This will export all plugins in the workspace to the `exportedplugins.zip` file in the workspace root directory.
#### exportfeatures ####
This will export all features and connected plugins in the workspace to the `exportedfeatures.zip` file in the workspace root directory.
#### exportproducts ####
This will search for all `*.product` files, and it will load and use it to export the product (RCP application). The product will be exported into a zip file in the workspace root directory.
#### dumpclasspath ####
The resolved classpath entries for every java project will be dumped to the workspace root directory as `<project_name>.classpath`
#### dumpreferences ####
Dumps the project build order, listing project static and dynamic references.
#### createserver <server name> <properties file> ####
_Under construction_ <br>
Creates a server instance of <server name>.<br>
The properties file should contain the server specific parameters. It will print out what is missing...<br>
<br>
<h2>How It Works</h2>
See the <a href='HowItWorks.md'>How It Works</a> page for details.