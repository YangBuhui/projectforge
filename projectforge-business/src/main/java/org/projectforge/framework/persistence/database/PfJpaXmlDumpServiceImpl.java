package org.projectforge.framework.persistence.database;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.filter.TeamCalCalendarFilter;
import org.projectforge.business.teamcal.filter.TemplateCalendarProperties;
import org.projectforge.business.teamcal.filter.TemplateEntry;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.UserXmlPreferencesDO;
import org.projectforge.business.user.UserXmlPreferencesDao;
import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.GlobalConfiguration;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.stereotype.Service;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.core.ReferenceByIdMarshallingStrategy;
import com.thoughtworks.xstream.core.TreeUnmarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlBeforePersistListener;
import de.micromata.genome.db.jpa.xmldump.api.XmlDumpRestoreContext;
import de.micromata.genome.db.jpa.xmldump.impl.JpaXmlDumpServiceImpl;
import de.micromata.genome.db.jpa.xmldump.impl.SkippUnkownElementsCollectionConverter;
import de.micromata.genome.db.jpa.xmldump.impl.XStreamRecordConverter;
import de.micromata.genome.db.jpa.xmldump.impl.XStreamReferenceByIdUnmarshaller;
import de.micromata.genome.jpa.EmgrFactory;
import de.micromata.genome.jpa.IEmgr;
import de.micromata.genome.jpa.metainf.EntityMetadata;
import de.micromata.mgc.jpa.hibernatesearch.impl.SearchEmgr;

