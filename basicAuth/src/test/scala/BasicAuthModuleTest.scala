import info.schleichardt.play2.basicauth.{BasicAuth, Credentials}
import org.junit.{Test, Before}
import play.libs.WS
import play.GlobalSettings
import play.mvc.Http.RequestHeader
import play.test.{FakeApplication, WithServer, Helpers}
import java.util.{Map => JMap, List => JList}
import com.google.common.collect.Lists._
import org.fest.assertions.Assertions.assertThat
import play.api.mvc.{Results, Action}
import scala.collection.JavaConversions._
import info.schleichardt.play2.basicauth.PlainCredentialsFromConfigAuthenticator

class BasicAuthModuleTest extends WithServer {
  val credentials = Credentials("michael", "secret")
  val secretContent = "the secret content"
  
  @Before def setUp {
    start()
  }

  @Test def noAccessWithoutTokens {
    val response = WS.url(s"http://localhost:$port").get.get(2000)
    assertThat(response.getStatus).isEqualTo(401)
    assertThat(response.getBody).doesNotContain(secretContent)
  }
  
  @Test def accessWithValidCredentials {
    val reponse = WS.url(s"http://localhost:$port").setAuth(credentials.username, credentials.password).get.get(2000)
    assertThat(reponse.getStatus).isEqualTo(200)
    assertThat(reponse.getBody).contains(secretContent)
  }
  
  @Test def NoAccessWithWrongCredentials {
    val response = WS.url(s"http://localhost:$port").setAuth(credentials.username, credentials.password + "x").get.get(2000)
    assertThat(response.getStatus).isEqualTo(401)
    assertThat(response.getBody).doesNotContain(secretContent)
  }

  override def start(fakeApplication: FakeApplication) = {
    import PlainCredentialsFromConfigAuthenticator.KeyPassword
    import PlainCredentialsFromConfigAuthenticator.KeyUsername
    val additionalConfiguration: JMap[String,Any] = Map(KeyUsername -> credentials.username, KeyPassword -> credentials.password)
    val additionalPlugin: JList[String] = newArrayList()
    val global: GlobalSettings = new GlobalSettings() {

      val basicAuth = new BasicAuth(new PlainCredentialsFromConfigAuthenticator)

      override def onRouteRequest(request: RequestHeader) = {
//        basicAuth.authenticate(request, super.onRouteRequest(request)) //this would be the default use case
        val handler = Action(Results.Ok(secretContent))//workaround since there are no routes present
        basicAuth.authenticate(request, handler)
      }
    }
    super.start(Helpers.fakeApplication(additionalConfiguration, additionalPlugin, global))
  }
}