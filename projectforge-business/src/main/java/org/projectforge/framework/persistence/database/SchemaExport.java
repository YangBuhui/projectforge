/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.framework.persistence.database;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Persistence;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class SchemaExport
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SchemaExport.class);

  /**
   * Generates the database schema for the current configured database.
   * 
   * @param filename Write the schema to the given file. No file output, if null.
   * @param script Print the DDL to the console.
   * @param export If true, the script will be executed (export the script to the database).
   */
  public void exportSchema(String filename)
  {
    Map<String, String> props = new HashMap<>();

    props.put("javax.persistence.schema-generation.scripts.action", "create");
    props.put("javax.persistence.schema-generation.scripts.create-target", filename);
    Persistence.generateSchema(PfEmgrFactory.get().getUnitName(), props);

  }

  /**
   * Generates the database schema for the current configured database.
   * 
   * @param filename Write the schema to the given file. No file output, if null.
   * @param script Print the DDL to the console.
   * @param export If true, the script will be executed (export the script to the database).
   */
  public void exportDropSchema(Configuration cfg, String filename, boolean script, boolean export)
  {
    //String[] dropSQL;
    try {
      org.hibernate.tool.hbm2ddl.SchemaExport exp = new org.hibernate.tool.hbm2ddl.SchemaExport(cfg);
      exp.setOutputFile(filename);
      exp.setDelimiter(";\n");
      exp.drop(script, export);
    } catch (HibernateException ex) {
      log.error("Cant't generate drop statements for schema: " + ex.getMessage(), ex);
      return;
    }
  }
}
