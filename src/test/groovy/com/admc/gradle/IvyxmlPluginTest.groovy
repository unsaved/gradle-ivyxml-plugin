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

    //@org.junit.Test(expected=GradleException.class)
    @org.junit.Test
    void trivial() {
        Project project = IvyxmlPluginTest.prepProject()
        project.configurations { defaultConf }
        IvyxmlPluginTest.load('trivial', project.ivyxml)
        //System.err.println('**' + project.configurations.defaultConf.files.join('|'))
        IvyxmlPluginTest.verifyResolve(project.configurations.defaultConf)
        assertEquals(1, project.configurations.defaultConf.files.size())
    }

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
