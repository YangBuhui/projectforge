package org.projectforge.web.configuration;

import java.util.HashMap;
import java.util.Map;

import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaServiceImpl;
import org.projectforge.renderer.custom.Formatter;
import org.projectforge.renderer.custom.FormatterFactory;
import org.projectforge.renderer.custom.MicromataFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;
import de.micromata.genome.db.jpa.tabattr.impl.TimeableServiceImpl;

@Configuration
public class ProjectforgeWebConfiguration
{

  @Autowired
  ApplicationContext applicationContext;

  @Bean
  public GuiAttrSchemaService guiAttrSchemaService()
  {
    return new GuiAttrSchemaServiceImpl();
  }

  @Bean
  public TimeableService timeableService()
  {
    TimeableServiceImpl ret = new TimeableServiceImpl();
    ret.setAttrSchemaService(guiAttrSchemaService());
    return ret;
  }

  @Bean
  public FormatterFactory formatterFactory()
  {
    FormatterFactory fac = new FormatterFactory();
    Map<String, Formatter> formatters = new HashMap<>();
    formatters.put("Micromata", applicationContext.getBean(MicromataFormatter.class));
    fac.setFormatters(formatters);
    return fac;
  }

}
