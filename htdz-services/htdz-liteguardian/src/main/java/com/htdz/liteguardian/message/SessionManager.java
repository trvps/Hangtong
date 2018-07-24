/*
 * Copyright (C) 2010 Moduad Co., Ltd.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.htdz.liteguardian.message;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.session.IoSession;

/** 
 * This class manages the sessions connected to the server.
 *
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class SessionManager {

    private static final Log log = LogFactory.getLog(SessionManager.class);

    private static SessionManager instance;

    private Map<Long, IoSession> sessions = new ConcurrentHashMap<Long, IoSession>();
    
    private SessionManager() {
    }

    /**
     * Returns the singleton instance of SessionManager.
     * 
     * @return the instance
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                instance = new SessionManager();
            }
        }
        return instance;
    }

    /**
     * Adds a new session that has been authenticated. 
     *  
     * @param session the session
     */
    public void addSession(IoSession session) {
    	sessions.put(session.getId(), session);
    }
    
    public IoSession getIdleSession() {
    	Set<Entry<Long, IoSession>> set= (Set<Entry<Long, IoSession>>)sessions.entrySet();
    	Iterator<Entry<Long, IoSession>> it=set.iterator();
    	while(it.hasNext()) {
    		IoSession session = (IoSession) it.next().getValue();
    		if (session.isBothIdle() && session.isConnected()) {
    			return session;
    		}
    	}
    	return MessageListener.createConnection();
    }

    /**
     * Returns the session associated with the username.
     * 
     * @param username the username of the client address
     * @return the session associated with the username
     */
    public IoSession getSession(long id) {
        return sessions.get(id);
    }

    /**
     * Removes a client session.
     * 
     * @param session the session to be removed
     * @return true if the session was successfully removed 
     */
    public boolean removeSession(IoSession session) {
        if (session == null || sessions.remove(session.getId()) == null) {
            return false;
        }
        return true;
    }

    /**
     * A listner to handle a session that has been closed.
     */
	private class IoSessionListener implements ConnectionCloseListener {

        public void onConnectionClose(Object handback) {
            try {
                IoSession session = (IoSession) handback;
                removeSession(session);
            } catch (Exception e) {
                log.error("Could not close socket", e);
            }
        }
    }

}
