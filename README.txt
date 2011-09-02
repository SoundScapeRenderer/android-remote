How to setup the development environment
----------------------------------------

You need to have JDK (Java Development Kit), Eclipse with JDT (Java Development Tools), the Android SDK and ADT (Android Development Tools for Eclipse) installed.
Instructions on how to do the whole setup can be found here: http://developer.android.com/sdk/installing.html

For maximum compatibility, the project still uses Android SDK 1.6, so make sure you install that version too.



How to build the project
------------------------
(This description is for Eclipse 3.5, in current newer versions things might be different.)

Import the project into Eclipse using "File -> Import... -> General -> Existing Projects into Workspace". The project root folder is the folder that contains this readme.

Depending on your default Java Compiler settings there might be a lot of errors. Make sure the Java Compiler compilance level of the project is set to 1.6 (above should work too): Right-click the project and go to "Properties -> Java Compiler", check "Enable project specific settings", set "Compiler compilance level" to "1.6" and check "Use default compilance settings".

Build and run the project: Right-click the project and do "Run As -> Android Application".