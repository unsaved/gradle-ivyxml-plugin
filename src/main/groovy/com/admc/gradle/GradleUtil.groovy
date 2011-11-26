package com.admc.gradle

import org.gradle.api.GradleException
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.DependencyArtifact
import java.util.regex.Pattern

class GradleUtil {
    /**
     * Throws if any dependencies of the specified Configuration are not
     * satisfied.
     *
     * Has some limitations, so study the output if it throws.
     * One limitation is that if an exclude directive correctly eliminates an
     * artifact, this method won't know about it.
     *
     * @throws GradleException containing list of non-satisfied dependencies
     */
    static final private Pattern variableVersionPattern =
            Pattern.compile('.*[^-\\w.].*')

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
            String depString = (dep.version.indexOf('SNAPSHOT') > -1
                    || GradleUtil.variableVersionPattern.matcher(dep.version)
                    .matches()) ? '' : dep.version
            Pattern pat = Pattern.compile(
                    '\\Q' + dep.name + '-' + depString + '\\E' + '[.-].+')
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
