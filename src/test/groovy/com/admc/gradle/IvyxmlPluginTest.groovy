package com.admc.gradle

import org.gradle.api.Project
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ResolveException
import org.gradle.testfixtures.ProjectBuilder
import static org.junit.Assert.*
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.DependencyArtifact
import java.util.regex.Pattern

class IvyxmlPluginTest {
    private Project project

    private static void load(String baseName, Ivyxml ix) {
        URL url = Thread.currentThread().contextClassLoader.getResource(
                baseName + '.xml')
        assert url != null:
            ("""XML file not found as resource in classpath:  $baseName"""
            + '''.xml
''')
        File newFile = File.createTempFile('ivytest', '.xml')
        newFile.deleteOnExit()
        newFile.write(url.getText('UTF-8'), 'UTF-8')
        ix.depFile = newFile
        ix.load()
    }

    {
        project = ProjectBuilder.builder().build()
        project.apply plugin: IvyxmlPlugin
        project.repositories { mavenCentral() }
    }

    @org.junit.Test
    void trivial() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('trivial', project.ivyxml)
        //System.err.println('**' + project.configurations.defaultConf.files.join('|'))
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

    @org.junit.Test
    void classifier() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('classifier', project.ivyxml)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
        assertEquals('sqltool-2.2.6-jdk5.jar',
                project.configurations.defaultConf.files.asList().first().name)
    }

    @org.junit.Test(expected=Exception.class)
    /**
     * Ivy itself requires the 'info' element, though with current version of
     * Ivy it does a terrible job at identifying the problem and throws a NPE.
     */
    void missingInfo() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('noinfo', project.ivyxml)
    }

    @org.junit.Test
    /**
     * We don't do anything with the 'publications' element, but users could
     * process it with something else, so leave it be.
     */
    void publications() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('publications', project.ivyxml)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

    /** Seems there is nothing in the API that allows us to detect existence
     * of this element.  Therefore, disabling this test until I learn otherwise.
     * getConflictManager(moduleId) does not seem to be what we are looking for
     * here.
    @org.junit.Test(expected=GradleException.class)
    void conflicts() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('conflicts', project.ivyxml)
    }
    */

    @org.junit.Test
    void dependencyExcludeNegOrg() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('dependencyExcludeNegOrg', project.ivyxml)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

    @org.junit.Test
    void dependencyExcludeNegMod() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('dependencyExcludeNegMod', project.ivyxml)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

    @org.junit.Test
    void dependencyExcludeNegAll() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('dependencyExcludeNegAll', project.ivyxml)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

    @org.junit.Test
    void dependencyExcludePosAll() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('dependencyExcludePosAll', project.ivyxml)
        assertEquals(0, project.configurations.defaultConf.files.size())
    }

    @org.junit.Test
    void dependencyExcludePosOrg() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('dependencyExcludePosOrg', project.ivyxml)
        assertEquals(0, project.configurations.defaultConf.files.size())
    }

    @org.junit.Test
    void dependencyExcludePosMod() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('dependencyExcludePosMod', project.ivyxml)
        assertEquals(0, project.configurations.defaultConf.files.size())
    }

    /**
     * To be supported ASAP.  But until then...
     */
    @org.junit.Test(expected=GradleException.class)
    void include() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('include', project.ivyxml)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

    /**
     * To be supported ASAP.  But until then...
     DISABLING SINCE IMPOSSIBLE TO DETECT
    @org.junit.Test(expected=GradleException.class)
    void dependenciesExclude() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('dependenciesExclude', project.ivyxml)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }
     */

    @org.junit.Test(expected=GradleException.class)
    void override() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('override', project.ivyxml)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
    }

    @org.junit.Test(expected=GradleException.class)
    void branch() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('branch', project.ivyxml)
    }

    @org.junit.Test(expected=GradleException.class)
    void force() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('force', project.ivyxml)
    }

    @org.junit.Test
    void artifact() {
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('artifact', project.ivyxml)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
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
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('confNarrowAttr', project.ivyxml)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
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
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('confNarrowEl', project.ivyxml)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
    }
     */

    @org.junit.Test
    /**
     * This also tests ivyVariables
     */
    void includeFile() {
        File incFile = File.createTempFile('ivyinclude', '.xml')
        incFile.deleteOnExit()
        incFile.write('''
<configurations defaultconf="defaultConf">
  <conf name="defaultConf" description="Trivially simple conf"
        transitive="false"/>
</configurations>
''', 'UTF-8')
        project.configurations { defaultConf }
        project.ivyxml.ivyVariables = [incFilePath: incFile.absolutePath]
        IvyxmlPluginTest.load('includeFile', project.ivyxml)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

    /* I know no way to detect the unsupported mapping conf element
     * <dependency><conf...>
     */

    @org.junit.Test
    void ivyDepFileSysProperty() {
        project.configurations { defaultConf }
        URL url = Thread.currentThread().contextClassLoader.getResource(
                'trivial.xml')
        assert url != null:
            '''XML file not found as resource in classpath:  trivial.xml
'''
        File newFile = File.createTempFile('ivytest', '.xml')
        newFile.deleteOnExit()
        newFile.write(url.getText('UTF-8'), 'UTF-8')
        String origSysPropertyValue = System.properties['ivy.dep.file']
        System.setProperty('ivy.dep.file', newFile.absolutePath)
        project.ivyxml.load()
        if (origSysPropertyValue == null)
            System.clearProperty('ivy.dep.file')
        else
            System.setProperty('ivy.dep.file', origSysPropertyValue)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

    @org.junit.Test
    void variables() {
        project.configurations { defaultConf }
        project.ivyxml.ivyVariables =
                [('utest.orgsuffix'): 'hsqldb', ('utest.module'): 'sqltool']
        IvyxmlPluginTest.load('variables', project.ivyxml)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
        assertTrue(
                project.configurations.defaultConf.files.asList().first().name
                .startsWith('sqltool-'))
    }

    @org.junit.Test
    void unsetVariables() {
        project.configurations { defaultConf }
        project.ivyxml.ivyVariables = [('utest.orgsuffix'): 'hsqldb']
        // Setting only 1 variable where 2 are required
        assertEquals(0, project.configurations.defaultConf.files.size())
    }

    @org.junit.Test
    void sysProperties() {
        project.configurations { defaultConf }
        String origOrgSuffixValue = System.properties['utest.orgsuffix']
        String origModuleValue = System.properties['utest.module']
        System.setProperty('utest.orgsuffix', 'hsqldb')
        System.setProperty('utest.module', 'sqltool')
        IvyxmlPluginTest.load('variables', project.ivyxml)
        if (origOrgSuffixValue == null)
            System.clearProperty('utest.orgsuffix')
        else
            System.setProperty('utest.orgsuffix', origOrgSuffixValue)
        if (origModuleValue == null)
            System.clearProperty('utest.module')
        else
            System.setProperty('utest.module', origModuleValue)
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
        assertTrue(
                project.configurations.defaultConf.files.asList().first().name
                .startsWith('sqltool-'))
    }

    @org.junit.Test
    void projProperties() {
        project.configurations { defaultConf }
        assert !project.hasProperty('utest.orgsuffix')
        assert !project.hasProperty('utest.module')
        project.setProperty('utest.orgsuffix', 'hsqldb')
        project.setProperty('utest.module', 'sqltool')
        IvyxmlPluginTest.load('projVariables', project.ivyxml)
        // Gradle does not support clearing of property values
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
        assertTrue(
                project.configurations.defaultConf.files.asList().first().name
                .startsWith('sqltool-'))
    }

    @org.junit.Test(expected=ResolveException.class)
    void unsetProjProperties() {
        project.configurations { defaultConf }
        assert !project.hasProperty('utest.orgsuffix')
        assert !project.hasProperty('utest.module')
        project.setProperty('utest.orgsuffix', 'hsqldb')
        project.setProperty('utest.module', 'sqltool')
        project.ivyxml.projIvyVariablePrefix = null
        IvyxmlPluginTest.load('projVariables', project.ivyxml)
        // Gradle does not support clearing of property values
        project.configurations.defaultConf.files.size()
    }

    @org.junit.Test(expected=ResolveException.class)
    void badProjPrefixProperties() {
        project.configurations { defaultConf }
        assert !project.hasProperty('utest.orgsuffix')
        assert !project.hasProperty('utest.module')
        project.setProperty('utest.orgsuffix', 'hsqldb')
        project.setProperty('utest.module', 'sqltool')
        project.ivyxml.projIvyVariablePrefix = 'bad.'
        IvyxmlPluginTest.load('projVariables', project.ivyxml)
        // Gradle does not support clearing of property values
        project.configurations.defaultConf.files.size()
    }

    @org.junit.Test
    void projPrefixProperties() {
        project.configurations { defaultConf }
        assert !project.hasProperty('utest.orgsuffix')
        assert !project.hasProperty('utest.module')
        project.setProperty('utest.orgsuffix', 'hsqldb')
        project.setProperty('utest.module', 'sqltool')
        project.ivyxml.projIvyVariablePrefix = 'explicitPref.'
        IvyxmlPluginTest.load('projPrefixVariables', project.ivyxml)
        // Gradle does not support clearing of property values
        GradleUtil.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
        assertTrue(
                project.configurations.defaultConf.files.asList().first().name
                .startsWith('sqltool-'))
    }
}
