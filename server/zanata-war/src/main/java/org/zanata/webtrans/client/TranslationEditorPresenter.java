/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.webtrans.client;

import org.zanata.common.TransUnitCount;
import org.zanata.common.TransUnitWords;
import org.zanata.common.TranslationStats;
import org.zanata.webtrans.client.editor.HasTranslationStats;
import org.zanata.webtrans.client.editor.table.TableEditorPresenter;
import org.zanata.webtrans.client.events.DocumentSelectionEvent;
import org.zanata.webtrans.client.events.DocumentSelectionHandler;
import org.zanata.webtrans.client.events.TransUnitUpdatedEvent;
import org.zanata.webtrans.client.events.TransUnitUpdatedEventHandler;
import org.zanata.webtrans.client.rpc.CachingDispatchAsync;
import org.zanata.webtrans.client.ui.HasPager;
import org.zanata.webtrans.shared.model.DocumentId;
import org.zanata.webtrans.shared.model.DocumentInfo;
import org.zanata.webtrans.shared.model.TransUnit;
import org.zanata.webtrans.shared.rpc.GetStatusCount;
import org.zanata.webtrans.shared.rpc.GetStatusCountResult;

import net.customware.gwt.dispatch.client.DispatchAsync;
import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.gen2.table.event.client.PageChangeEvent;
import com.google.gwt.gen2.table.event.client.PageChangeHandler;
import com.google.gwt.gen2.table.event.client.PageCountChangeEvent;
import com.google.gwt.gen2.table.event.client.PageCountChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class TranslationEditorPresenter extends WidgetPresenter<TranslationEditorPresenter.Display>
{

   public interface Display extends WidgetDisplay
   {

      void setTranslationMemoryView(Widget translationMemoryView);

      void setEditorView(Widget widget);

      void setTransUnitNavigation(Widget widget);

      void setTmViewVisible(boolean visible);

      HasTranslationStats getTransUnitCount();

      HasPager getPageNavigation();

      HasClickHandlers getHideTMViewButton();

      HasClickHandlers getShowTMViewButton();

      void setShowTMViewButtonVisible(boolean visible);

      void setUndoRedo(Widget undoRedoWidget);

   }

   private final TransUnitNavigationPresenter transUnitNavigationPresenter;
   private final TransMemoryPresenter transMemoryPresenter;
   private final TableEditorPresenter tableEditorPresenter;
   private final UndoRedoPresenter undoRedoPresenter;

   private DocumentInfo currentDocument;
   private final TranslationStats statusCount = new TranslationStats();

   private final DispatchAsync dispatcher;

   @Inject
   public TranslationEditorPresenter(Display display, EventBus eventBus, final CachingDispatchAsync dispatcher, final TransMemoryPresenter transMemoryPresenter, final TableEditorPresenter tableEditorPresenter, final TransUnitNavigationPresenter transUnitNavigationPresenter, final UndoRedoPresenter undoRedoPresenter)
   {
      super(display, eventBus);
      this.dispatcher = dispatcher;
      this.transMemoryPresenter = transMemoryPresenter;
      this.tableEditorPresenter = tableEditorPresenter;
      this.transUnitNavigationPresenter = transUnitNavigationPresenter;
      this.undoRedoPresenter = undoRedoPresenter;
   }

   @Override
   protected void onBind()
   {
      transMemoryPresenter.bind();
      display.setTranslationMemoryView(transMemoryPresenter.getDisplay().asWidget());

      tableEditorPresenter.bind();
      display.setEditorView(tableEditorPresenter.getDisplay().asWidget());

      transUnitNavigationPresenter.bind();
      display.setTransUnitNavigation(transUnitNavigationPresenter.getDisplay().asWidget());

      undoRedoPresenter.bind();
      display.setUndoRedo(undoRedoPresenter.getDisplay().asWidget());

      registerHandler(display.getPageNavigation().addValueChangeHandler(new ValueChangeHandler<Integer>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Integer> event)
         {
            tableEditorPresenter.cancelEdit();
            tableEditorPresenter.gotoPage(event.getValue() - 1, false);
         }
      }));

      // TODO this uses incubator's HandlerRegistration
      tableEditorPresenter.addPageChangeHandler(new PageChangeHandler()
      {
         @Override
         public void onPageChange(PageChangeEvent event)
         {
            display.getPageNavigation().setValue(event.getNewPage() + 1);
         }
      });

      // TODO this uses incubator's HandlerRegistration
      tableEditorPresenter.addPageCountChangeHandler(new PageCountChangeHandler()
      {
         @Override
         public void onPageCountChange(PageCountChangeEvent event)
         {
            display.getPageNavigation().setPageCount(event.getNewPageCount());
         }
      });

      registerHandler(eventBus.addHandler(DocumentSelectionEvent.getType(), new DocumentSelectionHandler()
      {
         @Override
         public void onDocumentSelected(DocumentSelectionEvent event)
         {
            if (currentDocument != null && currentDocument.getId().equals(event.getDocument().getId()))
            {
               return;
            }
            currentDocument = event.getDocument();
            requestStatusCount(event.getDocument().getId());
         }
      }));
      registerHandler(eventBus.addHandler(TransUnitUpdatedEvent.getType(), updateHandler));

      registerHandler(display.getHideTMViewButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            display.setTmViewVisible(false);
            transMemoryPresenter.unbind();
            display.setShowTMViewButtonVisible(true);
         }
      }));

      display.setShowTMViewButtonVisible(false);
      display.getShowTMViewButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            transMemoryPresenter.bind();
            display.setTmViewVisible(true);
            display.setShowTMViewButtonVisible(false);
            TransUnit tu = tableEditorPresenter.getSelectedTransUnit();
            if (tu != null)
            {
               transMemoryPresenter.showResultsFor(tu);
            }
         }
      });

   }

   private void requestStatusCount(final DocumentId newDocumentId)
   {
      dispatcher.execute(new GetStatusCount(newDocumentId), new AsyncCallback<GetStatusCountResult>()
      {
         @Override
         public void onFailure(Throwable caught)
         {
            Log.error("error fetching GetStatusCount: " + caught.getMessage());
         }

         @Override
         public void onSuccess(GetStatusCountResult result)
         {
            statusCount.set(result.getCount());
            display.getTransUnitCount().setStats(statusCount);
         }
      });
   }

   private final TransUnitUpdatedEventHandler updateHandler = new TransUnitUpdatedEventHandler()
   {
      @Override
      public void onTransUnitUpdated(TransUnitUpdatedEvent event)
      {
         if (currentDocument == null)
         {
            return;
         }
         if (!event.getDocumentId().equals(currentDocument.getId()))
         {
            return;
         }

         TransUnitCount unitCount = statusCount.getUnitCount();
         TransUnitWords wordCount = statusCount.getWordCount();

         unitCount.increment(event.getTransUnit().getStatus());
         unitCount.decrement(event.getPreviousStatus());
         wordCount.increment(event.getTransUnit().getStatus(), event.getWordCount());
         wordCount.decrement(event.getPreviousStatus(), event.getWordCount());

         display.getTransUnitCount().setStats(statusCount);

      }
   };

   @Override
   protected void onUnbind()
   {
      transMemoryPresenter.unbind();
      tableEditorPresenter.unbind();
      transUnitNavigationPresenter.unbind();
   }

   @Override
   public void onRevealDisplay()
   {
   }

}
