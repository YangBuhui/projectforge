package org.projectforge.test;

import org.projectforge.framework.configuration.DatabaseOrmConfiguration;
import org.projectforge.web.servlet.SMSReceiverServlet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ComponentScan({ "org.projectforge", "de.micromata.mgc.jpa.spring" })
public class TestConfiguration
{

  @Bean
  public JdbcTemplate jdbcTemplate()
  {
    return new JdbcTemplate(DatabaseOrmConfiguration.dataSource());
  }

  @Bean
  public InitTestDB initTestDB()
  {
    return new InitTestDB();
  }

  @Bean
  public SMSReceiverServlet smsReceiverServlet()
  {
    return new SMSReceiverServlet();
  }

}
