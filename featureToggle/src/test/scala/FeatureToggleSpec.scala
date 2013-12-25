import info.schleichardt.play2.featuretoggle.ImplementedFeature
import play.api.mvc.Handler
import play.api.test.{ FakeRequest, WithApplication, FakeApplication, PlaySpecification }
import play.core.j.{ JavaActionAnnotations, JavaAction }
import play.libs.F
import play.{ GlobalSettings => JGlobalSettings }
import play.mvc.Http.RequestHeader
import play.mvc.{ Result => JResult, Results => JResults }
import play.test.{ FakeApplication => JFakeApplication, WithServer => JWithServer, Helpers }
import java.util.{ Map => JMap, List => JList }
import scala.collection.JavaConversions._

class OrderCartFeature

object FeatureToggleSpec extends PlaySpecification {

  val handler: Handler = new JavaAction() {
    def parser = annotations.parser

    def invocation = F.Promise.pure(new TestController().index())

    val annotations = new JavaActionAnnotations(classOf[TestController], classOf[TestController].getMethod("index"))
  }

  val secretContent = "secret content"

  def FeatureToggleFakeApplication(featureEnabled: Boolean): FakeApplication = {
    val additionalPlugin: JList[String] = List("info.schleichardt.play2.featuretoggle.FeatureTogglePlugin")
    val additionalConfiguration: JMap[String, Any] = Map("application.features.OrderCartFeature" -> featureEnabled)
    val global = new JGlobalSettings() {
      override def onRouteRequest(request: RequestHeader): Handler = handler
    }
    Helpers.fakeApplication(additionalConfiguration, additionalPlugin, global).getWrappedApplication
  }

  class TestController extends play.mvc.Controller {
    @ImplementedFeature(Array(classOf[OrderCartFeature])) def index(): JResult = JResults.ok(secretContent)
  }

  "application must deny access for deactivated feature" in new WithApplication(FeatureToggleFakeApplication(false)) {
    val resultFuture = route(FakeRequest()).get
    status(resultFuture) === 404
    contentAsString(resultFuture) must not contain (secretContent)
  }

  "application must allow access for activated feature" in new WithApplication(FeatureToggleFakeApplication(true)) {
    val resultFuture = route(FakeRequest()).get
    status(resultFuture) === 200
    contentAsString(resultFuture) must contain(secretContent)
  }
}
