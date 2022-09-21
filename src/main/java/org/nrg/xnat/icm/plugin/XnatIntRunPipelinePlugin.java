
package org.nrg.xnat.icm.plugin;

import org.springframework.context.annotation.ComponentScan;
import org.nrg.framework.annotations.XnatPlugin;

@XnatPlugin(value = "xnatIntRunPipelinePlugin", name = "XNAT 1.7 INT Run Pipeline Plugin", description = "Run pipeline from a project", log4jPropertiesFile = "xnatIntRunPipelinePlugin-log4j.properties")
@ComponentScan({ "org.nrg.xnat.icm.rest" })
public class XnatIntRunPipelinePlugin
{
}