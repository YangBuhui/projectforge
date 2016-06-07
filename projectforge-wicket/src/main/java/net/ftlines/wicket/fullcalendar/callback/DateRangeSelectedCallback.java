/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.ftlines.wicket.fullcalendar.callback;

import net.ftlines.wicket.fullcalendar.CalendarResponse;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Request;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public abstract class DateRangeSelectedCallback extends AbstractAjaxCallback implements CallbackWithHandler {
  private static final long serialVersionUID = 7927720428216612850L;
  private final boolean ignoreTimezone;

  /**
   * If <var>ignoreTimezone</var> is {@code true}, then the remote client\"s time zone will be ignored when
   * determining the selected date range, resulting in a range with the selected start and end values, but in the
   * server\"s time zone.
   * 
   * @param ignoreTimezone
   *            whether or not to ignore the remote client\"s time zone when determining the selected date range
   */
  public DateRangeSelectedCallback(final boolean ignoreTimezone) {
    this.ignoreTimezone = ignoreTimezone;
  }

  @Override
  protected String configureCallbackScript(final String script, final String urlTail) {
    return script
        .replace(
            urlTail,
            "&timezoneOffset=\"+startDate.getTimezoneOffset()+\"&startDate=\"+startDate.getTime()+\"&endDate=\"+endDate.getTime()+\"&allDay=\"+allDay+\"");
  }

  @Override
  public IModel<String> getHandlerScript() {
    return new AbstractReadOnlyModel<String>() {
      private static final long serialVersionUID = 4021933370930781184L;

      /**
       * @see org.apache.wicket.model.AbstractReadOnlyModel#getObject()
       */
      @Override
      public String getObject()
      {
        return "function(startDate, endDate, allDay) { " + getCallbackScript() + "}";
      }
    };
  }

  @Override
  protected void respond(final AjaxRequestTarget target) {
    final Request r = getCalendar().getRequest();

    DateTime start = new DateTime(r.getRequestParameters().getParameterValue("startDate").toLong());
    DateTime end = new DateTime(r.getRequestParameters().getParameterValue("endDate").toLong());

    if (ignoreTimezone) {
      // Convert to same DateTime in local time zone.
      final int remoteOffset = -r.getRequestParameters().getParameterValue("timezoneOffset").toInt();
      final int localOffset = DateTimeZone.getDefault().getOffset(null) / 60000;
      final int minutesAdjustment = remoteOffset - localOffset;
      start = start.plusMinutes(minutesAdjustment);
      end = end.plusMinutes(minutesAdjustment);
    }
    final boolean allDay = r.getRequestParameters().getParameterValue("allDay").toBoolean();
    onSelect(new SelectedRange(start, end, allDay), new CalendarResponse(getCalendar(), target));

  }

  protected abstract void onSelect(SelectedRange range, CalendarResponse response);

}