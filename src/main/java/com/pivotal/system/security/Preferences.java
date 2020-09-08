/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.system.security;

import com.pivotal.system.hibernate.entities.UserEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.controllers.utils.GridFieldList;
import com.pivotal.web.controllers.utils.GridFilterCriteria;
import com.pivotal.web.controllers.utils.GridSortCriteria;
import com.pivotal.web.servlet.ServletHelper;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.io.Serializable;
import java.util.*;

/**
 * This class provides an abstraction of the preferences XML data
 * held in the user entity.<p>
 * It serves to give an easy and consistent mechanism for saving and
 * retrieving user preferences in whatever format they may exist.<p>
 * Fundamentally, it wraps a serializable map to store objects which
 * it doesn't care or know of what type, simple or complex just as long
 * as they can be serialized.<p>
 * For the most part, user preferences will be tied to specific pages
 * or widget display so to make this easier, keys are automatically
 * prefixed with the current HttpRequest path i.e. for a preference
 * of "columns" from a url of "/admin/transducers/list" the key will
 * be "admin.transducers.list.columns"<p>
 * Also, the system will attempt to find the preference in an order
 * of priority until it finds it for example;<p>
 *     "admin.transducers.list.columns"<br>
 *     "admin.transducers.columns"<br>
 *     "admin.columns"<br>
 *     "columns"<p>
 * If the key contains a dot e.g. "general.font-size" then the auto-prefix
 * is not applied.
 * There are three hierarchical layers in which a value is sought;<p>
 * 1. Session<br>
 * 2. Database<br>
 * 3. Defaults<br>
 * The Session is the primary source and values in here can mask those
 * at the lower levels. Values are stored here by calling putTransient
 * and can be cleared using removeTransient and clearTransientPage
 * The Database layer stores the values within the profile of the
 * current user
 * The Defaults is a transient map that is updated (normally by the
 * view) with last resort values for a preference.
 */
