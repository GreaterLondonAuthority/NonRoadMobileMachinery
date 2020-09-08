package com.pivotal.api;

import com.pivotal.system.hibernate.entities.DatasourceEntity;
import com.pivotal.system.monitoring.jmx.JMXUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;

/**
 * <p>PoolBroker class.</p>
 */
public class PoolBroker {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PoolBroker.class);
    private HashMap<String, DataSource> activePools;
    private static PoolBroker instance;

    /** Constant <code>MAX_ACTIVE=0</code> */
    public static final int MAX_ACTIVE = 0;
    /** Constant <code>MAX_IDLE=0</code> */
    public static final int MAX_IDLE = 0;
    /** Constant <code>MIN_IDLE=0</code> */
    public static final int MIN_IDLE = 0;
    /** Constant <code>INITIAL_SIZE=0</code> */
    public static final int INITIAL_SIZE = 0;
    /** Constant <code>MAX_WAIT=0</code> */
    public static final int MAX_WAIT = 0;
    /** Constant <code>REMOVE_ABANDONED=true</code> */
    public static final boolean REMOVE_ABANDONED = true;
    /** Constant <code>REMOVE_ABANDONED_TIMEOUT=0</code> */
    public static final int REMOVE_ABANDONED_TIMEOUT = 0;
    /** Constant <code>VALIDATION_QUERY="select 1"</code> */
    public static final String VALIDATION_QUERY = "select 1";
    /** Constant <code>INIT_SQL=""</code> */
    public static final String INIT_SQL = "";

    static {
        instance = null;
    }

    /**
     * Making sure this object is a singleton
     */
    private PoolBroker() {
        activePools = new HashMap<>();
        instance = null;
    }


    /**
     * <p>Getter for the field <code>instance</code>.</p>
     *
     * @return a {@link PoolBroker} object.
     */
    public synchronized static PoolBroker getInstance() {
        if (instance == null)
            instance = new PoolBroker();
        return instance;
    }

    /**
     * Will shut down the manager
     */
    public static void shutdown() {
        if (instance!=null) instance.destroyAllPools(true);
    }

    /**
     * Returns a ConnectionPool matching an NRMM's DatasourceEntity. If the pool is already in the availablePoolsCache it returns the existing one if not, it creates a new one, adds it to the availablePoolsCache and returns it.
     *
     * @param datasource NRMM's DatasourceEntity
     * @return Connection Pool
     */
    public DataSource getPool(final DatasourceEntity datasource) {
        logger.debug("Pool has been requested...");
        DataSource ret = null;
        String poolId = datasource != null ? datasource.getName() : null;
        if (poolId != null) {
            if (activePools.containsKey(poolId)) {
                ret = activePools.get(poolId);
            }
            else if (datasource.isUseConnectionPool()) {
                ret = new DataSource();
                ret.setPoolProperties(getPoolProps(datasource));
                ret.setName(poolId);
                activePools.put(poolId, ret);
            }
            else
                logger.debug("Data source [{}] is not using a pool", datasource.getName());
        }
        logger.debug("Returning Pool {}", ret);
        return ret;
    }

    /**
     * Returns true if there is a pool for the DatasourceEntity.
     *
     * @param datasource NRMM's DatasourceEntity
     * @return True if the datasource is pooled
     */
    public boolean isPooled(final DatasourceEntity datasource) {
        logger.debug("Pool Has been requested...");
        Boolean ret = false;
        if (datasource!=null) {
            ret = activePools.containsKey(datasource.getName());
            logger.debug("Data source [{}] is {} using a pool", datasource.getName(), ret ? "" : "not ");
        }
        return ret;
    }

    /**
     * Returns a connection from the available Pool matching an NRMM's DatasourceEntity. If there's no pool
     * yet, one will be created.
     *
     * @param datasource NRMM's DatasourceEntity
     * @return Database Connection
     */
    public Connection getConnection(DatasourceEntity datasource) {
        logger.debug("Connection Has been requested...");
        Connection ret = null;
        DataSource ds = getPool(datasource);
        try {
            if (ds != null) {
                ret = ds.getConnection();
                if (ret == null) throw new SQLException("Cannot get a connection from the pool");
                registerJmx(ds);
            }
        }
        catch (SQLException e) {
            logger.error("Cannot connect to database..." + PivotalException.getErrorMessage(e), e);
            throw new PivotalException("Cannot connect to database [" + ds.getUrl() + "] for datasource [" + datasource.getName() + "] " + PivotalException.getErrorMessage(e));
        }
        logger.debug("Returning Connection {}", ret);
        return ret;
    }

    /**
     * Builds a Pool configuration object from an NRMM's DatasourceEntity.
     * The returned object will be used in a pool instantiation.
     * All parameters should be set here an not anywhere else.
     *
     * @param datasource NRMM's DatasourceEntity
     * @return PoolConfiguration Object.
     */
    private static PoolConfiguration getPoolProps(DatasourceEntity datasource) {

        if (datasource.isUseConnectionPool()) {
            PoolProperties p = new PoolProperties();
            p.setUrl(datasource.getDatabaseUrl());
            p.setDriverClassName(datasource.getDriver());
            p.setUsername(datasource.getUsername());
            p.setPassword(datasource.getPassword());
            p.setMaxActive(datasource.getMaxActive());
            p.setMaxIdle(datasource.getMaxIdle());
            p.setMinIdle(datasource.getMinIdle());
            p.setInitialSize(datasource.getInitialSize());
            p.setMaxWait(datasource.getMaxWait());
            p.setRemoveAbandoned(datasource.isRemoveAbandoned());
            p.setJmxEnabled(true);

            //Test on Borrow should always be here. If it isn't and the underlying connection dies (or is made invalid for some reason) it will still stay in the pool as idle.

            p.setTestOnBorrow(true);

            // Only sets the timeout if removed abandoned is active.

            if (datasource.isRemoveAbandoned())
                p.setRemoveAbandonedTimeout(datasource.getRemoveAbandonedTimeout() != null ? datasource.getRemoveAbandonedTimeout() : REMOVE_ABANDONED_TIMEOUT);

            p.setValidationQuery(Common.isBlank(datasource.getValidationQuery()) ? null : datasource.getValidationQuery());
            p.setInitSQL(Common.isBlank(datasource.getInitSql()) ? null : datasource.getInitSql());

            p.setTestWhileIdle(false);
            p.setTestOnReturn(false);
            p.setValidationInterval(30000);
            p.setTimeBetweenEvictionRunsMillis(30000);
            p.setMinEvictableIdleTimeMillis(30000);
            p.setLogAbandoned(true);
            p.setFairQueue(true);
            p.setJdbcInterceptors("ConnectionState;StatementFinalizer");
            return p;
        }
        else
            return null;
    }

    /**
     * Returns a Collection with all the available pools at runtime.
     *
     * @return Collection with all the available pools.
     */
    public Collection<DataSource> getAllActivePools() {
        return activePools.values();
    }


    /**
     * Destroys the pool with the given name if it exists in the active pools map.
     * If there are active connections, the pool won't be killed.
     * This method is null safe and the access to the activePoolList is synchronized.
     *
     * @param poolName pool name to kill
     * @return true if the pool was killed, false otherwise.
     */
    public boolean destroyPool(String poolName) {
        return destroyPool(poolName, false);
    }

    /**
     * Destroys the pool with the given name if it exists in the active pools map.
     * If there are active connections, the pool won't be killed unless you force it.
     * This method is null safe and the access to the activePoolList is synchronized.
     *
     * @param poolName  pool name to kill
     * @param forceKill kill even if there's active connections
     * @return true if the pool was killed, false otherwise.
     */
    public boolean destroyPool(String poolName, boolean forceKill) {
        boolean res = false;

        if (!Common.isBlank(poolName)) {
            synchronized (activePools) {
                try {
                    DataSource refPool = activePools.get(poolName);
                    if (refPool != null) {

                        // Can only destroy it if there's no active connections

                        if (refPool.getNumActive() == 0 || forceKill) {
                            activePools.remove(poolName);
                            unregisterJmx(refPool);
                            refPool.close(forceKill);
                            res = true;
                        }
                    }
                }
                catch (Throwable e) {
                    logger.error("Problem trying to destroy pool[{}] - {}", poolName, PivotalException.getErrorMessage(e));
                }
            }
        }
        return res;
    }

    /**
     * Returns true if the pool exists
     *
     * @param poolName  pool name to kill
     * @return true if the pool exists
     */
    public boolean poolExists(String poolName) {
        return !Common.isBlank(poolName) && activePools.containsKey(poolName);
    }

    /**
     * Destroys the pool with the given name if it exists in the active pools map.
     * If there are active connections, the pool won't be killed unless you force it.
     * This method is null safe and the access to the activePoolList is synchronized.
     *
     * @param forceKill kill even if there's active connections
     */
    public void destroyAllPools(boolean forceKill) {
        synchronized (activePools) {
            for (DataSource refPool : activePools.values()) {

                // Can only destroy it if there's no active connections

                if (refPool.getNumActive() == 0 || forceKill) {
                    try {
                        activePools.remove(refPool.getName());
                        unregisterJmx(refPool);
                        refPool.close(forceKill);
                    }
                    catch (Throwable e) {
                        logger.error("Problem trying to destroy pool[{}] - {}", refPool.getName(), PivotalException.getErrorMessage(e));
                    }
                }
            }
        }
    }

    /**
     * Registers the ConnectionPoolMBean under a unique name based on the ObjectName for the DataSource
     *
     * @param dataSource Pool to register
     */
    private static void registerJmx(DataSource dataSource) {
        try {
            if (dataSource!=null && dataSource.getPool()!=null && dataSource.getPool().getJmxPool() !=null ) {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                mbs.registerMBean(dataSource.getPool().getJmxPool(), JMXUtils.getObjectName("ConnectionPools", dataSource.getName()));
            }
        } catch (Exception e) {
            logger.debug("Unable to register JDBC pool with JMX - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Removes the pool from JMX monitoring
     *
     * @param dataSource Pool to un-register
     */
    private static void unregisterJmx(DataSource dataSource) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.unregisterMBean(JMXUtils.getObjectName("ConnectionPools", dataSource.getName()));
        } catch (InstanceNotFoundException ignore) {
            // NOOP
        } catch (Exception e) {
            logger.debug("Unable to unregister JDBC pool with JMX - {}", PivotalException.getErrorMessage(e));
        }
    }
}
