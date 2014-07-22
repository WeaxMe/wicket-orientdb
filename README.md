wicket-orientdb
===============

Everything you need to work with Apache Wicket and OrientDB.
Library contains several functional part which can be used separatly or all together.

Initial setup of application
-----------------------

You should inherit WebApplication class from `OrientDbWebApplication` to use OrientDB and its authentication with in your application.

```java
public class WicketApplication extends OrientDbWebApplication
```

Specify connection properties
```java
		getOrientDbSettings().setDBUrl("local:localhost/"+DB_NAME);
		getOrientDbSettings().setDefaultUserName("admin");
		getOrientDbSettings().setDefaultUserPassword("admin");
```

Embedded OrientDB Server
------------------------

If you need to run OrientDB in embedded mode please use `EmbeddOrientDbApplicationListener`

```java
public class WicketApplication extends OrientDbWebApplication
{
@Override
	public void init()
	{
		super.init();
		getApplicationListeners().add(new EmbeddOrientDbApplicationListener(WicketApplication.class.getResource("db.config.xml"))
		{

			@Override
			public void onAfterServerStartupAndActivation() throws Exception {
				OServerAdmin serverAdmin = new OServerAdmin("localhost/"+DB_NAME).connect("root", "WicketOrientDB");
				if(!serverAdmin.existsDatabase())
			    serverAdmin.createDatabase(DB_NAME, "graph", "local");
			    
			}
			
		});
		getOrientDbSettings().setDBUrl("local:localhost/"+DB_NAME);
		getOrientDbSettings().setDefaultUserName("admin");
		getOrientDbSettings().setDefaultUserPassword("admin");
	}
}
```

PropertyModel
-------------

To gain access to Orient DB document from your application please use `ODocumentPropertyModel` instead of common `PropertyModel`.
Important! Following issue have been created to introduce support of custom properties models into wicket: 
https://issues.apache.org/jira/browse/WICKET-5623
If you interested in using PropertyModel as usual in wicket, please, take a look to following pull request: https://github.com/apache/wicket/pull/74

Security
--------

It's easy to integrate with OrientDB security stuff:
You can either specify static required orientDB resources

```java
@RequiredOrientResource(value = ODatabaseSecurityResources.SCHEMA, permissions={OrientPermission.READ, OrientPermission.WRITE})
public class MyPage extends WebPage {
...
```
```java
@RequiredOrientResources({
	@RequiredOrientResource(value = ODatabaseSecurityResources.SCHEMA, permissions=OrientPermission.READ),
	@RequiredOrientResource(value = ODatabaseSecurityResources.ALL_CLASSES, permissions=OrientPermission.READ),
})
public class MyPanel extends Panel {
...
```

or provide them dynamically: just implement ISecuredComponent

```java
public class SaveSchemaCommand<T> extends SavePrototypeCommand<T> implements ISecuredComponent {
...
	@Override
	public RequiredOrientResource[] getRequiredResources() {
		T object = objectModel.getObject();
		OrientPermission permission = (object instanceof IPrototype<?>)?OrientPermission.CREATE:OrientPermission.UPDATE;
		return OSecurityHelper.requireResource(ODatabaseSecurityResources.SCHEMA, permission);
	}

```

OrientDB objects prototypes
---------------------------

Sometimes it's useful to work with object without actual creation of that object. Examples: OClass, OProperty, OIndex - all this objects require pre-creation in DB. Prototyping microframeworks allows creation of 'Prototype' of some objects, modify it and only after that 'realize' it in real environment. 

Usage:
```java
OClass oClass = OClassPrototyper.newPrototype();
oClass.setName("MyClass");
oClass.setSuperClass(superClass);
OClass realOClass = ((IPrototype<OClass>)oClass).realizePrototype();
```










