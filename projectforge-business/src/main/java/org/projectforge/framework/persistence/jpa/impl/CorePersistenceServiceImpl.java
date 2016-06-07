package org.projectforge.framework.persistence.jpa.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.ICorePersistenceService;
import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;

import de.micromata.genome.jpa.EntityCopyStatus;
import de.micromata.genome.jpa.MarkDeletableRecord;
import de.micromata.genome.util.bean.PrivateBeanUtils;
import de.micromata.genome.util.runtime.ClassUtils;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 * @param <O>
 */
public class CorePersistenceServiceImpl<PK extends Serializable, ENT extends MarkDeletableRecord<PK>>
    implements ICorePersistenceService<PK, ENT>, IDao<ENT>
{

  private static final Logger LOG = Logger.getLogger(JpaPfPersistenceServiceImpl.class);
  @Autowired
  private PfEmgrFactory emf;

  @Autowired
  UserRightService userRights;

  @Autowired
  protected AccessChecker accessChecker;

  private Class<ENT> entityClass;

  @Override
  public Class<ENT> getEntityClass()
  {
    if (entityClass != null) {
      return entityClass;
    }
    entityClass = (Class<ENT>) ClassUtils.getGenericTypeArgument(getClass(), 1);
    return entityClass;
  }

  @Override
  @Deprecated
  public ENT getById(final Serializable id) throws AccessException
  {
    return selectByPkDetached((PK) id);
  }

  @Override
  public ENT newInstance()
  {
    return PrivateBeanUtils.createInstance(getEntityClass());
  }

  @Override
  public ENT selectByPkDetached(PK pk) throws AccessException
  {
    return emf.runWoTrans((emgr) -> {
      return emgr.selectByPkDetached(getEntityClass(), pk);
    });

  }

  @Override
  public PK insert(ENT obj) throws AccessException
  {
    Validate.notNull(obj);
    emf.runInTrans((emgr) -> {
      emgr.insertDetached(obj);
      return obj.getPk();
    });

    return obj.getPk();
  }

  @Override
  public ModificationStatus update(ENT obj) throws AccessException
  {
    EntityCopyStatus mod = emf.runInTrans((emgr) -> {
      return emgr.update(obj.getClass(), obj.getClass(), obj, true);
    });
    return ModificationStatus.fromEntityCopyStatus(mod);
  }

  @Override
  public void markAsDeleted(ENT rec) throws AccessException
  {
    // special Pf handling see PfEmgr
    emf.runInTrans((emgr) -> emgr.markDeleted(rec));
  }

  @Override
  public void undelete(ENT obj) throws AccessException
  {
    // special Pf handling see PfEmgr
    emf.runInTrans((emgr) -> emgr.markUndeleted(obj));

  }

  @Override
  public void delete(ENT obj) throws AccessException
  {
    emf.runInTrans((emgr) -> {
      //TODO: RK Add boolean to fix comilable
      emgr.deleteDetached(obj, false);
      return null;
    });
  }

  @Override
  public List<ENT> getList(BaseSearchFilter filter)
  {
    LOG.error(
        "Not implemented yet: CorePersistenceServiceImpl.getList(BaseSearchFilter)");
    // TODO Auto-generated method stub
    return Collections.emptyList();
  }

  @Override
  public String[] getSearchFields()
  {
    String[] searchFields = HibernateSearchFilterUtils.determineSearchFields(getEntityClass(), new String[] {});
    return searchFields;
  }

  @Override
  public boolean isHistorizable()
  {
    return HistoryBaseDaoAdapter.isHistorizable(getEntityClass());
  }

  @Override
  public boolean hasInsertAccess(PFUserDO user)
  {
    Class<ENT> clz = getEntityClass();
    AUserRightId ur = clz.getAnnotation(AUserRightId.class);
    if (ur == null) {
      throw new IllegalArgumentException("Class " + clz.getName() + " missing EntityUserRightId annotation");
    }
    IUserRightId urid = userRights.getRightId(ur.value());
    return accessChecker.hasAccess(user, urid, null, null, OperationType.INSERT, false);
  }

}
