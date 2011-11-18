Ivyxml Gradle Plugin

This plugin loads dependency definitions from an Ivy dependency file
(aka "ivy.xml") into Gradle.

This work was motivated by discussion for issue GRADLE-197 at
http://issues.gradle.org/browse/GRADLE-197, and started with code snippets
from there.  I have added much more complete support since then.

Advanced features
    Support for configuration inheritance via 'extends'
    Support for 'transitive' settings on confs and/or dependencies
    Support for 'classifier's specified using a .../ivy/maven' namespace
    Automatically set Ivy variables for every Gradle String-type property.
      (With keys all prefixed with 'gproj|', e.g. use ${gproj|name}).
    User-configurable ivy dep file (a real File instance).
    User-configurable additional Ivy variables.


USAGE

Pull plugin from Internet.

    Couldn't be easier.  This will pull the plugin from Maven Central:

        buildscript {
            mavenCentral()
            dependencies {
                classpath 'com.admc:gradle-ivyxml-plugin:latest.milestone'
            }
        }
        apply plugin: 'ivyxml'
        apply plugin: 'java'  // Load any plugins that define configurations
        ...
        configurations {
        ...             // Define your custom configurations
        }
        // Before the "ivyxml.load()", you must define all configurations
        // that you want to populate from the ivy.xml file.  Other confs in
        // the ivy.xml file with be ignored.
        ivyxml.load()
        // Note that the load() does not resolve or download anything.
        // What load()s are dependency records.

        // Here's an example that triggers a resolve and download:
        println configurations['confName'].asPath

Use plugin jar file locally.

    Just use your browser to go to the IvyXml directory at Maven
    Central.  http://repo1.maven.org/maven2/com/admc/gradle-ivyxml-plugin
    Click into the version that you want.
    Right-click and download the only *.jar file in that directory.

    You can save the plugin jar with your project, thereby automatically
    sharing it with other project developers (assuming you use some SCM system).
    Or you can store it in a local directory, perhaps with other Gradle plugin
    jars.  The procedure is the same either way:

        buildscript { dependencies {
            classpath fileTree(
                dir: 'directory/containing/the/plugin/jar/file',
                include: 'gradle-ivyxml-plugin-*.jar
            )
        } }
        apply plugin: 'ivyxml'
        apply plugin: 'java'  // Load any plugins that define configurations
        ...
        configurations {
        ...             // Define your custom configurations
        }
        // Before the "ivyxml.load()", you must define all configurations
        // that you want to populate from the ivy.xml file.  Other confs in
        // the ivy.xml file with be ignored.
        ivyxml.load()
        // Note that the load() does not resolve or download anything.
        // What load()s are dependency records.

        // Here's an example that triggers a resolve and download:
        println configurations['confName'].asPath


SETTINGS

    File ivyxml.depFile
        The Ivy dependency file to load when ivyxml.load() is executed.
        Corresponds to native Ivy property 'ivy.dep.file'.
        Defaults to file('ivy.xml').

    Map<String, String> ivyxm.ivyProperties
        You can use these variables in the ivy.xml file like ${this}.
        By the way, you can always reference Java system properties in the
        same way.
        Defaults to null

    boolean ivyxml.variablizeProjStrings
        If true, then all String type properties of your Gradle Project may
        be used like ${this} in the ivy.xml file.
        Defaults to true.


CLASSIFIERS

    Beware that classifiers are incompatible with SNAPSHOT systems.

    Just code your ivy.xml file like this to use classifiers:

    <ivy-module version="2.0" xmlns:m="http://ant.apache.org/ivy/maven">
    ...
    <dependency org="org.hsqldb" name="hsqldb" rev="[2,)" m:classifier="jdk5"/>
