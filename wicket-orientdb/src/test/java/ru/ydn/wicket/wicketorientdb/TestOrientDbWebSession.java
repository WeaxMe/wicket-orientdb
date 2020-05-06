package ru.ydn.wicket.wicketorientdb;

import org.junit.ClassRule;
import org.junit.Test;
import ru.ydn.wicket.wicketorientdb.junit.WicketOrientDbTesterScope;

public class TestOrientDbWebSession {

  @ClassRule
  public static WicketOrientDbTesterScope wicket = new WicketOrientDbTesterScope("admin", "admin");


  @Test
  public void testGetUser() {
    OrientDbWebSession session = wicket.getTester().getSession();
    System.out.println("Session User: " + session.getUser());
    System.out.println("Database User: " + session.getDatabase().getUser());
  }
}