/**
 * PF extends to JpaXmlDumpServiceImpl.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Service
public class PfJpaXmlDumpServiceImpl extends JpaXmlDumpServiceImpl implements InitializingBean, PfJpaXmlDumpService
{
  public static final String TEST_CONFIGURATION_BASE_DUMP_FILE = "/data/init-test-configuration.xml";

  public static final String TEST_DATA_BASE_DUMP_FILE = "/data/init-test-data.xml";

  private static final Logger LOG = Logger.getLogger(PfJpaXmlDumpServiceImpl.class);

  @Autowired
  private PfEmgrFactory emfac;
  /**
   * Nasty hack to avoid problems with full text indice.
   */
  public static boolean isTransaction = false;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private ConfigurationDao configDao;

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private TeamCalDao teamCalDao;

  @Autowired
  private UserXmlPreferencesDao userXmlPrefDao;

  @Autowired
  private HibernateTemplate hibernateTemplate;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private TenantDao tenantDao;

  public PfJpaXmlDumpServiceImpl()
  {
    super();

  }

  @Override
  public void afterPropertiesSet() throws Exception
  {
    AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
    for (JpaXmlBeforePersistListener listener : getGlobalBeforeListener()) {
      factory.autowireBean(listener);
    }
  }

  @Override
  protected List<EntityMetadata> filterSortTableEntities(List<EntityMetadata> tables)
  {
    tables = super.filterSortTableEntities(tables);
    // move PfHistoryMasterDO to the end of all
    List<EntityMetadata> ret = tables.stream().filter((e) -> e.getJavaType() != PfHistoryMasterDO.class)
        .collect(Collectors.toList());
    ret.add(emfac.getMetadataRepository().getEntityMetadata(PfHistoryMasterDO.class));
    return ret;
  }

  @Override
  protected void insertEntities(EmgrFactory<?> fac, XmlDumpRestoreContext ctx, List<Object> objects,
      List<EntityMetadata> tableEnts)
  {
    try {
      isTransaction = true;
      super.insertEntities(fac, ctx, objects, tableEnts);
    } finally {
      isTransaction = false;
    }
    fac.runInTrans((emgr) -> {
      SearchEmgr<?> semgr = (SearchEmgr<?>) emgr;
      for (TaskDO task : emgr.selectAllAttached(TaskDO.class)) {
        semgr.getFullTextEntityManager().index(task);
      }
      return null;
    });
  }

  @Override
  protected void insertEntitiesInTrans(XmlDumpRestoreContext ctx, List<Object> objects, List<EntityMetadata> tableEnts,
      IEmgr<?> emgr)
  {

    //    SearchEmgr<?> semgr = (SearchEmgr<?>) emgr;
    //    FullTextEntityManager ftem = semgr.getFullTextEntityManager();
    //    FullTextSession fts = ftem.unwrap(FullTextSession.class);
    //    fts.setFlushMode(FlushMode.MANUAL);
    //    FlushModeType fmode = ftem.getFlushMode();
    //    semgr.getFullTextEntityManager().setFlushMode(FlushModeType.COMMIT);
    super.insertEntitiesInTrans(ctx, objects, tableEnts, emgr);

  }

  @Override
  protected <T> T createInstance(Class<T> clazz)
  {
    T bean = super.createInstance(clazz);

    AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
    factory.autowireBean(bean);
    return bean;
  }

  @Override
  public int createTestDatabase()
  {
    LOG.info("User wants to initialize database with test data.");
    //Insert test configuration
    InputStream is = openCpInputStream(TEST_CONFIGURATION_BASE_DUMP_FILE);
    int ret = restoreDb(emfac, is, RestoreMode.InsertAll);
    IOUtils.closeQuietly(is);
    GlobalConfiguration.getInstance().setExpired();
    //Insert test data
    is = openCpInputStream(TEST_DATA_BASE_DUMP_FILE);
    ret = restoreDb(emfac, is, RestoreMode.InsertAll);
    IOUtils.closeQuietly(is);
    GlobalConfiguration.getInstance().forceReload();
    correctTeamCalIds();
    correctAdressAndBookTaskId();
    return ret;
  }

  private void correctAdressAndBookTaskId()
  {
    Integer rootTaskId = 1;
    for (TaskDO tDO : taskDao.internalLoadAll()) {
      if (tDO.getTitle().equals("Root")) {
        rootTaskId = tDO.getId();
      }
    }
    for (ConfigurationDO cDO : configDao.internalLoadAll()) {
      if (cDO.getParameter().equals(ConfigurationParam.DEFAULT_TASK_ID_4_ADDRESSES.getKey())) {
        cDO.setTaskId(rootTaskId);
        configDao.internalUpdate(cDO);
      }
      if (cDO.getParameter().equals(ConfigurationParam.DEFAULT_TASK_ID_4_BOOKS.getKey())) {
        cDO.setTaskId(rootTaskId);
        configDao.internalUpdate(cDO);
      }
    }
  }

  @Override
  public int restoreDb(EmgrFactory<?> fac, InputStream inputStream, RestoreMode restoreMode)
  {
    XStream xstream = new XStream();
    xstream.setMarshallingStrategy(new ReferenceByIdMarshallingStrategy()
    {
      @Override
      protected TreeUnmarshaller createUnmarshallingContext(Object root, HierarchicalStreamReader reader,
          ConverterLookup converterLookup, Mapper mapper)
      {
        return new XStreamReferenceByIdUnmarshaller(root, reader, converterLookup, mapper);
      }
    });

    XStreamRecordConverter recorder = new XStreamRecordConverter(xstream, fac);
    xstream.registerConverter(recorder, 10);
    xstream.registerConverter(new SkippUnkownElementsCollectionConverter(xstream.getMapper()),
        XStream.PRIORITY_VERY_HIGH);

    List<Object> objects = new ArrayList<>();
    Object result = xstream.fromXML(inputStream, objects);
    objects = (List<Object>) result;
    List<Object> recObjects = recorder.getAllEnties();
    TenantDO defaultTenant = tenantService.getDefaultTenant();
    List<PFUserDO> users = new ArrayList<>();
    for (Object o : recObjects) {
      if (o instanceof DefaultBaseDO) {
        DefaultBaseDO baseObject = (DefaultBaseDO) o;
        baseObject.setTenant(defaultTenant);
        if (baseObject instanceof PFUserDO) {
          PFUserDO user = (PFUserDO) baseObject;
          users.add(user);
        }
      }
      if (o instanceof UserXmlPreferencesDO) {
        UserXmlPreferencesDO pref = (UserXmlPreferencesDO) o;
        pref.setTenant(defaultTenant);
      }
    }
    XmlDumpRestoreContext ctx = createRestoreContext(fac, recObjects);
    LOG.info("Readed object from xml: " + objects.size());
    if (restoreMode == RestoreMode.InsertAll) {
      insertAll(fac, ctx);
      for (PFUserDO user : users) {
        Set<TenantDO> tenantsToAssign = new HashSet<>();
        tenantsToAssign.add(defaultTenant);
        tenantDao.internalAssignTenants(user, tenantsToAssign, null, false, false);
      }
    } else {
      throw new UnsupportedOperationException("restoreMode " + restoreMode + " currently not supported");
    }
    LOG.info("Imported entities: " + objects.size());
    return objects.size();
  }

  private void correctTeamCalIds()
  {
    LOG.info("Correting TeamCal ids!");

    List<TeamCalDO> calList = teamCalDao.internalLoadAll();
    List<GroupDO> groupDOList = groupDao.internalLoadAll();

    updateGroupIdsInCalendar(calList, groupDOList);

    updateUserXmlPreferences(calList);
  }

  private void updateUserXmlPreferences(List<TeamCalDO> calList)
  {
    Map<Integer, String> nameIdMap = new HashMap<>();
    nameIdMap.put(1, "kai@work");
    nameIdMap.put(2, "Yellow web portal team");
    nameIdMap.put(3, "kai@home");
    nameIdMap.put(4, "ProjectForge team");

    final List<UserXmlPreferencesDO> list = (List<UserXmlPreferencesDO>) hibernateTemplate.find(
        "from UserXmlPreferencesDO u where u.key = ?",
        new Object[] { "TeamCalendarPage.userPrefs" });

    for (UserXmlPreferencesDO uxp : list) {
      Object deserialize = userXmlPrefDao.deserialize(null, uxp, true);
      if (deserialize instanceof TeamCalCalendarFilter) {
        TeamCalCalendarFilter filter = (TeamCalCalendarFilter) deserialize;
        for (TemplateEntry te : filter.getTemplateEntries()) {
          Set<TemplateCalendarProperties> calendarProperties = te.getCalendarProperties();
          for (TemplateCalendarProperties property : calendarProperties) {
            String calName = nameIdMap.get(property.getCalId());
            for (TeamCalDO cal : calList) {
              if (cal.getTitle().equals(calName)) {
                property.setCalId(cal.getId());
              }
            }
          }
        }
        userXmlPrefDao.saveOrUpdate(uxp.getUserId(), uxp.getKey(), filter, false);
      }

    }

  }

  private void updateGroupIdsInCalendar(List<TeamCalDO> calList, List<GroupDO> groupDOList)
  {
    for (TeamCalDO cal : calList) {
      List<String> newGroupIds = convertGroupIds(groupDOList, cal.getMinimalAccessGroupIds());
      if (newGroupIds.size() > 0) {
        cal.setMinimalAccessGroupIds(String.join(",", newGroupIds));
      }

      newGroupIds = convertGroupIds(groupDOList, cal.getReadonlyAccessGroupIds());
      if (newGroupIds.size() > 0) {
        cal.setReadonlyAccessGroupIds(String.join(",", newGroupIds));
      }

      newGroupIds = convertGroupIds(groupDOList, cal.getFullAccessGroupIds());
      if (newGroupIds.size() > 0) {
        cal.setFullAccessGroupIds(String.join(",", newGroupIds));
      }

      teamCalDao.internalUpdate(cal);
    }
  }

  private List<String> convertGroupIds(List<GroupDO> groupDOList, String groups)
  {
    Map<Integer, String> groupIdMap = new HashMap<>();
    groupIdMap.put(8, "PF_ProjectManager");
    groupIdMap.put(11, "My Company");
    groupIdMap.put(12, "ProjectForge");
    groupIdMap.put(14, "Yellow web portal");

    List<String> newGroupIds = new ArrayList<>();
    String[] groupIds = groups.split(",");
    for (String group : groupIds) {
      if (StringUtils.isBlank(group) == false) {
        String groupName = groupIdMap.get(Integer.parseInt(group));
        for (GroupDO groupDO : groupDOList) {
          if (groupDO.getName().equals(groupName)) {
            newGroupIds.add(String.valueOf(groupDO.getId()));
          }
        }
      }
    }
    return newGroupIds;
  }

  public static InputStream openCpInputStream(String path)
  {
    final ClassPathResource cpres = new ClassPathResource(path);
    try {
      InputStream in;
      if (path.endsWith(".gz") == true) {
        in = new GZIPInputStream(cpres.getInputStream());
      } else {
        in = cpres.getInputStream();
      }
      return in;
    } catch (final IOException ex) {
      LOG.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    }
  }

}
