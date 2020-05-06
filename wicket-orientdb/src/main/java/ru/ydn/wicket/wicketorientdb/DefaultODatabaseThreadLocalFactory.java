package ru.ydn.wicket.wicketorientdb;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.orientechnologies.orient.core.db.*;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.security.OSecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ODatabaseThreadLocalFactory} for obtaining {@link ODatabaseDocument} according to {@link IOrientDbSettings}
 */
public class DefaultODatabaseThreadLocalFactory implements ODatabaseThreadLocalFactory
{
	private static final Logger LOG = LoggerFactory.getLogger(DefaultODatabaseThreadLocalFactory.class);

	private final ConcurrentLinkedHashMap<String, ODatabaseDocumentInternal> cachedThreadDatabases =
					new ConcurrentLinkedHashMap.Builder<String, ODatabaseDocumentInternal>()
									.maximumWeightedCapacity(100)
									.build();


	private OrientDbWebApplication app;
	
	public DefaultODatabaseThreadLocalFactory(OrientDbWebApplication app)
	{
		this.app = app;
	}
	
	@Override
	public ODatabaseDocumentInternal getThreadDatabase() {
		IOrientDbSettings settings = app.getOrientDbSettings();
		OrientDbWebSession session = OrientDbWebSession.exists()?OrientDbWebSession.get():null;
		String username;
		String password;
		if(session!=null && session.isSignedIn())
		{
			username = session.getUsername();
			password = session.getPassword();
		}
		else
		{
			username = settings.getGuestUserName();
			password = settings.getGuestPassword();
		}
		ODatabaseDocumentInternal db = getThreadDatabaseFromCache(settings.getDbName(), username, password);

		if (db == null) {
			db = (ODatabaseDocumentInternal) settings.getContext().cachedPool(settings.getDbName(), username, password).acquire();
			cachedThreadDatabases.put(getCacheKey(settings.getDbName(), username, password), db);
		}

		return db;
	}

	private ODatabaseDocumentInternal getThreadDatabaseFromCache(String database, String username, String password) {
		ODatabaseDocumentInternal db = cachedThreadDatabases.get(getCacheKey(database, username, password));
		if (db != null && !db.isClosed()) {
			db.activateOnCurrentThread();
			return db;
		}
		return null;
	}

	private String getCacheKey(String database, String username, String password) {
		return Thread.currentThread().getName() + "-" + Thread.currentThread().hashCode() +
						"-" + OSecurityManager.instance().createSHA256(Thread.currentThread().hashCode() + database + username + password);
	}

	/**
	 * Utility method to obtain {@link ODatabaseDocument} from {@link ODatabase}
	 * @param db {@link ODatabase} to cast from
	 * @return {@link ODatabaseDocument} for a specified {@link ODatabase}
	 */
	public static ODatabaseDocument castToODatabaseDocument(ODatabase<?> db)
	{
		while(db!=null && !(db instanceof ODatabaseDocument))
		{
			if(db instanceof ODatabaseInternal<?>)
			{
				db = ((ODatabaseInternal<?>)db).getUnderlying();
			}
		}
		return (ODatabaseDocument)db;
	}
}