package org.zanata.webtrans.client;

import java.util.HashMap;

import org.zanata.webtrans.client.events.EnterWorkspaceEvent;
import org.zanata.webtrans.client.events.ExitWorkspaceEvent;
import org.zanata.webtrans.client.events.TransUnitEditEvent;
import org.zanata.webtrans.client.events.TransUnitUpdatedEvent;
import org.zanata.webtrans.client.rpc.CachingDispatchAsync;
import org.zanata.webtrans.shared.model.WorkspaceContext;
import org.zanata.webtrans.shared.rpc.EnterWorkspace;
import org.zanata.webtrans.shared.rpc.ExitWorkspace;
import org.zanata.webtrans.shared.rpc.HasEnterWorkspaceData;
import org.zanata.webtrans.shared.rpc.HasExitWorkspaceData;
import org.zanata.webtrans.shared.rpc.HasTransUnitEditData;
import org.zanata.webtrans.shared.rpc.HasTransUnitUpdatedData;
import org.zanata.webtrans.shared.rpc.SessionEventData;
import org.zanata.webtrans.shared.rpc.TransUnitEditing;
import org.zanata.webtrans.shared.rpc.TransUnitUpdated;

import net.customware.gwt.presenter.client.EventBus;


import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.gen2.table.client.TableModel.Callback;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

import de.novanic.eventservice.client.event.Event;
import de.novanic.eventservice.client.event.RemoteEventService;
import de.novanic.eventservice.client.event.RemoteEventServiceFactory;
import de.novanic.eventservice.client.event.domain.Domain;
import de.novanic.eventservice.client.event.domain.DomainFactory;
import de.novanic.eventservice.client.event.listener.RemoteEventListener;

public class EventProcessor implements RemoteEventListener
{

   private interface EventFactory<T extends GwtEvent<?>>
   {
      T create(SessionEventData event);
   }

   public static interface StartCallback
   {
      void onSuccess();

      void onFailure(Throwable e);
   }

   private static class EventRegistry
   {
      HashMap<Class<? extends SessionEventData>, EventFactory<?>> factories = new HashMap<Class<? extends SessionEventData>, EventFactory<?>>();

      public EventRegistry()
      {

         // put any additional factories here
         factories.put(TransUnitUpdated.class, new EventFactory<TransUnitUpdatedEvent>()
         {
            @Override
            public TransUnitUpdatedEvent create(SessionEventData event)
            {
               return new TransUnitUpdatedEvent((HasTransUnitUpdatedData) event);
            }
         });

         factories.put(TransUnitEditing.class, new EventFactory<TransUnitEditEvent>()
         {
            @Override
            public TransUnitEditEvent create(SessionEventData event)
            {
               return new TransUnitEditEvent((HasTransUnitEditData) event);
            }
         });

         factories.put(ExitWorkspace.class, new EventFactory<ExitWorkspaceEvent>()
         {
            @Override
            public ExitWorkspaceEvent create(SessionEventData event)
            {
               return new ExitWorkspaceEvent((HasExitWorkspaceData) event);
            }
         });

         factories.put(EnterWorkspace.class, new EventFactory<EnterWorkspaceEvent>()
         {
            @Override
            public EnterWorkspaceEvent create(SessionEventData event)
            {
               return new EnterWorkspaceEvent((HasEnterWorkspaceData) event);
            }
         });
      }

      public GwtEvent<?> getEvent(SessionEventData sessionEventData)
      {
         EventFactory<?> factory = factories.get(sessionEventData.getClass());
         if (factories == null)
         {
            Log.warn("Could not find factory for class " + sessionEventData.getClass());
            return null;
         }
         return factory.create(sessionEventData);
      }
   }

   private final EventRegistry eventRegistry;
   private final RemoteEventService remoteEventService;
   private final Domain domain;
   private final EventBus eventBus;

   @Inject
   public EventProcessor(EventBus eventBus, CachingDispatchAsync dispatcher, WorkspaceContext workspaceContext)
   {
      this.eventBus = eventBus;
      this.eventRegistry = new EventRegistry();
      this.remoteEventService = RemoteEventServiceFactory.getInstance().getRemoteEventService();
      this.domain = DomainFactory.getDomain(workspaceContext.getWorkspaceId().toString());
   }

   public void start(final StartCallback callback)
   {
      remoteEventService.addListener(domain, this, new AsyncCallback<Void>()
      {
         @Override
         public void onSuccess(Void result)
         {
            Log.info("EventProcessor is now listening for events in the domain " + domain.getName());
            callback.onSuccess();
         }

         @Override
         public void onFailure(Throwable e)
         {
            Log.error("Failed to start EventProcessor", e);
            callback.onFailure(e);
         }
      });
   }

   @Override
   public void apply(Event event)
   {
      // Log.info("received remote event "+event);
      if (event instanceof SessionEventData)
      {
         SessionEventData ed = (SessionEventData) event;
         GwtEvent<?> gwtEvent = eventRegistry.getEvent(ed);
         if (gwtEvent != null)
         {
            Log.info("received event " + event + ", GWT event " + gwtEvent.getClass().getName());
            eventBus.fireEvent(gwtEvent);
         }
         else
         {
            Log.warn("unknown event " + event);
         }
      }
   }

}
