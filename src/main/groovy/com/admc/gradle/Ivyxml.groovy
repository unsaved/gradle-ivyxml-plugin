package com.admc.gradle;

import org.gradle.api.Project
import org.gradle.api.GradleException
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser
import org.apache.ivy.core.module.descriptor.DependencyDescriptor
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor
import org.apache.ivy.core.module.descriptor.Configuration.Visibility
import org.apache.ivy.core.module.descriptor.OverrideDependencyDescriptorMediator

import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyArtifact
import org.gradle.api.internal.artifacts.DefaultExcludeRule
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
 * Set project property depFile to the Ivy dependecy file (type java.io.File)
 * you want to use a file other than "ivy.xml" in the project directory.
 * This corresponds to Ivy property 'ivy.dep.file'.
 *
 * Set project property ivyProperties to a Map<String, String>, for the
 * obvious purpose.
 *
 * Does not attempt to load any ivysettings files or do any repository setup,
 * only dependency settings.
 *
 * @author Blaine Simpson  (blaine dot simpson at admc dot com)
 */

class Ivyxml {
    /*
     * I don't see from Ivy API how I can detect a publications element.
     * If learn how to, display a warning that this plugin does nothing with
     * that element.
     *
     * DefaultDependencyArtifact is pretty essential here, and is not in the
     * public API.
     * http://massapi.com/source/gradle-0.9-rc-3/subprojects/gradle-core/src/main/groovy/org/gradle/api/internal/artifacts/dependencies/DefaultDependencyArtifact.java.html
     */
    private Project gp
    File depFile
    Map<String, String> ivyProperties
    boolean variablizeProjStrings = true

    Ivyxml(Project p) {
        gp = p
        depFile = gp.file('ivy.xml')
    }

    void load() {
        def gradleProjConfMap =
                gp.configurations.collectEntries { [(it.name): it] }

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
        if (ivyProperties != null) {
            ivySettings.addAllVariables(ivyProperties, true)
            gp.logger.info('Added ' + ivyProperties.size()
                    + ' properties from user-supplied map as Ivy variables')
        }
        DefaultModuleDescriptor moduleDescriptor =
                (DefaultModuleDescriptor) XmlModuleDescriptorParser.instance
                .parseDescriptor(ivySettings, depFile.toURL(), false)
        if (moduleDescriptor.allDependencyDescriptorMediators.any {
            it.allRules.values().any {
                it instanceof OverrideDependencyDescriptorMediator
            }
        }) throw new GradleException(
                '''Ivy 'override' element not supported yet''')
        String mavenNsPrefix = null
        for (e in moduleDescriptor.extraAttributesNamespaces)
            if (e.value.endsWith('/ivy/maven')) { mavenNsPrefix = e.key; break; }
        moduleDescriptor.configurationsNames.each { confName ->
            if (!gradleProjConfMap.containsKey(confName)) return
            org.apache.ivy.core.module.descriptor.Configuration c =
                    moduleDescriptor.getConfiguration(confName)
            Configuration gradleConfig = gp.configurations.getByName(confName)
            c.extends.each { parentConfName ->
                gradleConfig.extendsFrom(gp.configurations.getByName(parentConfName))
            }
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
                if (descriptor.canExclude())
                    // This catches <dependency><exclude>, but not
                    // <dependencies><exclude>.
                    throw new GradleException(
                    '''Plugin does not yet support dependency 'exclude'.''')
                if (descriptor.allIncludeRules.size() != 0)
                    throw new GradleException(
                    '''Plugin does not yet support dependency 'include'.''')
                if (descriptor.force)
                    throw new GradleException(
                            '''Gradle does not support Ivy 'force'.''')
                ModuleRevisionId id = descriptor.dependencyRevisionId
                if (id.branch != null)
                    throw new GradleException(
                            '''Gradle does not support Ivy 'branches'.''')
                def depAttrs = [
                    group: id.organisation,
                    name: id.name,
                    version: id.revision
                ]
                if (mavenNsPrefix != null
                        && descriptor.qualifiedExtraAttributes.containsKey(
                        mavenNsPrefix + ':classifier'))
                    depAttrs['classifier'] = descriptor.qualifiedExtraAttributes[
                            mavenNsPrefix + ':classifier']
                DefaultExternalModuleDependency dep
                gp.dependencies { dep = add(mappableConfName, depAttrs) }
                dep.changing = descriptor.changing
                dep.transitive = descriptor.transitive

                descriptor.allDependencyArtifacts.each {
                    DependencyArtifactDescriptor depArt ->
                       // depArt.configurations will always be populated here,
                       // even if no conf attribute or element inside of
                       // <dependencies>.
                        // FUCKING IMPOSSIBLE to detect conf attr or subelement
                        // here!  The 'conf' attr doesn't even appear like
                        // every other attr does with
                        // depArt.getAttribute('conf')
                        // or depArt.getAttributes().
                        dep.addArtifact(new DefaultDependencyArtifact(
                          // TODO:  Try to set classifier here.
                          // the param of this constructor is for classifier.
                                depArt.name, depArt.type, depArt.ext, null, depArt.url))
                }

                def excRuleContainer = dep.excludeRules
                descriptor.excludeRules?.values().each {
                    def ruleList -> ruleList.each {
                        throw new GradleException(
                                'Excludes not supported by plugin yet')
                        // Following statement is totally broken.
                        // Ivy and Gradle use different names for these
                        // attributes, so you can't just share an att. map.
                        excRuleContainer.add(new DefaultExcludeRule(it.attributes))
                    }
                }
                gradleProjConfMap[mappableConfName].getDependencies().add(dep)
            }
        }
    }
}
