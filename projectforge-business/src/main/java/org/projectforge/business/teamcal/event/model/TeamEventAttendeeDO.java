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

package org.projectforge.business.teamcal.event.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.search.annotations.Indexed;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_CALENDAR_EVENT_ATTENDEE",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_attendee_team_event_fk",
            columnList = "team_event_fk"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_attendee_user_id", columnList = "user_id"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_attendee_tenant_id", columnList = "tenant_id")
    })
@WithHistory(noHistoryProperties = "loginToken")
public class TeamEventAttendeeDO extends DefaultBaseDO implements Comparable<TeamEventAttendeeDO>
{
  private static final long serialVersionUID = -3293247578185393730L;

  private TenantDO tenant;

  private Short number;

  private String url;

  private PFUserDO user;

  private String loginToken;

  private TeamAttendeeStatus status = TeamAttendeeStatus.NEEDS_ACTION;

  private String comment;

  private String commentOfAttendee;

  private static final Set<String> NON_HISTORIZABLE_ATTRIBUTES;

  public static final int URL_MAX_LENGTH = 255;

  static {
    NON_HISTORIZABLE_ATTRIBUTES = new HashSet<String>();
    NON_HISTORIZABLE_ATTRIBUTES.add("loginToken");
  }

  /**
   * Is set if the attendee is a ProjectForge user.
   * 
   * @return the userId
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id")
  public PFUserDO getUser()
  {
    return user;
  }

  /**
   * @param user the user to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setUser(final PFUserDO user)
  {
    this.user = user;
    return this;
  }

  @Transient
  public Integer getUserId()
  {
    if (this.user == null) {
      return null;
    }
    return user.getId();
  }

  /**
   * Is used if the attendee isn't a ProjectForge user for authentication.
   * 
   * @return the loginToken
   */
  @Column(name = "login_token", length = 255)
  public String getLoginToken()
  {
    return loginToken;
  }

  /**
   * @param loginToken the loginToken to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setLoginToken(final String loginToken)
  {
    this.loginToken = loginToken;
    return this;
  }

  /**
   * The url (mail) of the attendee. Isn't used if the attendee is a ProjectForge user.
   * 
   * @return the url
   */
  @Column(length = URL_MAX_LENGTH)
  public String getUrl()
  {
    return url;
  }

  /**
   * @param url the url to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setUrl(final String url)
  {
    this.url = url;
    return this;
  }

  /**
   * @return the status
   */
  @Enumerated(EnumType.STRING)
  @Column(length = 100)
  public TeamAttendeeStatus getStatus()
  {
    return status;
  }

  /**
   * @param status the status to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setStatus(final TeamAttendeeStatus status)
  {
    this.status = status;
    return this;
  }

  /**
   * @return the comment
   */
  @Column(length = 4000)
  public String getComment()
  {
    return comment;
  }

  /**
   * @param comment the comment to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }

  /**
   * @return the commentOfAttendee
   */
  @Column(length = 4000, name = "comment_of_attendee")
  public String getCommentOfAttendee()
  {
    return commentOfAttendee;
  }

  /**
   * @param commentOfAttendee the commentOfAttendee to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setCommentOfAttendee(final String commentOfAttendee)
  {
    this.commentOfAttendee = commentOfAttendee;
    return this;
  }

  /**
   * @return the number
   */
  @Column
  public Short getNumber()
  {
    return number;
  }

  /**
   * @param number the number to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setNumber(final Short number)
  {
    this.number = number;
    return this;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final TeamEventAttendeeDO arg0)
  {
    if (this.getId() != null && ObjectUtils.equals(this.getId(), arg0.getId()) == true) {
      return 0;
    }
    return this.toString().toLowerCase().compareTo(arg0.toString().toLowerCase());
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(this.getId());
    if (this.getId() != null) {
      return hcb.toHashCode();
    }
    hcb.append(this.getUserId());
    hcb.append(this.url);
    return hcb.toHashCode();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof TeamEventAttendeeDO == false) {
      return false;
    }
    final TeamEventAttendeeDO other = (TeamEventAttendeeDO) o;
    if (this.getId() != null && ObjectUtils.equals(this.getId(), other.getId()) == true) {
      return true;
    }
    if (ObjectUtils.equals(this.getUserId(), other.getUserId()) == false) {
      return false;
    }
    if (StringUtils.equals(this.getUrl(), other.getUrl()) == false) {
      return false;
    }
    return true;
  }
}
