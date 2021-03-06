package ru.ydn.wicket.wicketorientdb.utils;

import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.type.ODocumentWrapper;
import ru.ydn.wicket.wicketorientdb.IOrientDbSettings;
import ru.ydn.wicket.wicketorientdb.OrientDbWebApplication;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Closure for execution of portion queries/command on database for different user (commonly, under admin)
 * @param <V> return type
 */
public abstract class DBClosure<V> implements Serializable
{
	private static final long serialVersionUID = 1L;
	private final String dbUrl;
	private final String username;
	private final String password;
	
	public DBClosure()
	{
		this(null, null, null);
	}
	
	public DBClosure(String username, String password)
	{
		this(null, username, password);
	}
	
	public DBClosure(String dbUrl, String username, String password)
	{
		this.dbUrl = dbUrl;
		this.username = username;
		this.password = password;
	}
	/**
	 * @return result of execution
	 */
	public final V execute()
	{
		ODatabaseDocument db = null;
		ODatabaseRecordThreadLocal orientDbThreadLocal = ODatabaseRecordThreadLocal.instance();
		ODatabaseDocument oldDb = orientDbThreadLocal.getIfDefined();
		if(oldDb!=null) orientDbThreadLocal.remove(); //Required to avoid stack of transactions
		try
		{
			db = getSettings().getDatabasePoolFactory().get(getDBUrl(), getUsername(), getPassword()).acquire();
			db.activateOnCurrentThread();
			return execute(db);
		} 
		finally
		{
			if(db!=null) db.close();
			if(oldDb!=null) orientDbThreadLocal.set((ODatabaseDocumentInternal)oldDb);
			else orientDbThreadLocal.remove();
		}
	}
	
	protected String getDBUrl()
	{
		return dbUrl!=null?dbUrl:getSettings().getDBUrl();
	}
	
	protected String getUsername()
	{
		return username!=null?username:getSettings().getAdminUserName();
	}
	
	protected String getPassword()
	{
		return password!=null?password:getSettings().getAdminPassword();
	}
	
	protected IOrientDbSettings getSettings()
	{
		return OrientDbWebApplication.lookupApplication().getOrientDbSettings();
	}
	
	/**
	 * @param db temporal DB for other user
	 * @return results for execution on supplied DB
	 */
	protected abstract V execute(ODatabaseDocument db);
	
	/**
	 * Simplified function to execute under admin
	 * @param func function to be executed
	 * @param <R> type of returned value
	 * @return result of a function
	 */
	public static <R> R sudo(Function<ODatabaseDocument, R> func) {
		return new DBClosure<R>() {
			@Override
			protected R execute(ODatabaseDocument db) {
				return func.apply(db);
			}
		}.execute();
	}

	/**
	 * Simplified consumer to execute under admin
	 * @param consumer - consumer to be executed
	 */
	public static void sudoConsumer(Consumer<ODatabaseDocument> consumer) {
		new DBClosure<Void>() {
			@Override
			protected Void execute(ODatabaseDocument db) {
				consumer.accept(db);
				return null;
			}
		}.execute();
	}
	
	
	/**
	 * Allow to save set of document under admin
	 * @param docs set of document to be saved
	 */
	public static void sudoSave(final ODocument... docs) {
		if(docs==null || docs.length==0) return;
		new DBClosure<Boolean>() {

			@Override
			protected Boolean execute(ODatabaseDocument db) {
				db.begin();
				for (ODocument doc : docs) {
					db.save(doc);
				}
				db.commit();
				return true;
			}
		}.execute();
	}
	
	/**
	 * Allow to save set of document wrappers under admin
	 * @param dws set of document to be saved
	 */
	public static void sudoSave(final ODocumentWrapper... dws) {
		if(dws==null || dws.length==0) return;
		new DBClosure<Boolean>() {

			@Override
			protected Boolean execute(ODatabaseDocument db) {
				db.begin();
				for (ODocumentWrapper dw : dws) {
					dw.save();
				}
				db.commit();
				return true;
			}
		}.execute();
	}
}
