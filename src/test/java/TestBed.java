
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestBed {

  @Test
  public void name0() {
    assumeTrue(true);
    assumeTrue("true", true);
    Assume.assumeTrue("true", true);
    assumeFalse(false);
    assumeFalse("false", false);
    Assume.assumeFalse("false", false);
  }

  @Test()
  public void name1() {

  }

  @Test(timeout = 20L)
  public void name2() {
    name21();
  }

  @Test(expected = NullPointerException.class, timeout = 20L)
  public void name21() {
    name2();
  }

  @Test
  @Ignore
  public void name01() {

  }

  @Test
  @Ignore("because")
  public void name011() {

  }

  @Before
  public void before() {

  }

  @BeforeClass
  public static void before1() {

  }

  @After
  public void after() {

  }

  @AfterClass
  public static void after1() {

  }
}
