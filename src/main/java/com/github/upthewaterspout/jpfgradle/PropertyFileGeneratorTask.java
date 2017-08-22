/**
 * Copyright 2017 Dan Smith
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.upthewaterspout.jpfgradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.PropertyState;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Task to generate a jpf.properties file based on the configuration
 * of the gradle project
 */
public class PropertyFileGeneratorTask extends DefaultTask {
  private final File outputFile;
  private PropertyState<String> sourceSetProperty;
  private PropertyState<Map> propertiesProperty;

  public PropertyFileGeneratorTask() {
    outputFile = new File(getProject().getRootDir(), "jpf.properties");
    sourceSetProperty = getProject().property(String.class);
    propertiesProperty = getProject().property(Map.class);
  }

  @Input
  public void setSourceSet(Provider<String> sourceSets) {
    this.sourceSetProperty.set(sourceSets);
  }

  @Input
  public void setProperties(Provider<Map> properties) {
    this.propertiesProperty.set(properties);
  }

  @OutputFile
  public File getOutputFile() {
    return outputFile;
  }

  @TaskAction
  @SuppressWarnings("unchecked")
  public void generateJpfProperties() throws IOException {
    Properties properties = new Properties();
    properties.putAll(propertiesProperty.get());
    setClasspathAndSourcePath(properties);

    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      properties.store(fos, "Generated by jpf-gradle plugin. Do not edit by hand");
    }
  }

  private void setClasspathAndSourcePath(final Properties properties) {
    Project project = getProject();
    project.getPlugins().apply(JavaPlugin.class);
    JavaPluginConvention javaConvention =
        project.getConvention().getPlugin(JavaPluginConvention.class);
    SourceSet sourceSet = javaConvention.getSourceSets().getByName(sourceSetProperty.get());

    String classpath = sourceSet.getRuntimeClasspath().getAsPath();
    properties.put("classpath", classpath);

    List<FileCollection> sourceList = javaConvention.getSourceSets().stream()
        .map(source -> source.getAllJava().getSourceDirectories())
        .collect(Collectors.toList());

    String sourcepath = project.files(sourceList.toArray()).getAsPath();
    properties.put("sourcepath", sourcepath);
  }
}
