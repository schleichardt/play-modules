import com.ning.http.client.Realm.AuthScheme
import info.schleichardt.play2.basicauth.{ BasicAuth, Credentials }
import play.api.libs.ws.{ Response, WS }
import play.api.{ GlobalSettings, mvc }
import play.api.test.{ WithServer, WithApplication, PlaySpecification, FakeApplication }
import play.{ GlobalSettings => JGlobalSettings }
import play.mvc.Http.RequestHeader
import play.test.{ FakeApplication => JFakeApplication, WithServer => JWithServer, Helpers }
import java.util.{ Map => JMap, List => JList }
import com.google.common.collect.Lists._
import play.api.mvc.{ Handler, Results, Action }
import scala.collection.JavaConversions._
import info.schleichardt.play2.basicauth.PlainCredentialsFromConfigAuthenticator
import scala.concurrent.Await
import scala.concurrent.duration._

object BasicAuthModuleSpec extends PlaySpecification {
  val credentials = Credentials("michael", "secret")
  import credentials._
  import PlainCredentialsFromConfigAuthenticator.KeyPassword
  import PlainCredentialsFromConfigAuthenticator.KeyUsername
  val secretContent = "the secret content"

  def javaFakeApplication: FakeApplication = {
    val additionalConfiguration: JMap[String, Any] = Map(KeyUsername -> username, KeyPassword -> password)
    val additionalPlugin: JList[String] = newArrayList()
    val global = new JGlobalSettings() {

      val basicAuth = new BasicAuth(new PlainCredentialsFromConfigAuthenticator)

      override def onRouteRequest(request: RequestHeader) = {
        //        basicAuth.authenticate(request, super.onRouteRequest(request)) //this would be the default use case
        val handler = Action(Results.Ok(secretContent)) //workaround since there are no routes present
        basicAuth.authenticate(request, handler)
      }
    }
    Helpers.fakeApplication(additionalConfiguration, additionalPlugin, global).getWrappedApplication
  }

  def scalaFakeApplication: FakeApplication = {
    val additionalConfiguration: Map[String, Any] = Map(KeyUsername -> credentials.username, KeyPassword -> credentials.password)
    val global = new GlobalSettings() {

      val basicAuth = new BasicAuth(new PlainCredentialsFromConfigAuthenticator)

      override def onRouteRequest(request: mvc.RequestHeader): Option[Handler] = {
        //workaround since there are no routes present
        basicAuth.authenticate(request, Option(Action(Results.Ok(secretContent))))
      }
    }
    FakeApplication(additionalConfiguration = additionalConfiguration, withGlobal = Option(global))
  }

  Seq(("Java API" -> javaFakeApplication _), ("Scala API" -> scalaFakeApplication _)).foreach {
    case (name, func) =>
      s"the $name must deny access without credentials" in new WithServer(func()) {
        assertNoAccessGranted(flowWithoutCredentials(port))
      }

      s"the $name must allow access with valid credentials" in new WithServer(func()) {
        assertAccessGranted(flowWithValidCredentials(port))
      }

      s"the $name must deny access with invalid credentials" in new WithServer(func()) {
        assertNoAccessGranted(flowWithInvalidCredentials(port))
      }
  }

  def assertAccessGranted(response: Response) = {
    response.status === 200
    response.body must contain(secretContent)
  }

  def flowWithInvalidCredentials(port: Int): Response = {
    Await.result(WS.url(s"http://localhost:$port").withAuth(username, password + "x", AuthScheme.BASIC).get(), 2 seconds)
  }

  def flowWithValidCredentials(port: Int): Response = {
    Await.result(WS.url(s"http://localhost:$port").withAuth(username, password, AuthScheme.BASIC).get(), 2 seconds)
  }

  def flowWithoutCredentials(port: Int): Response = {
    Await.result(WS.url(s"http://localhost:$port").get(), 2 seconds)
  }

  def assertNoAccessGranted(response: Response) = {
    response.status === 401
    response.body must not contain (secretContent)
  }
}
