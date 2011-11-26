Ivyxml Gradle Plugin

This plugin loads dependency definitions from an Ivy dependency file
(aka "ivy.xml") into Gradle.

This work was motivated by discussion for issue GRADLE-197 at
http://issues.gradle.org/browse/GRADLE-197, and started with code snippets
from there.  I have added much more complete support since then.

This plugin also bundles the Gradle utility method
    public void com.admc.gradle.VerifyResolve(Configuration)
This is a work-around for problems where Gradle silently fails to satisfy some
dependencies.


Advanced features

    Support for configuration inheritance via 'extends'
    Support for 'transitive' settings on confs and/or dependencies
    Support for 'classifier's specified using a .../ivy/maven' namespace
    Automatically set Ivy variables for every Gradle String-type property.
      (With keys all prefixed with 'gproj|', e.g. use ${gproj|name}).
    User-configurable ivy dep file (a real File instance).
    User-configurable additional Ivy variables.

UNSUPPORTED ivy.xml features
    The following ivy.xml elements and attributes are ignored.  I did my best
    to detect and either, support, throw or warn, but the public Ivy API just
    does not allow for access to these settings.
        <conflicts> (and all attrs and sub-elements)
        <dependencies><exclude> 
        <dependencies><dependency><conf>  (For conf-mapping)
        <dependencies><dependency><artifact conf="...">  attr.
        <dependencies><dependency><*><conf>

    The following elements and attribute are purposefully prohibited.  We don't
    support them, it would be misleading to silently ignore then, and we can
    detect their usage.
        <dependencies><dependency><exclude artifact="..." type="..."
            ext="..." matcher="..." cont="..."
            (i.e. only 'org' and 'module' attrs are supported).
        <dependencies><override>
        <dependencies><dependency branch="..." force="..."  attrs.
         (Branch and force have no support in Gradle).
        
    These elements and attributes are purposefully ignored, because the user may
    want to profitably process the same "ivy.xml" file with another tool the can
    make use of them.
        <info> (and all attrs and sub-elements)  (Element is required by Ivy)
        <publications>  (and all attrs and sub-elements)
        <dependencies><dependency revConstraint="...">
          (Will differ from "rev" only when publishing, which is not a use case
          for us).
        <dependencies><dependency revConstraint="..."  attr.


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
        be used like this in the ivy.xml attribute values:
            ${gproj|propertyName}
        Defaults to true.


UTILITY METHOD

    void com.admc.gradle.GradleUtil.verifyResolve(Configuration)
    Throws if all dependencies of the specified Configuration have not been
    satisfied by at least one artifact.
    This method is entirely independent of Ivy XML functionality and is
    bundled here only because it was convenient for me to do so.
    Use it like so:

    buildscript {
        repositories { mavenCentral() }
        dependencies {
            classpath 'com.admc:gradle-ivyxml-plugin:latest.milestone'
        }
    }

    apply plugin: 'ivyxml'

    import com.admc.gradle.GradleUtil

    compileJava.doFirst { GradleUtil.verifyResolve(configurations.compile) }
    // And so forth for all tasks that elicit an Ivy resolve.

    If you get a complaint like "ERRORS a required artifact is not listed by
    module descriptor: *!.*", just rerun the same Gradle command with the
    -s switch.  You will get a clean report about the missing artifacts.



CLASSIFIERS

    Beware that classifiers are incompatible with SNAPSHOT systems.

    Just code your ivy.xml file like this to use classifiers:

    <ivy-module version="2.0" xmlns:m="http://ant.apache.org/ivy/maven">
    ...
    <dependency org="org.hsqldb" name="hsqldb" rev="[2,)" m:classifier="jdk5"/>
