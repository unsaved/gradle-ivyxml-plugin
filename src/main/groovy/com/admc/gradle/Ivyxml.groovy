package com.admc.gradle;

import org.gradle.api.Project
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser
import org.apache.ivy.core.module.descriptor.DependencyDescriptor
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyArtifact
import org.gradle.api.internal.artifacts.DefaultExcludeRule
import org.apache.ivy.core.module.descriptor.Configuration.Visibility
import org.gradle.api.artifacts.Configuration;

/**
 * This Gradle support for 'ivy.xml' is based on contributed fragments in Jira
 * issue for GRADLE-197 at http://issues.gradle.org/browse/GRADLE-197 .
 *
 * The code here includes my own enhancements, including
 *     Code review
 *     Support for configuration inheritance via 'extends'
 *     Support for 'transitive' settings on confs and/or dependencies
 *     Support for 'classifier's specified using a .../ivy/maven' namespace
 *     Automatically set Ivy variables for every Gradle String-type property.
 *       (With keys all prefixed with 'gproj|', e.g. use ${gproj|name}).
 *     User-configurable ivy dep file (a real File instance).
 *     User-configurable additional Ivy variables.
 *
 * Set project property ivyDepFile to File of the Ivy dependencies file if
 * you want to use a file other than "ivy.xml" in the project directory.
 * This corresponds to Ivy property 'ivy.dep.file'.
 *
 * Set project property ivyProperties, for the obvious purpose.
 *
 * Does not attempt to load any ivysettings files or do any repository setup,
 * only dependency settings.
 *
 * @author Blaine Simpson  (blaine dot simpson at admc dot com)
 */

class Ivyxml {
    private Project gp
    File depFile
    Map<String, String> ivyProperties
    boolean variablizeProjStrings = true

    Ivyxml(Project p) {
        gp = p
        depFile = gp.file('ivy.xml')
    }

    void load() {
        // configurations apparently not a Collection, since this doesn't work:
        //def gradleProjConfMap = configurations.collectEntries { [(it.name): it] }
        def gradleProjConfMap = [:]
        for (p in gp.configurations) gradleProjConfMap[p.name] = p

        IvySettings ivySettings = new IvySettings();
        ivySettings.defaultInit();
        if (variablizeProjStrings) gp.project.properties.each {
            if (it.value instanceof String)
                ivySettings.setVariable('gproj|' + it.key, it.value, true)
        }
        assert depFile.isFile() && depFile.canRead():
            """Ivy dep file inaccessible:  $depFile.absolutePath
Set plugin property 'ivyxml.depFile' to a File object for your ivy xml file.
"""
        if (ivyProperties != null)
            ivySettings.addAllVariables(ivyProperties, true)
        DefaultModuleDescriptor moduleDescriptor =
                (DefaultModuleDescriptor) XmlModuleDescriptorParser.instance
                .parseDescriptor(ivySettings, depFile.toURL(), false)
        String mavenNsPrefix = null
        for (e in moduleDescriptor.extraAttributesNamespaces)
            if (e.value.endsWith('/ivy/maven')) { mavenNsPrefix = e.key; break; }
        for (def confName in moduleDescriptor.configurationsNames) {
            if (!gradleProjConfMap.containsKey(confName)) continue
            org.apache.ivy.core.module.descriptor.Configuration c =
                    moduleDescriptor.getConfiguration(confName)
            Configuration gradleConfig = gp.configurations.getByName(confName)
            for (parentConfName in c.getExtends())
                gradleConfig.extendsFrom(gp.configurations.getByName(parentConfName))
            gradleConfig.transitive = c.transitive
            gradleConfig.visible = c.visibility == Visibility.PUBLIC
            if (gradleConfig.description == null)
                gradleConfig.description = c.description
        }
        moduleDescriptor.dependencies.each {
            DependencyDescriptor descriptor ->
                def mappableConfNames = descriptor.moduleConfigurations.findAll {
                    gradleProjConfMap.containsKey(it)
                }
            for (mappableConfName in mappableConfNames) {
                if (!mappableConfName) return
                ModuleRevisionId id = descriptor.dependencyRevisionId
                DefaultExternalModuleDependency dep
                def depAttrs = [
                    group: id.organisation,
                    name: id.name,
                    version: id.revision
                ]
                if (mavenNsPrefix != null
                        && descriptor.qualifiedExtraAttributes.containsKey(
                        mavenNsPrefix + ':classifier'))
        {
                    depAttrs['classifier'] = descriptor.qualifiedExtraAttributes[
                            mavenNsPrefix + ':classifier']
        println "CLASSIFIER=$depAttrs.classifier"
        }
                gp.dependencies { dep = add(mappableConfName, depAttrs) }
                dep.changing = descriptor.changing
                dep.transitive = descriptor.transitive

                descriptor.allDependencyArtifacts.each {
                    DependencyArtifactDescriptor depArt ->
                        dep.addArtifact(new DefaultDependencyArtifact(
                                depArt.name, depArt.type, depArt.ext, null, depArt.url))
                }

                def excRuleContainer = dep.excludeRules
                descriptor.excludeRules?.values().each {
                    def ruleList -> ruleList.each {
                        excRuleContainer.add(new DefaultExcludeRule(it.attributes))
                    }
                }
                gradleProjConfMap[mappableConfName].getDependencies().add(dep)
            }
        }
    }
}
