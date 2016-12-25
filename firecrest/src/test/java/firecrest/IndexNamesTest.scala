package firecrest

import org.junit.Test
import org.assertj.core.api.Assertions._
import org.joda.time.DateTime

class IndexNamesTest {

  private val indexNames = new IndexNames()

  @Test()
  def foo(): Unit = {
    assertThat(1).isEqualTo(1)
  }

  @Test()
  def testIndexName(): Unit = {
    assertThat(indexNames.indexName(new DateTime(2016, 1, 2, 0, 0)))
      .isEqualTo("log-2016-01-02")
  }

  @Test()
  def testParseValidName(): Unit = {
    assertThat(indexNames.parseIndexName("log-2016-01-02"))
      .isEqualTo(Some(new DateTime(2016, 1, 2, 0, 0)))
  }

  @Test()
  def testParseInvalidName(): Unit = {
    assertThat(indexNames.parseIndexName("log2016-01-02"))
      .isEqualTo(None)
  }

  @Test()
  def testIsValid(): Unit = {
    assertThat(indexNames.isValidName("log-2016-01-02")).isTrue
  }

}
