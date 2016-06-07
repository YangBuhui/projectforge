package org.projectforge.business.user.service;

import java.util.Collection;
import java.util.List;

import org.projectforge.framework.persistence.user.entities.PFUserDO;

public interface UserService
{

  String getUserIds(Collection<PFUserDO> users);

  Collection<PFUserDO> getSortedUsers(String userIds);

  Collection<PFUserDO> getSortedUsers();

  List<String> getUserNames(String userIds);

}
