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
 * Usage documentation is in the README.txt file for this project.
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
    Map<String, String> ivyVariables
    String projIvyVariablePrefix = 'gproj|'
    boolean instantiateConfigurations = true

    Ivyxml(Project p) {
        gp = p
    }

    void load() {
        def gradleProjConfMap =
                gp.configurations.collectEntries { [(it.name): it] }

        IvySettings ivySettings = new IvySettings();
        ivySettings.defaultInit();
        if (projIvyVariablePrefix != null) gp.properties.each {
            if (it.value instanceof String)
                ivySettings.setVariable(
                        projIvyVariablePrefix + it.key, it.value, true)
        }
        File file = ((depFile == null)
                ? gp.file((System.properties['ivy.dep.file'] == null)
                        ? 'ivy.xml' : System.properties['ivy.dep.file'])
                : depFile)
        assert file.isFile() && file.canRead():
            """Ivy dep file inaccessible:  $file.absolutePath
Set plugin property 'ivyxml.depFile' to a File object for your ivy xml file.
"""
        if (ivyVariables != null) {
            ivySettings.addAllVariables(ivyVariables, true)
            gp.logger.info('Added ' + ivyVariables.size()
                    + ' properties from user-supplied map as Ivy variables')
        }
        DefaultModuleDescriptor moduleDescriptor =
                (DefaultModuleDescriptor) XmlModuleDescriptorParser.instance
                .parseDescriptor(ivySettings, file.toURL(), false)
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
            if (!gradleProjConfMap.containsKey(confName)) {
                if (!instantiateConfigurations) return
                gp.configurations {
                    gradleProjConfMap[confName] = add(confName)
                }
            }
            org.apache.ivy.core.module.descriptor.Configuration c =
                    moduleDescriptor.getConfiguration(confName)
            Configuration gradleConfig = gp.configurations.getByName(confName)
            c.extends.each { parentConfName ->
                if (!gradleProjConfMap.containsKey(parentConfName))
                    gp.configurations {
                        gradleProjConfMap[parentConfName] = add(parentConfName)
                    }
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
                /*
                if (descriptor.canExclude())
                N.b. this catches the case we don't want to catch,
                <dependency><exclude>, instead of <dependencies><exclude>.
                    throw new GradleException(
                    '''Plugin does not yet support dependency 'exclude'.''')
                */
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
                          // param 4 of this constructor is for classifier.
                                depArt.name, depArt.type,
                                depArt.ext, null, depArt.url))
                }

                def excRuleContainer = dep.excludeRules
                descriptor.excludeRules?.values().each {
                    def ruleList -> ruleList.each {
                        def excludeAttrs = [:]
                        it.attributes.each { k, v ->
                            if (k == 'matcher') {
                                if (v == 'exact') return
                            } else if (v == '*') {
                                return
                            }
                            if (k == 'organisation') excludeAttrs['group'] = v
                            else if (k == 'module') excludeAttrs['module'] = v
                            else throw new GradleException(
                                    '''Dependency 'exclude ' does not '''
                                    + "support Ivy attribute '$k'")
                        }
                        assert excludeAttrs.size() > 0
                        excRuleContainer.add(
                                new DefaultExcludeRule(excludeAttrs))
                    }
                }
                gradleProjConfMap[mappableConfName].getDependencies().add(dep)
            }
        }
    }
}
