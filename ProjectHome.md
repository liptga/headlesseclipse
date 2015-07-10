# News #

## The project is officially dead now. Both major contributor became Maven fan. If you feel like you would continue this project, mail to us, and we will grant you the permissions you need. Good luck, and go to use Maven :) ##

1.1.8 published on 2010.01.14. See [release notes](http://bit.ly/4CNOtY) for details

## Installation ##
Update site is moved to http://eclipse.indweb.hu.
To install, use update manager of Eclipse. There are other plugins in this site, so use the [following setting.](http://headlesseclipse.googlecode.com/svn/pictures/install.png)

Invite us for a beer
<a href='http://bit.ly/29b7mZ'><img src='https://www.paypal.com/en_US/i/btn/btn_donateCC_LG.gif' /></a>
## About ##
**Eclipse projects contain a lot of information** about dependencies. Such information is necessary to compile the projects if you want to use some continuous integration tool. A lot of ant scripts must be created if one want to do the same as Eclipse does during build, export and so on. The information will be **duplicated**. If dependency changes, ant script will change as well. It would be **so great to avoid such work and use Eclipse itself for building**. Would not it? This plugin does the tricks instead of You. The plugin can be well used to create builds of traditional Java EE web modules and applications.

# One Minute Version of [Usage Guide](http://code.google.com/p/headlesseclipse/wiki/Documentation) #


On Windows:
```
eclipsec -nosplash -data <workspace_dir> -application com.ind.eclipse.headlessworkspace.Application [parameters]
```
On Unix like systems, you need to start a fake X server, like Xvfb:
```
Xvfb :7 &
export DISPLAY=:7
eclipse -nosplash -data <workspace_dir> -application com.ind.eclipse.headlessworkspace.Application [parameters]
```
### Example ###
```
mkdir ProjectWorkspace
cd ProjectWorkspace
svn checkout http://svn.somewhere.org/svn/Project/trunk .
eclipsec -nosplash -data . -application com.ind.eclipse.headlessworkspace.Application import 
   clean build exportwars exportjars exportears exportplugins exportfeatures  exportproducts dumpclasspath
```
This will create a directory, check out projects from svn, then call this plugin to setup a workspace (import), clean output directories (if necessary), build the workspace.
<br><br>
Then export wars (dynamic web projects), export jars (actually running <a href='JarDesc.md'>*.jardesc</a> files in all project root directories), export ear files (Enterprise Application projects)  to the workspace root directory.<br>
<br><br>
Then it will export all plugin projects to the <code>exportedplugins.zip</code> file in the workspace root, and will export all features and connected plugins to the <code>exportedfeatures.zip</code> file in the workspace root directory.<br>
<br><br>
Then it will export all products, looking for all <code>*.product</code> files in the workspace. The products are exported in a zip file in the workspace root directory.<br>
<br><br>
Then creates <code>*</code>.classpath files for each java project (with resolved entries: only directory and jar file references).<br>
<br><br>
War and Ear files are ready to deploy. So this plugin is very useful for continuous integration. The next step is to deploy your application, and run your tests.<br>
<br><br>
<i>(We are planning, that this plugin will capable of creating a server and deploy your projects to this server, and then run unit tests)</i>

<h2>Contribute!</h2>
Feedback, feedback, feedback... Install it, and use! Any report is appreciated. If you feel to take part, contact us please.