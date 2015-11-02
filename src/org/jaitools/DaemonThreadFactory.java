/* 
 *  Copyright (c) 2011, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   

package org.jaitools;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Simple ThreadFactory implementation to supply daemon threads with 
 * specified priority. Used by JAITools classes that run polling services
 * on background threads to avoid blocking application exit.
 * 
 * @author Michael Bedward
 * @since 1.1
 * @version $Id$
 */
public class DaemonThreadFactory implements ThreadFactory {

    private static final String DEFAULT_ROOT_NAME = "daemon-";
    
    private final ReentrantLock lock = new ReentrantLock();
    private static final AtomicInteger threadCount = new AtomicInteger(0);
    private final int priority;
    private final String rootName;

    
    /**
     * Creates a new factory which will supply daemon threads having
     * normal priority.
     */
    public DaemonThreadFactory() {
        this(Thread.NORM_PRIORITY, DEFAULT_ROOT_NAME);
    }
    
    /**
     * Creates a new factory which will supply daemon threads to run
     * at the specified priority.
     * 
     * @param priority thread priority
     */
    public DaemonThreadFactory(int priority) {
        this(priority, DEFAULT_ROOT_NAME);
    }
    
    
    /**
     * Creates a new factory which will supply daemon threads to run
     * at the specified priority. Threads will be named {@code rootName-n}
     * where {@code n} is the count of threads produced by all instances
     * of this class.
     * 
     * @param priority thread priority
     * @param rootName root name to label threads
     */
    public DaemonThreadFactory(int priority, String rootName) {
        String s = rootName == null ? "" : rootName.trim();
        if (s.length() == 0) {
            this.rootName = DEFAULT_ROOT_NAME;
        } else if (s.endsWith("-")) {
            this.rootName = s;
        } else {
            this.rootName = s + "-";
        }
        
        this.priority = Math.min(Thread.MAX_PRIORITY, Math.max(Thread.MIN_PRIORITY, priority));
    }
    
    /**
     * Creates a new daemon thread with name and priority assigned 
     * as per the values supplied when creating this thread factory.
     * 
     * @param r target for the new thread
     * 
     * @return new thread
     */
    public Thread newThread(Runnable r) {
        lock.lock();
        try {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("daemon-" + threadCount.getAndIncrement());
            t.setPriority(priority);
            return t;
        } finally {
            lock.unlock();
        }
    }

}
