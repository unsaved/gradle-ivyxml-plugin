package com.admc.gradle

import org.gradle.api.Project
import org.gradle.api.Plugin

class IvyxmlPlugin implements Plugin<Project> {
    def void apply(Project p) {
        def ddTask = p.task('displayDeps')
        ddTask.description =
                '''Lists Ivy deps only for specified 'config.name'.'''
        ddTask << {
            assert project.hasProperty('config.name'):
            '''Project property 'config.name' required by task 'echoDeps'.
'''
            println (p.configurations[project['config.name']]
                    .allDependencies.size() + ' dependencies for '
                    + p.project['config.name'] + '\n    '
                    +  p.configurations[p.project['config.name']].asPath
                    .replace(System.properties['path.separator'], '\n    '))
        }
        p.extensions.ivyxml = new Ivyxml(p)
    }
}
