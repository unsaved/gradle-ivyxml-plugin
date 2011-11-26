package com.admc.gradle

import org.gradle.api.Project
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import static org.junit.Assert.*
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.DependencyArtifact
import java.util.regex.Pattern

class IvyxmlPluginTest {
    private static void load(String baseName, Ivyxml ix) {
        URL url = Thread.currentThread().contextClassLoader.getResource(
                baseName + '.xml')
        assert url != null:
            """XML file not found as resource in classpath:  $baseName
"""
        File newFile = File.createTempFile('ivytest', '.xml')
        newFile.deleteOnExit()
        newFile.write(url.getText('UTF-8'), 'UTF-8')
        ix.depFile = newFile
        ix.load()
    }

    private static Project prepProject() {
        Project proj = ProjectBuilder.builder().build()
        proj.apply plugin: IvyxmlPlugin
        proj.repositories { mavenCentral() }
        return proj
    }

    @org.junit.Test
    void trivial() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('trivial', project.ivyxml)
        //System.err.println('**' + project.configurations.defaultConf.files.join('|'))
        IvyxmlPluginTest.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

    @org.junit.Test(expected=Exception.class)
    /**
     * Ivy itself requires the 'info' element, though with current version of
     * Ivy it does a terrible job at identifying the problem and throws a NPE.
     */
    void missingInfo() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('noinfo', project.ivyxml)
    }

    @org.junit.Test
    /**
     * We don't do anything with the 'publications' element, but users could
     * process it with something else, so leave it be.
     */
    void publications() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('publications', project.ivyxml)
        IvyxmlPluginTest.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

    /** Seems there is nothing in the API that allows us to detect existence
     * of this element.  Therefore, disabling this test until I learn otherwise.
     * getConflictManager(moduleId) does not seem to be what we are looking for
     * here.
    @org.junit.Test(expected=GradleException.class)
    void conflicts() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('conflicts', project.ivyxml)
    }
    */

    /**
     * To be supported ASAP.  But until then...
     */
    @org.junit.Test(expected=GradleException.class)
    void dependencyExclude() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('dependencyExclude', project.ivyxml)
        IvyxmlPluginTest.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

    /**
     * To be supported ASAP.  But until then...
     */
    @org.junit.Test(expected=GradleException.class)
    void include() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('include', project.ivyxml)
        IvyxmlPluginTest.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

    /**
     * To be supported ASAP.  But until then...
     DISABLING SINCE IMPOSSIBLE TO DETECT
    @org.junit.Test(expected=GradleException.class)
    void dependenciesExclude() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('dependenciesExclude', project.ivyxml)
        IvyxmlPluginTest.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }
     */

    @org.junit.Test(expected=GradleException.class)
    void override() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('override', project.ivyxml)
        IvyxmlPluginTest.verifyResolve(project.configurations.defaultConf)
    }

    @org.junit.Test(expected=GradleException.class)
    void branch() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('branch', project.ivyxml)
    }

    @org.junit.Test(expected=GradleException.class)
    void force() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('force', project.ivyxml)
    }

    @org.junit.Test
    void artifact() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('artifact', project.ivyxml)
        IvyxmlPluginTest.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
        assertTrue(project.configurations.defaultConf.files.asList()
                .first().name.endsWith('.pom'))
    }

    /*
     * Gradle should support narrowing 'conf' at the artifact level, but
     * apparentely can not.
     * The Gradle configuration attribute is apparently for some other type of
     * configuration not applicable to Maven repositories, according to
     * http://www.gradle.org/current/docs/userguide/dependency_management.html
     IMPOSSIBLE TO DETECT
    @org.junit.Test(expected=GradleException.class)
    void confNarrowAttr() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('confNarrowAttr', project.ivyxml)
        IvyxmlPluginTest.verifyResolve(project.configurations.defaultConf)
    }
    */

    /*
     * Gradle should support narrowing 'conf' at the artifact level, but
     * apparentely can not.
     * The Gradle configuration attribute is apparently for some other type of
     * configuration not applicable to Maven repositories, according to
     * http://www.gradle.org/current/docs/userguide/dependency_management.html
     IMPOSSIBLE TO DETECT
    @org.junit.Test(expected=GradleException.class)
    void confNarrowEl() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('confNarrowEl', project.ivyxml)
        IvyxmlPluginTest.verifyResolve(project.configurations.defaultConf)
    }
     */

    @org.junit.Test
    void includeFile() {
        File incFile = File.createTempFile('ivyinclude', '.xml')
        incFile.deleteOnExit()
        incFile.write('''
<configurations defaultconf="defaultConf">
  <conf name="defaultConf" description="Trivially simple conf"
        transitive="false"/>
</configurations>
''', 'UTF-8')
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        project.ivyxml.ivyProperties = [incFilePath: incFile.absolutePath]
        IvyxmlPluginTest.load('includeFile', project.ivyxml)
        IvyxmlPluginTest.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

    /* I know no way to detect the unsupported mapping conf element
     * <dependency><conf...>
     */


    /**
     * Throws if any dependencies of the specified Configuration are not satisfied
     *
     * @throws GradleException containing list of non-satisfied dependencies
     */
    static void verifyResolve(config) {
        Set<String> artifactSet = [] as Set
        String classifierStr
        boolean satisfied
        List<ModuleDependency> unsatisfiedDeps = []
        List<DependencyArtifact> unsatisfiedArts = []
        config.allDependencies.each { it.artifacts.each {
            // The ModuleDependencies only have artifict members if either
            // classifier or extension/type are specified for the dependency.
            // If neither is, then we'll check the dependency in the findAll
            // loop below.
            DependencyArtifact depArt ->
            classifierStr =
                    (depArt.classifier == null) ? '' : ('-' + depArt.classifier)
            Pattern pat = Pattern.compile(
                    depArt.name + '-.+' + classifierStr + '.' + depArt.extension)
            satisfied = false
            for (File f in config.files) {
                if (pat.matcher(f.name).matches()) {
                    satisfied = true
                    break
                }
            }
            if (!satisfied) {
                unsatisfiedDeps << it
                unsatisfiedArts << depArt
            }
        } }
        config.allDependencies.findAll { it.artifacts.size() == 0 }.each {
            ModuleDependency dep ->
            Pattern pat = Pattern.compile(
                    dep.name + '-' + dep.version + '[.-].+')
            satisfied = false
            for (File f in config.files) {
                if (pat.matcher(f.name).matches()) {
                    satisfied = true
                    break
                }
            }
            if (!satisfied) {
                unsatisfiedDeps << dep
                unsatisfiedArts << null
            }
        }
        if (unsatisfiedDeps.size() == 0) return
        StringBuilder sb = new StringBuilder()
        DependencyArtifact depArt
        int oneBasedInt
        unsatisfiedDeps.eachWithIndex{ ModuleDependency dep, int i ->
            depArt = unsatisfiedArts[i]
            sb.append('    ').append(i + 1)
            sb.append(": $dep.group:$dep.name:$dep.version")
            if (depArt != null) {
                sb.append('  ARTIFACT: ').append("$depArt.name $depArt.extension")
                if (depArt.classifier != null)
                    sb.append(" (classifier '$depArt.classifier')")
            }
            sb.append('\n')
        }
        throw new GradleException(
            "Unsatisfied dependencies for configuration '$config.name':\n" + sb)
    }
}
