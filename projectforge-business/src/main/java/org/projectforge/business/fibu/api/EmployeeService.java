package org.projectforge.business.fibu.api;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;

/**
 * Access to employee.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface EmployeeService extends IPersistenceService<EmployeeDO>, IDao<EmployeeDO>
{
  void setPfUser(EmployeeDO employee, Integer userId);

  void setKost1(final EmployeeDO employee, final Integer kost1Id);

  public EmployeeTimedDO addNewTimeAttributeRow(final EmployeeDO employee);
}
