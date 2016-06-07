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

package org.projectforge.framework.persistence.xstream;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.io.output.NullWriter;
import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.proxy.HibernateProxyHelper;
import org.projectforge.framework.persistence.hibernate.HibernateCompatUtils;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.thoughtworks.xstream.MarshallingStrategy;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.mapper.MapperWrapper;

/**
 * Hilfsklasse zum Laden und Speichern einer gesamten Hibernate-Datenbank im XML-Format. Zur Darstellung der Daten in
 * XML wird XStream zur Serialisierung eingesetzt. Alle Lazy-Objekte aus Hibernate werden vollständig initialisiert.
 * http://jira.codehaus.org/browse/XSTR-377
 * 
 * @author Wolfgang Jung (w.jung@micromata.de)
 * 
 */
public class HibernateXmlConverter
{
  /** The logger */
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HibernateXmlConverter.class);

  /** the wrapper to hibernate */
  private HibernateTemplate hibernate;

  // Ignore these objects listing in the top level list saving because the are saved implicit by their parent objects.
  private final Set<Class<?>> ignoreFromTopLevelListing = new HashSet<Class<?>>();

  public HibernateXmlConverter()
  {
    // TODO HISTORY
    //    this.ignoreFromTopLevelListing.add(PropertyDelta.class);
    //    this.ignoreFromTopLevelListing.add(SimplePropertyDelta.class);
    //    this.ignoreFromTopLevelListing.add(AssociationPropertyDelta.class);
    //    this.ignoreFromTopLevelListing.add(CollectionPropertyDelta.class);
  }

  /**
   * Initialisierung der Hibernate-verbindung.
   * 
   * @param hibernate ein bereits initialisiertes HibernateTemplate
   */
  public void setHibernate(final HibernateTemplate hibernate)
  {
    this.hibernate = hibernate;
  }

  /**
   * Schreibt alle Objekte der Datenbank in den angegebenen Writer.<br/>
   * <b>Warnung!</b> Bei der Serialisierung von Collections wird derzeit nur {@link java.util.Set} sauber unterstützt.
   * 
   * @param writer Ziel für die XML-Datei.
   * @param includeHistory bei false werden die History Einträge nicht geschrieben
   */
  public void dumpDatabaseToXml(final Writer writer, final boolean includeHistory)
  {
    dumpDatabaseToXml(writer, includeHistory, true);
  }

  /**
   * Schreibt alle Objekte der Datenbank in den angegebenen Writer.<br/>
   * <b>Warnung!</b> Bei der Serialisierung von Collections wird derzeit nur {@link java.util.Set} sauber unterstützt.
   * 
   * @param writer Ziel für die XML-Datei.
   * @param includeHistory bei false werden die History Einträge nicht geschrieben
   * @param preserveIds If true, the object ids will be preserved, otherwise new ids will be assigned through xstream.
   */
  public void dumpDatabaseToXml(final Writer writer, final boolean includeHistory, final boolean preserveIds)
  {
    final TransactionTemplate tx = new TransactionTemplate(
        new HibernateTransactionManager(hibernate.getSessionFactory()));
    tx.execute(new TransactionCallback()
    {
      @Override
      public Object doInTransaction(final TransactionStatus status)
      {
        hibernate.execute(new HibernateCallback()
        {
          @Override
          public Object doInHibernate(final Session session) throws HibernateException
          {
            writeObjects(writer, includeHistory, session, preserveIds);
            status.setRollbackOnly();
            return null;
          }
        });
        return null;
      }
    });
  }

  public HibernateXmlConverter appendIgnoredTopLevelObjects(final Class<?>... types)
  {
    if (types != null) {
      for (final Class<?> type : types) {
        this.ignoreFromTopLevelListing.add(type);
      }
    }
    return this;
  }

  /**
   * @param writer
   * @param includeHistory
   * @param session
   * @throws DataAccessException
   * @throws HibernateException
   */
  private void writeObjects(final Writer writer, final boolean includeHistory, final Session session,
      final boolean preserveIds)
          throws DataAccessException, HibernateException
  {
    // Container für die Objekte
    final List<Object> all = new ArrayList<Object>();
    final XStream stream = initXStream(session, true);
    final XStream defaultXStream = initXStream(session, false);

    session.flush();
    // Alles laden
    //    final List<Class<?>> entities = new ArrayList<Class<?>>();
    final List<Class<?>> entities = PfEmgrFactory.get().getMetadataRepository().getTableEntities().stream()
        .map((e) -> e.getJavaType()).collect(Collectors.toList());
    //    entities.addAll(HibernateEntities.instance().getOrderedEntities());
    //    entities.addAll(HibernateEntities.instance().getOrderedHistoryEntities());
    for (final Class<?> entityClass : entities) {
      final String entitySimpleName = entityClass.getSimpleName();
      final String entityType = entityClass.getName();

      if (includeHistory == false
          && entityType.startsWith("org.projectforge.framework.persistence.history.entities.") == true) {
        // Skip history entries.
        continue;
      }
      List<?> list = session.createQuery("select o from " + entityType + " o").setReadOnly(true).list();
      list = (List<?>) CollectionUtils.select(list, PredicateUtils.uniquePredicate());
      final int size = list.size();
      log.info("Writing " + size + " objects");
      for (final Iterator<?> it = list.iterator(); it.hasNext();) {
        final Object obj = it.next();
        if (log.isDebugEnabled()) {
          log.debug("loaded object " + obj);
        }
        Hibernate.initialize(obj);
        final Class<?> targetClass = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
        final ClassMetadata classMetadata = session.getSessionFactory().getClassMetadata(targetClass);
        if (classMetadata == null) {
          log.fatal("Can't init " + obj + " of type " + targetClass);
          continue;
        }
        // initalisierung des Objekts...
        defaultXStream.marshal(obj, new CompactWriter(new NullWriter()));

        if (preserveIds == false) {
          // Nun kann die ID gelöscht werden
          HibernateCompatUtils.setClassMetaDataSetIdentifier(classMetadata, obj, EntityMode.POJO);
        }
        if (log.isDebugEnabled()) {
          log.debug("loading evicted object " + obj);
        }
        if (this.ignoreFromTopLevelListing.contains(targetClass) == false) {
          all.add(obj);
        }
      }
    }
    // und schreiben
    try {
      writer.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
    } catch (final IOException ex) {
      // ignore, will fail on stream.marshal()
    }
    log.info("Wrote " + all.size() + " objects");
    final MarshallingStrategy marshallingStrategy = new ProxyIdRefMarshallingStrategy();
    stream.setMarshallingStrategy(marshallingStrategy);
    stream.marshal(all, new PrettyPrintWriter(writer));
  }

  /**
   * Overload this method if you need further initializations before reading xml stream. Does nothing at default.
   * 
   * @param xstream
   */
  protected void init(final XStream xstream)
  {
  }

  /**
   * @return
   */
  private XStream initXStream(final Session session, final boolean nullifyPk)
  {
    final XStream xstream = new XStream()
    {
      @Override
      protected MapperWrapper wrapMapper(final MapperWrapper next)
      {
        return new HibernateMapper(new HibernateCollectionsMapper(next));
      }
    };
    // Converter für die Hibernate-Collections
    xstream.registerConverter(new HibernateCollectionConverter(xstream.getConverterLookup()));
    xstream.registerConverter(
        new HibernateProxyConverter(xstream.getMapper(), new PureJavaReflectionProvider(),
            xstream.getConverterLookup()),
        XStream.PRIORITY_VERY_HIGH);
    xstream.setMarshallingStrategy(new XStreamMarshallingStrategy(XStreamMarshallingStrategy.RELATIVE));
    init(xstream);
    return xstream;
  }
}
