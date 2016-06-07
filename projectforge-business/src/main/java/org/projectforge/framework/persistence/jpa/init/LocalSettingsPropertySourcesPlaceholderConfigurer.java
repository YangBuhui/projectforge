package org.projectforge.framework.persistence.jpa.init;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import de.micromata.genome.util.runtime.LocalSettings;

/**
 * Resolve ${} expression.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class LocalSettingsPropertySourcesPlaceholderConfigurer extends PropertyPlaceholderConfigurer
{
  private static final Logger log = Logger.getLogger(LocalSettingsPropertySourcesPlaceholderConfigurer.class);

  @Override
  protected String resolvePlaceholder(String placeholder, Properties props)
  {
    String ret = LocalSettings.get().get(placeholder);
    if (log.isDebugEnabled() == true) {
      log.debug("LocalSettings; " + placeholder + "=" + ret);
    }
    return ret;
    //return StringUtils.defaultString(ret);
  }
}
