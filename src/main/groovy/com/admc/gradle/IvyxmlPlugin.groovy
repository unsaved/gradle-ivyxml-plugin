package com.admc.gradle

import org.gradle.api.Project
import org.gradle.api.Plugin

class IvyxmlPlugin implements Plugin<Project> {
	def void apply(Project p) {
		p.extensions.ivyxml = new Ivyxml(p)
	}
}