public class Preferences<V> implements Map<String,V>, Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Preferences.class);
    /** Constant <code>USER_PREFERENCES="UserPreferences"</code> */
    public static final String USER_PREFERENCES = "UserPreferences";
    private static final long serialVersionUID = 4702253417427228802L;

    private enum PreferenceType {
        DATABASE, SESSION, DEFAULT
    }

    private PreferenceType type = PreferenceType.DATABASE;
    private Integer userId;
    private Map<String,V> preferences = new LinkedCaseInsensitiveMap<>();
    private Preferences<V> defaults = null;
    private Preferences<V> session = null;
    private String namespace = null;

    /**
     * Constructor used internally to create clones
     */
    private Preferences() {
    }

    /**
     * Constructor used to map the session and transient defaults
     * @param type Type of preference storage to use
     * @param namespace This is the namespace to use for all preferences
     */
    @SuppressWarnings("unchecked")
    private Preferences(PreferenceType type, String namespace) {
        this.type = type;
        this.namespace = namespace;
        if (ServletHelper.getSession()!=null) {
            Map<String,V> tmp = (Map<String,V>)ServletHelper.getSession().getAttribute(USER_PREFERENCES);
            if (tmp!=null) preferences.putAll(tmp);
        }
    }

    /**
     * Creates a preferences object for the given User
     *
     * @param user User to get preferences for
     * @param namespace This is the namespace to use for all preferences
     * @throws com.pivotal.utils.PivotalException if any.
     */
    @SuppressWarnings("unchecked")
    public Preferences(UserEntity user, String namespace) throws PivotalException {
        if (user==null)
            throw new PivotalException("User object is null");
        logger.debug("Getting preferences for [{}] in namespace[{}]", user, namespace);
        this.userId = user.getId();
        this.namespace = namespace;

        // De-serialise the preferences

        if (!Common.isBlank(user.getPreferencesXML()))
            preferences = (LinkedCaseInsensitiveMap)getXStream().fromXML(user.getPreferencesXML());
        if (preferences==null)
            preferences = new LinkedCaseInsensitiveMap<>();

        // Create the preferences for the session and defaults

        session = new Preferences<>(PreferenceType.SESSION, namespace);
        defaults = new Preferences<>(PreferenceType.DEFAULT, namespace);
    }

    /**
     * Returns a XStream instance to use with all the required aliases
     * already added
     * @return XStream instance
     */
    private XStream getXStream() {
        XStream xstream = new XStream(new StaxDriver());
//        xstream.alias("preferences", CaseInsensitiveMap.class);
        xstream.alias("preferences", CaseInsensitiveMap.class);
        xstream.alias("sortcriteria", GridSortCriteria.class);
        xstream.alias("searchcriteria", GridFilterCriteria.class);
        xstream.alias("fieldlist", GridFieldList.class);
        return xstream;
    }

    /**
     * Returns the map of default values that can be used in the
     * event that a particular object isn't available from the
     * user preferences
     *
     * @return Case insensitive map of default values
     */
    public Preferences<V> getDefaults() {
        return defaults;
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return getAll().size();
    }

    /**
     * Returns an amalgamated of all the preferences
     *
     * @return Amalgamated map of preferences from all the various sources
     */
    private Map<String,V> getAll() {
        Map<String,V> tmp = new LinkedCaseInsensitiveMap<>();
        if (defaults!=null) tmp.putAll(defaults);
        if (preferences!=null) tmp.putAll(preferences);
        if (session!=null) tmp.putAll(session);
        return tmp;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return getAll().isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsKey(Object key) {
        return getAll().containsKey(getKey((String)key));
    }

    /**
     * Returns true if the local value is not set so the system would use
     * a default value - used to determine if a user preference is not set
     * @param key Key to check
     * @return True if there is no user preference
     */
    public boolean isUsingDefault(Object key) {
        return !preferences.containsKey(key);
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsValue(Object value) {
        return getAll().containsValue(value);
    }

    /** {@inheritDoc} */
    @Override
    public V get(Object key) {
        if (!Common.isBlank(key))
            return getValue((String)key);
        else
            return null;
    }

    /**
     * Returns a preference from the user - if null, then the
     * defaultValue is returned
     *
     * @param key Name of the preference
     * @param defaultValue Default value to return if name doesn't exist
     * @return Object value - could be anything
     */
    public Object get(Object key, Object defaultValue) {
        Object returnValue= get(key);
        if (returnValue!=null)
            return returnValue;
        else
            return defaultValue;
    }

    /**
     * Returns the value of the preference or the defaultValue
     * if it doesn't exist
     *
     * @param key Preference name
     * @param defaultValue Default value
     * @return Value or default
     */
    public String get(Object key, String defaultValue) {
        return (String)get(key, (Object)defaultValue);
    }

    /**
     * Returns the value of the preference or the defaultValue
     * if it doesn't exist
     *
     * @param key Preference name
     * @param defaultValue Default value
     * @return Value or default
     */
    public Date get(Object key, Date defaultValue) {
        Object value = get(key, (Object)defaultValue);
        if (value!=null) {
            if (value instanceof String)
                return Common.parseDate((String)value);
            else
                return (Date)value;
        }
        else
            return defaultValue;
    }

    /**
     * Returns the value of the preference or the defaultValue
     * if it doesn't exist
     *
     * @param key Preference name
     * @param defaultValue Default value
     * @return Value or default
     */
    public Double get(Object key, Double defaultValue) {
        Object value = get(key, (Object)defaultValue);
        if (value!=null) {
            if (value instanceof String)
                return Common.parseDouble((String)value);
            else
                return (Double)value;
        }
        else
            return defaultValue;
    }

    /**
     * Returns the value of the preference or the defaultValue
     * if it doesn't exist
     *
     * @param key Preference name
     * @param defaultValue Default value
     * @return Value or default
     */
    public Integer get(Object key, Integer defaultValue) {
        Object value = get(key, (Object)defaultValue);
        if (value!=null) {
            if (value instanceof String)
                return Common.parseInt((String)value);
            else
                return (Integer)value;
        }
        else
            return defaultValue;
    }


    /**
     * Returns the value of the preference or the defaultValue
     * if it doesn't exist
     *
     * @param key Preference name
     * @param defaultValue Default value
     * @return Value or default
     */
    public Long get(Object key, Long defaultValue) {
        Object value = get(key, (Object)defaultValue);
        if (value!=null) {
            if (value instanceof String)
                return Common.parseLong((String)value);
            else
                return (Long)value;
        }
        else
            return defaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public V put(String key, V value) {
        if (key.contains(".") || Common.isBlank(namespace)) {
            logger.debug("Preference key is not a string or contains a period or there is no request to use [{}]", key);
            preferences.put(key, value);
        }
        else {
            // Prefix the key with the request namespace

            if (Common.isBlank(namespace)) {
                logger.debug("Preference key stored at root [{}]", key);
                preferences.put(key, value);
            }
            else {
                logger.debug("Preference key stored with prefix [{}.{}]", namespace, key);
                preferences.put(namespace + '.' + key, value);
            }
        }
        save();
        return get(key);
    }

    /** {@inheritDoc} */
    @Override
    public V remove(Object key) {
        V returnValue = null;
        Object keyToUse = getKey((String)key);
        if (keyToUse != null) {
            if (session!=null)
                returnValue = session.remove(keyToUse);
            if (preferences!=null) {
                if (returnValue==null)
                    returnValue = preferences.remove(keyToUse);
                else
                    preferences.remove(keyToUse);
            }
            if (defaults!=null) {
                if (returnValue==null)
                    returnValue = defaults.remove(keyToUse);
                else
                    defaults.remove(keyToUse);
            }
            save();
        }
        return returnValue;
    }

    /** {@inheritDoc} */
    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
        preferences.putAll(m);
        save();
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        boolean save=!preferences.isEmpty();
        if (session!=null) session.clear();
        if (preferences!=null) preferences.clear();
        if (defaults!=null) defaults.clear();
        if (save) save();
    }

    /**
     * Clears all the settings for the current page namespace given
     * by the URI pattern
     */
    public void clearPage() {
        if (!Common.isBlank(namespace)) {
            boolean saveRequired = false;
            String searchPath = namespace.toLowerCase() + '.';
            for (Object key : new HashSet<Object>(preferences.keySet())) {
                if (key instanceof String && ((String) key).toLowerCase().startsWith(searchPath)) {
                    saveRequired = true;
                    preferences.remove(key);
                }
            }
            if (session!=null) {
                for (Object key : new HashSet<Object>(session.keySet())) {
                    if (key instanceof String && ((String) key).toLowerCase().startsWith(searchPath)) {
                        session.remove(key);
                    }
                }
            }
            if (defaults!=null) {
                for (Object key : new HashSet<Object>(defaults.keySet())) {
                    if (key instanceof String && ((String) key).toLowerCase().startsWith(searchPath)) {
                        defaults.remove(key);
                    }
                }
            }
            if (saveRequired)
                save();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> keySet() {
        return getAll().keySet();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<V> values() {
        return getAll().values();
    }

    /** {@inheritDoc} */
    @Override
    public Set<Entry<String,V>> entrySet() {
        return getAll().entrySet();
    }

    /**
     * Gets the value from either the preferences or if empty,
     * the defaults
     *
     * @param key Key of the object to get
     *
     * @return Object value
     */
    private V getValue(String key) {
        V returnValue = null;
        String keyToUse = getKey(key);
        if (keyToUse != null) {
            returnValue = getAll().get(keyToUse);
        }
        return returnValue;
    }

    /**
     * Gets the key to use for the given start key
     *
     * @param key Key of the object to get
     *
     * @return Key to use
     */
    private String getKey(String key) {

        String returnValue = null;
        if (key!=null) {

            // Check to see if the key contains a dot or is not even a string
            // or there is no namespace to use

            if (key.contains(".") || Common.isBlank(namespace)) {
                logger.debug("Preference key is not a string or contains a period or there is no request to use [{}]", key);
                returnValue = key;
            }
            else {

                // Get a list of the constituents of the path

                Map tmp = getAll();
                List<String> parts = Common.splitToList(namespace, "\\.");
                while (returnValue==null && parts!=null) {
                    String stringKey = Common.join(parts, ".") + (parts.isEmpty()?"":'.') + key;
                    logger.debug("Trying key [{}]", stringKey);
                    if (tmp.containsKey(stringKey)) {
                        logger.debug("Found preference with key [{}]", stringKey);
                        returnValue = stringKey;
                    }

                    // Knock another bit off the search key

                    else {
                        if (parts.isEmpty())
                            parts = null;
                        else
                            parts.remove(parts.size() - 1);
                    }
                }
            }
        }
        return returnValue;
    }

    /**
     * Returns the session map - this allows us to capture write events to
     * force a save back to the Session
     *
     * @return Map of Session values
     */
    public Preferences<V> getSession() {
        return session;
    }

    /**
     * Save the values to the database
     * This would need to be called if an object contained within the collection
     * was changed without calling a put or a putall
     */
    private void save() {

        // Are we being used as a persistent store

        if (type.equals(PreferenceType.DATABASE)) {
            UserEntity user = HibernateUtils.getEntity(UserEntity.class, userId);
            if (user!=null) {

                // Serialize the preferences to an XML stream and save it to the database

                logger.debug("Saving preferences for [{}]", user.getEmail());
                user.setPreferencesXML(Common.isBlank(preferences)?null:getXStream().toXML(preferences));
                HibernateUtils.save(user);

                // If this is the same user as that in the Session then make sure
                // we update the user in the session

                UserManager.updateUserInSession(user);
            }
        }

        // Perhaps a Session store

        else if (type.equals(PreferenceType.SESSION)) {
            if (ServletHelper.getSession()!=null) {
                logger.debug("Saving session level preferences");
                ServletHelper.getSession().setAttribute(USER_PREFERENCES, this);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder tmp = new StringBuilder();
        for (Map.Entry entry : getAll().entrySet()) {
            tmp.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }
        return tmp.toString();
    }

    /**
     * Get the current namespace being used
     *
     * @return Namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the namespace to use when looking up values in the preferences
     *
     * @param namespace Namespace e.g. admin.user.grid
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * This method creates a soft clone of itself whereby it copies
     * the references to all the constituent parts of the Preferences object but
     * gives them a new namespace.
     * This allows us to share the same storage mechanism as the original but
     * using a different namespace.
     *
     * @param namespace Namespace to use
     * @return Preferences object
     */
    public Preferences<V> clone(String namespace) {
        Preferences<V> tmp = new Preferences<>();
        tmp.namespace = namespace;
        tmp.preferences = preferences;
        tmp.userId = userId;
        if (type.equals(PreferenceType.DATABASE)) {
            tmp.defaults = defaults.clone(namespace);
            tmp.session = new Preferences<>(PreferenceType.SESSION, namespace);
        }
        return tmp;
    }

}
