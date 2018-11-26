/*
 *
 * Copyright (C) 2012-2014 R T Huitema. All Rights Reserved.
 * Web: www.42.co.nz
 * Email: robert@42.co.nz
 * Author: R T Huitema
 *
 * This file is part of the signalk-server-java project
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package nz.co.fortytwo.signalk.artemis.subscription;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQQueueExistsException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * Track and manage the sessionId's and corresponding subscriptions for a consumer
 *
 * @author robert
 *
 */
public class SubscriptionManagerService{

	private static Logger logger = LogManager.getLogger(SubscriptionManagerService.class);
	
	protected ThreadLocal<ClientSession> txSession = new ThreadLocal<>();
	protected ThreadLocal<ClientProducer> producer  = new ThreadLocal<>();
	private Timer timer;
	protected ConcurrentLinkedQueue<Subscription> subscriptions = new ConcurrentLinkedQueue<Subscription>();
	
	public SubscriptionManagerService() {
		super();
		timer = new Timer( true);
		try {
			getTxSession();
			getProducer();

		} catch (Exception e) {
			logger.error(e, e);
		}
	}

	/**
	 * Add a new subscription.
	 * 
	 * @param sub
	 * @throws Exception
	 */
	public void addSubscription(Subscription sub) throws Exception {
		if (!subscriptions.contains(sub)) {
			if (logger.isDebugEnabled())
				logger.debug("Adding {}",sub);
			subscriptions.add(sub);
			// start if we have to.
			
			if (!hasExistingRoute(sub)) {
				sub.setActive(true);
				if (logger.isDebugEnabled())
					logger.debug("Started route for {} ",sub);
			}
			if (logger.isDebugEnabled())
				logger.debug("Subs size = {}",subscriptions.size());
		}

	}

	
	
	/**
	 * True if another subscription has the same route and is active
	 * 
	 * @param sub
	 * @return
	 */
	private boolean hasExistingRoute(Subscription sub) {
		for (Subscription s : getSubscriptions(sub.getSessionId())) {
			if (sub.equals(s))
				continue;
			if (sub.isSameRoute(s) && s.isActive()) {
				sub.setRouteId(s.getRouteId());
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove a subscription
	 * 
	 * @param sub
	 * @throws Exception
	 */
	public void removeSubscription(Subscription sub) throws Exception {
		if (subscriptions.contains(sub)) {
			if (logger.isDebugEnabled())
				logger.debug("Removing {}", sub);
			for (Subscription s : getSubscriptions(sub.getSessionId())) {
				if (sub.equals(s)){
					subscriptions.remove(s);
					sub.setActive(false);
					s.setActive(false);
					if (logger.isDebugEnabled())logger.debug("Stopped route for {}", s);
				}
			}
			
			if (logger.isDebugEnabled())
				logger.debug("Subs size ={}", subscriptions.size());
		}

	}

	public ConcurrentLinkedQueue<Subscription> getSubscriptions(String wsSession) {
		ConcurrentLinkedQueue<Subscription> subs = new ConcurrentLinkedQueue<Subscription>();
		for (Subscription s : subscriptions) {
			if (s.getSessionId().equals(wsSession)) {
				subs.add(s);
			}
		}
		return subs;
	}
	public ConcurrentLinkedQueue<Subscription> getSubscriptionsByTempQ(String tempQ) {
		ConcurrentLinkedQueue<Subscription> subs = new ConcurrentLinkedQueue<Subscription>();
		for (Subscription s : subscriptions) {
			if (s.getDestination().equals(tempQ)) {
				subs.add(s);
			}
		}
		return subs;
	}


	public void removeByTempQ(String tempQ) throws Exception {
		ConcurrentLinkedQueue<Subscription> subs = getSubscriptionsByTempQ(tempQ);
		for(Subscription sub:subs){
			try {
				sub.setActive(false);
				if (logger.isDebugEnabled())logger.debug("Stopped route for {}", sub);
			} catch (Exception e) {
				logger.error(e,e);
			}
		}
		subscriptions.removeAll(subs);
		if (logger.isDebugEnabled())
			logger.debug("Subs size ={}", subscriptions.size());

	}

	
	public void send(String destination, String format, String correlation, String token, Json json)
			throws ActiveMQException {
		if (json == null || json.isNull())
			json = Json.object();
		//ClientMessage txMsg = new ClientMessageImpl((byte) 0, false, 5000, System.currentTimeMillis(), (byte) 4, ActiveMQClient.DEFAULT_INITIAL_MESSAGE_PACKET_SIZE);
		ClientMessage txMsg = getTxSession().createMessage(false);
		txMsg.setExpiration(System.currentTimeMillis() + 5000);
		if (correlation != null)
			txMsg.putStringProperty(Config.AMQ_CORR_ID, correlation);
		txMsg.putStringProperty(Config.AMQ_SUB_DESTINATION, destination);
		txMsg.putStringProperty(Config.AMQ_USER_TOKEN, token);
		txMsg.putBooleanProperty(Config.SK_SEND_TO_ALL, false);
		txMsg.putStringProperty(SignalKConstants.FORMAT, format);
		txMsg.putBooleanProperty(SignalKConstants.REPLY, true);
		txMsg.getBodyBuffer().writeString(json.toString());
		if (logger.isDebugEnabled())
			logger.debug("Sending to {}, Msg body = {}", destination, json.toString());

		
		getProducer().send(new SimpleString("outgoing.reply." +destination), txMsg);
	

	}

	public void createTempQueue(String destination) {
		try {
			getTxSession().createTemporaryQueue("outgoing.reply." + destination, RoutingType.ANYCAST, destination);
			
			if (logger.isDebugEnabled())logger.debug("created temp queue: {}", destination);
		} catch (ActiveMQQueueExistsException e) {
			logger.debug(e);
		} catch (Exception e) {
			logger.error(e, e);
		}
	}
	
	protected void closeSession() {

		if (producer.get() != null) {
			try {
				producer.get().close();
				producer.remove();
			} catch (ActiveMQException e) {
				logger.warn(e, e);
			}
		}
		if (txSession.get() != null) {
			try {
				txSession.get().close();
				txSession.remove();
			} catch (ActiveMQException e) {
				logger.warn(e, e);
			}
		}

	}
	@Override
	protected void finalize() throws Throwable {
		for(Subscription sub:subscriptions){
			try {
				sub.setActive(false);
				if (logger.isDebugEnabled())logger.debug("Stopped route for " + sub);
			} catch (Exception e) {
				logger.error(e,e);
			}
		}
		subscriptions.clear();
		closeSession();
		
		super.finalize();
	}

	public ClientSession getTxSession() throws ActiveMQException {
		if(txSession.get()==null || txSession.get().isClosed()){
			try {
				txSession.set(Util.getVmSession(Config.getConfigProperty(Config.ADMIN_USER),
						Config.getConfigProperty(Config.ADMIN_PWD)));
			} catch (Exception e) {
				throw new ActiveMQException(e.getMessage());
			}
			txSession.get().start();
		}
		return txSession.get();
	}

	public ClientProducer getProducer() throws ActiveMQException {
		if(producer.get()==null || producer.get().isClosed()){
			producer.set(getTxSession().createProducer());
		}
		return producer.get();
	}

	public void schedule(TimerTask task, long period) {
		timer.schedule(task, 10, period);
		
	}


}
