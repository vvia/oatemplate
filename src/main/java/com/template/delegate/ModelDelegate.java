// Copied from OATemplate project by OABuilder 02/13/19 10:11 AM
package com.template.delegate;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.template.model.oa.*;
import com.template.model.oa.cs.ClientRoot;
import com.template.model.oa.cs.ServerRoot;
import com.template.model.oa.filter.*;
import com.viaoa.hub.*;
import com.viaoa.sync.OASyncDelegate;

/**
 * This is used to access all of the Root level Hubs.  This is so that they 
 * will not have to be passed into and through the models.
 * After client login, the Hubs will be shared with the Hubs in the ServerRoot object from the server.
 * @author vincevia
 * 
 * @see ClientController#initializeClientModel
 */
public class ModelDelegate {
    private static Logger LOG = Logger.getLogger(ModelDelegate.class.getName());
    
    private static final Hub<AppUserLogin> hubLocalAppUserLogin = new Hub<AppUserLogin>(AppUserLogin.class); 
    private static final Hub<AppUser> hubLocalAppUser = new Hub<AppUser>(AppUser.class); 

    /*$$Start: ModelDelegate1 $$*/
    // lookups, preselects
    private static final Hub<AppServer> hubAppServers = new Hub<AppServer>(AppServer.class);
    private static final Hub<AppUser> hubAppUsers = new Hub<AppUser>(AppUser.class);
    // filters
    // UI containers
    private static final Hub<AppUserLogin> hubAppUserLogins = new Hub<AppUserLogin>(AppUserLogin.class);
    private static final Hub<AppUserError> hubAppUserErrors = new Hub<AppUserError>(AppUserError.class);
    /*$$End: ModelDelegate1 $$*/    
    
    
    public static void initialize(ServerRoot rootServer, ClientRoot rootClient) {
        LOG.fine("selecting data");

        /*$$Start: ModelDelegate2 $$*/
        // lookups, preselects
        setSharedHub(getAppServers(), rootServer.getAppServers());
        setSharedHub(getAppUsers(), rootServer.getAppUsers());
        // filters
        // UI containers
        getAppUserLogins().setSharedHub(rootServer.getAppUserLogins());
        getAppUserErrors().setSharedHub(rootServer.getAppUserErrors());
        /*$$End: ModelDelegate2 $$*/    
    
        for (int i=0; i<120 ;i++) {
            if (aiExecutor.get() == 0) break;
            if (i > 5) {
                LOG.fine(i+"/120) waiting on initialize to finish sharing hubs");
            }
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {}
        }
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
            queExecutorService = null;
        }
        LOG.fine("completed selecting data");
    }

    public static Hub<AppUser> getLocalAppUserHub() {
        return hubLocalAppUser;
    }
    public static AppUser getLocalAppUser() {
        return getLocalAppUserHub().getAO();
    }
    public static void setLocalAppUser(AppUser user) {
        getLocalAppUserHub().add(user);
        getLocalAppUserHub().setAO(user);
    }

    public static Hub<AppUserLogin> getLocalAppUserLoginHub() {
        return hubLocalAppUserLogin;
    }
    public static AppUserLogin getLocalAppUserLogin() {
        return getLocalAppUserLoginHub().getAO();
    }
    public static void setLocalAppUserLogin(AppUserLogin userLogin) {
        getLocalAppUserLoginHub().add(userLogin);
        getLocalAppUserLoginHub().setAO(userLogin);
        if (userLogin != null) {
            setLocalAppUser(userLogin.getAppUser());
        }
    }
    
    /*$$Start: ModelDelegate3 $$*/
    public static Hub<AppServer> getAppServers() {
        return hubAppServers;
    }
    public static Hub<AppUser> getAppUsers() {
        return hubAppUsers;
    }
    public static Hub<AppUserLogin> getAppUserLogins() {
        return hubAppUserLogins;
    }
    public static Hub<AppUserError> getAppUserErrors() {
        return hubAppUserErrors;
    }
    /*$$End: ModelDelegate3 $$*/    


    // thread pool for initialize
    private static ThreadPoolExecutor executorService;
    private static LinkedBlockingQueue<Runnable> queExecutorService;
    private static final AtomicInteger aiExecutor = new AtomicInteger();
    private static void setSharedHub(final Hub h1, final Hub h2) {
        HubAODelegate.warnOnSettingAO(h1);
        if (executorService == null) {
            queExecutorService = new LinkedBlockingQueue<Runnable>(Integer.MAX_VALUE);
            // min/max must be equal, since new threads are only created when queue is full
            executorService = new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, queExecutorService); 
            executorService.allowCoreThreadTimeOut(true); // ** must have this
        }
        
        aiExecutor.incrementAndGet();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    h1.setSharedHub(h2, false);
                }
                finally {
                    aiExecutor.decrementAndGet();
                }
            }
        });
    }
}