package org.activityinfo.client.dispatch.remote;

/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collections;

import org.activityinfo.client.MockEventBus;
import org.activityinfo.client.dispatch.CommandCache;
import org.activityinfo.client.dispatch.Dispatcher;
import org.activityinfo.client.dispatch.remote.cache.CacheManager;
import org.activityinfo.client.dispatch.remote.cache.CacheResult;
import org.activityinfo.client.dispatch.remote.cache.CachingDispatcher;
import org.activityinfo.shared.auth.AuthenticatedUser;
import org.activityinfo.shared.command.Command;
import org.activityinfo.shared.command.GetSchema;
import org.activityinfo.shared.command.RemoteCommandServiceAsync;
import org.activityinfo.shared.command.result.CommandResult;
import org.activityinfo.shared.dto.SchemaDTO;
import org.activityinfo.shared.exception.CommandException;
import org.easymock.Capture;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;

import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.testing.StubScheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class RemoteDispatcherTest {

    private static final String AUTH_TOKEN = "XYZ";

    private RemoteCommandServiceAsync service;
    private Dispatcher dispatcher;
    private CommandCache proxy;
    private CacheManager proxyManager = new CacheManager(new MockEventBus());

    private Capture<AsyncCallback> remoteCallback = new Capture<AsyncCallback>();

    private IncompatibleRemoteHandler dummyIncompatibleRemoteHandler = new IncompatibleRemoteHandler() {
        @Override
        public void handle() {
        }
    };

    private StubScheduler scheduler = new StubScheduler();

    @Before
    public void setUp() {
        service = createMock("remoteService", RemoteCommandServiceAsync.class);
        proxy = createMock("proxy", CommandCache.class);
        AuthenticatedUser auth = new AuthenticatedUser(AUTH_TOKEN, 1,
            "alex@alex.com");

        dispatcher = new CachingDispatcher(proxyManager,
            new MergingDispatcher(
                new RemoteDispatcher(new MockEventBus(), auth, service),
                scheduler));
    }

    @Test
    public void commandShouldBeSentToServerIfThereAreNoProxiesAndNoPendingCommands() {

        // define our expectations
        expectRemoteCall(new GetSchema());
        replay(service);

        // trigger a call
        dispatcher.execute(new GetSchema(), makeNullCallback());
        processPendingCommands();

        // verify that the command was dispatched to the server
        verify(service);
    }

    @Test
    public void duplicateCommandsShouldBeMergedWithPendingRequests() {

        expectRemoteCall(new GetSchema());
        replay(service);

        // simulate successive dispatches of the same command from different
        // components of the application
        dispatcher.execute(new GetSchema(), makeNullCallback());
        dispatcher.execute(new GetSchema(), makeNullCallback());
        processPendingCommands();

        // verify that only one command was sent
        verify(service);
    }

    @Test
    public void duplicateCommandsShouldBeMergedWithExecutingRequests() {

        expectRemoteCall(new GetSchema());
        replay(service);

        // simulate successive dispatches of the same command from different
        // components of the application
        dispatcher.execute(new GetSchema(), makeNullCallback());
        processPendingCommands();
        dispatcher.execute(new GetSchema(), makeNullCallback());

        // verify that only one command was sent
        verify(service);
    }

    @Test
    public void mergedCommandsShouldEachReceiveACallback() {

        expectRemoteCall(new GetSchema());
        andCallbackWihSuccess(new SchemaDTO());
        replay(service);

        AsyncCallback callback1 = makeCallbackThatExpectsNonNullSuccess();
        AsyncCallback callback2 = makeCallbackThatExpectsNonNullSuccess();

        // simulate successive dispatches of the same command from different
        // components of the application
        dispatcher.execute(new GetSchema(), callback1);
        dispatcher.execute(new GetSchema(), callback2);
        processPendingCommands();

        // verify that only one command was sent
        verify(callback1);
        verify(callback2);
    }

    @Test
    public void successiveCommandsServedByProxyAreCorrectlyHandleded() {

        GetSchema command = new GetSchema();

        expect(proxy.maybeExecute(eq(command))).andReturn(
            new CacheResult(new SchemaDTO())).anyTimes();
        replay(proxy);

        replay(service); // no calls should be made to the remote service

        final AsyncCallback callback2 = makeCallbackThatExpectsNonNullSuccess();

        proxyManager.registerProxy(GetSchema.class, proxy);
        dispatcher.execute(new GetSchema(), new AsyncCallback<SchemaDTO>() {

            @Override
            public void onFailure(Throwable arg0) {
                throw new AssertionError();
            }

            @Override
            public void onSuccess(SchemaDTO arg0) {
                dispatcher.execute(new GetSchema(), callback2);
            }

        });
        processPendingCommands();
        processPendingCommands();

        verify(proxy, service, callback2);

    }

    @Test
    public void commandsSuccessfullyExecutedThroughProxiesShouldNotBeSentToServer() {

        GetSchema command = new GetSchema();

        expect(proxy.maybeExecute(eq(command))).andReturn(
            new CacheResult(new SchemaDTO()));
        replay(proxy);

        replay(service); // no calls should be made to the remote service

        AsyncCallback callback = makeCallbackThatExpectsNonNullSuccess();

        proxyManager.registerProxy(GetSchema.class, proxy);
        dispatcher.execute(new GetSchema(), callback);
        processPendingCommands();

        verify(proxy, service, callback);
    }

    @Test
    public void commandsUnsuccessfullyExecutedThroughProxiesShouldBeSentToServer() {

        GetSchema command = new GetSchema();

        expect(proxy.maybeExecute(eq(command))).andReturn(
            CacheResult.couldNotExecute());
        replay(proxy);

        expectRemoteCall(command);
        andCallbackWihSuccess(new SchemaDTO());
        replay(service);

        AsyncCallback callback = makeCallbackThatExpectsNonNullSuccess();

        proxyManager.registerProxy(GetSchema.class, proxy);
        dispatcher.execute(new GetSchema(), callback);
        processPendingCommands();

        verify(proxy, service, callback);
    }

    @Test
    public void commandExceptionsShouldBeCalledBackWithFailure() {

        expectRemoteCall(new GetSchema());
        andCallbackWihSuccess(new CommandException()); // remote call succeeded,
                                                       // command failed
        replay(service);

        AsyncCallback callback = makeCallbackThatExpectsFailure();

        dispatcher.execute(new GetSchema(), callback);
        processPendingCommands();

        verify(service, callback);
    }

    /**
     * The RemoteDispatcher will group and bundle commands together-- we need to
     * make sure that different components remain isolated from failures within
     * other components.
     */
    @Test
    public void exceptionsThrownByCallbacksDoNotDistubOthers() {

        expectRemoteCall(new GetSchema());
        andCallbackWihSuccess(new SchemaDTO());
        replay(service);

        // Here we set up one component that will call request a command
        // but something will go wrong when the command return (successfully)
        // the error is unrelated to the remote command -- it just happens to be
        // there.
        dispatcher.execute(new GetSchema(), null,
            new AsyncCallback<SchemaDTO>() {
                @Override
                public void onFailure(Throwable caught) {

                }

                @Override
                public void onSuccess(SchemaDTO result) {
                    throw new RuntimeException();
                }
            });

        // the second command independently requests the same command,
        // we need to make sure we receive a result
        AsyncCallback secondCallback = makeCallbackThatExpectsNonNullSuccess();
        dispatcher.execute(new GetSchema(), null, secondCallback);

        processPendingCommands();

        verify(secondCallback);
    }

    private void processPendingCommands() {
        for (RepeatingCommand command : scheduler.getRepeatingCommands()) {
            command.execute();
        }
    }

    private AsyncCallback<SchemaDTO> makeNullCallback() {
        return new AsyncCallback<SchemaDTO>() {
            @Override
            public void onFailure(Throwable throwable) {
            }

            @Override
            public void onSuccess(SchemaDTO o) {
            }
        };
    }

    private AsyncCallback makeCallbackThatExpectsNonNullSuccess() {
        AsyncCallback callback = createMock(AsyncCallback.class);
        callback.onSuccess(notNull());
        replay(callback);
        return callback;
    }

    private AsyncCallback makeCallbackThatExpectsFailure() {
        AsyncCallback callback = createMock(AsyncCallback.class);
        callback.onFailure(isA(Throwable.class));
        replay(callback);
        return callback;
    }

    private void expectRemoteCall(GetSchema command) {
        service.execute(
            eq(AUTH_TOKEN),
            eq(Collections.<Command> singletonList(command)),
            capture(remoteCallback));
    }

    private void andCallbackWihSuccess(final CommandResult result) {
        expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                ((AsyncCallback) getCurrentArguments()[2])
                    .onSuccess(Collections.singletonList(result));
                return null;
            }
        });
    }

}