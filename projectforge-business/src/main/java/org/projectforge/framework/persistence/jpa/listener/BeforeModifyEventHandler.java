package org.projectforge.framework.persistence.jpa.listener;

import org.projectforge.business.multitenancy.TenantChecker;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.JpaPfGenericPersistenceService;
import org.projectforge.framework.persistence.jpa.PfEmgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.micromata.genome.jpa.DbRecord;
import de.micromata.genome.jpa.events.EmgrBeforeDeleteEvent;
import de.micromata.genome.jpa.events.EmgrEventHandler;
import de.micromata.genome.jpa.events.EmgrInitForInsertEvent;
import de.micromata.genome.jpa.events.EmgrInitForModEvent;
import de.micromata.genome.jpa.events.EmgrInitForUpdateEvent;

/**
 * The listener interface for receiving checkPartOfTenantUpdate events. The class that is interested in processing a
 * checkPartOfTenantUpdate event implements this interface, and the object created with that class is registered with a
 * component using the component's <code>addCheckPartOfTenantUpdateListener<code> method. When the
 * checkPartOfTenantUpdate event occurs, that object's appropriate method is invoked.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Component
public class BeforeModifyEventHandler implements EmgrEventHandler<EmgrInitForModEvent>
{
  @Autowired
  protected AccessChecker accessChecker;

  @Autowired
  private JpaPfGenericPersistenceService genericPersistenceService;

  @Autowired
  TenantChecker tenantChecker;

  /**
   * {@inheritDoc}
   *
   */
  @Override
  public void onEvent(EmgrInitForModEvent event)
  {
    DbRecord<?> rec = event.getRecord();
    if ((rec instanceof BaseDO) == false) {
      return;
    }
    PfEmgr emgr = (PfEmgr) event.getEmgr();
    if (emgr.isCheckAccess() == false) {
      return;
    }
    BaseDO baseDo = (BaseDO) rec;
    OperationType operationType;
    if (event instanceof EmgrInitForInsertEvent) {
      operationType = OperationType.INSERT;
    } else if (event instanceof EmgrInitForUpdateEvent) {
      // see AccessCheckUpdateCopyFilterListener
      return;
    } else if (event instanceof EmgrBeforeDeleteEvent) {
      operationType = OperationType.DELETE;
    } else {
      throw new IllegalArgumentException("Unsuported event to BeforeModifyEventHandler:" + event.getClass().getName());
    }
    accessChecker.checkRestrictedOrDemoUser();
    tenantChecker.checkPartOfCurrentTenant(baseDo);
    IUserRightId rightId = genericPersistenceService.getUserRight(baseDo);
    accessChecker.hasLoggedInUserAccess(rightId, baseDo, null, operationType, true);
  }

}
