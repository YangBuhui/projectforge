/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.common.timeattr;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.wicket.Application;
import org.apache.wicket.Localizer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.ButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.IconButtonPanel;
import org.projectforge.web.wicket.flowlayout.IconType;
import org.projectforge.web.wicket.flowlayout.ToggleContainerPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.AttrSchema;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow;
import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

/**
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
public abstract class TimedAttributeTogglePanel extends ToggleContainerPanel
{
  private final EntityWithTimeableAttr<?, ?> data;

  @SpringBean
  private GuiAttrSchemaService attrSchemaService;

  @SpringBean
  private TimeableService timeableService;
  /**
   * The schema used.
   */
  private final AttrSchema attrSchema;

  /**
   * The form.
   */
  private final Form<?> form;
  //  private FeedbackPanel feedbackPanel;

  public TimedAttributeTogglePanel(final Form<?> form, final String id,
      final EntityWithTimeableAttr<?, ?> data,
      final String attrDescId)
  {
    super(id);
    this.form = form;
    this.data = data;

    attrSchema = attrSchemaService.getAttrSchema(attrDescId);

    if (attrSchema != null) {
      createContent();
      setHeading(getRenderedHeading());
    }
  }

  public boolean isAttrSchemaAvailable()
  {
    return attrSchema != null;
  }

  /**
   * Adds a new entry to time temeables list.
   */
  abstract protected void addNewEntry();

  @Override
  protected boolean wantsOnStatusChangedNotification()
  {
    return true;
  }

  @Override
  public GridBuilder createGridBuilder()
  {
    final DivPanel content = new DivPanel(ToggleContainerPanel.CONTENT_ID);
    //    this.addOrReplace(content);
    this.add(content);
    final GridBuilder gridBuilder = new GridBuilder(content, content.newChildId());
    return gridBuilder;
  }

  public GridBuilder createContent()
  {
    final GridBuilder gridBuilder = createGridBuilder();
    final DivPanel cpanel = gridBuilder.getPanel();
    //    feedbackPanel = new FeedbackPanel("feedback");
    //    cpanel.add(feedbackPanel);

    final List<FormComponent<Date>> timeComponents = new ArrayList<>();
    gridBuilder.newGridPanel();

    gridBuilder.newSplitPanel(GridSize.SPAN2);
    //    gridBuilder.newFieldset("StartDatum");

    gridBuilder.getPanel().add(new Label(gridBuilder.getRowPanel().newChildId(), getString("timeable.startTime")));
    //    gridBuilder.getPanel().add(new Label(/*getString("timeable.startTime")*/"Startdatum"));
    for (final AttrDescription desc : attrSchema.getColumns()) {
      gridBuilder.newSplitPanel(GridSize.fromInt(desc.getSpan()));
      //      gridBuilder.newFieldset(getString(desc.getI18nkey()));
      gridBuilder.getPanel().add(new Label(gridBuilder.getRowPanel().newChildId(), getString(desc.getI18nkey())));
      //      gridBuilder.getPanel().add(new Label(getString(desc.getI18nkey())));
    }
    //    gridBuilder.newGridPanel();
    for (final TimeableAttrRow tdo : data.getTimeableAttributes()) {
      {
        gridBuilder.newGridPanel();
        // Start date
        gridBuilder.newSplitPanel(GridSize.SPAN2);
        final FieldsetPanel fs = gridBuilder.newFieldset("");
        final DatePanel dp = new DatePanel(fs.newChildId(), new PropertyModel<Date>(tdo, "startTime"),
            new DatePanelSettings());
        dp.setRequired(true);
        dp.getDefaultModelObject();
        timeComponents.add(dp.getDateField());
        fs.add(dp);
      }
      //      gridBuilder.newGridPanel();
      for (final AttrDescription desc : attrSchema.getColumns()) {
        gridBuilder.newSplitPanel(GridSize.fromInt(desc.getSpan()));
        final FieldsetPanel fs = gridBuilder.newFieldset(""); //localizer.getString(desc.getI18nkey(), this)
        attrSchemaService.createWicketComponent(fs, desc, tdo);
      }

      gridBuilder.newSplitPanel(GridSize.SPAN1);
      addDeleteRowButton(tdo, gridBuilder.getPanel());
    }

    addAddButton(cpanel);

    form.add(new IFormValidator()
    {

      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        return timeComponents.toArray(new FormComponent[] {});
      }

      @SuppressWarnings("unchecked")
      @Override
      public void validate(final Form<?> form)
      {
        final Set<Date> dclist = new TreeSet<>();
        final Set<Date> duplicate = new TreeSet<>();
        for (final FormComponent<Date> tc : timeComponents) {
          final Date fd = tc.getConvertedInput();
          if (fd != null) {
            if (dclist.contains(fd) == true) {
              duplicate.add(fd);
            }
            dclist.add(fd);
          }
        }
        for (final FormComponent<Date> tc : timeComponents) {
          final Date fd = tc.getConvertedInput();
          if (fd != null && duplicate.contains(fd) == true) {
            tc.error("duplicatestartdate");
          }
        }
      }

    });
    initHeading();
    return gridBuilder;
  }

  public TimedAttributeTogglePanel initHeading()
  {
    setHeading(getRenderedHeading());
    return this;
  }

  protected void rebuildEntries()
  {
    toggleContainer.remove("content");
    createContent();
  }

  protected void addDeleteRowButton(final TimeableAttrRow tdo, final Panel cpanel)
  {
    {
      final Button deleteRowButton = new Button(ButtonPanel.BUTTON_ID)
      {
        @Override
        public void onSubmit()
        {
          data.getTimeableAttributes().remove(tdo);
          rebuildEntries();
        }

      };
      final IconButtonPanel icnbutton = new IconButtonPanel("deleteEntry", deleteRowButton, IconType.TRASH, null)
          .setLight();
      //      final SingleButtonPanel addPositionButtonPanel = new SingleButtonPanel(cpanel.newChildId(), icnbutton,
      //          getString("remove"));
      //      icnbutton.setTooltip(getString("fibu.auftrag.tooltip.addPosition"));
      cpanel.add(icnbutton);

    }
  }

  protected void addAddButton(final DivPanel cpanel)
  {
    final Button addPositionButton = new Button(SingleButtonPanel.WICKET_ID)
    {
      @Override
      public final void onSubmit()
      {
        addNewEntry();
        rebuildEntries();
        //        paymentSchedulePanel.setVisible(true);
      }
    };
    final SingleButtonPanel addPositionButtonPanel = new SingleButtonPanel(cpanel.newChildId(), addPositionButton,
        getString("add"));
    addPositionButtonPanel.setTooltip(getString("fibu.auftrag.tooltip.addPosition"));
    cpanel.add(addPositionButtonPanel);

  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ToggleContainerPanel#onToggleStatusChanged(org.apache.wicket.ajax.AjaxRequestTarget,
   *      boolean)
   */
  @Override
  protected void onToggleStatusChanged(final AjaxRequestTarget target, final ToggleStatus toggleStatus)
  {
    setHeading(getRenderedHeading());
  }

  protected String getRenderedHeading()
  {
    final StringBuilder sb = new StringBuilder();
    final Localizer localizer = Application.get().getResourceSettings().getLocalizer();

    for (final AttrDescription desc : attrSchema.getColumns()) {
      final Object obj = timeableService.getAttrValue(data, desc.getPropertyName(), desc.getType());
      if (sb.length() > 0) {
        sb.append("; ");
      }
      final String label = localizer.getString(desc.getI18nkey(), this);
      sb.append(label).append(": ").append(obj);
    }
    return sb.toString();
  }

  public GuiAttrSchemaService getAttrSchemaService()
  {
    return attrSchemaService;
  }

  public void setAttrSchemaService(final GuiAttrSchemaService attrSchemaService)
  {
    this.attrSchemaService = attrSchemaService;

  }
}
