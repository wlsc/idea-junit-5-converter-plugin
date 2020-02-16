
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class JUnit4Test {

  @Test
  public void name0a() {
    assertTrue(true);
    assertTrue("true", true);
    Assert.assertTrue("true", true);
    assertFalse(false);
    assertFalse("false", false);
    Assert.assertFalse("false", false);
    assertArrayEquals(new Object[][]{}, new Object[][]{});
    assertArrayEquals("text", new Object[]{}, new Object[]{});
    assertEquals(25, 28);
    assertEquals("text", 25, 28);
    assertNotEquals(25, 28);
    assertNotEquals("text", 25, 28);
    assertSame("text", 25);
    assertNotSame("text", 25);
    assertNull(this);
    assertNotNull(this);
    Assert.assertNotNull(this);
    assertNotNull("text", this);
  }

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
